package net.jandie1505.commandsystem.tests.server.tools;

import net.jandie1505.commandsystem.core.data.CommandSender;
import net.jandie1505.commandsystem.core.data.CompleteRequest;
import net.jandie1505.commandsystem.server.tools.JSONDeserializer;
import net.jandie1505.commandsystem.server.tools.JSONSerializer;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CompleteRequestJSONTest {

    @Test
    void toJsonContainsTokensAndPartial() {
        CompleteRequest request = new CompleteRequest(CommandSender.ADMIN, List.of("foo"), "ba");
        JSONObject json = JSONSerializer.serialize(request);
        assertEquals("ba", json.getString("partial"));
        assertEquals(1, json.getJSONArray("tokens").length());
        assertEquals("foo", json.getJSONArray("tokens").getString(0));
    }

    @Test
    void fromJsonRestoresFields() {
        JSONObject json = new JSONObject()
                .put("tokens", List.of("a", "b"))
                .put("partial", "c");
        CompleteRequest request = JSONDeserializer.deserializeCompleteRequest(CommandSender.ADMIN, json);
        assertEquals(List.of("a", "b"), request.tokens());
        assertEquals("c", request.partial());
        assertSame(CommandSender.ADMIN, request.sender());
    }

    @Test
    void jsonRoundTrip() {
        CompleteRequest original = new CompleteRequest(CommandSender.ADMIN, List.of("sub", "command"), "par");
        CompleteRequest restored = JSONDeserializer.deserializeCompleteRequest(CommandSender.ADMIN, JSONSerializer.serialize(original));
        assertEquals(original.tokens(), restored.tokens());
        assertEquals(original.partial(), restored.partial());
    }

    @Test
    void emptyPartialAllowed() {
        CompleteRequest request = new CompleteRequest(CommandSender.ADMIN, List.of(), "");
        JSONObject json = JSONSerializer.serialize(request);
        assertEquals("", json.getString("partial"));
    }
}
