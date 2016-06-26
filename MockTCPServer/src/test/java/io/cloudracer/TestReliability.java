package io.cloudracer;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Start and stop the Client and Server many times, in a tight, without sending a message.
 *
 * @author John McDonnell
 */
public class TestReliability extends AbstractTestTools {

    private final Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    private static final int TOTAL_SERVER_RESTARTS = 1000;

    @Override
    @Before
    public void setUp() throws IOException {
        this.getServer();
        this.getClient();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    /**
     * Start and stop the Client and Server many time, in a tight, without sending a message.
     *
     * @throws Exception
     */
    @Test(timeout = TestConstants.TEST_TIMEOUT_10_MINUTE)
    public void serverRestart() throws Exception {
        this.resetLogMonitor();

        for (int i = 0; i < TestReliability.TOTAL_SERVER_RESTARTS; i++) {
            this.logger.info(String.format("Restart itteration: %d", i));

            this.cleanUp();
            this.setUp();
        }

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Server accepts another connection after a disconnect, repeatedly disconnecting and then reconnecting.
     *
     * @throws IOException
     */
    @Test(timeout = TestConstants.TEST_TIMEOUT_5_MINUTE)
    public void clientReconnect() throws IOException {
        final int totalReconnects = 10;

        for (int i = 1; i <= totalReconnects; i++) {
            this.logger.info(String.format("Reconnect %d/%d", i, totalReconnects));

            this.getClient().connect();
            this.getClient().close();
        }

        this.checkLogMonitorForUnexpectedMessages();
    }
}
