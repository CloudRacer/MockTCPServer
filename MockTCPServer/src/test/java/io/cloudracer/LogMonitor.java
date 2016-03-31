package io.cloudracer;

import java.io.File;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Integrates with log4j2 and adds a new appender called TEST if, <b>and only if</b>, the appender is configure in the log4j2 configuration file.
 * <p>
 * This is an example log4j2 configuration file that will instruct log4j2 to start this appender: -
 *
 * <pre>
<code>
&lt?xml version="1.0" encoding="UTF-8"?&gt
&ltConfiguration status="warn" packages="io.cloudracer"&gt
    &ltAppenders&gt
        &ltConsole name="CONSOLE" target="SYSTEM_OUT"&gt
            &ltPatternLayout pattern="%d{ISO8601} %-5p [%t] %C(%L) - %m%n" /&gt
        &lt/Console&gt
        &ltFile name="FILE" fileName="logs/mocktcpserver.log" append="false"&gt
            &ltPatternLayout pattern="%d{ISO8601} %-5p [%t] %C(%L) - %m%n" /&gt
        &lt/File&gt
        <b>&ltLogMonitor name="TEST"&gt
            &ltPatternLayout pattern="%d{ISO8601} %-5p [%t] %C(%L) - %m%n" /&gt
            &ltThresholdFilter level="ERROR" onMatch="ACCEPT" onMismatch="DENY"/&gt
        &lt/LogMonitor&gt</b>
        &ltAsync name="ASYNC" includeLocation="true"&gt
            &ltAppenderRef ref="CONSOLE" /&gt
            &ltAppenderRef ref="FILE" /&gt
            <b>&ltAppenderRef ref="TEST" /&gt</b>
        &lt/Async&gt
    &lt/Appenders&gt
    &ltLoggers&gt
        &ltRoot level="all"&gt
            &ltAppenderRef ref="ASYNC" /&gt
        &lt/Root&gt
    &lt/Loggers&gt
&lt/Configuration&gt
</code>
 * </pre>
 *
 * @author John McDonnell
 */
@Plugin(name = "LogMonitor", category = "Core", elementType = "appender", printObject = true)
public final class LogMonitor extends AbstractAppender {

    private static final long serialVersionUID = -4319748955513985321L;

    private static LogMonitor logMonitor;

    private static LogEvent lastEventLogged = null;

    private LogMonitor(String name, Filter filter, Layout<?> layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    /**
     * Log4j will parse the configuration and call this factory method to construct an appender instance with the configured attributes.
     *
     * @param name
     * @param layout the name of a log4j2 Layout defined in the log4j2 configuration file.
     * @param filter the name of a log4j2 Filter defined in the log4j2 configuration file.
     * @param otherAttribute other attributes defined in the log4j2 configuration file.
     * @return a LogMonitor instance.
     */
    @PluginFactory
    public static LogMonitor createAppender(@PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<?> layout, @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for TestLog4j2Appender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        setLogMonitor(new LogMonitor(name, filter, layout, true));
        return getLogMonitor();
    }

    // The append method is where the appender does the work.
    // Given a log event, you are free to do with it what you want.
    // This example demonstrates:
    // 1. Concurrency: this method may be called by multiple threads concurrently
    // 2. How to use layouts
    // 3. Error handling
    @Override
    public void append(LogEvent event) {
        LogMonitor.setLastEventLogged(event);
    }

    /**
     * Add a new LogMonitor log4j2 Appender to the provided {@link Logger}.
     *
     * @param logger
     * @return a LogMonitor instance.
     */
    public static LogMonitor createAppender(final Logger logger) {
        setLogMonitor(createAppender(logger.getName(), null, null, null));

        addLogMonitor(logger);

        return logMonitor;
    }

    /**
     * Get the {@link LogEvent} most recently appended by this Appender.
     *
     * @return the {@link LogEvent} most recently appended by this Appender.
     */
    public static LogEvent getLastEventLogged() {
        return lastEventLogged;
    }

    static void setLastEventLogged(LogEvent lastEventLogged) {
        LogMonitor.lastEventLogged = lastEventLogged;
    }

    /**
     * The name of the file being appended to by the FILE Appender, if a FILE Appender is specified in the log4j2 configuration.
     *
     * @return the file being appended to by the FILE Appender, or null if a FILE Appender is <b>not</b> specified in the log4j2 configuration.
     */
    public static URI getFileName() {
        if (getLogMonitor() != null) {
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            final Configuration configuration = context.getConfiguration();

            return new File(((FileAppender) configuration.getAppender("FILE")).getFileName()).toURI();
        } else {
            return null;
        }
    }

    private static void addLogMonitor(final Logger logger) {
        logger.addAppender(getLogMonitor());
    }

    private static LogMonitor getLogMonitor() {
        return logMonitor;
    }

    private static void setLogMonitor(LogMonitor logMonitor) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = context.getConfiguration();

        logMonitor.start();
        configuration.addAppender(logMonitor);
        context.updateLoggers();

        LogMonitor.logMonitor = logMonitor;
    }
}