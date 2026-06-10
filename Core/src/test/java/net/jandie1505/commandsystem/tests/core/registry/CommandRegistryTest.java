package net.jandie1505.commandsystem.tests.core.registry;

import net.jandie1505.commandsystem.core.data.*;
import net.jandie1505.commandsystem.core.dispatch.Command;
import net.jandie1505.commandsystem.core.dispatch.CommandDispatcher;
import net.jandie1505.commandsystem.core.registry.CommandRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
    }

    @Test
    void executeUnknownCommandReturnsNotFound() {
        ExecuteResponse response = registry.executeCommand("missing",
                new ExecuteRequest(CommandSender.ADMIN, List.of()));
        assertSame(ExecuteResponse.NOT_FOUND, response);
    }

    @Test
    void completeUnknownCommandReturnsEmpty() {
        CompleteResponse response = registry.completeCommand("missing",
                new CompleteRequest(CommandSender.ADMIN, List.of(), ""));
        assertSame(CompleteResponse.EMPTY, response);
    }

    @Test
    void registerAndExecuteCommand() {
        registry.registerCommand("hello", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return new ExecuteResponse(true, "world");
            }
        });
        ExecuteResponse response = registry.executeCommand("hello",
                new ExecuteRequest(CommandSender.ADMIN, List.of()));
        assertTrue(response.success());
        assertEquals("world", response.output());
    }

    @Test
    void commandLookupIsCaseInsensitive() {
        registry.registerCommand("hello", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return new ExecuteResponse(true, "ok");
            }
        });
        ExecuteResponse response = registry.executeCommand("HELLO",
                new ExecuteRequest(CommandSender.ADMIN, List.of()));
        assertTrue(response.success());
    }

    @Test
    void registerNameIsLowercased() {
        registry.registerCommand("MixedCase", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return new ExecuteResponse(true, "ok");
            }
        });
        assertTrue(registry.getCommands().contains("mixedcase"));
        assertNotNull(registry.getCommand("mixedcase"));
        assertNotNull(registry.getCommand("MIXEDCASE"));
    }

    @Test
    void registerDuplicateThrows() {
        registry.registerCommand("dup", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }
        });
        assertThrows(IllegalStateException.class, () ->
                registry.registerCommand("dup", new CommandDispatcher() {
                    @Override
                    public ExecuteResponse onExecute(ExecuteRequest request) {
                        return null;
                    }
                }));
    }

    @Test
    void registerDuplicateThrowsForCommandObject() {
        Command cmd = new Command("dup", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }
        });
        registry.registerCommand(cmd);
        assertThrows(IllegalStateException.class, () -> registry.registerCommand(cmd));
    }

    @Test
    void unregisterRemovesCommand() {
        registry.registerCommand("byebye", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return new ExecuteResponse(true, "still here");
            }
        });
        registry.unregisterCommand("byebye");
        assertSame(ExecuteResponse.NOT_FOUND, registry.executeCommand("byebye",
                new ExecuteRequest(CommandSender.ADMIN, List.of())));
    }

    @Test
    void executeWrapsExceptionsAsException() {
        registry.registerCommand("boom", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                throw new RuntimeException("kaboom");
            }
        });
        ExecuteResponse response = registry.executeCommand("boom",
                new ExecuteRequest(CommandSender.ADMIN, List.of()));
        assertSame(ExecuteResponse.EXCEPTION, response);
    }

    @Test
    void completeWrapsExceptionsAsEmpty() {
        registry.registerCommand("boom", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }

            @Override
            public CompleteResponse onComplete(CompleteRequest request) {
                throw new RuntimeException("kaboom");
            }
        });
        CompleteResponse response = registry.completeCommand("boom",
                new CompleteRequest(CommandSender.ADMIN, List.of(), ""));
        assertSame(CompleteResponse.EMPTY, response);
    }

    @Test
    void getCommandsReturnsUnmodifiableView() {
        registry.registerCommand("foo", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return null;
            }
        });
        assertThrows(UnsupportedOperationException.class, () -> registry.getCommands().add("bar"));
    }

    @Test
    void executeRequestTokensReachDispatcher() {
        registry.registerCommand("echo", new CommandDispatcher() {
            @Override
            public ExecuteResponse onExecute(ExecuteRequest request) {
                return new ExecuteResponse(true, String.join(",", request.tokens()));
            }
        });
        ExecuteResponse response = registry.executeCommand("echo",
                new ExecuteRequest(CommandSender.ADMIN, List.of("a", "b", "c")));
        assertEquals("a,b,c", response.output());
    }
}