package io.cloudracer;

abstract class TestConstants {

    protected static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_NAME = "mocktcpserver.configuration.initialisation.enabled";
    protected static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_VALUE_TRUE = "true";
    protected static final String CONFIGURATION_INITIALISATION_ENABLED_PROPERTY_VALUE_FALSE = "false";

    protected static final String MOCKTCPSERVER_XML_FULL_PATH_SUFFIX = "/MockTCPServer/MockTCPServer/configuration/mocktcpserver.xml";
    protected static final String MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME = "target/classes/mocktcpserver.xml";
    protected static final String MOCKTCPSERVER_XML_FULL_RESOURCE_PATH_SUFFIX = "/MockTCPServer/MockTCPServer/" + MOCKTCPSERVER_XML_RESOURCE_TARGET_FILE_NAME;

    protected static final int TEST_TIMEOUT_5_MINUTE = 300000;
    protected static final int TEST_TIMEOUT_10_MINUTE = TEST_TIMEOUT_5_MINUTE * 2;

    protected final static int CLIENT_PORT = 6000;
    protected final static int MOCK_SERVER_PORT = 6789;
    protected final static int MOCK_SERVER_NEW_PORT = 1111;

    protected final static byte[] ACK = { 65 }; // Default ACK.
    protected final static byte[] NAK = { 78 }; // Default NAK.
    protected final static long SERVER_CLEANUP_WAIT_DURATION = 10000;

    protected final static String HLM_RECVQTAB_QUEUE_NAME = "HLM_RECV";
    protected final static String HLM_RECVQTAB_TABLE_NAME = String.format("%sQTAB", HLM_RECVQTAB_QUEUE_NAME);
    protected final static String HLM_SENDQTAB_QUEUE_NAME = "HLM_SEND";
    protected final static String HLM_SENDQTAB_TABLE_NAME = String.format("%sQTAB", HLM_SENDQTAB_QUEUE_NAME);
    protected final static String HOST_CLIENTQTAB_QUEUE_NAME = "HOST_CLIENT";
    protected final static String HOST_CLIENTQTAB_TABLE_NAME = String.format("%sQTAB", HOST_CLIENTQTAB_QUEUE_NAME);

    // Am Container Module names.
    protected final static String HOST_CLIENT_MODULE_NAME = "HOST_CLIENT";

    // Test terminators.
    protected final static String DEFAULT_TERMINATOR = "\r\n\n";
    protected final static String CUSTOM_TERMINATOR = "xyz";
    protected final static String INVALID_TERMINATOR = "INVALID_TERMINATOR";
    // Test XML.
    protected final static String WELLFORMED_XML = "<test-root><test-element></test-element></test-root>";
    protected final static String MALFORMED_XML = "<test-root><test-element><test-element></test-root>";
    // Test well-formed XML.
    protected final static String WELLFORMED_XML_WITH_VALID_TERMINATOR = WELLFORMED_XML + DEFAULT_TERMINATOR;
    protected final static String WELLFORMED_XML_WITH_INVALID_TERMINATOR = WELLFORMED_XML + INVALID_TERMINATOR;
    // Test malformed XML.
    protected final static String MALFORMED_XML_WITH_VALID_TERMINATOR = MALFORMED_XML + DEFAULT_TERMINATOR;
    protected final static String MALFORMED_XML_WITH_INVALID_TERMINATOR = MALFORMED_XML + INVALID_TERMINATOR;
}