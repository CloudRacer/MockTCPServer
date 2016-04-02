package io.cloudracer;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Start and stop the Client and Server, in a tight, without sending a message {@link TestReliability#TOTAL_SERVER_RESTARTS many} times.
 *
 * @author John McDonnell
 */
public class TestReliability extends AbstractTestTools {

    private final Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    private final static int TOTAL_SERVER_RESTARTS = 1000;

    @Override
    @Before
    public void setUp() throws IOException {
        getServer();
        getClient();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    /**
     * Start and stop the Client and Server, in a tight, without sending a message {@link TestReliability#TOTAL_SERVER_RESTARTS many} times.
     *
     * @throws Exception
     */
    @Test(timeout = TestConstants.TEST_TIMEOUT_10_MINUTE)
    public void serverRestart() throws Exception {
        resetLogMonitor();

        for (int i = 0; i < TestReliability.TOTAL_SERVER_RESTARTS; i++) {
            this.logger.info(String.format("Restart itteration: %d", i));

            cleanUp();
            setUp();
        }

        checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Server accepts another connection after a disconnect, repeatedly disconnecting and then reconnecting.
     * 
     * @throws IOException
     */
    @Test(timeout = TEST_TIMEOUT_5_MINUTE)
    public void clientReconnect() throws IOException {
        final int totalReconnects = 10;

        for (int i = 1; i <= totalReconnects; i++) {
            logger.info(String.format("Reconnect %d/%d", i, totalReconnects));

            getClient().connect();
            getClient().close();
        }

        this.checkLogMonitorForUnexpectedMessages();
    }
}
