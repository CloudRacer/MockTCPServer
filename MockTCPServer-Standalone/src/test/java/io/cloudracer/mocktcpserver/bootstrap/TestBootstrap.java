package io.cloudracer.mocktcpserver.bootstrap;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;
import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;

/**
 * Bootstrap multiple servers using details from the configuration file.
 *
 * @author John McDonnell
 */
public class TestBootstrap extends AbstractTestTools {

    /**
     * Bootstrap multiple servers using details from the configuration file.
     *
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     * @throws IOException error while sending responses
     */
    @Test(timeout = TestConstants.TEST_TIMEOUT_5_MINUTE)
    public void startup() throws InterruptedException, ConfigurationException, IOException { // NOSONAR
        final Runnable runnable = new Runnable() { // NOSONAR

            @Override
            public void run() {
                try {
                    String[] createPool = { "-1" };
                    io.cloudracer.mocktcpserver.standalone.Bootstrap.main(createPool);
                } catch (ConfigurationException | InterruptedException | IOException e) { // NOSONAR
                    e.printStackTrace();
                }
            }
        };
        final Thread bootstrap = new Thread(runnable);
        bootstrap.start();
        bootstrap.join(TestConstants.TWO_SECONDS);

        try (final MockTCPServer mockTCPServer1234 = new MockTCPServer(TestConstants.MOCK_SERVER_PORT_1234); // NOSONAR
                final MockTCPServer mockTCPServer6789 = new MockTCPServer(TestConstants.MOCK_SERVER_PORT_6789); // NOSONAR
                final MockTCPServer mockTCPServer2345 = new MockTCPServer(TestConstants.MOCK_SERVER_PORT_2345); // NOSONAR
                final MockTCPServer mockTCPServer5678 = new MockTCPServer(TestConstants.MOCK_SERVER_PORT_5678); // NOSONAR
                final TCPClient client1234 = new TCPClient(TestConstants.MOCK_SERVER_PORT_1234);
                final TCPClient client6789 = new TCPClient(TestConstants.MOCK_SERVER_PORT_6789);) {
            assertArrayEquals(TestConstants.getAck(), client1234.send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());
            assertArrayEquals(TestConstants.getAck(), client6789.send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());
        }

        checkLogMonitorForUnexpectedMessages();
    }
}