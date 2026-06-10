package net.jandie1505.commandsystem.server.protocol;

import net.jandie1505.commandsystem.core.data.CommandSender;
import net.jandie1505.commandsystem.core.data.CompleteRequest;
import net.jandie1505.commandsystem.core.data.ExecuteRequest;
import net.jandie1505.commandsystem.server.tools.JSONDeserializer;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Command JSON protocol for client-server communication.
 */
public final class CommandProtocol {

    /**
     * Parses a command request.
     * @param in input stream
     * @param maxLineLength max line length
     * @return request
     * @throws IOException io error
     * @throws ProtocolException protocol error
     */
    public static Request parseRequest(InputStream in, int maxLineLength) throws IOException, ProtocolException {
        String line = readLine(in, maxLineLength);  // bis \n, mit Limit
        if (line == null) throw new ProtocolException(ErrorType.EOF, "no request");

        JSONObject json;
        try {
            json = new JSONObject(line);
        } catch (JSONException e) {
            throw new ProtocolException(ErrorType.INVALID_JSON, e.getMessage());
        }

        String op = json.optString("op", null);
        return switch (op) {
            case "execute" -> parseExecute(json);
            case "complete" -> parseComplete(json);
            case null -> throw new ProtocolException(ErrorType.MISSING_OP, "no 'op'");
            default -> throw new ProtocolException(ErrorType.UNKNOWN_OP, op);
        };
    }

    /**
     * Write command response.
     * @param out output stream
     * @param response response
     * @throws IOException io error
     */
    public static void writeResponse(OutputStream out, Response response) throws IOException {
        JSONObject json = switch (response) {
            case ExecuteResult r -> new JSONObject()
                    .put("type", "execute")
                    .put("success", r.success())
                    .put("output", r.output());
            case CompleteResult r -> new JSONObject()
                    .put("type", "complete")
                    .put("completions", r.completions());
            case ErrorResult r -> new JSONObject()
                    .put("type", "error")
                    .put("error", r.errorType().name())
                    .put("message", r.message());
        };

        out.write(json.toString().getBytes(StandardCharsets.UTF_8));
        out.write('\n');
        out.flush();
    }

    /**
     * Reads a line.
     * @param in input stream
     * @param maxLength max length
     * @return line that has been read
     * @throws IOException io error
     * @throws ProtocolException protocol error
     */
    private static String readLine(InputStream in, int maxLength) throws IOException, ProtocolException {
        var buf = new ByteArrayOutputStream();

        int b;
        while ((b = in.read()) != -1) {

            if (b == '\n') {
                byte[] bytes = buf.toByteArray();
                int len = bytes.length;
                if (len > 0 && bytes[len - 1] == '\r') len--;
                return new String(bytes, 0, len, StandardCharsets.UTF_8);
            }

            if (buf.size() > maxLength) {
                throw new ProtocolException(ErrorType.LINE_TOO_LONG, "Line too long");
            }

            buf.write(b);
        }
        return buf.size() == 0 ? null : buf.toString(StandardCharsets.UTF_8);
    }

    /**
     * Parse execute call.
     * @param json execute call JSON
     * @return execute request
     * @throws ProtocolException protocol error
     */
    private static @NotNull Request parseExecute(JSONObject json) throws ProtocolException {
        return new ExecuteCall(json.getString("command"), JSONDeserializer.deserializeExecuteRequest(CommandSender.ADMIN, json));
    }

    /**
     * Parse complete call.
     * @param json complete call JSON-
     * @return complete request
     * @throws ProtocolException protocol error
     */
    private static @NotNull Request parseComplete(JSONObject json) throws ProtocolException {
        @NotNull String cmdStr = json.optString("command", "");
        return new CompleteCall(cmdStr.isEmpty() ? null : cmdStr , JSONDeserializer.deserializeCompleteRequest(CommandSender.ADMIN, json));
    }

    // --- REQUEST ---

    /**
     * Complete request.
     */
    public sealed interface Request {}

    /**
     * Execute call.
     * @param command command
     * @param payload execute request payload
     */
    public record ExecuteCall(String command, ExecuteRequest payload) implements Request {}

    /**
     * Complete call.
     * @param command command
     * @param payload complete request payload
     */
    public record CompleteCall(String command, CompleteRequest payload) implements Request {}

    // --- RESPONSE ---

    /**
     * Response.
     */
    public sealed interface Response {}

    /**
     * Execute result.
     * @param success success
     * @param output output
     */
    public record ExecuteResult(boolean success, String output) implements Response {}

    /**
     * Complete result.
     * @param completions completions
     */
    public record CompleteResult(List<String> completions) implements Response {}

    /**
     * Error result.
     * @param errorType error type
     * @param message error message
     */
    public record ErrorResult(ErrorType errorType, String message) implements Response {}

    // --- ERROR ---

    /**
     * Error type.
     */
    public enum ErrorType {

        /**
         * End of file reached.
         */
        EOF,

        /**
         * JSON is invalid.
         */
        INVALID_JSON,

        /**
         * 'op' missing
         */
        MISSING_OP,

        /**
         * 'op' unknown
         */
        UNKNOWN_OP,

        /**
         * invalid field
         */
        INVALID_FIELD,

        /**
         * line too long (line limit exceeded)
         */
        LINE_TOO_LONG
    }

    /**
     * Protocol exception.
     */
    public static class ProtocolException extends Exception {

        /**
         * Error type.
         */
        private final ErrorType errorType;

        /**
         * Creates a new protocol exception.
         * @param errorType error type
         * @param message message
         */
        public ProtocolException(ErrorType errorType, String message) {
            super(message);
            this.errorType = errorType;
        }

        /**
         * Returns the error type.
         * @return error type
         */
        public ErrorType getErrorType() {
            return errorType;
        }

    }

    // --- PRIVATE CONSTRUCTOR ---

    private CommandProtocol() {}

}

