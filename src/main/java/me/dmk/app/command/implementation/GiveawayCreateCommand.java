package me.dmk.app.command.implementation;

import me.dmk.app.command.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayManager;
import me.dmk.app.util.GiveawayUtil;
import me.dmk.app.util.MessageUtil;
import me.dmk.app.util.StringUtil;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Created by DMK on 29.03.2023
 */

public class GiveawayCreateCommand extends Command {

    private final GiveawayManager giveawayManager;

    public GiveawayCreateCommand(GiveawayManager giveawayManager) {
        super("giveaway-create", "Stwórz konkurs");

        this.giveawayManager = giveawayManager;

        this.addOptions(
                SlashCommandOption.createChannelOption("channel", "Kanał", true, Collections.singleton(ChannelType.SERVER_TEXT_CHANNEL)),
                SlashCommandOption.createStringOption("award", "Nagroda", true),
                SlashCommandOption.createStringOption("expire", "Czas trwania konkursu (np. 7d)", true),
                SlashCommandOption.createLongOption("winners", "Ilość zwycięzców", true),
                SlashCommandOption.createBooleanOption("ping", "Ping everyone?", false)
        );
    }

    @Override
    public void execute(SlashCommandInteraction interaction, Server server, User user) {
        ServerTextChannel serverTextChannel = interaction.getArgumentChannelValueByName("channel").orElseThrow()
                .asServerTextChannel().orElseThrow();

        String award = interaction.getArgumentStringValueByName("award").orElseThrow();
        String expire = interaction.getArgumentStringValueByName("expire").orElseThrow();
        int winners = interaction.getArgumentLongValueByName("winners").orElseThrow().intValue();
        boolean ping = interaction.getArgumentBooleanValueByName("ping").orElse(true);

        if (!serverTextChannel.canYouWrite()) {
            EmbedMessage embedMessage = new EmbedMessage(server).error();

            embedMessage.setDescription("Nie posiadam uprawnień do kanału " + serverTextChannel.getMentionTag() + ".");
            embedMessage.createImmediateResponder(interaction, true);
            return;
        }

        if (winners < 1) {
            EmbedMessage embedMessage = new EmbedMessage(server).error();

            embedMessage.setDescription("Liczba zwycięzców musi być wyższa niż 1.");
            embedMessage.createImmediateResponder(interaction, true);
            return;
        }

        Optional<Instant> expireOptional = StringUtil.toInstant(expire);
        if (expireOptional.isEmpty()) {
            EmbedMessage embedMessage = new EmbedMessage(server).error();

            embedMessage.setDescription("Podano nieprawidłowy czas trwania konkursu.");
            embedMessage.createImmediateResponder(interaction, true);
            return;
        }

        Date expireDate = Date.from(expireOptional.get());

        InteractionOriginalResponseUpdater responseUpdater = interaction.respondLater(true).join();

        new MessageBuilder()
                .setContent("Tworzenie konkursu...")
                .send(serverTextChannel)
                .thenAcceptAsync(message -> {
                    Giveaway giveaway = new Giveaway(server, message, award, winners, expireDate);

                    this.createGiveaway(responseUpdater, server, message, giveaway, ping);
                })
                .exceptionally(throwable -> {
                    this.respondError(responseUpdater, server, throwable);
                    return null;
                });
    }

    private void createGiveaway(InteractionOriginalResponseUpdater responseUpdater, Server server, Message message, Giveaway giveaway, boolean ping) {
        this.giveawayManager.add(giveaway)
                .thenAcceptAsync(unused -> this.updateMessage(server, message, giveaway, ping)
                        .thenAcceptAsync(m -> {
                            EmbedBuilder embedBuilder = new EmbedMessage(server)
                                    .success()
                                    .setDescription("Stworzno nowy " + MessageUtil.createJumpUrl("konkurs", server, message) + ".");

                            responseUpdater.addEmbed(embedBuilder).update()
                                    .exceptionally(ExceptionLogger.get());
                        })
                        .exceptionally(throwable -> {
                            this.giveawayManager.delete(giveaway).exceptionally(ExceptionLogger.get());
                            this.respondError(responseUpdater, server, throwable);
                            message.delete().exceptionally(ExceptionLogger.get());
                            return null;
                        })
                )
                .exceptionally(throwable -> {
                    this.respondError(responseUpdater, server, throwable);
                    message.delete().exceptionally(ExceptionLogger.get());
                    return null;
                });

    }

    private CompletableFuture<Message> updateMessage(Server server, Message message, Giveaway giveaway, boolean ping) {
        return message.createUpdater()
                .setContent(ping ? server.getEveryoneRole().getMentionTag() : "")
                .setEmbed(GiveawayUtil.getMessageTemplate(server, giveaway))
                .addComponents(ActionRow.of(GiveawayUtil.getGiveawayJoinButton()))
                .applyChanges();
    }

    private void respondError(InteractionOriginalResponseUpdater responseUpdater, Server server, Throwable throwable) {
        EmbedMessage embedMessage = new EmbedMessage(server).error();

        embedMessage.setDescription("Wystąpił błąd.");
        embedMessage.addField("Szczegóły", throwable.getMessage());

        responseUpdater.addEmbed(embedMessage).update()
                .exceptionally(ExceptionLogger.get());
    }
}
