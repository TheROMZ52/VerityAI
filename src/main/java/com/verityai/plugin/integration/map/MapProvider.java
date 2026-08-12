package com.verityai.plugin.integration.map;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * A live-map plugin VerityAI can pull location/visibility info from (BlueMap,
 * Dynmap, and — later — anything else). Implementations must:
 *   - never throw: wrap the underlying plugin's API calls in try/catch and
 *     return Optional.empty()/false on any failure, so a broken or
 *     mid-reload map plugin can never crash or hang VerityAI;
 *   - do NO background polling: every method here is called on-demand only
 *     (from a command or an AI function call), never on a timer, so this
 *     integration has zero idle resource cost;
 *   - be safe to call from any thread for read-only lookups, since
 *     MapIntegrationManager may be queried from an async AI request.
 *
 * To add a new map provider later: implement this interface and register it
 * in {@link MapIntegrationManager#hookAll()} — nothing else needs to change.
 */
public interface MapProvider {

    /** Short display name, e.g. "BlueMap" or "Dynmap". */
    String getName();

    /** True only once the underlying plugin is installed AND its API has actually finished loading. */
    boolean isAvailable();

    /** Best-effort public URL of the whole web map (not player-specific), if the provider exposes one. */
    Optional<String> getMapUrl();

    /** A deep link centered on this player's current position, if the provider supports building one. */
    Optional<String> getPlayerMapUrl(Player player);

    /** Whether this player is currently visible/tracked on the live map (some players can hide themselves). */
    boolean isPlayerVisible(Player player);
}
