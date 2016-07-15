package io.cloudracer.mocktcpserver.standalone;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.AbstractTestTools;

/**
 * Print the version of the MockTCPServer to the console.
 *
 * @author John McDonnell
 *
 */
public class TestPrintVersion extends AbstractTestTools {

    private static final Logger logger = LogManager.getLogger();

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    /**
     * Before
     */
    @Before
    public void setup() {
        System.setOut(new PrintStream(this.outContent));
    }

    /**
     * After
     */
    @After
    public void cleanUpStreams() {
        System.setOut(null);
    }

    /**
     * Print the version of the MockTCPServer to the console.
     *
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     * @throws ClassNotFoundException
     * @throws IOException the server pool
     */
    @Test
    public void printVersion() throws ConfigurationException, InterruptedException, ClassNotFoundException, IOException {
        if (!isInDebug()) {
            final String[] parameters = { "--version" };
            final String expected = String.format("1.6.0%s", System.getProperty("line.separator"));

            Bootstrap.main(parameters);
            assertEquals(expected, this.outContent.toString());
        } else {
            logger.warn("This routine is designed to extract the version number from the JAR filename. As this class was not loaded from a JAR, this routine cannot be tested. If this test is being from within an IDE, please try running it outside of the IDE.");
        }

        checkLogMonitorForUnexpectedMessages();
    }
}