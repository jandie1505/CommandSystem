package net.jandie1505.commandsystem.server.tools;

import net.jandie1505.commandsystem.core.data.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * JSON serializer.
 */
public final class JSONSerializer {

    /**
     * Serializes an execute request.
     * @param request request
     * @return request json
     */
    public static @NotNull JSONObject serialize(@NotNull ExecuteRequest request) {
        JSONObject json = new JSONObject();
        JSONArray tokens = new JSONArray();
        request.tokens().forEach(tokens::put);
        json.put("tokens", tokens);
        return json;
    }

    /**
     * Serializes an execute response.
     * @param response response
     * @return response json
     */
    public static @NotNull JSONObject serialize(@NotNull ExecuteResponse response) {
        return new JSONObject()
                .put("success", response.success())
                .put("output", response.output());
    }

    /**
     * Serializes a complete request.
     * @param request request
     * @return request json
     */
    public static @NotNull JSONObject serialize(@NotNull CompleteRequest request) {
        JSONObject jsonObject = new JSONObject();
        JSONArray tokensArray = new JSONArray();
        request.tokens().forEach(tokensArray::put);
        jsonObject.put("tokens", tokensArray);
        jsonObject.put("partial", request.partial());
        return jsonObject;
    }

    /**
     * Serializes a complete response.
     * @param response response
     * @return response json
     */
    public static @NotNull JSONObject serialize(@NotNull CompleteResponse response) {
        JSONObject jsonObject = new JSONObject();
        JSONArray completions = new JSONArray();
        response.completions().forEach(completions::put);
        jsonObject.put("completions", completions);
        return jsonObject;
    }

    private JSONSerializer() {}

}
