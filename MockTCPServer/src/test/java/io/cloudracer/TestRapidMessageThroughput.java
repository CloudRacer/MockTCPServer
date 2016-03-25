package io.cloudracer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestRapidMessageThroughput extends AbstractTestTools {

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    @Test(timeout = TestConstants.TEST_TIMEOUT_10_MINUTE)
    public void rapidMessageThroughput() throws Exception {
        final int totalServerRestarts = 1000;

        for (int i = 0; i < totalServerRestarts; i++) {
            final String message = String.format("Test %d%s", i, TestConstants.DEFAULT_TERMINATOR);

            assertEquals("Unexpected server response.", TestConstants.ACK, getClient().send(message).toString());
        }

        checkLogMonitorForUnexpectedMessages();
    }

}
