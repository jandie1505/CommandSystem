package net.jandie1505.commandsystem.demo.commands;

import net.jandie1505.commandsystem.core.data.CompleteRequest;
import net.jandie1505.commandsystem.core.data.CompleteResponse;
import net.jandie1505.commandsystem.core.data.ExecuteRequest;
import net.jandie1505.commandsystem.core.data.ExecuteResponse;
import net.jandie1505.commandsystem.core.dispatch.CommandDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EchoCommand implements CommandDispatcher {

    @Override
    public @Nullable ExecuteResponse onExecute(@NotNull ExecuteRequest request) {

        var sb = new StringBuilder();
        var i = request.tokens().iterator();
        while (i.hasNext()) {
            var token = i.next();
            sb.append(token);
            if (i.hasNext()) sb.append(" ");
        }

        return new ExecuteResponse(true, sb.toString());
    }

    @Override
    public @Nullable CompleteResponse onComplete(@NotNull CompleteRequest request) {

        List<String> completions;
        switch (request.tokens().size()) {
            case 0 -> completions = List.of("The", "Another");
            case 1 -> completions = List.of("cake", "wonderful");
            case 2 -> completions = List.of("is", "useless");
            case 3 -> completions = List.of("a", "completion");
            case 4 -> completions = List.of("lie!", "options.");
            default -> completions = List.of();
        }

        return new CompleteResponse(completions);
    }

}
