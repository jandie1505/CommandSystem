package net.jandie1505.commandsystem.server.tools;

import net.jandie1505.commandsystem.core.data.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON deserializer.
 */
public final class JSONDeserializer {

    /**
     * Deserializes an execute request.
     * @param executor command executor
     * @param jsonObject request json
     * @return execute request
     */
    public static @NotNull ExecuteRequest deserializeExecuteRequest(@NotNull CommandSender executor, @NotNull JSONObject jsonObject) {
        List<String> tokens = new ArrayList<>();
        JSONArray tokensArray = jsonObject.getJSONArray("tokens");
        tokensArray.forEach(o -> tokens.add(o.toString()));
        return new ExecuteRequest(executor, tokens);
    }

    /**
     * Deserializes an execute response.
     * @param jsonObject response json
     * @return execute response
     */
    public static @NotNull ExecuteResponse deserializeExecuteResponse(@NotNull JSONObject jsonObject) {
        return new ExecuteResponse(jsonObject.getBoolean("success"), jsonObject.getString("output"));
    }

    /**
     * Deserializes a complete request.
     * @param executor command executor
     * @param jsonObject request json
     * @return complete request
     */
    public static @NotNull CompleteRequest deserializeCompleteRequest(@NotNull CommandSender executor, @NotNull JSONObject jsonObject) {
        List<String> tokens = new ArrayList<>();
        JSONArray tokensArray = jsonObject.optJSONArray("tokens", new JSONArray());
        tokensArray.forEach(o -> tokens.add(o.toString()));
        return new CompleteRequest(executor, tokens, jsonObject.optString("partial", ""));
    }

    /**
     * Deserializes an complete response
     * @param jsonObject response json
     * @return complete response
     */
    public static @NotNull CompleteResponse deserializeCompleteResponse(@NotNull JSONObject jsonObject) {
        JSONArray completions = jsonObject.getJSONArray("completions");
        List<String> completionsList = new ArrayList<>();
        completions.forEach(completion -> completionsList.add(completion.toString()));
        return new CompleteResponse(completionsList);
    }

    private JSONDeserializer() {}

}
