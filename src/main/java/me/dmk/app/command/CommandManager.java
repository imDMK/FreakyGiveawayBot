package me.dmk.app.command;

import lombok.RequiredArgsConstructor;
import me.dmk.app.GiveawayApp;
import me.dmk.app.command.implementation.GiveawayCreateCommand;
import me.dmk.app.command.implementation.GiveawayEndCommand;
import me.dmk.app.command.implementation.GiveawayReRollCommand;
import me.dmk.app.command.implementation.StatusCommand;
import me.dmk.app.giveaway.GiveawayManager;
import org.javacord.api.DiscordApi;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DMK on 29.03.2023
 */

@RequiredArgsConstructor
public class CommandManager {

    private final GiveawayApp giveawayApp;
    private final DiscordApi discordApi;
    private final GiveawayManager giveawayManager;

    private final Map<String, Command> commandMap = new ConcurrentHashMap<>();

    public void registerCommands() {
        Command giveawayCreateCommand = new GiveawayCreateCommand(this.giveawayManager);
        Command giveawayEndCommand = new GiveawayEndCommand(this.giveawayManager);
        Command giveawayRerollCommand = new GiveawayReRollCommand(this.giveawayManager);
        Command statusCommand = new StatusCommand(this.giveawayApp);

        this.register(
                giveawayCreateCommand,
                giveawayEndCommand,
                giveawayRerollCommand,
                statusCommand
        );
    }

    private void register(Command... commands) {
        for (Command command : commands) {
            this.commandMap.put(command.getName(), command);
        }

        this.discordApi.bulkOverwriteGlobalApplicationCommands(Set.of(
                commands
        ));
    }

    public Optional<Command> get(String name) {
        return Optional.ofNullable(
                this.commandMap.get(name)
        );
    }
}
