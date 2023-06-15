package me.dmk.app.util;

import lombok.experimental.UtilityClass;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

/**
 * Created by DMK on 30.03.2023
 */

@UtilityClass
public final class MessageUtil {

    public static String createJumpUrl(String text, Server server, Message message) {
        return String.format("[%s](%s)", text, getUrl(server, message));
    }

    public static String getUrl(Server server, Message message) {
        return String.format("https://discordapp.com/channels/%s/%s/%s", server.getIdAsString(), message.getChannel().getIdAsString(), message.getIdAsString());
    }
}
