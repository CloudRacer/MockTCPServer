/*
 *
 */
package io.cloudracer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.cloudracer.mocktcpserver.responses.ResponseDAO;
import io.cloudracer.mocktcpserver.responses.Responses;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;

/**
 * Predefined values used by the test routines.
 *
 * @author John McDonnell
 *
 */
public abstract class TestConstants {

    /**
     * True.
     */
    public static final String TRUE = "true";
    protected static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_VALUE_FALSE = "false";

    /**
     * System property to indicate if a default configuration file to disk if one does not already exist.
     */
    public static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_NAME = "mocktcpserver.configuration.initialisation.enabled"; // NOSONAR
    /**
     *
     */

    public static final String MOCKTCPSERVER_XML_FULL_PATH_SUFFIX = "/MockTCPServer/configuration/mocktcpserver.xml";
    protected static final String MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME = "target/test-classes/mocktcpserver.xml";
    /**
     * Configuration filename.
     */
    public static final String MOCKTCPSERVER_XML_FULL_RESOURCE_PATH_SUFFIX = "/MockTCPServer/" + MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME;

    /**
     * Duration of 1/10th of one second, expressed in milliseconds.
     */
    public static final int ONE_TENTH_OF_A_SECOND = 100;
    /**
     * Duration of 1 second, expressed in milliseconds.
     */
    public static final int ONE_SECOND = ONE_TENTH_OF_A_SECOND * 10;
    /**
     * Duration of 2 seconds, expressed in milliseconds.
     */
    public static final int TWO_SECONDS = ONE_SECOND * 2;
    /**
     * Duration of 5 seconds, expressed in milliseconds.
     */
    public static final int FIVE_SECONDS = ONE_SECOND * 5;
    /**
     * Duration of 10 seconds, expressed in milliseconds.
     */
    public static final int TEN_SECONDS = ONE_SECOND * 10;
    /**
     * Duration of 1 minute, expressed in milliseconds.
     */
    public static final int ONE_MINUTE = ONE_SECOND * 60;
    /**
     * Duration of 1/10th of one second, expressed in milliseconds.
     */
    public static final int SERVER_TIMEOUT = (ONE_MINUTE * 2) + (ONE_SECOND * 30);
    /**
     * Duration of 5 minutes, expressed in milliseconds.
     */
    public static final int TEST_TIMEOUT_5_MINUTE = ONE_MINUTE * 5;
    protected static final int TEST_TIMEOUT_10_MINUTE = TEST_TIMEOUT_5_MINUTE * 2;

    protected static final int CLIENT_PORT = 6000;
    /**
     * Server port 6789.
     */
    public static final int MOCK_SERVER_PORT_6789 = 6789;
    /**
     * Server port 1234.
     */
    public static final int MOCK_SERVER_PORT_1234 = 1234;
    /**
     * Server port 5678.
     */
    public static final int MOCK_SERVER_PORT_5678 = 5678;
    /**
     * Server port 2345.
     */
    public static final int MOCK_SERVER_PORT_2345 = 2345;
    /**
     * Server port 1111.
     */
    public static final int MOCK_SERVER_PORT_1111 = 1111;
    /**
     * Server port 2222.
     */
    public static final int MOCK_SERVER_PORT_2222 = 2222;
    /**
     * Default set of configured ports.
     */
    public static final Set<Integer> PORT_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MOCK_SERVER_PORT_1234, MOCK_SERVER_PORT_6789, MOCK_SERVER_PORT_1111)));

    private static final byte[] ACK = { 65 }; // Default ACK.
    private static final byte[] NAK = { 78 }; // Default NAK.
    protected static final long SERVER_CLEANUP_WAIT_DURATION = 10000;

    /**
     * Test terminators.
     */
    public static final String DEFAULT_TERMINATOR = "\r\n\n";
    protected static final String CUSTOM_TERMINATOR = "xyz";
    protected static final String INVALID_TERMINATOR = "INVALID_TERMINATOR";
    /**
     * Test XML.
     */
    public static final String WELLFORMED_XML = "<test-root><test-element></test-element></test-root>";
    protected static final String MALFORMED_XML = "<test-root><test-element><test-element></test-root>";
    /**
     * Test well-formed XML.
     */
    public static final String WELLFORMED_XML_WITH_VALID_TERMINATOR = WELLFORMED_XML + DEFAULT_TERMINATOR;
    protected static final String WELLFORMED_XML_WITH_INVALID_TERMINATOR = WELLFORMED_XML + INVALID_TERMINATOR;
    // Test malformed XML.
    protected static final String MALFORMED_XML_WITH_VALID_TERMINATOR = MALFORMED_XML + DEFAULT_TERMINATOR;
    protected static final String MALFORMED_XML_WITH_INVALID_TERMINATOR = MALFORMED_XML + INVALID_TERMINATOR;
    // Responses
    private static final String TCPCLIENT_TEMPLATE = "%s [hostName=%s, port=%s]";
    private static final String RESPONSEDAO_TEMPLATE = "%s [machineName=%s, port=%s, response=%s]";
    /**
     * Generic Machine Name of the local machine.
     */
    public static final String MACHINE_LOCALHOST_NAME = "localhost";
    private static final int MACHINE_LOCALHOST_PORT = 5678;
    /**
     * Incoming message.
     */
    public static final String INCOMING_MESSAGE_ONE = "Incoming Message One";
    /**
     * Machine name.
     */
    public static final String MACHINE_A_NAME = "destinationA";
    /**
     * Port number.
     */
    public static final int MACHINE_A_PORT = 1234;
    /**
     * Port number to responded to.
     */
    public static final int MACHINE_A_RESPONSE_PORT = 2345;
    /**
     * Response message.
     */
    public static final String MACHINE_A_RESPONSE_MESSAGE = "Response to destinationA\\u000d\\u000a\\u000a";
    /**
     * Machine name.
     */
    public static final String MACHINE_B_NAME = "destinationB";
    /**
     * Port number.
     */
    public static final int MACHINE_B_PORT = 6789;
    /**
     * Port number to responded to.
     */
    public static final int MACHINE_B_RESPONSE_PORT = 5678;
    /**
     * Response message.
     */
    public static final String MACHINE_B_RESPONSE_MESSAGE = "Response to destinationB\\u000d\\u000a\\u000a";
    /**
     * Client definition.
     */
    public static final String EXPECTED_CLIENT_A = String.format(TCPCLIENT_TEMPLATE, TCPClient.class.getSimpleName(), MACHINE_A_NAME, MACHINE_A_PORT);
    /**
     * Client definition.
     */
    public static final String EXPECTED_CLIENT_B = String.format(TCPCLIENT_TEMPLATE, TCPClient.class.getSimpleName(), MACHINE_B_NAME, MACHINE_B_PORT);
    /**
     * Client definition.
     */
    public static final String EXPECTED_CLIENT_LOCALHOST = String.format(TCPCLIENT_TEMPLATE, TCPClient.class.getSimpleName(), MACHINE_LOCALHOST_NAME, MACHINE_LOCALHOST_PORT);
    /**
     * Client list for the listener to port 6789.
     */
    public static final String EXPECTED_CLIENT_LIST_FOR_PORT_6789 = String.format("{%s=[%s]}", WELLFORMED_XML, EXPECTED_CLIENT_LOCALHOST);
    /**
     * Response DAO.
     */
    public static final String EXPECTED_MACHINE_A_RESPONSE_DAO_RESULT = String.format(RESPONSEDAO_TEMPLATE, ResponseDAO.class.getSimpleName(), MACHINE_A_NAME, MACHINE_A_PORT, MACHINE_A_RESPONSE_MESSAGE);
    /**
     * Response DAO.
     */
    public static final String EXPECTED_MACHINE_B_RESPONSE_DAO_RESULT = String.format(RESPONSEDAO_TEMPLATE, ResponseDAO.class.getSimpleName(), MACHINE_B_NAME, MACHINE_B_PORT, MACHINE_B_RESPONSE_MESSAGE);
    /**
     * Response DAO.
     */
    public static final String EXPECTED_LOCALHOST_A_RESPONSE_DAO_RESULT = String.format(RESPONSEDAO_TEMPLATE, ResponseDAO.class.getSimpleName(), MACHINE_LOCALHOST_NAME, MACHINE_B_RESPONSE_PORT, MACHINE_A_RESPONSE_MESSAGE);
    /**
     * Response DAO.
     */
    public static final String EXPECTED_LOCALHOST_B_RESPONSE_DAO_RESULT = String.format(RESPONSEDAO_TEMPLATE, ResponseDAO.class.getSimpleName(), MACHINE_LOCALHOST_NAME, MACHINE_B_RESPONSE_PORT, MACHINE_B_RESPONSE_MESSAGE);
    /**
     * Incoming message responses.
     */
    public static final String EXPECTED_INCOMING_MESSAGE_ONE_RESPONSES_RESULT = String.format("%s [messageResponses={%s=[%s, %s]}]", Responses.class.getSimpleName(), INCOMING_MESSAGE_ONE, EXPECTED_MACHINE_A_RESPONSE_DAO_RESULT, EXPECTED_MACHINE_B_RESPONSE_DAO_RESULT);

    /**
     * Incoming message responses for port 6789.
     */
    public static final String EXPECTED_INCOMING_MESSAGE_RESPONSES_RESULT_FOR_PORT_6789 = String.format("%s [messageResponses={%s=[%s, %s]}]", Responses.class.getSimpleName(), WELLFORMED_XML, EXPECTED_LOCALHOST_A_RESPONSE_DAO_RESULT, EXPECTED_LOCALHOST_B_RESPONSE_DAO_RESULT);

    private TestConstants() {
        // Do nothing. This class cannot be instantiated.
    }

    /**
     * Acknowledgement.
     *
     * @return acknowledgement
     */
    public static byte[] getAck() {
        return ACK.clone();
    }

    /**
     * Not acknowledged.
     *
     * @return not acknowledged
     */
    public static byte[] getNak() {
        return NAK.clone();
    }
}