package net.jandie1505.commandsystem.core.dispatch;

import net.jandie1505.commandsystem.core.data.CompleteRequest;
import net.jandie1505.commandsystem.core.data.CompleteResponse;
import net.jandie1505.commandsystem.core.data.ExecuteRequest;
import net.jandie1505.commandsystem.core.data.ExecuteResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for command execute and complete action.
 */
public interface CommandDispatcher {

    /**
     * This is called when the command is executed.
     * @param request request
     * @return response
     */
    @Nullable ExecuteResponse onExecute(@NotNull ExecuteRequest request);

    /**
     * This is executed when the command is completed.
     * @param request request
     * @return response
     */
    default @Nullable CompleteResponse onComplete(@NotNull CompleteRequest request) {
        return CompleteResponse.EMPTY;
    }

}
