package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.mocktcpserver.datastream.DataStream;

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class TestMockTCPServerST extends AbstractTestTools {

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
     * An ACK is returned when expected.
     *
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     */
    @Test
    public void mockTCPServerACK() throws ClassNotFoundException, IOException {
        final String message = String.format("%s%s", "Test message!!", DEFAULT_TERMINATOR);

        assertEquals("Unexpected server response.", ACK, this.getClient().send(message).toString());

        this.checkLogMonitorForUnexpectedMessages();
    }

    /**
     * An ACK is returned when expected, when using a customised {@link DataStream#getTailMaximumLength() maximum tail length}.
     *
     * @throws ClassNotFoundException see source documentation.
     * @throws IOException see source documentation.
     */
    @Test
    public void mockTCPServerACKWithCustomTerminator() throws ClassNotFoundException, IOException {
        final String message = String.format("%s%s", "Test message!!", CUSTOM_TERMINATOR);

        this.getServer().setTerminator(CUSTOM_TERMINATOR.getBytes());

        assertEquals("Unexpected server response.", ACK, this.getClient().send(message).toString());
    }
}