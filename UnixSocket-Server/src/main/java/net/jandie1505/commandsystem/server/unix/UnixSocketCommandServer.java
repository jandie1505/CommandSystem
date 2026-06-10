package net.jandie1505.commandsystem.server.unix;

import net.jandie1505.commandsystem.core.data.CompleteResponse;
import net.jandie1505.commandsystem.core.data.ExecuteResponse;
import net.jandie1505.commandsystem.core.registry.CommandRegistry;
import net.jandie1505.commandsystem.server.protocol.CommandProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Can receive user commands using a unix socket.<br/>
 * How to use: echo '{"op":"execute","command":"test","tokens":["123"]}' | socat - UNIX-CONNECT:/tmp/stormflood-controlcenter.sock
 */
public class UnixSocketCommandServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnixSocketCommandServer.class);

    @NotNull private final Path socketPath;
    @NotNull private final CommandRegistry registry;
    @NotNull private final Set<SocketChannel> sockets;
    @UnknownNullability private ExecutorService pool;
    @UnknownNullability private volatile ServerSocketChannel serverSocket;
    @UnknownNullability private Thread serverThread;
    @UnknownNullability private volatile Path activeSocketPath;

    /**
     * Creates a new UnixSocketCommandServer.
     * @param socketPath socket path
     * @param registry command registry
     */
    public UnixSocketCommandServer(@NotNull Path socketPath, @NotNull CommandRegistry registry) {
        this.socketPath = socketPath.toAbsolutePath();
        this.registry = registry;
        this.sockets = ConcurrentHashMap.newKeySet();
    }

    // ----- TASKS -----

    /**
     * The server thread task which accepts new connections.
     */
    private void serverThreadTask() {
        while (!Thread.currentThread().isInterrupted() && this.serverSocket.isOpen()) {

            try {
                SocketChannel socket = this.serverSocket.accept();
                this.sockets.add(socket);
                this.pool.execute(() -> this.handleConnection(socket));
            } catch (IOException e) {
                LOGGER.error("Failed to accept connection.", e);
            }

        }
    }

    /**
     * The connection task which handles client connections accepted by {@link #serverThreadTask()}.
     * @param client client
     */
    private void handleConnection(@NotNull SocketChannel client) {

        Thread watchdog = Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(30));
                client.close();
            } catch (InterruptedException ignored) {
                // finished
            } catch (IOException e) {
                LOGGER.warn("Failed to close timed-out client.", e);
            }
        });

        try (
                client;
                var in = new BufferedInputStream(Channels.newInputStream(client));
                var out = new BufferedOutputStream(Channels.newOutputStream(client));
        ) {

            CommandProtocol.Response response;
            try {
                CommandProtocol.Request request = CommandProtocol.parseRequest(in, 64 * 1024);

                response = switch (request) {
                    case CommandProtocol.ExecuteCall e -> {
                        ExecuteResponse r =
                                registry.executeCommand(e.command(), e.payload());
                        yield new CommandProtocol.ExecuteResult(r.success(),
                                r.output());
                    }
                    case CommandProtocol.CompleteCall c -> {
                        CompleteResponse r =
                                registry.completeCommand(c.command(), c.payload());
                        yield new CommandProtocol.CompleteResult(r.completions());
                    }
                };
            } catch (CommandProtocol.ProtocolException e) {
                response = new CommandProtocol.ErrorResult(e.getErrorType(), e.getMessage());
            } catch (Exception e) {
                response = new CommandProtocol.ErrorResult(CommandProtocol.ErrorType.INVALID_JSON, "internal error");
            }

            CommandProtocol.writeResponse(out, response);
        } catch (Exception e) {
            LOGGER.error("Failed to process connection.", e);
        } finally {
            this.sockets.remove(client);
            watchdog.interrupt();
        }
    }

    // ----- START/STOP/STATUS -----

    /**
     * Starts the Command Server.
     * @throws IOException if the start has failed
     * @throws IllegalStateException if the server is not fully shut down before
     */
    public void start() throws IOException {
        if (!this.isShutdown()) throw new IllegalStateException("CommandServer must be shut down completely before it can be restarted.");
        LOGGER.debug("Starting Command Server on socket location {}.", this.socketPath);

        this.pool = Executors.newVirtualThreadPerTaskExecutor();

        try {
            this.activeSocketPath = this.bindToFreeSocketPath();
        } catch (Exception e) {
            this.pool.shutdownNow();
            throw e;
        }

        try {
            Files.setPosixFilePermissions(this.activeSocketPath, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) { /* Windows */ }

        this.serverThread = new Thread(this::serverThreadTask, "connection-acceptor-:" + this.activeSocketPath);
        this.serverThread.start();

        LOGGER.info("Command Server started on socket location {}.", this.activeSocketPath);
    }

    /**
     * Tries to bind the server socket to the configured path. If that path already exists on disk
     * or the bind fails, appends 0, 1, 2, ... to the configured path until a free path is found.
     * @return the path the server socket was bound to.
     * @throws IOException if no free socket path could be bound within {@link Integer#MAX_VALUE} attempts.
     */
    private @NotNull Path bindToFreeSocketPath() throws IOException {
        if (this.serverSocket != null && this.serverSocket.isOpen()) throw new IllegalStateException("Socket already in use.");

        IOException lastException = null;

        for (int i = -1; i < Integer.MAX_VALUE; i++) {
            Path candidate = (i < 0) ? this.socketPath : Path.of(this.socketPath.toString() + i);

            if (Files.exists(candidate)) continue;

            var channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            channel.bind(UnixDomainSocketAddress.of(candidate));
            this.serverSocket = channel;
            return candidate;
        }

        throw new IOException("Failed to find a free socket path after " + Integer.MAX_VALUE + " attempts.", lastException);
    }

    /**
     * Shuts down the Command Server.
     * @throws InterruptedException Thread has been interrupted while waiting for ExecutorService shutdown
     * @throws IllegalStateException On timeout while waiting for ExecutorService shutdown (should never happen, since the timeout is set to the MAX_LONG days.
     */
    public void shutdown() throws InterruptedException {
        LOGGER.debug("Shutting down Command Server on socket location {}.", this.activeSocketPath);

        // Close server socket
        if (this.serverSocket != null) {

            try {
                this.serverSocket.close();
                LOGGER.debug("Server socket of {} closed.", this.activeSocketPath);
            } catch (IOException e) {
                LOGGER.error("Failed to close server socket.", e);
            }

        }

        // Interrupt server thread
        if (this.serverThread != null) {
            this.serverThread.interrupt();
            this.serverThread.join();
            LOGGER.debug("Server thread of {} closed.", this.activeSocketPath);
        }

        // Close active client connections.
        this.closeActiveConnections();

        if (this.activeSocketPath != null) {
            try {
                Files.deleteIfExists(this.activeSocketPath);
                LOGGER.debug("Socket file of {} deleted.", this.activeSocketPath);
            } catch (IOException e) {
                LOGGER.error("Failed to delete socket file.", e);
            }
        }

        // Shutdown sender service
        if (this.pool != null) {

            this.pool.shutdownNow();
            if (!this.pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                LOGGER.debug("Timeout while waiting for sender shutdown of socket {}", this.activeSocketPath);
                throw new IllegalStateException("Timeout while waiting for ExecutorService to shutdown");
            }

        }

        this.activeSocketPath = null;
    }

    /**
     * Returns the path the server socket is currently bound to, or null if the server is not running.
     * This can differ from the configured socket path if a numeric suffix was appended to avoid collisions.
     * @return active socket path, or null
     */
    public @UnknownNullability Path getActiveSocketPath() {
        return this.activeSocketPath;
    }

    /**
     * Schedules a shutdown of the Command Server.
     * @return Future which completes when the server has been shut down.
     */
    public @NotNull CompletableFuture<Void> shutdownAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                this.shutdown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Closes all active client connections.
     */
    private void closeActiveConnections() {
        Iterator<SocketChannel> iterator = this.sockets.iterator();
        while (iterator.hasNext()) {

            SocketChannel socket = iterator.next();
            try {
                socket.close();
                LOGGER.debug("Connection {} of Socket {} closed.", socket.getRemoteAddress(), this.activeSocketPath);
            } catch (IOException e) {
                LOGGER.error("Failed to close client socket.", e);
            }

            iterator.remove();
        }
    }

    /**
     * Returns true if the server has completely shutdown.
     * @return server is shutdown
     */
    public boolean isShutdown() {
        boolean executorServiceDown = this.pool == null || (this.pool.isShutdown() && this.pool.isTerminated());
        boolean serverSocketDown = this.serverSocket == null || !this.serverSocket.isOpen();
        boolean serverThreadDown = this.serverThread == null || !this.serverThread.isAlive();
        boolean clientsShutdown = this.sockets.isEmpty();
        return executorServiceDown && serverSocketDown && serverThreadDown && clientsShutdown;
    }

}
