package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;
import io.cloudracer.mocktcpserver.datastream.DataStream;
import io.cloudracer.mocktcpserver.responses.ResponseDAO;

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class TestMockTCPServerSendResponsesST extends AbstractTestTools {

    private static final int ONE_TENTH_OF_A_SECOND = 100;
    private static final int ONE_SECOND = ONE_TENTH_OF_A_SECOND * 10;
    private static final int TIMEOUT = ONE_SECOND * 10;
    private static final int SERVER_TIMEOUT = ONE_SECOND * 5;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    /**
     * Test that responses a sent.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws ConfigurationException
     */
    @Test(timeout = TIMEOUT)
    public void sendResponses() throws IOException, InterruptedException, ConfigurationException {
        final List<String> actualMessages = new ArrayList<>();
        final List<String> expectedMessages = new ArrayList<>(Arrays.asList(StringEscapeUtils.unescapeJava(TestConstants.MACHINE_A_RESPONSE_MESSAGE), StringEscapeUtils.unescapeJava(TestConstants.MACHINE_B_RESPONSE_MESSAGE)));
        final List<ResponseDAO> expectedResponses = Arrays.asList(
                new ResponseDAO("localhost", TestConstants.MOCK_SERVER_PORT_1234, TestConstants.MACHINE_A_RESPONSE_MESSAGE),
                new ResponseDAO("localhost", TestConstants.MOCK_SERVER_PORT_1234, TestConstants.MACHINE_B_RESPONSE_MESSAGE));

        final MockTCPServer mockTCPServer = new MockTCPServer(TestConstants.MOCK_SERVER_PORT_1234) {

            @Override
            public void onMessage(DataStream message) {
                actualMessages.add(message.toString());

                super.onMessage(message);

                if (actualMessages.size() == expectedMessages.size()) {
                    setIsCloseAfterNextResponse(true);
                }
            }
        };
        assertArrayEquals(TestConstants.getAck(), this.getClient().send(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR).toByteArray());
        this.getClient().close();

        for (int i = 0; i < SERVER_TIMEOUT; i = i + (ONE_TENTH_OF_A_SECOND)) {
            mockTCPServer.join(ONE_TENTH_OF_A_SECOND);

            if (!mockTCPServer.isAlive()) {
                break;
            }
            if (i == (SERVER_TIMEOUT - ONE_TENTH_OF_A_SECOND)) {
                System.out.println(String.format("Timed out waiting for the Server listening to port %d to close.", mockTCPServer.getPort()));
            }
        }

        assertEquals(expectedResponses, getServer().getResponsesSent());

        mockTCPServer.close();

        assertEquals(expectedMessages, actualMessages);

        this.checkLogMonitorForUnexpectedMessages();
    }
}