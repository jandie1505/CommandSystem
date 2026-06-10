package net.jandie1505.commandsystem.core.data;

import org.jetbrains.annotations.NotNull;

/**
 * A command execute response.
 * @param success if the command has been executed successfully
 * @param output command output message
 */
public record ExecuteResponse(boolean success, @NotNull String output) {

    /**
     * Default message for command not found.
     */
    public static final ExecuteResponse NOT_FOUND = new ExecuteResponse(false, "Command not found.");

    /**
     * Default message for command has returned null.
     */
    public static final ExecuteResponse NO_RESPONSE = new ExecuteResponse(false, "Command did not respond.");

    /**
     * Default message for command has thrown an exception.
     */
    public static final ExecuteResponse EXCEPTION = new ExecuteResponse(false, "Exception occurred during command execution. Check logs for more information.");

}
