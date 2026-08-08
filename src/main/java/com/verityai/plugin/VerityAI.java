package com.verityai.plugin;

import com.verityai.api.VerityAIApi;
import com.verityai.api.VerityAIApiImpl;
import com.verityai.plugin.ai.AIHandler;
import com.verityai.plugin.ai.EmbeddingService;
import com.verityai.plugin.command.AiCommandExecutor;
import com.verityai.plugin.command.VerityCommand;
import com.verityai.plugin.config.ConfigManager;
import com.verityai.plugin.context.ContextBuilder;
import com.verityai.plugin.context.WorldQueryService;
import com.verityai.plugin.integration.HookManager;
import com.verityai.plugin.lessons.LessonsService;
import com.verityai.plugin.listener.ChatActivityLogger;
import com.verityai.plugin.listener.ChatListener;
import com.verityai.plugin.listener.GameEventListener;
import com.verityai.plugin.listener.PlayerQuitListener;
import com.verityai.plugin.memory.ConversationManager;
import com.verityai.plugin.memory.HistoryLogger;
import com.verityai.plugin.memory.LongTermMemoryStore;
import com.verityai.plugin.quests.QuestService;
import com.verityai.plugin.stats.StatsService;
import com.verityai.plugin.tasks.TaskService;
import com.verityai.plugin.util.DebugLogger;
import com.verityai.plugin.util.RateLimiter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VerityAI - an in-game AI assistant for Paper servers, backed by OpenRouter.
 * Talk to Verity in chat with "@verity <message>" or start a hands-free
 * conversation with /verity chat.
 *
 * Other plugins can integrate via Bukkit's ServicesManager
 * ({@code com.verityai.api.VerityAIApi}) and by listening for
 * {@code com.verityai.api.events.*} — see that package's Javadoc.
 */
public class VerityAI extends JavaPlugin {

    // https://bstats.org/plugin/bukkit/VerityAI/33005
    private static final int BSTATS_PLUGIN_ID = 33005;

    private ConfigManager configManager;
    private DebugLogger debugLogger;
    private RateLimiter rateLimiter;
    private ConversationManager conversationManager;
    private LongTermMemoryStore longTermMemoryStore;
    private HistoryLogger historyLogger;
    private HookManager hookManager;
    private WorldQueryService worldQueryService;
    private ContextBuilder contextBuilder;
    private AIHandler aiHandler;
    private AiCommandExecutor aiCommandExecutor;
    private EmbeddingService embeddingService;
    private StatsService statsService;
    private TaskService taskService;
    private QuestService questService;
    private LessonsService lessonsService;
    private ChatActivityLogger chatActivityLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.debugLogger = new DebugLogger(this);
        this.statsService = new StatsService();
        this.rateLimiter = new RateLimiter(this);
        this.conversationManager = new ConversationManager(this);
        this.embeddingService = new EmbeddingService(this);
        this.longTermMemoryStore = new LongTermMemoryStore(this);
        this.historyLogger = new HistoryLogger(this);
        this.hookManager = new HookManager(this);
        this.worldQueryService = new WorldQueryService(this);
        this.contextBuilder = new ContextBuilder(this, worldQueryService);
        this.aiCommandExecutor = new AiCommandExecutor(this);
        this.aiHandler = new AIHandler(this);
        this.taskService = new TaskService(this);
        this.questService = new QuestService(this);
        this.questService.restartAutoQuests();
        this.lessonsService = new LessonsService(this);
        this.chatActivityLogger = new ChatActivityLogger(this);

        hookManager.hookAll();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new GameEventListener(this), this);
        getServer().getPluginManager().registerEvents(chatActivityLogger, this);

        var executor = new VerityCommand(this);
        var command = getCommand("verity");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getServer().getServicesManager().register(VerityAIApi.class, new VerityAIApiImpl(this), this, ServicePriority.Normal);

        scheduleMaintenanceTasks();
        historyLogger.cleanupOldLogs();
        setupMetrics();

        getLogger().info("VerityAI enabled - Verity is ready to help players!");
    }

    /**
     * Anonymous usage statistics via bStats (server count, versions, etc. are
     * collected automatically). Respects the server's global bStats opt-out
     * (plugins/bStats/config.yml) automatically; no separate toggle needed.
     */
    private void setupMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("ai_model", () -> configManager.getModel()));
        metrics.addCustomChart(new SimplePie("function_calling_enabled",
                () -> String.valueOf(configManager.isFunctionCallingEnabled())));
        metrics.addCustomChart(new SimplePie("long_term_memory_enabled",
                () -> String.valueOf(configManager.isLongTermEnabled())));
    }

    /**
     * Periodic upkeep: idle-conversation cleanup, long-term-memory autosave,
     * history log rotation cleanup, due-reminder checks, and reminder autosave.
     */
    private void scheduleMaintenanceTasks() {
        long idleCleanupTicks = 5L * 60 * 20; // every 5 minutes
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> conversationManager.purgeIdleConversations(), idleCleanupTicks, idleCleanupTicks);

        long autosaveTicks = Math.max(1, configManager.getLongTermAutosaveMinutes()) * 60L * 20;
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> longTermMemoryStore.flushDirty(), autosaveTicks, autosaveTicks);

        long historyCleanupTicks = 60L * 60 * 20; // once an hour
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> historyLogger.cleanupOldLogs(), historyCleanupTicks, historyCleanupTicks);

        // Reminders touch Player#sendMessage, so this one runs on the main thread.
        long taskCheckTicks = 60L * 20; // once a minute
        getServer().getScheduler().runTaskTimer(this,
                () -> taskService.checkDue(), taskCheckTicks, taskCheckTicks);
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> taskService.flushDirty(), autosaveTicks, autosaveTicks);
    }

    @Override
    public void onDisable() {
        if (longTermMemoryStore != null) {
            longTermMemoryStore.flushDirty();
        }
        if (taskService != null) {
            taskService.flushDirty();
        }
        if (hookManager != null) {
            hookManager.unregisterPlaceholders();
        }
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("VerityAI disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DebugLogger getDebugLogger() { return debugLogger; }
    public RateLimiter getRateLimiter() { return rateLimiter; }
    public ConversationManager getConversationManager() { return conversationManager; }
    public LongTermMemoryStore getLongTermMemoryStore() { return longTermMemoryStore; }
    public HistoryLogger getHistoryLogger() { return historyLogger; }
    public HookManager getHookManager() { return hookManager; }
    public WorldQueryService getWorldQueryService() { return worldQueryService; }
    public ContextBuilder getContextBuilder() { return contextBuilder; }
    public AIHandler getAIHandler() { return aiHandler; }
    public AiCommandExecutor getAiCommandExecutor() { return aiCommandExecutor; }
    public EmbeddingService getEmbeddingService() { return embeddingService; }
    public StatsService getStatsService() { return statsService; }
    public TaskService getTaskService() { return taskService; }
    public QuestService getQuestService() { return questService; }
    public LessonsService getLessonsService() { return lessonsService; }
    public ChatActivityLogger getChatActivityLogger() { return chatActivityLogger; }
}
