package me.dmk.app.listener;

import lombok.AllArgsConstructor;
import me.dmk.app.command.CommandManager;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.util.Optional;

/**
 * Created by DMK on 30.03.2023
 */

@AllArgsConstructor
public class SlashCommandListener implements SlashCommandCreateListener {

    private final CommandManager commandManager;

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        User user = interaction.getUser();

        String commandName = interaction.getCommandName();

        Optional<Server> serverOptional = interaction.getServer();
        if (serverOptional.isEmpty()) {
            return;
        }

        Server server = serverOptional.get();

        this.commandManager.get(commandName).ifPresent(command ->
                command.execute(interaction, server, user)
        );
    }
}
