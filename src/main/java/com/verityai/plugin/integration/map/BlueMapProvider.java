package com.verityai.plugin.integration.map;

import com.verityai.plugin.VerityAI;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * BlueMap integration.
 *
 * BlueMap loads its API asynchronously and can be enabled/disabled
 * independently of server startup.
 */
public class BlueMapProvider implements MapProvider {

    // Plugin instance used by this provider.
    private final VerityAI plugin;

    // Current BlueMap API instance.
    private volatile BlueMapAPI api;

    // Listener references are kept so HookManager can unregister them cleanly.
    private final Consumer<BlueMapAPI> onEnableListener;
    private final Consumer<BlueMapAPI> onDisableListener;

    public BlueMapProvider(VerityAI plugin) {
        // Store the plugin instance first.
        this.plugin = plugin;

        // Capture the constructor parameter directly.
        this.onEnableListener = api -> {
            this.api = api;
            plugin.getLogger().info("VerityAI: BlueMap became available.");
        };

        // Create the disable listener.
        this.onDisableListener = api -> this.api = null;
    }

    public void register() {
        // Register BlueMap lifecycle listeners.
        BlueMapAPI.onEnable(onEnableListener);
        BlueMapAPI.onDisable(onDisableListener);
    }

    public void unregister() {
        try {
            // Unregister both listeners during plugin shutdown.
            BlueMapAPI.unregisterListener(onEnableListener);
            BlueMapAPI.unregisterListener(onDisableListener);
        } catch (Throwable ignored) {
            // Best-effort cleanup only.
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
        // The configured public URL is the source of truth.
        if (!isAvailable()) {
            return Optional.empty();
        }

        String configured = plugin.getConfigManager().getMapWebUrl();

        return (configured == null || configured.isBlank())
                ? Optional.empty()
                : Optional.of(configured);
    }

    @Override
    public Optional<String> getPlayerMapUrl(Player player) {
        // Get the current API instance.
        BlueMapAPI current = api;

        if (current == null || player == null) {
            return Optional.empty();
        }

        // Get the configured public map URL.
        String base = getMapUrl().orElse(null);

        if (base == null) {
            return Optional.empty();
        }

        try {
            // Find the player's world in BlueMap.
            var world = current.getWorld(player.getWorld());

            if (world.isEmpty() || world.get().getMaps().isEmpty()) {
                return Optional.empty();
            }

            // Use the first available map for this world.
            String mapId = world.get().getMaps().iterator().next().getId();

            var loc = player.getLocation();

            // Build a best-effort BlueMap client URL.
            String url = String.format(
                    Locale.US,
                    "%s#%s:%d:%d:%d:0:0:100:0:free",
                    base.endsWith("/") ? base : base + "/",
                    mapId,
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ()
            );

            return Optional.of(url);

        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isPlayerVisible(Player player) {
        // Get the current API instance.
        BlueMapAPI current = api;

        if (current == null || player == null) {
            return false;
        }

        try {
            // A player is considered visible when their world is rendered by BlueMap.
            return current.getWorld(player.getWorld()).isPresent();

        } catch (Throwable t) {
            return false;
        }
    }
}