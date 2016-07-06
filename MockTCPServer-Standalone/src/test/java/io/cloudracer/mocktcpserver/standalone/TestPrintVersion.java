package io.cloudracer.mocktcpserver.standalone;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Print the version of the MockTCPServer to the console.
 *
 * @author John McDonnell
 *
 */
public class TestPrintVersion {

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
     */
    @Test
    public void printVersion() {
        final String[] parameters = { "--version" };
        final String expected = String.format("1.6.0%s", System.getProperty("line.separator"));

        Bootstrap.main(parameters);
        assertEquals(expected, this.outContent.toString());
    }
}