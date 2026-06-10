package net.jandie1505.commandsystem.core.data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A command execution request.
 * @param sender command sender
 * @param tokens command arguments
 */
public record ExecuteRequest(@NotNull CommandSender sender, @NotNull List<@NotNull String> tokens) {}
