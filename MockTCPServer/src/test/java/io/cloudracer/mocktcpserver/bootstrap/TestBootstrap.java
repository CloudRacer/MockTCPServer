package io.cloudracer.mocktcpserver.bootstrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;
import io.cloudracer.mocktcpserver.responses.ResponseDAO;

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
    public void startup() throws ConfigurationException, InterruptedException, IOException {
        final Bootstrap bootstrap = new Bootstrap();

        bootstrap.startup();

        final List<String> expectedMessages = new ArrayList<>(Arrays.asList(StringEscapeUtils.unescapeJava(TestConstants.MACHINE_A_RESPONSE_MESSAGE), StringEscapeUtils.unescapeJava(TestConstants.MACHINE_B_RESPONSE_MESSAGE)));

        List<ResponseDAO> expectedResponses = Arrays.asList(
                new ResponseDAO(TestConstants.MACHINE_LOCALHOST_NAME, TestConstants.MOCK_SERVER_PORT_2345, TestConstants.MACHINE_A_RESPONSE_MESSAGE),
                new ResponseDAO(TestConstants.MACHINE_LOCALHOST_NAME, TestConstants.MOCK_SERVER_PORT_2345, TestConstants.MACHINE_B_RESPONSE_MESSAGE));
        testResponses(bootstrap.getServerPool().get(TestConstants.MOCK_SERVER_PORT_1234), TestConstants.MOCK_SERVER_PORT_2345, TestConstants.INCOMING_MESSAGE_ONE + TestConstants.DEFAULT_TERMINATOR, expectedResponses, expectedMessages, TestConstants.SERVER_TIMEOUT, TestConstants.ONE_TENTH_OF_A_SECOND);
        checkLogMonitorForUnexpectedMessages();
        expectedResponses = Arrays.asList(
                new ResponseDAO(TestConstants.MACHINE_LOCALHOST_NAME, TestConstants.MOCK_SERVER_PORT_5678, TestConstants.MACHINE_A_RESPONSE_MESSAGE),
                new ResponseDAO(TestConstants.MACHINE_LOCALHOST_NAME, TestConstants.MOCK_SERVER_PORT_5678, TestConstants.MACHINE_B_RESPONSE_MESSAGE));
        testResponses(bootstrap.getServerPool().get(TestConstants.MOCK_SERVER_PORT_6789), TestConstants.MOCK_SERVER_PORT_5678, TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR, expectedResponses, expectedMessages, TestConstants.SERVER_TIMEOUT, TestConstants.ONE_TENTH_OF_A_SECOND);
        checkLogMonitorForUnexpectedMessages();

        bootstrap.shutdown();
    }
}
