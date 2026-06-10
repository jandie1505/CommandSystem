package net.jandie1505.commandsystem.core.dispatch;

import net.jandie1505.commandsystem.core.data.*;
import org.jetbrains.annotations.NotNull;

/**
 * User command.<br/>
 * Can be registered to {@link net.jandie1505.commandsystem.core.registry.CommandRegistry}.
 */
public class Command {
    @NotNull private final String name;
    @NotNull private final CommandDispatcher dispatcher;

    /**
     * Creates a new command.
     * @param name command name
     * @param dispatcher command dispatcher
     */
    public Command(@NotNull String name, @NotNull CommandDispatcher dispatcher) {
        this.name = name.toLowerCase();
        this.dispatcher = dispatcher;
    }

    // ----- EXECUTE / TAB COMPLETE -----

    /**
     * Executes an {@link ExecuteRequest}.
     * @param request execute request
     * @return execute response
     */
    public @NotNull ExecuteResponse execute(@NotNull ExecuteRequest request) {
        ExecuteResponse response = this.dispatcher.onExecute(request);

        if (response == null) {
            return ExecuteResponse.NO_RESPONSE;
        }

        return response;
    }

    /**
     * Completes a {@link CompleteRequest}.
     * @param request complete request.
     * @return complete response
     */
    public @NotNull CompleteResponse complete(@NotNull CompleteRequest request) {
        CompleteResponse response = this.dispatcher.onComplete(request);

        if (response == null) {
            return CompleteResponse.EMPTY;
        }

        return response;
    }

    // ----- INFO -----

    /**
     * Returns the command name.
     * @return command name
     */
    public final @NotNull String getName() {
        return this.name;
    }

}
