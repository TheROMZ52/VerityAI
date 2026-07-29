package com.verityai.plugin.integration;

import com.verityai.plugin.VerityAI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Exposes VerityAI's state to other plugins (scoreboards, tab lists, holograms,
 * etc.) via PlaceholderAPI. Registered from {@link HookManager} only when
 * PlaceholderAPI is actually present.
 *
 * Available placeholders:
 *   %verityai_enabled%          - "true"/"false", whether Verity is currently active
 *   %verityai_model%            - the active AI model id
 *   %verityai_personality%      - the active personality preset name
 *   %verityai_owner%            - the configured server owner (blank if unset)
 *   %verityai_requests_total%   - total AI requests since last restart
 *   %verityai_requests_success% - successful AI requests since last restart
 *   %verityai_requests_failed%  - failed AI requests since last restart
 *   %verityai_avg_response_ms%  - average response time in milliseconds
 *   %verityai_tokens_used%      - total tokens used since last restart
 *   %verityai_balance%          - the requesting player's Vault balance (if Vault is hooked)
 */
public class VerityPlaceholderExpansion extends PlaceholderExpansion {

    private final VerityAI plugin;

    public VerityPlaceholderExpansion(VerityAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "verityai";
    }

    @Override
    public @NotNull String getAuthor() {
        return "VerityAI";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** Keep this expansion registered across /papi reload and PlaceholderAPI restarts. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(org.bukkit.entity.Player player, @NotNull String params) {
        var cfg = plugin.getConfigManager();
        var stats = plugin.getStatsService();

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "enabled" -> String.valueOf(cfg.isAiEnabled());
            case "model" -> cfg.getModel();
            case "personality" -> cfg.getActivePersonality();
            case "owner" -> cfg.getOwnerName();
            case "requests_total" -> String.valueOf(stats.getTotalRequests());
            case "requests_success" -> String.valueOf(stats.getSuccessfulRequests());
            case "requests_failed" -> String.valueOf(stats.getFailedRequests());
            case "avg_response_ms" -> String.format(Locale.ROOT, "%.0f", stats.getAverageResponseMs());
            case "tokens_used" -> String.valueOf(stats.getTotalTokensUsed());
            case "balance" -> resolveBalance(player);
            default -> null; // unknown placeholder — let PlaceholderAPI show it as invalid, don't guess
        };
    }

    private String resolveBalance(OfflinePlayer offlinePlayer) {
        if (!(offlinePlayer instanceof org.bukkit.entity.Player player)) return "0";
        return plugin.getHookManager().getBalance(player)
                .map(balance -> String.format(Locale.ROOT, "%.2f", balance))
                .orElse("0");
    }
}
