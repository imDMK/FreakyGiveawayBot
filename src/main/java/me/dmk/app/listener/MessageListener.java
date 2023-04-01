package me.dmk.app.listener;

import lombok.AllArgsConstructor;
import me.dmk.app.giveaway.GiveawayManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.api.listener.message.MessageDeleteListener;

import java.util.Optional;

/**
 * Created by DMK on 01.04.2023
 */

@AllArgsConstructor
public class MessageListener implements MessageDeleteListener {

    private final GiveawayManager giveawayManager;

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        Optional<Message> messageOptional = event.getMessage();
        if (messageOptional.isEmpty()) {
            return;
        }

        Message message = messageOptional.get();

        this.giveawayManager.getOrElseFind(message.getId())
                .ifPresent(giveawayManager::deleteOne);
    }
}
