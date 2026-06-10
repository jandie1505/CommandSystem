package net.jandie1505.commandsystem.tools.subcommand;

import java.util.Map;

/**
 * The dynamic subcommand provider can be used to add/remove subcommands to {@link SubcommandCommand}s right when the player is typing/executing the command.
 * For example, if you have a two different game modes, each game mode can provide a subcommand that is only available when that specific game mode is currently running.
 */
public interface DynamicSubcommandProvider {

    /**
     * Returns the map of subcommands of this dynamic subcommand provider.
     * @return map of commands
     */
    Map<String, SubcommandEntry> getDynamicSubcommands();

}
