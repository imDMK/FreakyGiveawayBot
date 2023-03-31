package me.dmk.app.giveaway;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by DMK on 29.03.2023
 */

@Data
@NoArgsConstructor
public class Giveaway {

    private long serverId;
    private long channelId;
    private long messageId;

    private int winners;

    private String award;
    private Date expireAt;
    private Set<Long> participants;

    private boolean ended;

    public Giveaway(Server server, Message message, String award, int winners, Date expire) {
        this.serverId = server.getId();
        this.channelId = message.getChannel().getId();
        this.messageId = message.getId();
        this.winners = winners;

        this.award = award;
        this.expireAt = expire;
        this.participants = new HashSet<>();
    }

    public void addParticipant(long userId) {
        this.participants.add(userId);
    }

    public boolean isParticipant(long userId) {
        return this.participants.contains(userId);
    }

    public void removeParticipant(long userId) {
        this.participants.remove(userId);
    }
}
