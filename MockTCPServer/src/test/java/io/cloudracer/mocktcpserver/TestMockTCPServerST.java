package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;

public class TestMockTCPServerST extends AbstractTestTools {

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    @Test
    public void mockTCPServerACK() throws ClassNotFoundException, IOException {
        final String message = String.format("%s%s", "Test message!!", TestConstants.DEFAULT_TERMINATOR);

        assertEquals("Unexpected server response.", TestConstants.ACK, getClient().send(message).toString());

        checkLogMonitorForUnexpectedMessages();
    }

    @Test
    public void mockTCPServerACKWithCustomTerminator() throws ClassNotFoundException, IOException {
        final String message = String.format("%s%s", "Test message!!", TestConstants.CUSTOM_TERMINATOR);

        getServer().setTerminator(TestConstants.CUSTOM_TERMINATOR.getBytes());

        assertEquals("Unexpected server response.", TestConstants.ACK, getClient().send(message).toString());
    }
}