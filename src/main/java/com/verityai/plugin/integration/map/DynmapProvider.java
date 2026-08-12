package com.verityai.plugin.integration.map;

import com.verityai.plugin.VerityAI;
import org.bukkit.entity.Player;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;

import java.util.Locale;
import java.util.Optional;

/**
 * Dynmap integration. Like BlueMap, Dynmap's API is registered/unregistered
 * asynchronously via a listener rather than a one-time lookup, since Dynmap
 * can (re)load independently of VerityAI's own startup.
 */
public class DynmapProvider implements MapProvider {

    private final VerityAI plugin;
    private volatile DynmapCommonAPI api;
    private DynmapCommonAPIListener listener;

    public DynmapProvider(VerityAI plugin) {
        this.plugin = plugin;
    }

    public void register() {
        listener = new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
                api = dynmapCommonAPI;
                plugin.getLogger().info("VerityAI: Dynmap became available.");
            }

            @Override
            public void apiDisabled(DynmapCommonAPI dynmapCommonAPI) {
                api = null;
            }
        };
        DynmapCommonAPIListener.register(listener);
    }

    public void unregister() {
        // DynmapCommonAPIListener has no public unregister — it's cleaned up
        // by Dynmap itself when VerityAI's classloader is discarded on disable/reload.
        api = null;
    }

    @Override
    public String getName() {
        return "Dynmap";
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    @Override
    public Optional<String> getMapUrl() {
        if (!isAvailable()) return Optional.empty();
        String configured = plugin.getConfigManager().getMapWebUrl();
        return (configured == null || configured.isBlank()) ? Optional.empty() : Optional.of(configured);
    }

    @Override
    public Optional<String> getPlayerMapUrl(Player player) {
        if (!isAvailable() || player == null) return Optional.empty();
        String base = getMapUrl().orElse(null);
        if (base == null) return Optional.empty();

        try {
            var loc = player.getLocation();
            // Dynmap's standard web-client query-parameter format for jumping to a
            // location — documented/commonly used across Dynmap web UIs.
            String url = String.format(Locale.US, "%s%sworldname=%s&mapname=flat&zoom=3&x=%d&y=%d&z=%d",
                    base, base.contains("?") ? "&" : "?",
                    player.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            return Optional.of(url);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isPlayerVisible(Player player) {
        // Dynmap's per-player hide/show state isn't exposed through a stable public
        // API method in DynmapCommonAPI, so — same conservative default as
        // BlueMapProvider — assume visible whenever Dynmap itself is active.
        return isAvailable() && player != null;
    }
}
