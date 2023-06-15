package me.dmk.app.listener;

import lombok.AllArgsConstructor;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.listener.interaction.ButtonClickListener;

import java.util.Optional;

/**
 * Created by DMK on 31.03.2023
 */

@AllArgsConstructor
public class ButtonListener implements ButtonClickListener {

    private final GiveawayManager giveawayManager;

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        ButtonInteraction interaction = event.getButtonInteraction();
        User user = interaction.getUser();
        Message message = interaction.getMessage();
        String customId = interaction.getCustomId();

        Optional<Server> serverOptional = interaction.getServer();
        if (serverOptional.isEmpty()) {
            return;
        }

        Server server = serverOptional.get();

        if (customId.equals("giveaway-join")) {
            this.onGiveawayJoinButtonClick(interaction, user, server, message);
        }
    }

    private void onGiveawayJoinButtonClick(ButtonInteraction interaction, User user, Server server, Message message) {
        this.giveawayManager.find(message.getId())
                .thenAcceptAsync(giveawayOptional -> {
                    if (giveawayOptional.isEmpty()) {
                        EmbedMessage embedMessage = new EmbedMessage(server).error();
                        embedMessage.setDescription("Konkurs nie istnieje.");

                        embedMessage.createImmediateResponder(interaction, true);
                        return;
                    }

                    Giveaway giveaway = giveawayOptional.get();

                    this.joinGiveaway(interaction, server, user, giveaway);
                })
                .exceptionallyAsync(throwable -> {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();

                    embedMessage.setDescription("Wystąpił błąd.");
                    embedMessage.addField("Szczegóły", throwable.getMessage());

                    embedMessage.createImmediateResponder(interaction, true);
                    return null;
                });
    }

    private void joinGiveaway(ButtonInteraction interaction, Server server, User user, Giveaway giveaway) {
        if (giveaway.isParticipant(user.getId())) {
            this.leaveGiveaway(interaction, server, user, giveaway);
            return;
        }

        this.giveawayManager.addUserToGiveaway(giveaway, user.getId())
                .thenAcceptAsync(unused -> interaction.createImmediateResponder().respond())
                .exceptionallyAsync(throwable -> {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();

                    embedMessage.setDescription("Wystąpił błąd podczas próby dołączenia do konkursu.");
                    embedMessage.addField("Szczegóły", throwable.getMessage());

                    embedMessage.createImmediateResponder(interaction, true);
                    return null;
                });
    }

    private void leaveGiveaway(ButtonInteraction interaction, Server server, User user, Giveaway giveaway) {
        if (!giveaway.isParticipant(user.getId())) {
            this.joinGiveaway(interaction, server, user, giveaway);
            return;
        }

        this.giveawayManager.removeUserFromGiveaway(giveaway, user.getId())
                .thenAcceptAsync(unused -> {
                    EmbedMessage embedMessage = new EmbedMessage(server).success();

                    embedMessage.setDescription("Opuszczono konkurs.");
                    embedMessage.createImmediateResponder(interaction, true);
                })
                .exceptionallyAsync(throwable -> {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();

                    embedMessage.setDescription("Wystąpił błąd podczas próby odejścia od konkursu.");
                    embedMessage.addField("Szczegóły", throwable.getMessage());

                    embedMessage.createImmediateResponder(interaction, true);
                    return null;
                });
    }
}
