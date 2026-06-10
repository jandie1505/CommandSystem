package net.jandie1505.commandsystem.tests.core.dispatch;

import net.jandie1505.commandsystem.core.data.*;
import net.jandie1505.commandsystem.core.dispatch.Command;
import net.jandie1505.commandsystem.core.dispatch.CommandDispatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CommandTest {

    @Test
    void nameIsLowercased() {
        Command cmd = new Command("FooBar", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return new ExecuteResponse(true, "ok");
            }
        });
        assertEquals("foobar", cmd.getName());
    }

    @Test
    void executeReturnsDispatcherResult() {
        ExecuteResponse expected = new ExecuteResponse(true, "result");
        Command cmd = new Command("test", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return expected;
            }
        });
        ExecuteResponse actual = cmd.execute(new ExecuteRequest(CommandSender.ADMIN, List.of()));
        assertSame(expected, actual);
    }

    @Test
    void executeReturnsNoResponseWhenDispatcherReturnsNull() {
        Command cmd = new Command("test", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }
        });
        ExecuteResponse actual = cmd.execute(new ExecuteRequest(CommandSender.ADMIN, List.of()));
        assertSame(ExecuteResponse.NO_RESPONSE, actual);
    }

    @Test
    void completeReturnsDispatcherResult() {
        CompleteResponse expected = new CompleteResponse(List.of("a", "b"));
        Command cmd = new Command("test", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }

            @Override
            public CompleteResponse onComplete(CompleteRequest request) {
                return expected;
            }
        });
        CompleteResponse actual = cmd.complete(new CompleteRequest(CommandSender.ADMIN, List.of(), ""));
        assertSame(expected, actual);
    }

    @Test
    void completeReturnsEmptyWhenDispatcherReturnsNull() {
        Command cmd = new Command("test", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }

            @Override
            public CompleteResponse onComplete(CompleteRequest request) {
                return null;
            }
        });
        CompleteResponse actual = cmd.complete(new CompleteRequest(CommandSender.ADMIN, List.of(), ""));
        assertSame(CompleteResponse.EMPTY, actual);
    }

    @Test
    void defaultCompleteIsEmpty() {
        Command cmd = new Command("test", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }
        });
        CompleteResponse actual = cmd.complete(new CompleteRequest(CommandSender.ADMIN, List.of(), ""));
        assertSame(CompleteResponse.EMPTY, actual);
    }
}