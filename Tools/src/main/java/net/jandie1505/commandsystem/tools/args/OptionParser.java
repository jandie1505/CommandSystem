package net.jandie1505.commandsystem.tools.args;

import net.jandie1505.commandsystem.core.data.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A class that can parse options from a command.
 */
public final class OptionParser {

    private OptionParser() {}

    /**
     * Option parser for {@link net.jandie1505.commandsystem.core.dispatch.CommandDispatcher}.
     * @param arguments args
     * @return {@link Result}
     */
    public static Result parse(@NotNull List<String> arguments) {
        List<String> args = new ArrayList<>(arguments);
        Map<@NotNull String, @NotNull String> options = new HashMap<>();

        Iterator<String> iterator = args.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();

            // Parse long options with their values
            if (arg.startsWith("--")) {
                arg = arg.substring(2);

                String[] split = arg.split("=", 2);
                options.put(split[0], split.length > 1 ? split[1] : "");

                iterator.remove();
                continue;
            }

            if (arg.startsWith("-")) {
                arg = arg.substring(1);

                for (char c : arg.toCharArray()) {
                    options.put(String.valueOf(c), "");
                }

                iterator.remove();
                continue;
            }

        }

        return new Result(Collections.unmodifiableList(args), options);
    }

    /**
     * Stores the result from parsing the options.
     * @param args arguments (without options)
     * @param options options
     */
    public record Result(List<String> args, Map<@NotNull String, @NotNull String> options) {

        /**
         * Checks if the specified option is set.
         * @param option option
         * @return option set
         */
        public boolean hasOption(@NotNull String... option) {

            for (String s : option) {
                if (this.options.containsKey(s)) {
                    return true;
                }
            }

            return false;
        }

    }

    /**
     * Tab-completer for {@link net.jandie1505.commandsystem.core.dispatch.CommandDispatcher}.
     * @param sender sender
     * @param args args
     * @param partial arg the user is currently typing
     * @param argsCompleter Completer that completes command arguments (not options).
     * @param uncompletedAvailableOptions options that have no own tab completer (options without a value, format: --my-option)
     * @param completedAvailableOptions options that have an own completer (options with a value, format: --my-option=value)
     * @return available values
     */
    public static List<String> complete(@NotNull CommandSender sender, @NotNull Result args, @Nullable String partial, @Nullable OptionCompleter argsCompleter, @Nullable Set<String> uncompletedAvailableOptions, @Nullable Map<@NotNull String, @NotNull OptionCompleter> completedAvailableOptions) {
        Set<String> currentOptions = new HashSet<>(args.options().keySet());

        List<String> suggestions = new ArrayList<>();

        // Complete available options without a completer
        if (uncompletedAvailableOptions != null) {

            for (String option : uncompletedAvailableOptions) {
                if (!currentOptions.contains(option)) {
                    suggestions.add("--" + option);
                }
            }

        }

        // Complete available options with a completer
        if (completedAvailableOptions != null) {

            for (String option : completedAvailableOptions.keySet()) {
                if (currentOptions.contains(option)) continue;

                OptionCompleter optionCompleter = completedAvailableOptions.get(option);
                if (optionCompleter != null) {

                    try {
                        List<String> optionCompletions = optionCompleter.onComplete(sender, args, partial);
                        if (optionCompletions != null && !optionCompletions.isEmpty()) {
                            for (String value : optionCompletions) {
                                suggestions.add("--" + option + "=" + value);
                            }
                        } else {
                            suggestions.add("--" + option + "=");
                        }
                    } catch (Exception e) {
                        suggestions.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }

                }

            }

        }

        // Complete arguments
        if (argsCompleter != null) {

            try {
                suggestions.addAll(argsCompleter.onComplete(sender, args, partial));
            } catch (Exception e) {
                suggestions.add(e.getClass().getSimpleName() + ": " + e.getMessage());
            }

        }

        // Filter
        if (partial != null) {
            suggestions.removeIf(suggestion -> !suggestion.startsWith(partial));
        }

        return suggestions;
    }

    /**
     * Tab-completer for the {@link #complete(CommandSender, Result, String, OptionCompleter, Set, Map)}  method.<br/>
     * Works as {@link net.jandie1505.commandsystem.core.dispatch.CommandDispatcher}'s complete method, but does not have the command and label arguments.
     */
    public interface OptionCompleter {

        /**
         * Called when an argument/option is completed.
         * @param sender sender
         * @param args args
         * @param partial arg the user is currently typing
         * @return available values
         */
        List<String> onComplete(@NotNull CommandSender sender, @NotNull Result args, @Nullable String partial);

    }

}
