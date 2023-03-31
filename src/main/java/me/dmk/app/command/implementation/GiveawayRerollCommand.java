package me.dmk.app.command.implementation;

import me.dmk.app.command.Command;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import me.dmk.app.giveaway.GiveawayManager;
import me.dmk.app.util.GiveawayUtil;
import me.dmk.app.util.StringUtil;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

import java.util.List;
import java.util.Optional;

/**
 * Created by DMK on 31.03.2023
 */

public class GiveawayRerollCommand extends Command {

    private final GiveawayManager giveawayManager;

    public GiveawayRerollCommand(GiveawayManager giveawayManager) {
        super("giveaway-reroll", "Ponownie rozlosuj zwycięzców konkursu.");

        this.giveawayManager =  giveawayManager;

        this.addOptions(
                SlashCommandOption.createStringOption("messageId", "ID wiadomości", true), //Must be a string because discord doesn't support long IDs.
                SlashCommandOption.createLongOption("winners", "Ilość zwycięzców", true)
        );
    }

    @Override
    public void execute(SlashCommandInteraction interaction, Server server, User user) {
        String messageIdArgument = interaction.getArgumentStringValueByName("messageId").orElseThrow();
        int winners = interaction.getArgumentLongValueByName("winners").orElseThrow().intValue();

        if (StringUtil.isNotLong(messageIdArgument)) {
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
                .thenAcceptAsync(message -> {
                    Optional<Giveaway> giveawayOptional = this.giveawayManager.getOrElseFind(message.getId());
                    if (giveawayOptional.isEmpty()) {
                        EmbedMessage embedMessage = new EmbedMessage(server).error();

                        embedMessage.setDescription("Nie znaleziono konkursu.");
                        embedMessage.createImmediateResponder(interaction, true);
                        return;
                    }

                    Giveaway giveaway = giveawayOptional.get();

                    if (!giveaway.isEnded()) {
                        EmbedMessage embedMessage = new EmbedMessage(server).error();

                        embedMessage.setDescription("Konkurs jeszcze trwa.");
                        embedMessage.createImmediateResponder(interaction, true);
                        return;
                    }

                    if (winners > giveaway.getParticipants().size()) {
                        EmbedMessage embedMessage = new EmbedMessage(server).error();

                        embedMessage.setDescription(
                                "Zbyt mało osób wzięło udział, aby ponownie rozlosować zwycięzców.",
                                "Wzięło udział: " + giveaway.getParticipants().size(),
                                "Wymagane: " + winners
                        );
                        embedMessage.createImmediateResponder(interaction, true);
                        return;
                    }

                    List<String> selectedWinners = GiveawayUtil.selectWinners(
                            giveaway.getParticipants(),
                            winners
                    );

                    String messageContent = "Gratulacje " + String.join(", ", selectedWinners) + " " + (selectedWinners.size() > 1 ? "wygraliście" : "wygrałeś(-aś)") + " w ponownym losowaniu konkursu na " + giveaway.getAward() + "!";

                    new MessageBuilder()
                            .setContent(messageContent)
                            .replyTo(message)
                            .send(textChannel)
                            .thenAcceptAsync(msg -> {
                                EmbedMessage embedMessage = new EmbedMessage(server).success();

                                embedMessage.setDescription("Rozlosowano nowych zwycięzców.");
                                embedMessage.createImmediateResponder(interaction, true);
                            }).exceptionallyAsync(throwable -> {
                                interaction.createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).addEmbed(new EmbedMessage(server).error().setDescription("Wystąpił błąd poczas wysyłania wiadomości.").addField("Błąd", throwable.getMessage())).respond();
                                return null;
                            });
                }).exceptionallyAsync(throwable -> {
                    EmbedMessage embedMessage = new EmbedMessage(server).error();

                    embedMessage.setDescription("Nie znaleziono wiadomości.");
                    embedMessage.createImmediateResponder(interaction, true);
                    return null; //Don't get exception - message does not exists
                });
    }
}
