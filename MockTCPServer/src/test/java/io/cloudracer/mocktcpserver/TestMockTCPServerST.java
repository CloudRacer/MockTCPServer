package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;

public class TestMockTCPServerST extends AbstractTestTools {

    private TCPClient client;
    private MockTCPServer server;

    @Before
    public void setUp() throws Exception {
        resetLogMonitor();

        server = new MockTCPServer(TestConstants.MOCK_SERVER_PORT);
        client = new TCPClient(TestConstants.MOCK_SERVER_PORT);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        server.close();
    }

    @Test
    public void mockTCPServerACK() throws ClassNotFoundException, IOException {
        final String message = String.format("%s%s", "Test message!!", TestConstants.DEFAULT_TERMINATOR);

        assertEquals("Unexpected server response.", TestConstants.ACK, client.send(message).toString());

        checkLogMonitorForUnexpectedMessages();
    }

    @Test
    public void mockTCPServerACKWithCustomTerminator() throws ClassNotFoundException, IOException {
        final String message = String.format("%s%s", "Test message!!", TestConstants.CUSTOM_TERMINATOR);

        server.setTerminator(TestConstants.CUSTOM_TERMINATOR.getBytes());

        assertEquals("Unexpected server response.", TestConstants.ACK, client.send(message).toString());
    }
}