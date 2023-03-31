package me.dmk.app.task;

import lombok.RequiredArgsConstructor;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.Optional;

/**
 * Created by DMK on 30.03.2023
 */

@RequiredArgsConstructor
public class GiveawayExpireTask implements Runnable {

    private final DiscordApi discordApi;
    private final GiveawayManager giveawayManager;

    @Override
    public void run() {
        for (Giveaway giveaway : this.giveawayManager.getExpiredGiveaways()) {
            Optional<Server> serverOptional = this.discordApi.getServerById(giveaway.getServerId());
            if (serverOptional.isEmpty()) {
                this.giveawayManager.deleteOne(giveaway);
                continue;
            }

            Server server = serverOptional.get();

            Optional<ServerTextChannel> serverTextChannelOptional = server.getTextChannelById(giveaway.getChannelId());
            if (serverTextChannelOptional.isEmpty()) {
                this.giveawayManager.deleteOne(giveaway);
                return;
            }

            final ServerTextChannel serverTextChannel = serverTextChannelOptional.get();

            serverTextChannel.getMessageById(giveaway.getMessageId())
                    .thenAcceptAsync(message ->
                            this.giveawayManager.endGiveaway(giveaway, server, message)
                    )
                    .exceptionallyAsync(throwable -> {
                        this.giveawayManager.deleteOne(giveaway);
                        return null; //Don't get exception - message deleted
                    });
        }
    }
}
