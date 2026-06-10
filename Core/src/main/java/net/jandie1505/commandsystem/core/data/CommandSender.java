package net.jandie1505.commandsystem.core.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a command sender.
 */
public interface CommandSender {

    /**
     * Command sender with all permissions.
     */
    @NotNull CommandSender ADMIN = new CommandSender() {
        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }
    };

    /**
     * Check if the sender has the specified permission.
     * @param permission permission
     * @return has permission
     */
    boolean hasPermission(@NotNull String permission);

}
