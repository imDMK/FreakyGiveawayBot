package me.dmk.app.task;

import lombok.RequiredArgsConstructor;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by DMK on 30.03.2023
 */

@RequiredArgsConstructor
public class GiveawayExpireTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DiscordApi discordApi;
    private final GiveawayManager giveawayManager;

    @Override
    public void run() {
        for (Giveaway giveaway : this.giveawayManager.getExpiredGiveaways()) {
            Optional<Server> serverOptional = this.discordApi.getServerById(giveaway.getServerId());
            if (serverOptional.isEmpty()) {
                this.deleteGiveaway(giveaway);

                this.logger.debug("Deleted giveaway due to the server is invalid, giveaway: " + giveaway);
                return;
            }

            Server server = serverOptional.get();

            Optional<ServerTextChannel> serverTextChannelOptional = server.getTextChannelById(giveaway.getChannelId());
            if (serverTextChannelOptional.isEmpty()) {
                this.deleteGiveaway(giveaway);

                this.logger.debug("Deleted giveaway due to the channel is invalid, giveaway: " + giveaway);
                return;
            }

            ServerTextChannel serverTextChannel = serverTextChannelOptional.get();

            if (!serverTextChannel.canYouSee() || !serverTextChannel.canYouWrite()) {
                this.deleteGiveaway(giveaway);

                this.logger.debug("Deleted giveaway due to I don't have permission to edit giveaway message, giveaway: " + giveaway);
                return;
            }

            serverTextChannel.getMessageById(giveaway.getMessageId())
                    .thenAcceptAsync(message ->
                            this.giveawayManager.endGiveaway(giveaway, server, message)
                    )
                    .exceptionallyAsync(throwable -> {
                        this.deleteGiveaway(giveaway);

                        this.logger.debug("Deleted giveaway due to the message is invalid, giveaway: " + giveaway);
                        return null; //Don't get exception - message invalid
                    });
        }
    }

    private void deleteGiveaway(Giveaway giveaway) {
        this.giveawayManager.delete(giveaway)
                .exceptionally(ExceptionLogger.get());
    }
}
