package io.cloudracer.mocktcpserver.standalone;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPrintVersion {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void setup() {
        System.setOut(new PrintStream(this.outContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(null);
    }

    @Test
    public void printVersion() {
        final String[] parameters = { "--version" };
        final String expected = String.format("1.4.0%s", System.getProperty("line.separator"));

        Bootstrap.main(parameters);
        assertEquals(expected, this.outContent.toString());
    }
}
