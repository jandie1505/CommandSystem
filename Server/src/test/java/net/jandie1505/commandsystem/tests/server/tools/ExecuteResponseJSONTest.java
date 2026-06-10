package net.jandie1505.commandsystem.tests.server.tools;

import net.jandie1505.commandsystem.core.data.ExecuteResponse;
import net.jandie1505.commandsystem.server.tools.JSONDeserializer;
import net.jandie1505.commandsystem.server.tools.JSONSerializer;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteResponseJSONTest {

    @Test
    void predefinedConstantsAreFailures() {
        assertFalse(ExecuteResponse.NOT_FOUND.success());
        assertFalse(ExecuteResponse.NO_RESPONSE.success());
        assertFalse(ExecuteResponse.EXCEPTION.success());
        assertNotNull(ExecuteResponse.NOT_FOUND.output());
        assertNotNull(ExecuteResponse.NO_RESPONSE.output());
        assertNotNull(ExecuteResponse.EXCEPTION.output());
    }

}
