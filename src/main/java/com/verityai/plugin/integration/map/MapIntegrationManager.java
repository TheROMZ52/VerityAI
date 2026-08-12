package com.verityai.plugin.integration.map;

import com.verityai.plugin.VerityAI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Detects and owns every {@link MapProvider}. To add a new map plugin later:
 * write a class implementing MapProvider, then add one line to hookAll()
 * below — nothing else in VerityAI needs to change, since everything else
 * talks to "the active map provider" through this class only.
 *
 * Never polls: providers only do work when a caller actually asks (a
 * command, or an AI function call), so an idle server pays zero cost for
 * this integration existing.
 */
public class MapIntegrationManager {

    private final VerityAI plugin;
    private final List<MapProvider> providers = new ArrayList<>();
    private BlueMapProvider blueMapProvider;
    private DynmapProvider dynmapProvider;

    public MapIntegrationManager(VerityAI plugin) {
        this.plugin = plugin;
    }

    public void hookAll() {
        var cfg = plugin.getConfigManager();

        if (cfg.isBlueMapIntegrationEnabled()) {
            try {
                blueMapProvider = new BlueMapProvider(plugin);
                blueMapProvider.register();
                providers.add(blueMapProvider);
            } catch (Throwable t) {
                // Most likely cause: BlueMap isn't installed, so its API classes aren't
                // on the classpath at all. Never let that crash VerityAI — just log it
                // and move on without this provider.
                plugin.getLogger().warning("VerityAI: BlueMap integration unavailable: " + t.getMessage());
            }
        }

        if (cfg.isDynmapIntegrationEnabled()) {
            try {
                dynmapProvider = new DynmapProvider(plugin);
                dynmapProvider.register();
                providers.add(dynmapProvider);
            } catch (Throwable t) {
                plugin.getLogger().warning("VerityAI: Dynmap integration unavailable: " + t.getMessage());
            }
        }

        // Add future providers here, e.g.:
        //   if (cfg.isSquaremapIntegrationEnabled()) { ... providers.add(new SquaremapProvider(plugin)); }
    }

    public void unhookAll() {
        if (blueMapProvider != null) blueMapProvider.unregister();
        if (dynmapProvider != null) dynmapProvider.unregister();
    }

    /**
     * The provider VerityAI should currently use, or empty if none are
     * installed/enabled/loaded. If more than one is active, config's
     * map.preferred-provider picks which one wins ("auto" = first
     * registered, i.e. BlueMap before Dynmap).
     */
    public Optional<MapProvider> getActiveProvider() {
        String preferred = plugin.getConfigManager().getPreferredMapProvider();

        if (!"auto".equalsIgnoreCase(preferred)) {
            for (MapProvider provider : providers) {
                if (provider.getName().equalsIgnoreCase(preferred) && provider.isAvailable()) {
                    return Optional.of(provider);
                }
            }
            // Fall through to "auto" behavior if the explicitly preferred one isn't
            // actually available right now, rather than reporting no map at all.
        }

        for (MapProvider provider : providers) {
            if (provider.isAvailable()) {
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }

    public boolean isAnyMapAvailable() {
        return getActiveProvider().isPresent();
    }
}
