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

// note: class name need not match the @Plugin name.
@Plugin(name = "LogMonitor", category = "Core", elementType = "appender", printObject = true)
public final class LogMonitor extends AbstractAppender {

    private static final long serialVersionUID = -4319748955513985321L;

    private static LogMonitor logMonitor;

    private static LogEvent lastEventLogged = null;

    private LogMonitor(String name, Filter filter,
            Layout layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    // Your custom appender needs to declare a factory method
    // annotated with `@PluginFactory`. Log4j will parse the configuration
    // and call this factory method to construct an appender instance with
    // the configured attributes.
    @PluginFactory
    public static LogMonitor createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout layout,
            @PluginElement("Filter") final Filter filter,
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
        LogMonitor.lastEventLogged = event;
    }

    public static LogMonitor createAppender(final Logger logger) {
        setLogMonitor(createAppender(logger.getName(), null, null, null));

        addLogMonitor(logger);

        return logMonitor;
    }

    public static LogEvent getLastEventLogged() {
        return lastEventLogged;
    }

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