package io.cloudracer;

public abstract class HostMessageConstants {

    public static final int TEST_TIMEOUT_5_MINUTE = 300000;
    public static final int TEST_TIMEOUT_10_MINUTE = TEST_TIMEOUT_5_MINUTE * 2;

    public final static int CLIENT_PORT = 6000;
    public final static int MOCK_SERVER_PORT = 6789;
    public final static String ACK = "A";
    public final static String NAK = "N";
    public final static long SERVER_CLEANUP_WAIT_DURATION = 10000;

    public final static String HLM_RECVQTAB_QUEUE_NAME = "HLM_RECV";
    public final static String HLM_RECVQTAB_TABLE_NAME = String.format("%sQTAB", HLM_RECVQTAB_QUEUE_NAME);
    public final static String HLM_SENDQTAB_QUEUE_NAME = "HLM_SEND";
    public final static String HLM_SENDQTAB_TABLE_NAME = String.format("%sQTAB", HLM_SENDQTAB_QUEUE_NAME);
    public final static String HOST_CLIENTQTAB_QUEUE_NAME = "HOST_CLIENT";
    public final static String HOST_CLIENTQTAB_TABLE_NAME = String.format("%sQTAB", HOST_CLIENTQTAB_QUEUE_NAME);

    // Am Container Module names.
    public final static String HOST_CLIENT_MODULE_NAME = "HOST_CLIENT";

    // Test CML terminators.
    public final static String INVALID_TERMINATOR = "INVALID_TERMINATOR";
    // Test XML.
    public final static String WELLFORMED_XML = "<test-root><test-element></test-element></test-root>";
    public final static String MALFORMED_XML = "<test-root><test-element><test-element></test-root>";
    // Test well-formed XML.
    public final static String WELLFORMED_XML_WITH_VALID_TERMINATOR = WELLFORMED_XML + HostMessageTypeConstants.VALID_TERMINATOR;
    public final static String WELLFORMED_XML_WITH_INVALID_TERMINATOR = WELLFORMED_XML + INVALID_TERMINATOR;
    // Test malformed XML.
    public final static String MALFORMED_XML_WITH_VALID_TERMINATOR = MALFORMED_XML + HostMessageTypeConstants.VALID_TERMINATOR;
    public final static String MALFORMED_XML_WITH_INVALID_TERMINATOR = MALFORMED_XML + INVALID_TERMINATOR;

    /**
     * Constants only - not for instantiation or inheritance.
     */
    private HostMessageConstants() {};
}