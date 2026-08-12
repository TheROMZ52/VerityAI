package com.verityai.plugin.tasks;

import com.verityai.plugin.VerityAI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Simple daily reminders per player ("remind me at 8am to check the farm").
 * Persisted under plugins/VerityAI/tasks/<uuid>.yml, checked once a minute
 * on the main thread. A reminder only actually reaches the player if they're
 * online at that exact minute — there's no offline mail/queue here, just a
 * lightweight in-game nudge, which keeps this simple and dependency-free.
 *
 * Timezones: "8:00" only means the same thing to every player if the server
 * and every player share a timezone, which usually isn't true (e.g. a server
 * hosted in Germany with a player in Iran — 3.5 hours apart). Minecraft/Bukkit
 * has no reliable way to detect a player's real-world timezone automatically,
 * so each player can optionally set their own UTC offset with
 * /verity task timezone <+HH:mm>; their reminder times are then interpreted
 * against THAT offset instead of the server JVM's local clock. Players who
 * never set one keep the old behavior (server's local time) — fully backward
 * compatible.
 */
public class TaskService {

    /** One reminder: fires once per day at hour:minute, tracked so it won't repeat within the same day. */
    public record Reminder(String id, int hour, int minute, String message, String lastTriggeredDate) {}

    private final VerityAI plugin;
    private final File folder;
    private final Map<UUID, List<Reminder>> cache = new ConcurrentHashMap<>();
    /** Per-player UTC offset override; a player with no entry uses the server's local time zone. */
    private final Map<UUID, ZoneOffset> timezoneCache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    public TaskService(VerityAI plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "tasks");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("VerityAI: could not create tasks folder.");
        }
    }

    private File fileFor(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

    private int maxPerPlayer() {
        return Math.max(1, plugin.getConfigManager().getMaxTasksPerPlayer());
    }

    public List<Reminder> list(UUID uuid) {
        return List.copyOf(loadIfAbsent(uuid));
    }

    private List<Reminder> loadIfAbsent(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            File file = fileFor(id);
            List<Reminder> result = new ArrayList<>();
            if (!file.exists()) return result;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (Map<?, ?> row : yaml.getMapList("reminders")) {
                try {
                    result.add(new Reminder(
                            String.valueOf(row.get("id")),
                            Integer.parseInt(row.get("hour").toString()),
                            Integer.parseInt(row.get("minute").toString()),
                            String.valueOf(row.get("message")),
                            row.get("lastTriggeredDate") != null ? row.get("lastTriggeredDate").toString() : ""));
                } catch (Exception ignored) {
                    // skip a malformed row rather than failing the whole load
                }
            }
            String tz = yaml.getString("timezone-offset", null);
            if (tz != null && !tz.isBlank()) {
                try {
                    timezoneCache.put(id, ZoneOffset.of(tz));
                } catch (DateTimeParseException ignored) {
                    // corrupted/hand-edited value — fall back to server default rather than fail the whole load
                }
            }
            return result;
        });
    }

    /** Returns the new reminder's id, or null if the player is already at their cap. */
    public String add(UUID uuid, int hour, int minute, String message) {
        List<Reminder> reminders = loadIfAbsent(uuid);
        if (reminders.size() >= maxPerPlayer()) {
            return null;
        }
        // A UUID-derived id (not a per-JVM counter) so ids never collide with
        // ones already saved to disk from before the last restart.
        String id = "r" + UUID.randomUUID().toString().substring(0, 6);
        reminders.add(new Reminder(id, hour, minute, message, ""));
        dirty.add(uuid);
        return id;
    }

    public boolean remove(UUID uuid, String id) {
        List<Reminder> reminders = loadIfAbsent(uuid);
        boolean removed = reminders.removeIf(r -> r.id().equals(id));
        if (removed) dirty.add(uuid);
        return removed;
    }

    /**
     * Sets this player's personal UTC offset (e.g. "+03:30" for Iran Standard
     * Time), used to interpret their reminder times instead of the server's
     * local clock. Accepts "+HH:mm", "-HH:mm", or a bare "+H"/"-H". Returns
     * false if the text couldn't be parsed as a valid offset (config/state
     * unchanged in that case).
     */
    public boolean setTimezone(UUID uuid, String offsetText) {
        ZoneOffset offset;
        try {
            offset = parseOffset(offsetText);
        } catch (Exception e) {
            return false;
        }
        timezoneCache.put(uuid, offset);
        loadIfAbsent(uuid); // ensure the reminders list is loaded before we mark this uuid dirty
        dirty.add(uuid);
        return true;
    }

    /** Parses "+3", "-5", "+03:30", "-5:00", or "Z"/"0" into a ZoneOffset. */
    private ZoneOffset parseOffset(String text) {
        String t = text.trim();
        if (t.equalsIgnoreCase("Z") || t.equals("0") || t.equals("+0") || t.equals("-0")) {
            return ZoneOffset.UTC;
        }
        var matcher = java.util.regex.Pattern.compile("^([+-])(\\d{1,2})(?::?(\\d{2}))?$").matcher(t);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a valid UTC offset: " + text);
        }
        int hours = Integer.parseInt(matcher.group(2));
        int minutes = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        String normalized = matcher.group(1) + String.format("%02d:%02d", hours, minutes);
        return ZoneOffset.of(normalized);
    }

    /** Clears a player's personal timezone override, reverting them to the server's local time. */
    public void clearTimezone(UUID uuid) {
        if (timezoneCache.remove(uuid) != null) {
            loadIfAbsent(uuid);
            dirty.add(uuid);
        }
    }

    /** A human-readable description of what clock this player's reminders currently use. */
    public String describeTimezone(UUID uuid) {
        ZoneOffset offset = timezoneCache.get(uuid);
        return offset == null ? "server's local time (no personal timezone set)" : "UTC" + offset;
    }

    /** Checked once a minute (main thread): fires any due reminder for currently online players. */
    public void checkDue() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            List<Reminder> reminders = loadIfAbsent(uuid); // cheap no-op once cached
            if (reminders.isEmpty()) continue;

            ZoneOffset offset = timezoneCache.get(uuid);
            LocalTime now;
            String today;
            if (offset != null) {
                OffsetDateTime nowForPlayer = OffsetDateTime.now(offset);
                now = nowForPlayer.toLocalTime();
                today = nowForPlayer.toLocalDate().toString();
            } else {
                now = LocalTime.now();
                today = LocalDate.now().toString();
            }

            for (int i = 0; i < reminders.size(); i++) {
                Reminder r = reminders.get(i);
                if (r.hour() == now.getHour() && r.minute() == now.getMinute() && !today.equals(r.lastTriggeredDate())) {
                    player.sendMessage(Component.text("⏰ Reminder: " + r.message(), NamedTextColor.YELLOW));
                    reminders.set(i, new Reminder(r.id(), r.hour(), r.minute(), r.message(), today));
                    dirty.add(uuid);
                }
            }
        }
    }

    public void flushDirty() {
        if (dirty.isEmpty()) return;
        for (UUID uuid : Set.copyOf(dirty)) {
            List<Reminder> reminders = cache.get(uuid);
            if (reminders != null) {
                save(uuid, reminders, timezoneCache.get(uuid));
            }
            dirty.remove(uuid);
        }
    }

    private void save(UUID uuid, List<Reminder> reminders, ZoneOffset offset) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Reminder r : reminders) {
            rows.add(Map.of("id", r.id(), "hour", r.hour(), "minute", r.minute(),
                    "message", r.message(), "lastTriggeredDate", r.lastTriggeredDate()));
        }
        yaml.set("reminders", rows);
        if (offset != null) {
            yaml.set("timezone-offset", offset.getId());
        }
        try {
            yaml.save(fileFor(uuid));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "VerityAI: failed to save reminders for " + uuid, e);
        }
    }
}
