package net.jandie1505.commandsystem.jline;

import net.jandie1505.commandsystem.core.data.CommandSender;
import net.jandie1505.commandsystem.core.data.CompleteRequest;
import net.jandie1505.commandsystem.core.data.ExecuteRequest;
import net.jandie1505.commandsystem.core.registry.CommandRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * JLine console.
 */
public class JLineConsole {
    @NotNull private static final Logger LOGGER = LoggerFactory.getLogger(JLineConsole.class);

    @NotNull private final Terminal terminal;
    @NotNull private final LineReader reader;

    @NotNull private final CommandRegistry registry;

    @Nullable private final String prompt;
    @NotNull private final JLineConsole.UserInterruptAction userInterruptAction;

    @NotNull private final Thread thread;

    @Nullable private PrintStream backedUpSystemOut;
    @Nullable private PrintStream backedUpSystemErr;

    /**
     * Creates a new JLineConsole.
     * @param terminal terminal
     * @param registry command registry
     * @param prompt prompt (e.g. "> ...")
     * @param userInterruptAction specifies what happens when a keyboard interrupt is detected
     */
    public JLineConsole(@NotNull Terminal terminal, @NotNull CommandRegistry registry, @Nullable String prompt, @NotNull UserInterruptAction userInterruptAction) {
        this.terminal = Objects.requireNonNull(terminal);
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(this::complete)
                .build();
        this.registry = Objects.requireNonNull(registry);
        this.prompt = prompt;
        this.userInterruptAction = Objects.requireNonNull(userInterruptAction);

        this.backedUpSystemOut = null;
        this.backedUpSystemErr = null;

        this.thread = new Thread(this::task);
        this.thread.setName(this.getClass().getName() + "-" + System.identityHashCode(this));
        this.thread.start();
    }

    private void task() {

        try {

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    var input = reader.readLine(this.prompt);
                    if (input.isBlank()) continue;

                    var split = input.trim().split("\\s+");
                    if (split.length < 1) continue;

                    String command = split[0];
                    var args = Arrays.stream(Arrays.copyOfRange(split, 1, split.length)).toList();

                    var request = new ExecuteRequest(CommandSender.ADMIN, args);
                    var response = this.registry.executeCommand(command, request);

                    reader.printAbove(response.output());
                    LOGGER.debug("Executed command {} with arguments {}, success {} and response message {}.", command, args, response.success(), response.output());
                } catch (UserInterruptException e) {

                    switch (this.userInterruptAction) {
                        case SHUTDOWN -> {
                            LOGGER.info("KeyboardInterrupt detected. Shutting down...");
                            System.exit(130);
                            return;
                        }
                        case TERMINATE -> {
                            LOGGER.warn("KeyboardInterrupt detected. Terminating...");
                            Runtime.getRuntime().halt(130);
                            return;
                        }
                        case INFO -> LOGGER.info("KeyboardInterrupt detected.");
                        case IGNORE -> {}
                    }

                } catch (EndOfFileException e) {
                    LOGGER.warn("End of file.");
                    break;
                }

            }

        } finally {

            this.restoreOriginalSystemStreams();

            try {
                this.terminal.close();
            } catch (IOException e) {
                LOGGER.error("Error closing terminal.", e);
            }

        }

    }

    private void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {

        var words = line.words();
        if (words.isEmpty()) return;

        String command = words.getFirst();
        List<String> args = words.subList(1, line.wordIndex());
        var partial = line.word();

        var request = new CompleteRequest(CommandSender.ADMIN, args, partial);
        var response = this.registry.completeCommand(command, request);

        response.completions().forEach(completion -> {
            var candidate = new Candidate(completion);
            candidates.add(candidate);
        });
    }

    /**
     * Closes the console.
     */
    public void close() {
        this.thread.interrupt();

        try {
            this.terminal.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close terminal.", e);
        }

    }

    /**
     * Returns the terminal.
     * @return terminal
     */
    public @NotNull Terminal getTerminal() {
        return this.terminal;
    }

    /**
     * Prints a message on the console.
     * @param message message
     */
    public void print(@NotNull String message) {
        Objects.requireNonNull(message);
        this.reader.printAbove(message);
    }

    /**
     * Redirects {@link System#out} and {@link System#err} streams to the JLine console.<br/>
     * The original {@link System#out} and {@link System#err} streams are backed up and can be restored by using {@link #restoreOriginalSystemStreams()} or closing this object.
     * @throws IllegalArgumentException if the streams have already been backed up
     */
    public synchronized void redirectSystemStreams() {

        if (this.backedUpSystemOut != null || this.backedUpSystemErr != null) {
            throw new IllegalStateException("Streams have already been backed up.");
        }

        this.backedUpSystemOut = System.out;
        this.backedUpSystemErr = System.err;

        var out = new PrintStream(new LineOutputStream(this::print, StandardCharsets.UTF_8), true, StandardCharsets.UTF_8);
        System.setOut(out);
        System.setErr(out);
    }

    /**
     * Restores the original {@link System#out} and {@link System#err} streams, which will be backed up when calling {@link #redirectSystemStreams()}.<br/>
     * Does nothing when no backups exist (which happens when {@link #redirectSystemStreams()} was not called before).
     */
    public synchronized void restoreOriginalSystemStreams() {

        if (this.backedUpSystemOut != null) {
            System.setOut(this.backedUpSystemOut);
            this.backedUpSystemOut = null;
        }

        if (this.backedUpSystemErr != null) {
            System.setErr(this.backedUpSystemErr);
            this.backedUpSystemErr = null;
        }

    }

    // ----- INNER CLASSES -----

    /**
     * Specifies what is done when a keyboard interrupt is detected.
     */
    public enum UserInterruptAction {

        /**
         * Shuts down the application using {@link System#exit(int)}.
         */
        SHUTDOWN,

        /**
         * Terminates the application using {@link Runtime#halt(int)} (not recommended!)
         */
        TERMINATE,

        /**
         * Informs the user that a keyboard interrupt has been detected, but does nothing.
         */
        INFO,

        /**
         * Ignores the keyboard interrupt without doing anything.
         */
        IGNORE

    }

}
