package io.cloudracer;

import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;

/**
 * Tools <b>exclusively</b> for use by test routines.
 *
 * @author John McDonnell
 */
public abstract class AbstractTestTools {

    /**
     * Asserts that the "TEST" {@link Appender log4j Appender} did not log any messages. Adjust the log4j.xml Appenders to match on only the messages that should cause this method to raise {@link AssertionError}.
     */
    protected final void checkLogMonitorForUnexpectedMessages() {
        try {
            final int delayDuration = 1;
            // Pause to allow messages to be flushed to the disk (and, hence, through the appenders).
            TimeUnit.SECONDS.sleep(delayDuration);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertNull(String.format("An unexpected message was logged to the file \"%s\".", LogMonitor.getFileName()), LogMonitor.getLastEventLogged());
    }

    /**
     * Checks if the Class containing the method that called this one (i.e. the calling method) was loadded from a JAR.
     * <p>
     * If the calling method was loaded from a JAR, it is assumed to be running outside of an IDE.
     * <p>
     * Although there are occasions when code will be running inside the IDE that <u>was</u> loaded from a JAR file.
     *
     * @return true is the calling class has been loaded from a JAR file
     * @throws ClassNotFoundException
     */
    public static boolean isInDebug() throws ClassNotFoundException {
        boolean isInDebug = false;

        final String jarResourceNamePrefix = "jar:";

        final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTraceElements.length; i++) {
            final StackTraceElement callingRoutine = stackTraceElements[i];
            if (!callingRoutine.getClassName().contains("junit") && !callingRoutine.getClassName().contains("java.lang.") && !callingRoutine.getClassName().equalsIgnoreCase(AbstractTestTools.class.getCanonicalName())) {
                final String[] classnameComponents = callingRoutine.getClassName().split("\\.");
                final String classname = classnameComponents[classnameComponents.length - 1].concat(".class");
                final String callerClassFile = Class.forName(callingRoutine.getClassName()).getResource(classname).toString();

                isInDebug = !callerClassFile.startsWith(jarResourceNamePrefix);

                break;
            }
        }

        return isInDebug;
    }
}