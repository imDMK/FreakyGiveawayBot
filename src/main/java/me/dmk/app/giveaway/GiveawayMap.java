package me.dmk.app.giveaway;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DMK on 30.03.2023
 */

public class GiveawayMap {

    private final Map<Long, Giveaway> giveawayMap = new ConcurrentHashMap<>();

    public void put(Giveaway giveaway) {
        this.giveawayMap.put(giveaway.getMessageId(), giveaway);
    }

    public Optional<Giveaway> get(long messageId) {
        return Optional.ofNullable(
                this.giveawayMap.get(messageId)
        );
    }

    public void remove(Giveaway giveaway) {
        this.giveawayMap.remove(giveaway.getMessageId());
    }
}
