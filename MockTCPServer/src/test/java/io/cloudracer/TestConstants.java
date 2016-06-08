/*
 * 
 */
package io.cloudracer;

public abstract class TestConstants {

    public static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_NAME = "mocktcpserver.configuration.initialisation.enabled";
    public static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_VALUE_TRUE = "true";
    protected static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_VALUE_FALSE = "false";

    public static final String MOCKTCPSERVER_XML_FULL_PATH_SUFFIX = "/MockTCPServer/configuration/mocktcpserver.xml";
    protected static final String MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME = "target/classes/mocktcpserver.xml";
    public static final String MOCKTCPSERVER_XML_FULL_RESOURCE_PATH_SUFFIX = "/MockTCPServer/" + MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME;

    protected static final int TEST_TIMEOUT_5_MINUTE = 300000;
    protected static final int TEST_TIMEOUT_10_MINUTE = TEST_TIMEOUT_5_MINUTE * 2;

    protected static final int CLIENT_PORT = 6000;
    public static final int MOCK_SERVER_PORT = 6789;
    public static final int MOCK_SERVER_NEW_PORT = 1111;

    private static final byte[] ACK = { 65 }; // Default ACK.
    private static final byte[] NAK = { 78 }; // Default NAK.
    protected static final long SERVER_CLEANUP_WAIT_DURATION = 10000;

    // Test terminators.
    public static final String DEFAULT_TERMINATOR = "\r\n\n";
    protected static final String CUSTOM_TERMINATOR = "xyz";
    protected static final String INVALID_TERMINATOR = "INVALID_TERMINATOR";
    // Test XML.
    public static final String WELLFORMED_XML = "<test-root><test-element></test-element></test-root>";
    protected static final String MALFORMED_XML = "<test-root><test-element><test-element></test-root>";
    // Test well-formed XML.
    public static final String WELLFORMED_XML_WITH_VALID_TERMINATOR = WELLFORMED_XML + DEFAULT_TERMINATOR;
    protected static final String WELLFORMED_XML_WITH_INVALID_TERMINATOR = WELLFORMED_XML + INVALID_TERMINATOR;
    // Test malformed XML.
    protected static final String MALFORMED_XML_WITH_VALID_TERMINATOR = MALFORMED_XML + DEFAULT_TERMINATOR;
    protected static final String MALFORMED_XML_WITH_INVALID_TERMINATOR = MALFORMED_XML + INVALID_TERMINATOR;

    private TestConstants() {
        // Do nothing. This class cannot be instantiated.
    }

    public static byte[] getAck() {
        return ACK.clone();
    }

    public static byte[] getNak() {
        return NAK.clone();
    }
}