import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.TestConstants;
import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.tcpclient.TCPClient;

/**
 * ================================================================================
 *
 * Project: Procter and Gamble - Skelmersdale.
 *
 * $HeadURL$
 *
 * $Author$
 *
 * $Revision$
 *
 * $Date$
 *
 * $Log$
 *
 * ============================== (c) Swisslog(UK) Ltd, 2005 ======================
 */
public class TestRobustness {

    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

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

    @Test
    public void serverRestart() throws Exception {
        final int totalServerRestarts = 1000;

        for (int i = 0; i < totalServerRestarts; i++) {
            logger.info(String.format("Restart itteration: %d", i));

            client.close();
            server.close();
            server = new MockTCPServer(TestConstants.MOCK_SERVER_PORT);
            client = new TCPClient(TestConstants.MOCK_SERVER_PORT);
        }
    }

}