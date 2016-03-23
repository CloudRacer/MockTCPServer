/**
================================================================================

   Project: Procter and Gamble - Skelmersdale.

   $HeadURL$

   $Author$

   $Revision$

   $Date$

$Log$

============================== (c) Swisslog(UK) Ltd, 2005 ======================
*/
package io.cloudracer;

import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;

public abstract class AbstractTestTools {
    /**
     * Asserts that the "TEST" {@link Appender log4j Appender} did not log any messages.
     *
     * Adjust the log4j.xml Appenders to match on only the messages that should cause this method to raise {@link AssertionError}.
     */
    public final void checkForUnexpectedLogMessages() {
        try {
            final int delayDuration = 1;
            // Pause to allow messages to be flushed to the disk (and, hence, through the appenders).
            TimeUnit.SECONDS.sleep(delayDuration);
        } catch (InterruptedException e) {
            // Do nothing
        }
        assertNull(String.format("An unexpected message was logged to the file \"%s\".", LogMonitor.getFileName()), LogMonitor.getLastEventLogged());
    }
}
