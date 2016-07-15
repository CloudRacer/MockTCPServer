package io.cloudracer.mocktcpserver;

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
import io.cloudracer.mocktcpserver.responses.ResponseDAO;

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class TestMockTCPServerSendResponsesST extends AbstractTestTools {

    @Override
    @Before
    public void setUp() throws IOException, ConfigurationException, InterruptedException {
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
    @Test(timeout = TestConstants.TEST_TIMEOUT_5_MINUTE)
    public void sendResponses() throws IOException, InterruptedException, ConfigurationException {
        final List<ResponseDAO> expectedResponses = Arrays.asList(
                new ResponseDAO("localhost", TestConstants.MOCK_SERVER_PORT_5678, TestConstants.MACHINE_A_RESPONSE_MESSAGE),
                new ResponseDAO("localhost", TestConstants.MOCK_SERVER_PORT_5678, TestConstants.MACHINE_B_RESPONSE_MESSAGE));
        final List<String> expectedMessages = new ArrayList<>(Arrays.asList(StringEscapeUtils.unescapeJava(TestConstants.MACHINE_A_RESPONSE_MESSAGE), StringEscapeUtils.unescapeJava(TestConstants.MACHINE_B_RESPONSE_MESSAGE)));

        testResponses(getServer(), TestConstants.MOCK_SERVER_PORT_5678, TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR, expectedResponses, expectedMessages, TestConstants.SERVER_TIMEOUT, TestConstants.ONE_TENTH_OF_A_SECOND);

        this.checkLogMonitorForUnexpectedMessages();
    }
}