package net.jandie1505.commandsystem.core.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A command complete request.
 * @param sender command sender
 * @param tokens tokens (arguments)
 * @param partial token the user is currently typing
 */
public record CompleteRequest(@NotNull CommandSender sender, @NotNull List<String> tokens, @NotNull String partial) {}
