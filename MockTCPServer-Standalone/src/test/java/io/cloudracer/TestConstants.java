package io.cloudracer;

/**
 * Predefined values used by the test routines.
 *
 * @author John McDonnell
 *
 */
public abstract class TestConstants {

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

    private static final byte[] ACK = { 65 }; // Default ACK.
    private static final byte[] NAK = { 78 }; // Default NAK.

    /**
     * Test terminators.
     */
    public static final String DEFAULT_TERMINATOR = "\r\n\n";
    /**
     * Test XML.
     */
    public static final String WELLFORMED_XML = "<test-root><test-element></test-element></test-root>";
    /**
     * Test well-formed XML.
     */
    public static final String WELLFORMED_XML_WITH_VALID_TERMINATOR = WELLFORMED_XML + DEFAULT_TERMINATOR;
    /**
     * Port number.
     */
    public static final int MACHINE_A_PORT = 1234;
    /**
     * Port number to responded to.
     */
    public static final int MACHINE_A_RESPONSE_PORT = 2345;
    /**
     * Port number.
     */
    public static final int MACHINE_B_PORT = 6789;
    /**
     * Port number to responded to.
     */
    public static final int MACHINE_B_RESPONSE_PORT = 5678;

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