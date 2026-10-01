package net.jandie1505.commandsystem.jline;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Output stream for redirecting system streams to the {@link JLineConsole}.
 */
public class LineOutputStream extends OutputStream {
    @NotNull private final Consumer<String> lineConsumer;
    @NotNull private final Charset charset;
    @NotNull private final ByteArrayOutputStream buffer;
    private boolean closed;

    /**
     * Creates a new LineOutputStream.
     * @param lineConsumer line consumer
     * @param charset charset
     */
    public LineOutputStream(@NotNull Consumer<String> lineConsumer, @NotNull Charset charset) {
        this.lineConsumer = Objects.requireNonNull(lineConsumer);
        this.charset = Objects.requireNonNull(charset);
        this.buffer = new ByteArrayOutputStream();
        this.closed = false;
    }

    /**
     * Creates a new LineOutputStream using {@link StandardCharsets#UTF_8} charset.
     * @param lineConsumer line consumer
     */
    public LineOutputStream(@NotNull Consumer<String> lineConsumer) {
        this(lineConsumer, StandardCharsets.UTF_8);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (this.closed) return;

        if (b == '\n') {
            this.emitLine();
        } else {
            this.buffer.write(b);
        }

    }

    @Override
    public synchronized void write(byte @NotNull [] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (this.closed) return;

        int start = off;
        int end = off + len;

        for (int i = off; i < end; i++) {
            if (b[i] == '\n') {
                this.buffer.write(b, start, i - start);
                this.emitLine();
                start = i + 1;
            }
        }

        this.buffer.write(b, start, end - start);
    }

    @Override
    public void flush() {}

    @Override
    public synchronized void close() {
        if (this.closed) return;
        if (this.buffer.size() > 0) this.emitLine();
        this.closed = true;
    }

    private void emitLine() {
        var line = this.buffer.toString(this.charset);
        this.buffer.reset();
        if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);

        this.lineConsumer.accept(line);
    }

}
