package io.cloudracer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;

public class TestServerRestartReliability extends AbstractTestTools {

    private Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    private TCPClient client;
    private MockTCPServer server;

    @Before
    public void setUp() throws Exception {
        server = new MockTCPServer(TestConstants.MOCK_SERVER_PORT);
        client = new TCPClient(TestConstants.MOCK_SERVER_PORT);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        server.close();
    }

    @Test(timeout = TestConstants.TEST_TIMEOUT_10_MINUTE)
    public void serverRestart() throws Exception {
        final int totalServerRestarts = 1000;

        for (int i = 0; i < totalServerRestarts; i++) {
            logger.info(String.format("Restart itteration: %d", i));

            tearDown();
            setUp();
        }

        checkForUnexpectedLogMessages();
    }
}
