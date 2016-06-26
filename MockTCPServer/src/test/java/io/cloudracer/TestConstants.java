/*
 *
 */
package io.cloudracer;

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

    protected static final int TEST_TIMEOUT_5_MINUTE = 300000;
    protected static final int TEST_TIMEOUT_10_MINUTE = TEST_TIMEOUT_5_MINUTE * 2;

    protected static final int CLIENT_PORT = 6000;
    /**
     * Default server port.
     */
    public static final int MOCK_SERVER_PORT = 6789;
    /**
     * New server port.
     */
    public static final int MOCK_SERVER_NEW_PORT = 1111;

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
    private static final String MACHINE_LOCALHOST_NAME = "localhost";
    private static final int MACHINE_LOCALHOST_PORT = 1234;
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
    public static final int MACHINE_B_PORT = 1234;
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
     * Client list.
     */
    public static final String EXPECTED_CLIENT_LIST = String.format("{%s=[%s, %s], %s=[%s]}", INCOMING_MESSAGE_ONE, EXPECTED_CLIENT_A, EXPECTED_CLIENT_B, WELLFORMED_XML, EXPECTED_CLIENT_LOCALHOST);
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
    public static final String EXPECTED_LOCALHOST_A_RESPONSE_DAO_RESULT = String.format(RESPONSEDAO_TEMPLATE, ResponseDAO.class.getSimpleName(), MACHINE_LOCALHOST_NAME, MACHINE_A_PORT, MACHINE_A_RESPONSE_MESSAGE);
    /**
     * Response DAO.
     */
    public static final String EXPECTED_LOCALHOST_B_RESPONSE_DAO_RESULT = String.format(RESPONSEDAO_TEMPLATE, ResponseDAO.class.getSimpleName(), MACHINE_LOCALHOST_NAME, MACHINE_B_PORT, MACHINE_B_RESPONSE_MESSAGE);
    /**
     * Incoming message responses.
     */
    public static final String EXPECTED_INCOMING_MESSAGE_ONE_RESPONSES_RESULT = String.format("%s [messageResponses={%s=[%s, %s]}]", Responses.class.getSimpleName(), INCOMING_MESSAGE_ONE, EXPECTED_MACHINE_A_RESPONSE_DAO_RESULT, EXPECTED_MACHINE_B_RESPONSE_DAO_RESULT);
    /**
     * Incoming message responses.
     */
    public static final String EXPECTED_INCOMING_ALL_MESSAGE_RESPONSES_RESULT = String.format("%s [messageResponses={%s=[%s, %s], %s=[%s, %s]}]", Responses.class.getSimpleName(), INCOMING_MESSAGE_ONE, EXPECTED_MACHINE_A_RESPONSE_DAO_RESULT, EXPECTED_MACHINE_B_RESPONSE_DAO_RESULT, WELLFORMED_XML, EXPECTED_LOCALHOST_A_RESPONSE_DAO_RESULT, EXPECTED_LOCALHOST_B_RESPONSE_DAO_RESULT);

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