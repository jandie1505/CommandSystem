package net.jandie1505.commandsystem.core.registry;

import net.jandie1505.commandsystem.core.data.*;
import net.jandie1505.commandsystem.core.dispatch.Command;
import net.jandie1505.commandsystem.core.dispatch.CommandDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for user commands.
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);
    @NotNull private final Map<String, Command> commands;

    /**
     * Creates a new command registry.
     */
    public CommandRegistry() {
        this.commands = new ConcurrentHashMap<>();
    }

    // ----- EXECUTE / COMPLETE -----

    /**
     * Executes the specified command.<br/>
     * If the command is not found, a "command not found" message is returned.<br/>
     * If the command throws an exception, it is logged and an "exception during command execution" message is returned.
     * @param command command name
     * @param request execute request
     * @return execute response
     */
    public @NotNull ExecuteResponse executeCommand(@NotNull String command, @NotNull ExecuteRequest request) {
        Command cmd = this.commands.get(command.toLowerCase());

        if (cmd == null) {
            return ExecuteResponse.NOT_FOUND;
        }

        try {
            return cmd.execute(request);
        } catch (Throwable t) {
            LOGGER.error("Error while executing command {}", command, t);
            return ExecuteResponse.EXCEPTION;
        }

    }

    /**
     * Completes the specified command.<br/>
     * If the command is not found, an empty complete response is sent.<br/>
     * If the command throws an exception, it is logged and an empty complete response is sent.
     * @param command command name
     * @param request complete request
     * @return complete response
     */
    public @NotNull CompleteResponse completeCommand(@Nullable String command, @NotNull CompleteRequest request) {

        if (command != null) {

            Command cmd = this.commands.get(command.toLowerCase());

            if (cmd == null) {
                return CompleteResponse.EMPTY;
            }

            try {
                return cmd.complete(request);
            } catch (Throwable t) {
                LOGGER.error("Error while executing command {}", command, t);
                return CompleteResponse.EMPTY;
            }

        } else {
            List<String> completions = new ArrayList<>();
            this.getCommands().forEach(cmd -> {
                if (cmd.startsWith(request.partial())) completions.add(cmd);
            });
            return new CompleteResponse(completions);
        }

    }

    // ----- MANAGEMENT -----

    /**
     * Returns an unmodifiable set of all registered commands.
     * @return set of command names
     */
    public @NotNull Set<String> getCommands() {
        return Collections.unmodifiableSet(this.commands.keySet());
    }

    /**
     * Returns the specified command if it exists.
     * @param command command name
     * @return command or null
     */
    public @NotNull Command getCommand(@NotNull String command) {
        return this.commands.get(command.toLowerCase());
    }

    /**
     * Registers a new command by command object.<br/>
     * Not recommended, use {@link #registerCommand(String, CommandDispatcher)} instead.
     * @param command command
     */
    public void registerCommand(@NotNull Command command) {
        if (this.commands.containsKey(command.getName())) throw new IllegalStateException("Command already exists!");
        this.commands.put(command.getName(), command);
    }

    /**
     * Registers a new command by name and dispatcher.
     * @param name command name
     * @param dispatcher command dispatcher
     */
    public void registerCommand(@NotNull String name, @NotNull CommandDispatcher dispatcher) {
        name = name.toLowerCase();
        if (this.commands.containsKey(name)) throw new IllegalStateException("Command already exists!");

        Command cmd = new Command(name, dispatcher);
        this.commands.put(name, cmd);
    }

    /**
     * Unregisters a command.
     * @param name command name
     */
    public void unregisterCommand(@NotNull String name) {
        this.commands.remove(name);
        this.commands.remove(name.toLowerCase());
    }

}
