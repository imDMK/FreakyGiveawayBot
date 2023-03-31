package me.dmk.app.command;

import lombok.Getter;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

/**
 * Created by DMK on 29.03.2023
 */

public abstract class Command extends SlashCommandBuilder {

    @Getter
    private final String name;

    public Command(String name, String description) {
        this.name = name;

        this.setName(name);
        this.setDescription(description);
    }

    public abstract void execute(SlashCommandInteraction interaction, Server server, User user);

    public void addOptions(SlashCommandOption... slashCommandOptions) {
        for (SlashCommandOption option : slashCommandOptions) {
            this.addOption(option);
        }
    }
}
