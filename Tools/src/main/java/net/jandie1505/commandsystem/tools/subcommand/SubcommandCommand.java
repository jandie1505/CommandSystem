package net.jandie1505.commandsystem.tools.subcommand;

import net.jandie1505.commandsystem.core.data.*;
import net.jandie1505.commandsystem.core.dispatch.CommandDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A command that can has multiple commands as subcommands.<br/>
 * You can add subcommands with {@link #addSubcommand(String, SubcommandEntry)}.<br/>
 * You can remove subcommands with {@link #removeSubcommand(String)}.<br/>
 * You can set a global permission for the command in the constructor<br/>
 * You can set a {@link DynamicSubcommandProvider} that can provide subcommands dynamically in the constructor.<br/>
 * You can override {@link #onExecutionWithUnknownSubcommand(ExecuteRequest)} to run code when the command is executed without subcommands.<br/>
 * You can override {@link #onExecutionWithoutPermission(ExecuteRequest, String)} to run code when the sender does not have the permission for the command.<br/>
 * You can override {@link #onExecutionWithUnknownSubcommand(ExecuteRequest)} to run code when the given subcommand does not exist.
 */
public class SubcommandCommand implements CommandDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubcommandCommand.class);
    @NotNull private final Map<String, SubcommandEntry> entries;
    @Nullable private final DynamicSubcommandProvider dynamicSubcommandProvider;
    @Nullable private final PermissionProvider permissionProvider;
    @Nullable private final CommandDispatcher noSubcommandExecutor;
    @Nullable private final CommandDispatcher noPermissionExecutor;
    @Nullable private final CommandDispatcher unknownSubcommandExecutor;

    /**
     * Creates a new SubcommandCommand.
     * @param permissionProvider permission provider
     * @param dynamicSubcommandProvider dynamic subcommand provider
     * @param noSubcommandExecutor This is executed when there is no subcommand provided
     * @param noPermissionExecutor This is executed when the player has no permission.
     * @param unknownSubcommandExecutor This is executed when the player specified an invalid subcommand.
     */
    public SubcommandCommand(
            @Nullable PermissionProvider permissionProvider,
            @Nullable DynamicSubcommandProvider dynamicSubcommandProvider,
            @Nullable CommandDispatcher noSubcommandExecutor,
            @Nullable CommandDispatcher noPermissionExecutor,
            @Nullable CommandDispatcher unknownSubcommandExecutor
    ) {
        this.entries = new HashMap<>();
        this.dynamicSubcommandProvider = dynamicSubcommandProvider;
        this.permissionProvider = permissionProvider;
        this.noSubcommandExecutor = noSubcommandExecutor;
        this.noPermissionExecutor = noPermissionExecutor;
        this.unknownSubcommandExecutor = unknownSubcommandExecutor;
    }

    /**
     * Creates a new SubcommandCommand.
     * @param permissionProvider permission provider
     * @param noSubcommandExecutor This is executed when there is no subcommand provided
     * @param noPermissionExecutor This is executed when the player has no permission.
     * @param unknownSubcommandExecutor This is executed when the player specified an invalid subcommand.
     */
    public SubcommandCommand(
            @Nullable PermissionProvider permissionProvider,
            @Nullable CommandDispatcher noSubcommandExecutor,
            @Nullable CommandDispatcher noPermissionExecutor,
            @Nullable CommandDispatcher unknownSubcommandExecutor
    ) {
        this(permissionProvider, null, noSubcommandExecutor, noPermissionExecutor, unknownSubcommandExecutor);
    }

    /**
     * Creates a new SubcommandCommand.
     * @param dynamicSubcommandProvider dynamic subcommand provider
     * @param noSubcommandExecutor This is executed when there is no subcommand provided
     * @param noPermissionExecutor This is executed when the player has no permission.
     * @param unknownSubcommandExecutor This is executed when the player specified an invalid subcommand.
     */
    public SubcommandCommand(
            DynamicSubcommandProvider dynamicSubcommandProvider,
            @Nullable CommandDispatcher noSubcommandExecutor,
            @Nullable CommandDispatcher noPermissionExecutor,
            @Nullable CommandDispatcher unknownSubcommandExecutor
    ) {
        this(null, dynamicSubcommandProvider, noSubcommandExecutor, noPermissionExecutor, unknownSubcommandExecutor);
    }

    /**
     * Creates a new SubcommandCommand.
     * @param permissionProvider permission provider
     * @param dynamicSubcommandProvider dynamic subcommand provider
     */
    public SubcommandCommand(@Nullable PermissionProvider permissionProvider, @Nullable DynamicSubcommandProvider dynamicSubcommandProvider) {
        this(permissionProvider, dynamicSubcommandProvider, null, null, null);
    }

    /**
     * Creates a new SubcommandCommand.
     * @param permissionProvider permission provider
     */
    public SubcommandCommand(@NotNull PermissionProvider permissionProvider) {
        this(permissionProvider, null);
    }

    /**
     * Creates a new SubcommandCommand.
     */
    public SubcommandCommand() {
        this((PermissionProvider) null, null);
    }

    /**
     * Creates a new SubcommandCommand.
     * @param permission permission
     * @param dynamicSubcommandProvider dynamic subcommand provider
     */
    public SubcommandCommand(@Nullable String permission, @Nullable DynamicSubcommandProvider dynamicSubcommandProvider) {
        this(permission != null ? sender -> sender.hasPermission(permission) : null, dynamicSubcommandProvider);
    }

    /**
     * Creates a new SubcommandCommand.
     * @param permission permission
     */
    public SubcommandCommand(@Nullable String permission) {
        this(permission, null);
    }

    // COMMAND

    @Override
    public final @Nullable ExecuteResponse onExecute(@NotNull ExecuteRequest request) {

        if (!this.hasPermission(request.sender())) {
            return this.onExecutionWithoutPermission(request, null);
        }

        if (request.tokens().size() > 0) {

            SubcommandEntry subcommand = this.getSubcommand(request.tokens().get(0));
            if (subcommand == null) {
                return this.onExecutionWithUnknownSubcommand(request);
            }

            if (!this.hasCommandPermission(subcommand, request.sender())) {
                return this.onExecutionWithoutPermission(request, request.tokens().get(0));
            }

            return subcommand.dispatcher().onExecute(new ExecuteRequest(
                    request.sender(),
                    this.subcommandArguments(request.tokens())
            ));
        } else {
            return this.onExecutionWithoutSubcommand(request);
        }

    }

    @Override
    public final @Nullable CompleteResponse onComplete(@NotNull CompleteRequest request) {

        if (!this.hasPermission(request.sender())) {
            return CompleteResponse.EMPTY;
        }

        if (!request.tokens().isEmpty()) {

            SubcommandEntry subcommand = this.getSubcommand(request.tokens().get(0));
            if (subcommand == null) return CompleteResponse.EMPTY;
            if (!this.hasCommandPermission(subcommand, request.sender())) return CompleteResponse.EMPTY;

            return subcommand.dispatcher().onComplete(new CompleteRequest(
                    request.sender(),
                    this.subcommandArguments(request.tokens()),
                    request.partial()
            ));
        } else {
            return new CompleteResponse(this.getSubcommandList(request.sender()));
        }

    }

    // ----- NO SUBCOMMAND ACTION -----

    /**
     * This is executed when no subcommand is specified.<br/>
     * This method is not called when a noSubcommandExecutor is specified.
     * @param request response
     * @return response
     */
    protected @Nullable ExecuteResponse onExecutionWithoutSubcommand(@NotNull ExecuteRequest request) {

        String message = "Available subcommands: ";

        Iterator<String> iterator = this.getSubcommandList(request.sender()).iterator();
        while (iterator.hasNext()) {
            String subcommand = iterator.next();

            message = message + subcommand;

            if (iterator.hasNext()) {
                message = message + ", ";
            }

        }

        return new ExecuteResponse(true, message);
    }

    /**
     * This is executed when the sender has no permission for the command.<br/>
     * This method is not called when a noPermissionExecutor is specified.
     * @param request request
     * @param subcommand the subcommand the sender has executed (null if it is the main command)
     * @return response
     */
    protected @Nullable ExecuteResponse onExecutionWithoutPermission(@NotNull ExecuteRequest request, @Nullable String subcommand) {
        return this.noPermissionExecutor != null ? this.noPermissionExecutor.onExecute(new ExecuteRequest(request.sender(), subcommand != null ? List.of(subcommand) : List.of())) : new ExecuteResponse(false, "No permission.");
    }

    /**
     * This is executed when the specified subcommand does not exist.<br/>
     * This method is not executed when a unknownSubcommandExecutor is specified.
     * @param request request
     * @return response
     */
    protected @Nullable ExecuteResponse onExecutionWithUnknownSubcommand(@NotNull ExecuteRequest request) {
        return this.unknownSubcommandExecutor != null ? this.unknownSubcommandExecutor.onExecute(request) : new ExecuteResponse(false, "Unknown subcommand");
    }

    // ----- UTILITIES -----

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasPermission(CommandSender sender) {
        if (this.permissionProvider == null) return true;

        try {
            return this.permissionProvider.hasPermission(sender);
        } catch (Exception e) {
            LOGGER.warn("Failed to check command permission: Exception in permission provider", e);
            return false;
        }
    }

    private List<String> subcommandArguments(List<String> args) {

        if (args.size() < 2) {
            return new ArrayList<>();
        }

        return new ArrayList<>(args.subList(1, args.size()));
    }

    private boolean hasCommandPermission(SubcommandEntry command, CommandSender sender) {
        if (command.permission() == null) return true;

        try {
            return command.permission().hasPermission(sender);
        } catch (Exception e) {
            LOGGER.warn("Failed to check command permission: Exception in permission provider", e);
            return false;
        }
    }

    private SubcommandEntry getSubcommand(String name) {

        SubcommandEntry entry = this.entries.get(name);

        if (entry != null) return entry; // Return if entry was found

        if (this.dynamicSubcommandProvider != null) {

            try {
                entry = this.dynamicSubcommandProvider.getDynamicSubcommands().get(name);
            } catch (Exception e) {
                LOGGER.warn("Exception in dynamic subcommand provider", e);
            }

        }

        return entry;
    }

    private List<String> getSubcommandList(CommandSender sender) {

        if (sender == null) return List.of();

        List<String> commandList = new ArrayList<>();

        for (String command : List.copyOf(this.entries.keySet())) {
            SubcommandEntry entry = this.entries.get(command);
            if (entry == null) continue;

            if (this.hasCommandPermission(entry, sender)) {
                commandList.add(command);
            }

        }

        if (this.dynamicSubcommandProvider != null) {
            Map<String, SubcommandEntry> entries = Map.copyOf(this.dynamicSubcommandProvider.getDynamicSubcommands());
            for (String command : entries.keySet()) {
                SubcommandEntry entry = entries.get(command);
                if (entry == null) continue;

                if (this.hasCommandPermission(entry, sender)) {
                    commandList.add(command);
                }

            }
        }

        return commandList;

    }

    // ----- MANAGE SUBCOMMANDS -----

    /**
     * Adds a subcommand.
     * @param command command string
     * @param data command data
     * @throws IllegalArgumentException when subcommand already exists
     */
    public final void addSubcommand(String command, SubcommandEntry data) {
        if (this.entries.containsKey(command)) throw new IllegalArgumentException("Duplicate subcommand");
        this.entries.put(command, data);
    }

    /**
     * Removes a subcommand.
     * @param command command string
     */
    public final void removeSubcommand(String command) {
        this.entries.remove(command);
    }

    /**
     * Clears all subcommands.
     */
    public final void clearSubcommands() {
        this.entries.clear();
    }

    // ----- GETTER / SETTER -----

    /**
     * Returns a map of all registered subcommands.
     * @return map of subcommands
     */
    @NotNull
    public final Map<String, SubcommandEntry> getSubcommands() {
        return Map.copyOf(this.entries);
    }

    /**
     * Returns the dynamic subcommand provider.
     * Read {@link DynamicSubcommandProvider} for more information.
     * @return dynamic subcommand provider
     */
    @Nullable
    public final DynamicSubcommandProvider getDynamicSubcommandProvider() {
        return this.dynamicSubcommandProvider;
    }

    /**
     * Returns the sender that is called when no subcommand was specified.
     * @return command sender
     */
    @Nullable
    public final CommandDispatcher getNoSubcommandExecutor() {
        return this.noSubcommandExecutor;
    }

    /**
     * Returns the sender that is called when the sender does not have the permission to run the command.
     * @return command sender
     */
    @Nullable
    public final CommandDispatcher getNoPermissionExecutor() {
        return this.noPermissionExecutor;
    }

    /**
     * Returns the sender that is called when the specified subcommand does not exist.
     * @return command sender
     */
    @Nullable
    public final CommandDispatcher getUnknownSubcommandExecutor() {
        return this.unknownSubcommandExecutor;
    }

    // ----- INNER CLASSES -----

    /**
     * Provides the permission for the command.<br/>
     * This allows for dynamically providing a permission instead of the permission string.
     */
    public interface PermissionProvider {

        /**
         * Returns true if the specified sender has the permission
         * @param sender sender
         * @return permission
         */
        boolean hasPermission(@NotNull CommandSender sender);

    }

}
