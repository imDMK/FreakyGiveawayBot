package me.dmk.app.embed;

import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.Color;
import java.util.Optional;

/**
 * Created by DMK on 30.03.2023
 */

public class EmbedMessage extends EmbedBuilder{

    private final Server server;

    private final Color successColor = new Color(50, 255, 0);
    private final Color errorColor = new Color(255, 0, 0);
    private final Color giveawayColor = new Color(255, 0, 240);

    public EmbedMessage(Server server) {
        this.server = server;
        this.setTimestampToNow();
        
        server.getIcon()
                .ifPresentOrElse(icon -> 
                                this.setFooter(server.getName(), icon), 
                        () -> this.setFooter(server.getName())
                );
    }

    public EmbedMessage success() {
        Optional<KnownCustomEmoji> successEmoji = this.server.getCustomEmojisByNameIgnoreCase("success").stream().findFirst();

        this.setTitle((successEmoji.map(KnownCustomEmoji::getMentionTag).orElse("✅")) + " Wykonano!");
        this.setColor(this.successColor);

        return this;
    }

    public EmbedMessage error() {
        Optional<KnownCustomEmoji> successEmoji = this.server.getCustomEmojisByNameIgnoreCase("error").stream().findFirst();

        this.setTitle((successEmoji.map(KnownCustomEmoji::getMentionTag).orElse("❌")) + " Błąd!");
        this.setColor(this.errorColor);

        return this;
    }

    public EmbedMessage giveaway() {
        Optional<KnownCustomEmoji> successEmoji = this.server.getCustomEmojisByNameIgnoreCase("giveaway").stream().findFirst();

        this.setTitle((successEmoji.map(KnownCustomEmoji::getMentionTag).orElse("\uD83C\uDF89")) + " Konkurs!");
        this.setColor(this.giveawayColor);

        return this;
    }

    public void setDescription(String... strings) {
        this.setDescription(
                String.join("\n", strings)
        );
    }

    public void createImmediateResponder(InteractionBase interactionBase) {
        interactionBase.createImmediateResponder()
                .addEmbed(this)
                .respond();
    }

    public void createImmediateResponder(InteractionBase interactionBase, boolean ephemeral, HighLevelComponent... highLevelComponents) {
        InteractionImmediateResponseBuilder responseBuilder = interactionBase.createImmediateResponder();

        responseBuilder.addEmbed(this);
        responseBuilder.addComponents(highLevelComponents);

        if (ephemeral) {
            responseBuilder.setFlags(MessageFlag.EPHEMERAL);
        }

        responseBuilder.respond()
                .exceptionally(ExceptionLogger.get());
    }
}
