package net.jandie1505.commandsystem.tests.core.data;

import net.jandie1505.commandsystem.core.data.ExecuteResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExecuteResponseTest {

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
