package net.jandie1505.commandsystem.tests.server.tools;

import net.jandie1505.commandsystem.core.data.CompleteResponse;
import net.jandie1505.commandsystem.server.tools.JSONDeserializer;
import net.jandie1505.commandsystem.server.tools.JSONSerializer;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteResponseJSONTest {

    @Test
    void toJsonContainsCompletions() {
        CompleteResponse response = new CompleteResponse(List.of("foo", "bar"));
        JSONObject json = JSONSerializer.serialize(response);
        assertEquals(2, json.getJSONArray("completions").length());
        assertEquals("foo", json.getJSONArray("completions").getString(0));
    }

    @Test
    void fromJsonRestoresCompletions() {
        JSONObject json = new JSONObject().put("completions", List.of("x", "y"));
        CompleteResponse response = JSONDeserializer.deserializeCompleteResponse(json);
        assertEquals(List.of("x", "y"), response.completions());
    }

    @Test
    void jsonRoundTrip() {
        CompleteResponse original = new CompleteResponse(List.of("a", "b", "c"));
        CompleteResponse restored = JSONDeserializer.deserializeCompleteResponse(JSONSerializer.serialize(original));
        assertEquals(original.completions(), restored.completions());
    }

}
