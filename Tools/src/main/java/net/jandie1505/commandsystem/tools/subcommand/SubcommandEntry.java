package net.jandie1505.commandsystem.tools.subcommand;

import net.jandie1505.commandsystem.core.dispatch.CommandDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A subcommand entry represents one command that was added to {@link SubcommandCommand}.
 * @param dispatcher {@link CommandDispatcher}
 * @param permission permission provider that is required to use the command or null for no permission
 */
public record SubcommandEntry(@NotNull CommandDispatcher dispatcher, @Nullable SubcommandCommand.PermissionProvider permission) {

    /**
     * Creates a SubcommandEntry.
     * @param command command with CommandExecutor and TabCompleter
     * @param permission permission
     * @return SubcommandEntry
     */
    public static SubcommandEntry of(@NotNull CommandDispatcher command, @NotNull SubcommandCommand.PermissionProvider permission) {
        return new SubcommandEntry(command, permission);
    }

    /**
     * Creates a SubcommandEntry.
     * @param command command with CommandExecutor and TabCompleter
     * @param permission permission
     * @return SubcommandEntry
     */
    public static SubcommandEntry of(@NotNull CommandDispatcher command, @Nullable String permission) {
        return new SubcommandEntry(command, permission != null ? sender -> sender.hasPermission(permission) : null);
    }

    /**
     * Creates a SubcommandEntry.
     * @param command command with CommandExecutor and TabCompleter
     * @return SubcommandEntry
     */
    public static SubcommandEntry of(@NotNull CommandDispatcher command) {
        return new SubcommandEntry(command, null);
    }

}
