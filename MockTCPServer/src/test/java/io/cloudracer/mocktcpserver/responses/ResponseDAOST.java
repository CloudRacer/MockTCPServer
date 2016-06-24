package io.cloudracer.mocktcpserver.responses;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class ResponseDAOST extends AbstractTestTools {

    private static final int TIMEOUT = 10000;

    // Responses [messageResponses={received message=[ResponseDAO [machineName=testMachineName, port=1357, response=Hello World!!]]}]
    @Override
    @Before
    public void setUp() throws IOException {
        resetLogMonitor();
    }

    /**
     * Create a {@link ResponseDAO} object.
     */
    @Test(timeout = TIMEOUT)
    public void createResponseDAO() {
        final ResponseDAO responseDAO = new ResponseDAO(TestConstants.MACHINE_A_NAME, TestConstants.MACHINE_A_PORT, TestConstants.MACHINE_A_RESPONSE_MESSAGE);

        assertEquals(TestConstants.EXPECTED_MACHINE_A_RESPONSE_DAO_RESULT, responseDAO.toString());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * Create a {@link Responses} object and populate it with a list of {@link ResponseDAO} objects.
     */
    @Test(timeout = TIMEOUT)
    public void createResponses() {
        final Responses responses = new Responses();

        ResponseDAO responseDAO = new ResponseDAO(TestConstants.MACHINE_A_NAME, TestConstants.MACHINE_A_PORT, TestConstants.MACHINE_A_RESPONSE_MESSAGE);
        responses.add(TestConstants.INCOMING_MESSAGE_ONE, responseDAO);
        responseDAO = new ResponseDAO(TestConstants.MACHINE_B_NAME, TestConstants.MACHINE_B_PORT, TestConstants.MACHINE_B_RESPONSE_MESSAGE);
        responses.add(TestConstants.INCOMING_MESSAGE_ONE, responseDAO);

        assertEquals(TestConstants.EXPECTED_INCOMING_MESSAGE_ONE_RESPONSES_RESULT, responses.toString());

        this.checkLogMonitorForUnexpectedMessages();
    }
}