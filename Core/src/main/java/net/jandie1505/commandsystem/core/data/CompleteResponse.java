package net.jandie1505.commandsystem.core.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A command complete response.
 * @param completions list of possible command arguments
 */
public record CompleteResponse(@NotNull List<String> completions) {

    /**
     * An empty complete response.
     */
    public static final CompleteResponse EMPTY = new CompleteResponse(List.of());

}
