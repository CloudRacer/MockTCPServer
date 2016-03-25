package io.cloudracer;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;

public class TestRapidMessageThroughput extends AbstractTestTools {

    private TCPClient client;
    private MockTCPServer server;

    @Before
    public void setUp() throws Exception {
        resetLogMonitor();

        server = new MockTCPServer(TestConstants.MOCK_SERVER_PORT);
        client = new TCPClient(TestConstants.MOCK_SERVER_PORT);
    }

    @After
    public void cleanUp() throws Exception {
        client.close();
        server.close();
    }

    @Test(timeout = TestConstants.TEST_TIMEOUT_10_MINUTE)
    public void rapidMessageThroughput() throws Exception {
        final int totalServerRestarts = 1000;

        for (int i = 0; i < totalServerRestarts; i++) {
            final String message = String.format("Test %d%s", i, TestConstants.DEFAULT_TERMINATOR);

            assertEquals("Unexpected server response.", TestConstants.ACK, client.send(message).toString());
        }

        checkLogMonitorForUnexpectedMessages();
    }

}
