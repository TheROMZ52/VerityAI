package com.verityai.plugin.command;

import com.verityai.plugin.VerityAI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class VerityCommand implements CommandExecutor, TabCompleter {

    /** Keep this in sync with the subcommands handled in {@link #onCommand}. */
    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "clear", "info", "debug", "toggle", "chat", "personality",
            "owner", "op", "map", "stats", "model", "task", "quest", "tutorial", "feedback"
    );

    private static final List<String> TASK_SUBCOMMANDS = List.of("add", "remove", "list", "timezone");
    private static final List<String> OP_SUBCOMMANDS = List.of("add", "remove", "list");

    private final VerityAI plugin;

    public VerityCommand(VerityAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "clear" -> handleClear(sender, args);
            case "info" -> handleInfo(sender);
            case "debug" -> handleDebug(sender);
            case "toggle", "on", "off" -> handleToggle(sender, args[0]);
            case "chat" -> handleChatMode(sender);
            case "personality" -> handlePersonality(sender, args);
            case "owner" -> handleOwner(sender, args);
            case "op" -> handleOp(sender, args);
            case "map" -> handleMap(sender);
            case "stats" -> handleStats(sender);
            case "model" -> handleModel(sender, args);
            case "task" -> handleTask(sender, args);
            case "quest" -> handleQuest(sender, args);
            case "tutorial" -> handleTutorial(sender, args);
            case "feedback" -> handleFeedback(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String label, String[] args) {
        // Bukkit falls back to suggesting online player names for any command that
        // doesn't register its own TabCompleter — which is why "/verity <tab>" was
        // showing player names instead of the subcommand list. Registering this
        // (see VerityAI#onEnable) fixes that; the logic below provides real,
        // context-aware suggestions instead.
        if (args.length == 1) {
            return filterStartingWith(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "clear", "owner" -> filterStartingWith(onlinePlayerNames(), args[1]);
                case "op" -> filterStartingWith(OP_SUBCOMMANDS, args[1]);
                case "task" -> filterStartingWith(TASK_SUBCOMMANDS, args[1]);
                case "quest" -> filterStartingWith(List.of("interval"), args[1]);
                case "toggle" -> filterStartingWith(List.of("on", "off"), args[1]);
                case "feedback" -> filterStartingWith(List.of("good", "bad"), args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("op")) {
            if (args[1].equalsIgnoreCase("add")) {
                return filterStartingWith(onlinePlayerNames(), args[2]);
            }
            if (args[1].equalsIgnoreCase("remove")) {
                return filterStartingWith(plugin.getConfigManager().getOpNames(), args[2]);
            }
        }

        return List.of();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> filterStartingWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("verity.reload")) {
            deny(sender);
            return true;
        }
        plugin.getConfigManager().load();
        sender.sendMessage(Component.text("VerityAI configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        UUID target;
        String targetName;

        if (args.length >= 2) {
            if (!sender.hasPermission("verity.clear.others")) {
                deny(sender);
                return true;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            target = offline.getUniqueId();
            targetName = args[1];
        } else if (sender instanceof Player player) {
            target = player.getUniqueId();
            targetName = "your";
        } else {
            sender.sendMessage(Component.text("Console must specify a player: /verity clear <player>", NamedTextColor.RED));
            return true;
        }

        plugin.getConversationManager().clear(target);
        if (plugin.getConfigManager().isLongTermEnabled()) {
            plugin.getLongTermMemoryStore().clear(target);
        }
        sender.sendMessage(Component.text("Cleared " + targetName + " conversation memory.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("verity.info")) {
            deny(sender);
            return true;
        }
        var cfg = plugin.getConfigManager();
        var hooks = plugin.getHookManager();

        sender.sendMessage(Component.text("--- VerityAI ---", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Status: " + (cfg.isAiEnabled() ? "enabled" : "disabled"), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Model: " + cfg.getModel(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Personality: " + cfg.getActivePersonality(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Streaming: " + cfg.isStreamEnabled(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Long-term memory: " + cfg.isLongTermEnabled()
                + " (auto-remember: " + cfg.isAutoRememberEnabled() + ")", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Owner: " + (cfg.getOwnerName().isBlank() ? "not set" : cfg.getOwnerName()),
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Command execution: " + cfg.isCommandsEnabled(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Function calling: " + cfg.isFunctionCallingEnabled()
                + " | Embeddings: " + cfg.isEmbeddingsEnabled(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(String.format("Hooks: PlaceholderAPI=%s Vault=%s LuckPerms=%s EssentialsX=%s",
                hooks.isPlaceholderApiHooked(), hooks.isVaultHooked(), hooks.isLuckPermsHooked(), hooks.isEssentialsHooked()),
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Live map: " + hooks.getActiveMapProviderName().orElse("none"), NamedTextColor.GRAY));
        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        if (!sender.hasPermission("verity.debug")) {
            deny(sender);
            return true;
        }
        boolean newValue = !plugin.getConfigManager().isDebugEnabled();
        plugin.getConfigManager().setDebugEnabled(newValue);
        sender.sendMessage(Component.text("Debug mode " + (newValue ? "enabled" : "disabled") + ".", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleToggle(CommandSender sender, String arg) {
        if (!sender.hasPermission("verity.toggle")) {
            deny(sender);
            return true;
        }
        boolean newValue = switch (arg.toLowerCase()) {
            case "on" -> true;
            case "off" -> false;
            default -> !plugin.getConfigManager().isAiEnabled();
        };
        plugin.getConfigManager().setAiEnabled(newValue);
        sender.sendMessage(Component.text("Verity is now " + (newValue ? "enabled" : "disabled") + ".",
                newValue ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    private boolean handleChatMode(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use conversation mode.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("verity.use")) {
            deny(sender);
            return true;
        }
        var cm = plugin.getConversationManager();
        if (cm.isInActiveConversation(player.getUniqueId())) {
            cm.endConversation(player.getUniqueId());
            player.sendMessage(Component.text("Conversation mode ended.", NamedTextColor.YELLOW));
        } else {
            cm.refreshConversationWindow(player.getUniqueId());
            player.sendMessage(Component.text("Conversation mode started — talk to Verity without the trigger. Say \"bye\" to stop.",
                    NamedTextColor.AQUA));
        }
        return true;
    }

    private boolean handlePersonality(CommandSender sender, String[] args) {
        if (!sender.hasPermission("verity.personality")) {
            deny(sender);
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Available: " + String.join(", ", plugin.getConfigManager().getPersonalityNames()),
                    NamedTextColor.GRAY));
            return true;
        }
        boolean ok = plugin.getConfigManager().setActivePersonality(args[1]);
        if (ok) {
            sender.sendMessage(Component.text("Verity's personality set to: " + args[1], NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Unknown personality preset: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleOwner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("verity.owner")) {
            deny(sender);
            return true;
        }
        var cfg = plugin.getConfigManager();
        if (args.length < 2) {
            String current = cfg.getOwnerName();
            sender.sendMessage(Component.text(
                    current.isBlank() ? "No server owner is set." : "Current server owner: " + current,
                    NamedTextColor.GRAY));
            return true;
        }
        String name = args[1];
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            // Require the target to be online so we can record their real UUID —
            // matching by name alone would be spoofable, especially on an
            // offline-mode server where usernames aren't authenticated at all.
            sender.sendMessage(Component.text("That player needs to be online right now to be set as owner "
                    + "(so Verity can record their real account, not just a name).", NamedTextColor.RED));
            return true;
        }
        cfg.setOwner(target);
        sender.sendMessage(Component.text("Server owner set to: " + target.getName(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleOp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("verity.op.manage")) {
            deny(sender);
            return true;
        }
        var cfg = plugin.getConfigManager();
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";

        switch (sub) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /verity op add <player>", NamedTextColor.YELLOW));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("That player needs to be online right now to add them as an op.",
                            NamedTextColor.RED));
                    return true;
                }
                boolean added = cfg.addOp(target);
                sender.sendMessage(added
                        ? Component.text(target.getName() + " added as a Verity op (gets owner-only-console-whitelist access).", NamedTextColor.GREEN)
                        : Component.text(target.getName() + " is already a Verity op.", NamedTextColor.GRAY));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /verity op remove <player>", NamedTextColor.YELLOW));
                    return true;
                }
                boolean removed = cfg.removeOp(args[2]);
                sender.sendMessage(removed
                        ? Component.text(args[2] + " removed from Verity ops.", NamedTextColor.GREEN)
                        : Component.text("No op with that name.", NamedTextColor.RED));
            }
            default -> {
                var names = cfg.getOpNames();
                sender.sendMessage(names.isEmpty()
                        ? Component.text("No Verity ops set (besides the owner). Add one: /verity op add <player>", NamedTextColor.GRAY)
                        : Component.text("Verity ops: " + String.join(", ", names), NamedTextColor.AQUA));
            }
        }
        return true;
    }

    private boolean handleMap(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can view the map.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("verity.map")) {
            deny(sender);
            return true;
        }
        plugin.getWorldQueryService().buildMiniMap(player).thenAccept(map -> {
            player.sendMessage(Component.text("--- Nearby map (you are P) ---", NamedTextColor.AQUA));
            player.sendMessage(map);

            var hooks = plugin.getHookManager();
            var providerName = hooks.getActiveMapProviderName();
            if (providerName.isPresent()) {
                String visibility = hooks.isPlayerVisibleOnMap(player) ? "you're visible on it" : "you may not be visible on it";
                player.sendMessage(Component.text(providerName.get() + " is live (" + visibility + ").", NamedTextColor.DARK_AQUA));
                hooks.getPlayerMapUrl(player).ifPresentOrElse(
                        url -> player.sendMessage(Component.text("Jump to your spot: " + url, NamedTextColor.BLUE)),
                        () -> hooks.getMapUrl().ifPresent(url ->
                                player.sendMessage(Component.text("Full web map: " + url, NamedTextColor.BLUE))));
            } else {
                String mapUrl = plugin.getConfigManager().getMapWebUrl();
                if (mapUrl != null && !mapUrl.isBlank()) {
                    player.sendMessage(Component.text("Full web map: " + mapUrl, NamedTextColor.BLUE));
                }
            }
        });
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("verity.stats")) {
            deny(sender);
            return true;
        }
        var stats = plugin.getStatsService();
        sender.sendMessage(Component.text("--- VerityAI stats (since last restart) ---", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Uptime: " + stats.getUptimeMinutes() + " min", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Requests: " + stats.getTotalRequests()
                + " (ok=" + stats.getSuccessfulRequests() + ", failed=" + stats.getFailedRequests() + ")", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(String.format("Avg response time: %.0fms", stats.getAverageResponseMs()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Tokens used: " + stats.getTotalTokensUsed(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("AI-run commands: " + stats.getCommandsExecuted(), NamedTextColor.GRAY));

        var topPlayers = stats.topPlayers(5);
        if (!topPlayers.isEmpty()) {
            StringBuilder sb = new StringBuilder("Top askers: ");
            for (var entry : topPlayers) {
                sb.append(entry.getKey()).append(" (").append(entry.getValue()).append("), ");
            }
            sender.sendMessage(Component.text(sb.substring(0, sb.length() - 2), NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleModel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("verity.model")) {
            deny(sender);
            return true;
        }
        var cfg = plugin.getConfigManager();
        if (args.length < 2) {
            sender.sendMessage(Component.text("Current model: " + cfg.getModel(), NamedTextColor.GRAY));
            if (!cfg.getFallbackModels().isEmpty()) {
                sender.sendMessage(Component.text("Fallback models: " + String.join(", ", cfg.getFallbackModels()), NamedTextColor.GRAY));
            }
            return true;
        }
        cfg.setModel(args[1]);
        sender.sendMessage(Component.text("Primary AI model set to: " + args[1], NamedTextColor.GREEN));
        return true;
    }

    private boolean handleTask(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can manage their own reminders.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("verity.task")) {
            deny(sender);
            return true;
        }
        var tasks = plugin.getTaskService();
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";

        switch (sub) {
            case "add" -> {
                if (args.length < 4) {
                    player.sendMessage(Component.text("Usage: /verity task add <HH:mm> <message>", NamedTextColor.YELLOW));
                    return true;
                }
                String[] time = args[2].split(":");
                int hour, minute;
                try {
                    hour = Integer.parseInt(time[0]);
                    minute = Integer.parseInt(time[1]);
                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) throw new NumberFormatException();
                } catch (Exception e) {
                    player.sendMessage(Component.text("Invalid time — use 24-hour HH:mm, e.g. 08:00", NamedTextColor.RED));
                    return true;
                }
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                String id = tasks.add(player.getUniqueId(), hour, minute, message);
                if (id == null) {
                    player.sendMessage(Component.text("You've hit your reminder limit — remove one first with /verity task remove <id>.", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("Reminder set for " + args[2] + " daily (id: " + id + ").", NamedTextColor.GREEN));
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /verity task remove <id>", NamedTextColor.YELLOW));
                    return true;
                }
                boolean removed = tasks.remove(player.getUniqueId(), args[2]);
                player.sendMessage(removed
                        ? Component.text("Reminder removed.", NamedTextColor.GREEN)
                        : Component.text("No reminder with that id.", NamedTextColor.RED));
            }
            case "timezone" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Your reminders currently use: "
                            + tasks.describeTimezone(player.getUniqueId()), NamedTextColor.AQUA));
                    player.sendMessage(Component.text("Usage: /verity task timezone <+HH:mm|reset>  — e.g. "
                            + "+03:30 for Iran, +02:00 for Germany. This fixes reminder times when you and the "
                            + "server aren't in the same timezone.", NamedTextColor.GRAY));
                    return true;
                }
                if (args[2].equalsIgnoreCase("reset") || args[2].equalsIgnoreCase("clear")) {
                    tasks.clearTimezone(player.getUniqueId());
                    player.sendMessage(Component.text("Reminder timezone reset to the server's local time.", NamedTextColor.GREEN));
                    return true;
                }
                boolean ok = tasks.setTimezone(player.getUniqueId(), args[2]);
                if (ok) {
                    player.sendMessage(Component.text("Your reminders now use " + tasks.describeTimezone(player.getUniqueId())
                            + ". Existing reminder times are unchanged — re-add any that need adjusting.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Couldn't parse that as a UTC offset — try something like +03:30 or -05:00.",
                            NamedTextColor.RED));
                }
            }
            default -> {
                var list = tasks.list(player.getUniqueId());
                player.sendMessage(Component.text("Reminders use: " + tasks.describeTimezone(player.getUniqueId()), NamedTextColor.DARK_GRAY));
                if (list.isEmpty()) {
                    player.sendMessage(Component.text("You have no reminders. Add one: /verity task add <HH:mm> <message>", NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("--- Your reminders ---", NamedTextColor.AQUA));
                    for (var r : list) {
                        player.sendMessage(Component.text(String.format("[%s] %02d:%02d — %s", r.id(), r.hour(), r.minute(), r.message()), NamedTextColor.GRAY));
                    }
                }
            }
        }
        return true;
    }

    private boolean handleQuest(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("interval")) {
            return handleQuestInterval(sender, args);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can request a quest.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("verity.quest")) {
            deny(sender);
            return true;
        }
        player.sendMessage(Component.text("Thinking of a quest for you...", NamedTextColor.GRAY));
        plugin.getQuestService().generate(player);
        return true;
    }

    private boolean handleQuestInterval(CommandSender sender, String[] args) {
        if (!sender.hasPermission("verity.quest.interval")) {
            deny(sender);
            return true;
        }
        var cfg = plugin.getConfigManager();
        if (args.length < 3) {
            int current = cfg.getQuestAutoIntervalMinutes();
            sender.sendMessage(Component.text(current <= 0
                    ? "Automatic quests are currently disabled."
                    : "Verity currently sends everyone online a new quest every " + current + " minute(s).",
                    NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Usage: /verity quest interval <minutes> (0 disables it)", NamedTextColor.GRAY));
            return true;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("That's not a valid number of minutes.", NamedTextColor.RED));
            return true;
        }
        if (minutes < 0) {
            sender.sendMessage(Component.text("Minutes can't be negative.", NamedTextColor.RED));
            return true;
        }
        cfg.setQuestAutoIntervalMinutes(minutes);
        plugin.getQuestService().restartAutoQuests();
        sender.sendMessage(Component.text(minutes == 0
                ? "Automatic quests disabled."
                : "Verity will now send everyone online a new quest every " + minutes + " minute(s).",
                NamedTextColor.GREEN));
        return true;
    }

    private boolean handleTutorial(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can request a tutorial.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("verity.use")) {
            deny(sender);
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /verity tutorial <topic>", NamedTextColor.YELLOW));
            return true;
        }
        String topic = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        plugin.getAIHandler().ask(player, "Please give me a clear, step-by-step tutorial on: " + topic);
        return true;
    }

    private boolean handleFeedback(CommandSender sender, String[] args) {
        if (!sender.hasPermission("verity.feedback")) {
            deny(sender);
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /verity feedback <good|bad> [correction if bad]", NamedTextColor.YELLOW));
            return true;
        }
        String kind = args[1].toLowerCase(Locale.ROOT);
        if (kind.equals("good")) {
            sender.sendMessage(Component.text("Thanks for the feedback!", NamedTextColor.GREEN));
            return true;
        }
        if (kind.equals("bad")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Add a correction so Verity can avoid repeating the mistake: "
                        + "/verity feedback bad <correction>", NamedTextColor.YELLOW));
                return true;
            }
            String correction = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            plugin.getLessonsService().addLesson(correction);
            sender.sendMessage(Component.text("Noted — Verity will keep that in mind going forward.", NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /verity feedback <good|bad> [correction if bad]", NamedTextColor.YELLOW));
        return true;
    }

    private void deny(CommandSender sender) {
        sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Usage: /verity <reload|clear [player]|info|debug|toggle|chat|personality [name]|owner [player]|op <add|remove|list> [player]|"
                        + "map|stats|model [name]|task <add|remove|list|timezone> [args]|quest|tutorial <topic>|feedback <good|bad> [correction]>",
                NamedTextColor.YELLOW));
    }
}
