package net.jandie1505.commandsystem.demo;

import net.jandie1505.commandsystem.core.registry.CommandRegistry;
import net.jandie1505.commandsystem.demo.commands.EchoCommand;
import net.jandie1505.commandsystem.jline.JLineConsole;
import org.jetbrains.annotations.NotNull;
import org.jline.terminal.TerminalBuilder;

public class DemoApplication {
    private static DemoApplication instance;
    @NotNull private final CommandRegistry registry;
    @NotNull private final JLineConsole console;

    public DemoApplication() throws Exception {
        if (instance != null) throw new IllegalStateException("Already initialized.");
        instance = this;

        this.registry = new CommandRegistry();
        this.registry.registerCommand("echo", new EchoCommand());

        this.console = new JLineConsole(TerminalBuilder.builder().system(true).build(), this.registry, "> ", JLineConsole.UserInterruptAction.SHUTDOWN);
        this.console.redirectSystemStreams();
    }

    // ----- STATIC -----

    public static void main(String[] args) throws Exception {
        new DemoApplication();
    }

    public static DemoApplication getInstance() {
        return instance;
    }

}
