package com.verityai.plugin.integration.map;

import com.verityai.plugin.VerityAI;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * BlueMap integration. BlueMap loads its API asynchronously and can be
 * enabled/disabled independently of server startup (e.g. on a BlueMap
 * reload), so this registers persistent onEnable/onDisable listeners rather
 * than doing a one-time lookup at VerityAI startup — that's the officially
 * recommended pattern (see BlueMapAPI's own Javadoc).
 */
public class BlueMapProvider implements MapProvider {

    private final VerityAI plugin;
    private volatile BlueMapAPI api;

    // Kept so HookManager can unregister them cleanly on VerityAI#onDisable.
    private final Consumer<BlueMapAPI> onEnableListener = a -> {
        this.api = a;
        plugin.getLogger().info("VerityAI: BlueMap became available.");
    };
    private final Consumer<BlueMapAPI> onDisableListener = a -> this.api = null;

    public BlueMapProvider(VerityAI plugin) {
        this.plugin = plugin;
    }

    public void register() {
        BlueMapAPI.onEnable(onEnableListener);
        BlueMapAPI.onDisable(onDisableListener);
    }

    public void unregister() {
        try {
            BlueMapAPI.unregisterListener(onEnableListener);
            BlueMapAPI.unregisterListener(onDisableListener);
        } catch (Throwable ignored) {
            // best-effort cleanup only
        }
    }

    @Override
    public String getName() {
        return "BlueMap";
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    @Override
    public Optional<String> getMapUrl() {
        // BlueMap's own webserver address isn't necessarily the server's public-facing
        // URL (it's very commonly reverse-proxied, or the webserver is even disabled
        // in favor of external hosting) — so, same as every other map integration here,
        // the admin-configured integrations.map-web-url is the source of truth for the
        // public link. This just confirms BlueMap itself is actually the thing running.
        if (!isAvailable()) return Optional.empty();
        String configured = plugin.getConfigManager().getMapWebUrl();
        return (configured == null || configured.isBlank()) ? Optional.empty() : Optional.of(configured);
    }

    @Override
    public Optional<String> getPlayerMapUrl(Player player) {
        BlueMapAPI current = api;
        if (current == null || player == null) return Optional.empty();
        String base = getMapUrl().orElse(null);
        if (base == null) return Optional.empty();

        try {
            var world = current.getWorld(player.getWorld());
            if (world.isEmpty() || world.get().getMaps().isEmpty()) {
                return Optional.empty(); // this world isn't rendered by BlueMap
            }
            String mapId = world.get().getMaps().iterator().next().getId();
            var loc = player.getLocation();
            // BlueMap's web-client URL hash format: #<map-id>:<x>:<y>:<z>:<tilt>:<rotation>:<distance>:<ortho>:free
            // (best-effort — BlueMap doesn't publish this as stable public API, so treat
            // it as "usually works", not guaranteed across every BlueMap version).
            String url = String.format(Locale.US, "%s#%s:%d:%d:%d:0:0:100:0:free",
                    base.endsWith("/") ? base : base + "/", mapId,
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            return Optional.of(url);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isPlayerVisible(Player player) {
        BlueMapAPI current = api;
        if (current == null || player == null) return false;
        try {
            // BlueMap doesn't expose a stable public "is this player hidden" API — the
            // closest well-supported signal is whether their current world is even
            // rendered by BlueMap at all.
            return current.getWorld(player.getWorld()).isPresent();
        } catch (Throwable t) {
            return false;
        }
    }
}
