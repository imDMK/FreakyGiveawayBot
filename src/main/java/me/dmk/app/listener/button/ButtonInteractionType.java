package me.dmk.app.listener.button;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by DMK on 31.03.2023
 */

@Getter
@AllArgsConstructor
public enum ButtonInteractionType {

    GIVEAWAY_JOIN("giveaway-join"),
    GIVEAWAY_LEAVE("giveaway-leave");

    private final String messageId;
}
