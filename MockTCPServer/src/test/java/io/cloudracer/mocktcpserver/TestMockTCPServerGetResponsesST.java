package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;
import io.cloudracer.TestConstants;

/**
 * Mock TCP Server tests.
 *
 * @author John McDonnell
 */
public class TestMockTCPServerGetResponsesST extends AbstractTestTools {

    private static final int TIMEOUT = 10000;

    @Override
    @Before
    public void setUp() throws IOException, ConfigurationException, InterruptedException {
        super.setUp();
    }

    @Override
    @After
    public void cleanUp() throws IOException {
        super.cleanUp();
    }

    /**
     * Server returns the expected ACK to a properly terminated message.
     *
     * @throws ConfigurationException
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(timeout = TIMEOUT)
    public void getResponses() throws ConfigurationException, IOException, InterruptedException {
        assertEquals(TestConstants.EXPECTED_CLIENT_LIST_FOR_PORT_6789, this.getServer().getResponses().toString());

        this.checkLogMonitorForUnexpectedMessages();
    }
}