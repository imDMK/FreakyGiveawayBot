package me.dmk.app.command.implementation;

import me.dmk.app.command.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayManager;
import me.dmk.app.util.StringUtil;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

import java.util.Optional;

/**
 * Created by DMK on 31.03.2023
 */

public class GiveawayEndCommand extends Command {

    private final GiveawayManager giveawayManager;

    public GiveawayEndCommand(GiveawayManager giveawayManager) {
        super("giveaway-end", "Zakończ trwający konkurs.");

        this.giveawayManager = giveawayManager;

        this.addOption(
                SlashCommandOption.createStringOption("messageId", "ID wiadomości konkursu", true) //Must be a string because discord doesn't support long IDs.
        );
    }

    @Override
    public void execute(SlashCommandInteraction interaction, Server server, User user) {
        String messageIdArgument = interaction.getArgumentStringValueByName("messageId").orElseThrow();

        if (!StringUtil.isLong(messageIdArgument)) {
            EmbedMessage embedMessage = new EmbedMessage(server).error();

            embedMessage.setDescription("Podano nieprawidłowe ID.");
            embedMessage.createImmediateResponder(interaction, true);
            return;
        }

        long messageId = Long.parseLong(messageIdArgument);

        Optional<TextChannel> textChannelOptional = interaction.getChannel();
        if (textChannelOptional.isEmpty()) {
            EmbedMessage embedMessage = new EmbedMessage(server).error();

            embedMessage.setDescription("Nie możesz użyć tej komendy na tym kanale.");
            embedMessage.createImmediateResponder(interaction, true);
            return;
        }

        TextChannel textChannel = textChannelOptional.get();

        if (!textChannel.canYouWrite()) {
            EmbedMessage embedMessage = new EmbedMessage(server).error();

            embedMessage.setDescription("Nie posiadam uprawnień do tego kanału.");
            embedMessage.createImmediateResponder(interaction, true);
            return;
        }

        textChannel.getMessageById(messageId)
                .thenAcceptAsync(message ->
                        this.endGiveaway(interaction, server, message))
                .exceptionallyAsync(throwable -> {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();

                    embedMessage.setDescription("Nie znaleziono wiadomości.");
                    embedMessage.createImmediateResponder(interaction, true);
                    return null; //Don't get exception - message not exist
                });
    }

    private void endGiveaway(SlashCommandInteraction interaction, Server server, Message message) {
        this.giveawayManager.find(message.getId())
                .thenAcceptAsync(giveawayOptional -> {
                    if (giveawayOptional.isEmpty()) {
                        EmbedMessage embedMessage = new EmbedMessage(server).error();

                        embedMessage.setDescription("Nie znaleziono konkursu.");
                        embedMessage.createImmediateResponder(interaction, true);
                        return;
                    }

                    Giveaway giveaway = giveawayOptional.get();

                    if (giveaway.isEnded()) {
                        EmbedMessage embedMessage = new EmbedMessage(server).error();

                        embedMessage.setDescription("Konkurs zakończył się już.");
                        embedMessage.createImmediateResponder(interaction, true);
                        return;
                    }

                    if (giveaway.getWinners() > giveaway.getParticipants().size()) {
                        EmbedMessage embedMessage = new EmbedMessage(server).error();

                        embedMessage.setDescription(
                                "Zbyt mało osób wzięło udział w tym konkursie, aby go zakończyć.",
                                "Wzięło udział: " + giveaway.getParticipants().size(),
                                "Wymagane: " + giveaway.getWinners()
                        );
                        embedMessage.createImmediateResponder(interaction, true);
                        return;
                    }

                    this.giveawayManager.endGiveaway(giveaway, server, message);

                    EmbedMessage embedMessage = new EmbedMessage(server).success();
                    embedMessage.setDescription("Zakończono konkurs.");

                    embedMessage.createImmediateResponder(interaction, true);
                })
                .exceptionallyAsync(throwable -> {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();
                    embedMessage.setDescription("Wystąpił błąd z bazą danych.");

                    embedMessage.createImmediateResponder(interaction, true);

                    throwable.printStackTrace();
                    return null;
                });

    }
}
