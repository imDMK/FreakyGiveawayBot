package me.dmk.app;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.json.gson.JsonGsonConfigurer;
import lombok.Getter;
import me.dmk.app.command.CommandManager;
import me.dmk.app.configuration.AppConfiguration;
import me.dmk.app.database.MongoClientService;
import me.dmk.app.giveaway.GiveawayManager;
import me.dmk.app.listener.MessageListener;
import me.dmk.app.listener.SlashCommandListener;
import me.dmk.app.listener.button.ButtonInteractionListener;
import me.dmk.app.task.GiveawayExpireTask;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.util.logging.FallbackLoggerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by DMK on 30.03.2023
 */

@Getter
public class GiveawayApp {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Instant startInstant;

    private final AppConfiguration appConfiguration;
    private final DiscordApi discordApi;

    private final MongoClientService mongoClientService;

    private final GiveawayManager giveawayManager;
    private final CommandManager commandManager;

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    protected GiveawayApp() {
        this.startInstant = Instant.now();

        this.appConfiguration = ConfigManager.create(AppConfiguration.class, (config) -> {
            config.withConfigurer(new JsonGsonConfigurer());
            config.withBindFile("configuration.json");
            config.withRemoveOrphans(true);
            config.saveDefaults();
            config.load(true);
        });

        FallbackLoggerConfiguration.setDebug(this.appConfiguration.isDebug());

        this.discordApi = new DiscordApiBuilder()
                .setToken(this.appConfiguration.getToken())
                .setAllIntents()
                .setWaitForServersOnStartup(true)
                .setWaitForUsersOnStartup(true)
                .login().join();

        int availableProcessors = Runtime.getRuntime().availableProcessors();

        this.executorService = Executors.newFixedThreadPool(availableProcessors);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(availableProcessors);

        /* Services */
        this.mongoClientService = new MongoClientService(this.appConfiguration.getDatabaseConfiguration());
        this.mongoClientService.connect();

        /* Managers */
        this.giveawayManager = new GiveawayManager(this.mongoClientService.getMongoDatabase(), executorService);

        this.commandManager = new CommandManager(this, this.discordApi, this.giveawayManager);
        this.commandManager.registerCommands();

        /* Tasks */
        this.scheduledExecutorService.scheduleWithFixedDelay(new GiveawayExpireTask(this.discordApi, this.giveawayManager), 1L, 1L, TimeUnit.SECONDS);

        /* Listeners */
        Stream.of(
                new ButtonInteractionListener(this.giveawayManager),
                new MessageListener(this.giveawayManager),
                new SlashCommandListener(this.commandManager)
        ).forEach(discordApi::addListener);

        /* Shutdown hook */
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

        this.logger.info("Application ready.");

        this.discordApi.updateActivity(
                this.appConfiguration.getActivityType(),
                this.appConfiguration.getActivityName()
        );

        if (this.discordApi.getServers().isEmpty()) {
            this.logger.info("Invite me using: " + this.discordApi.createBotInvite());
        }
    }

    public void onShutdown() {
        this.logger.info("Disabling application...");
        this.logger.info("Disabling tasks...");

        this.executorService.shutdown();
        this.scheduledExecutorService.shutdown();

        try {
            if (!this.executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
                this.executorService.shutdownNow();
            }

            if (this.scheduledExecutorService.awaitTermination(5L, TimeUnit.SECONDS)) {
                this.scheduledExecutorService.shutdownNow();
            }

            this.logger.info("Disabled all tasks.");
        } catch (InterruptedException interruptedException) {
            this.logger.error("Exception while disabling tasks", interruptedException);
        }

        this.logger.info("Closing database connection...");

        this.mongoClientService.close();

        this.logger.info("Successfully disabled application.");
    }
}
