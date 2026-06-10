package net.jandie1505.commandsystem.tests.server.tools;

import net.jandie1505.commandsystem.core.data.CommandSender;
import net.jandie1505.commandsystem.core.data.ExecuteRequest;
import net.jandie1505.commandsystem.server.tools.JSONDeserializer;
import net.jandie1505.commandsystem.server.tools.JSONSerializer;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExecuteRequestJSONTest {

    @Test
    void toJsonContainsTokens() {
        ExecuteRequest request = new ExecuteRequest(CommandSender.ADMIN, List.of("a", "b", "c"));
        JSONObject json = JSONSerializer.serialize(request);
        assertEquals(3, json.getJSONArray("tokens").length());
        assertEquals("a", json.getJSONArray("tokens").getString(0));
        assertEquals("b", json.getJSONArray("tokens").getString(1));
        assertEquals("c", json.getJSONArray("tokens").getString(2));
    }

    @Test
    void toJsonWithEmptyTokens() {
        ExecuteRequest request = new ExecuteRequest(CommandSender.ADMIN, List.of());
        JSONObject json = JSONSerializer.serialize(request);
        assertEquals(0, json.getJSONArray("tokens").length());
    }

    @Test
    void fromJsonRestoresTokens() {
        JSONObject json = new JSONObject().put("tokens", List.of("foo", "bar"));
        ExecuteRequest request = JSONDeserializer.deserializeExecuteRequest(CommandSender.ADMIN, json);
        assertEquals(List.of("foo", "bar"), request.tokens());
        assertSame(CommandSender.ADMIN, request.sender());
    }

    @Test
    void jsonRoundTrip() {
        ExecuteRequest original = new ExecuteRequest(CommandSender.ADMIN, List.of("token1", "token2", ""));
        ExecuteRequest restored = JSONDeserializer.deserializeExecuteRequest(CommandSender.ADMIN, JSONSerializer.serialize(original));
        assertEquals(original.tokens(), restored.tokens());
    }

}
