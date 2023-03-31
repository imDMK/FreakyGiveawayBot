package me.dmk.app.listener.button;

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

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by DMK on 31.03.2023
 */

@AllArgsConstructor
public class ButtonInteractionListener implements ButtonClickListener {

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

        Arrays.stream(ButtonInteractionType.values())
                .filter(interactionType -> interactionType.getMessageId().equals(customId))
                .forEachOrdered(interactionType ->
                        this.onButtonClick(interactionType, interaction, user, server, message)
                );
    }

    public void onButtonClick(ButtonInteractionType interactionType, ButtonInteraction interaction, User user, Server server, Message message) {
        switch (interactionType) {
            case GIVEAWAY_JOIN -> {
                Optional<Giveaway> giveawayOptional = this.giveawayManager.getOrElseFind(message.getId());
                if (giveawayOptional.isEmpty()) {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();

                    embedMessage.setDescription("Nie znaleziono konkursu.");
                    embedMessage.createImmediateResponder(interaction, true);
                    return;
                }

                Giveaway giveaway = giveawayOptional.get();

                if (giveaway.isParticipant(user.getId())) {
                    this.giveawayManager.removeUserFromGiveaway(giveaway, user.getId())
                            .thenAcceptAsync(unused -> {
                                EmbedMessage embedMessage = new EmbedMessage(server).success();

                                embedMessage.setDescription("Opuszczono konkurs.");
                                embedMessage.createImmediateResponder(interaction, true);
                            }).exceptionallyAsync(throwable -> {
                                EmbedMessage embedMessage = new EmbedMessage(server).error();

                                embedMessage.setDescription("Wystąpił błąd podczas próby opuszczenia konkursu.");
                                embedMessage.addField("Szczegóły błędu", throwable.getMessage());

                                embedMessage.createImmediateResponder(interaction, true);
                                return null;
                            });
                    return;
                }

                this.giveawayManager.addUserToGiveaway(giveaway, user.getId())
                        .thenAcceptAsync(unused -> interaction.createImmediateResponder().respond())
                        .exceptionallyAsync(throwable -> {
                            EmbedMessage embedMessage = new EmbedMessage(server).error();

                            embedMessage.setDescription("Wystąpił błąd podczas próby dołączenia do konkursu.");
                            embedMessage.addField("Szczegóły błędu", throwable.getMessage());

                            embedMessage.createImmediateResponder(interaction, true);
                            return null;
                        });
            }

            case GIVEAWAY_LEAVE -> {
                Optional<Giveaway> giveawayOptional = this.giveawayManager.getOrElseFind(message.getId());
                if (giveawayOptional.isEmpty()) {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();

                    embedMessage.setDescription("Nie znaleziono konkursu.");
                    embedMessage.createImmediateResponder(interaction, true);
                    return;
                }

                Giveaway giveaway = giveawayOptional.get();

                this.giveawayManager.removeUserFromGiveaway(giveaway, user.getId())
                        .thenAcceptAsync(unused -> interaction.createImmediateResponder().respond())
                        .exceptionallyAsync(throwable -> {
                            EmbedMessage embedMessage = new EmbedMessage(server).error();

                            embedMessage.setDescription("Wystąpił błąd podczas próby dołączenia do konkursu.");
                            embedMessage.addField("Szczegóły błędu", throwable.getMessage());

                            embedMessage.createImmediateResponder(interaction, true);
                            return null;
                        });
            }
        }
    }
}
