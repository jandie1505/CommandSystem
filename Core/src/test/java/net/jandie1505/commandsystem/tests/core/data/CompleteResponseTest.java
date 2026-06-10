package net.jandie1505.commandsystem.tests.core.data;

import net.jandie1505.commandsystem.core.data.CompleteResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteResponseTest {

    @Test
    void emptyConstantHasNoCompletions() {
        assertTrue(CompleteResponse.EMPTY.completions().isEmpty());
    }

}