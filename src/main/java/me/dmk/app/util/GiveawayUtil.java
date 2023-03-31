package me.dmk.app.util;

import lombok.experimental.UtilityClass;
import me.dmk.app.embed.EmbedMessage;
import me.dmk.app.giveaway.Giveaway;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by DMK on 30.03.2023
 */

@UtilityClass
public class GiveawayUtil {

    public static EmbedBuilder getMessageTemplate(Server server, Giveaway giveaway) {
        return new EmbedMessage(server).giveaway()
                .addField("Nagroda", "**" + giveaway.getWinners() + "x** " + giveaway.getAward())
                .addField("Zakończy się", "<t:" + giveaway.getExpireAt().toInstant().getEpochSecond() + ":R>");
    }

    public List<String> selectWinners(Set<Long> participants, int winners) {
        if (participants.size() < winners) {
            return Collections.emptyList();
        }

        ArrayList<String> winnerList = new ArrayList<>();

        for (int i = 0; i < winners; i++) {
            long winner = participants.stream().toList().get((int) (Math.random() * participants.size()));
            winnerList.add("<@" + winner + ">");
        }

        return winnerList;
    }
}
