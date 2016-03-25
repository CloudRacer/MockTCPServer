package io.cloudracer;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestServerRestartReliability extends AbstractTestTools {

    private Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    @Test(timeout = TestConstants.TEST_TIMEOUT_10_MINUTE)
    public void serverRestart() throws Exception {
        final int totalServerRestarts = 1000;

        for (int i = 0; i < totalServerRestarts; i++) {
            logger.info(String.format("Restart itteration: %d", i));

            cleanUp();
            setUp();
        }

        checkLogMonitorForUnexpectedMessages();
    }
}
