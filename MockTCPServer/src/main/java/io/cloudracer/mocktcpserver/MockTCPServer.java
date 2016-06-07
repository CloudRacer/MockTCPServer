package io.cloudracer.mocktcpserver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import io.cloudracer.mocktcpserver.datastream.DataStream;
import io.cloudracer.mocktcpserver.datastream.DataStreamRegexMatcher;
import io.cloudracer.properties.ConfigurationSettings;

/**
 * A TCP Server that is designed to simulate success <b>and</b> failure conditions in System/Integration test environments.
 *
 * @author John McDonnell
 */
public class MockTCPServer extends Thread implements Closeable {

    private final Logger logger = LogManager.getLogger(this.getRootLoggerName());

    private enum Status {
        OPEN, CLOSING, CLOSED
    }

    private static final byte[] DEFAULT_TERMINATOR = { 13, 10, 10 };
    private static final byte[] DEFAULT_ACK = { 65 };
    private static final byte[] DEFAULT_NAK = { 78 };

    private byte[] terminator = null;
    private byte[] ack = null;
    private byte[] nak = null;

    private AssertionError assertionError;

    private ServerSocket socket;
    private BufferedReader inputStream;
    private DataOutputStream outputStream;
    private DataStreamRegexMatcher expectedMessage;

    private DataStream dataStream;
    private Socket connectionSocket;

    private Integer port;
    private boolean setIsAlwaysNAKResponse = false;
    private boolean setIsAlwaysNoResponse = false;
    private boolean isCloseAfterNextResponse = false;
    private int messagesReceivedCount = 0;

    private Status status = Status.OPEN;
    private final ConfigurationSettings configurationSettings = new ConfigurationSettings();

    private abstract static class Print {

        private static final Logger logger = LogManager.getLogger(Print.class.getName());

        private Print() {
            // Do nothing. This class cannot be constructed.
        }

        private static void printVersion() {
            final String ideWorkingFolder = "classes";

            final File executableLocation = new File(MockTCPServer.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath());
            String version = executableLocation.getName();
            // If this is the IDE, use the full path.
            if (version.equals(ideWorkingFolder)) {
                version = executableLocation.getAbsoluteFile().toString();
            }

            final Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
            final Matcher matcher = pattern.matcher(version);
            if (matcher.find()) {
                logger.info(matcher.group(0));
            } else {
                logger.info("Version number cannot be identified.");
            }
        }

        private static void printHelp() {
            printHelp(null);
        }

        private static void printHelp(final ParseException e1) {
            if (e1 != null) {
                logger.info(String.format("Invalid command line: %s", e1.getMessage()));
            }

            // automatically generate the help statement
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("MockTCPServer", getCommandLineOptions());
        }
    }

    /**
     * Start the server on the default port. This constructor is the equivalent of passing null to the constructor {@link MockTCPServer#MockTCPServer(Integer)}.
     */
    public MockTCPServer() {
        this(null);
    }

    /**
     * Start the server on the specified port.
     *
     * @param port the TCP Server will listen on this port. If null, the default port will be used.
     */
    public MockTCPServer(final Integer port) {
        this.logger.info("Starting...");

        if (port != null) {
            this.setPort(port);
        }

        super.setName(String.format("%s-%d", this.getThreadName(), this.getPort()));

        this.start();
        /*
         * If this pause is not done here, a test that *immediately* tries to connect, may get a "connection refused" error.
         */
        try {
            final long sleepDuration = 20;
            TimeUnit.MILLISECONDS.sleep(sleepDuration);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This allows an operator to start a stand-alone MockTCPServer. The <code>startup.sh(cmd)</code> file can be used to start the server on a command-line.
     * <p>
     * <b>Note</b>: currently, there is no packaged bundle to include all the dependencies and scripts.
     *
     * @param args an alternative port number can be passed as the first (and only) parameter.
     */
    public static void main(String[] args) {
        final Logger logger = LogManager.getLogger();

        try {
            final CommandLine commandLine = new DefaultParser().parse(getCommandLineOptions(), args);
            // Version information only.
            if (commandLine.hasOption("version")) {
                Print.printVersion();
            } else if (commandLine.hasOption("help")) {
                Print.printHelp();
            } else {
                final MockTCPServer mockTCPServer;
                if (commandLine.hasOption("port")) {
                    final int port = Integer.parseInt(commandLine.getOptionValue("port"));
                    mockTCPServer = new MockTCPServer(port);
                } else {
                    mockTCPServer = new MockTCPServer();
                }

                // When the Operating System interrupts the thread (kill or CTRL-C), stop the server.
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        logger.info("Operating System interrupt detected.");

                        mockTCPServer.close();
                    }
                });

                try {
                    waitForThreadToStop(mockTCPServer);
                } finally {
                    IOUtils.closeQuietly(mockTCPServer);
                }
            }
        } catch (final ParseException e1) {
            Print.printHelp(e1);
        }
    }

    private static void waitForThreadToStop(final MockTCPServer mockTCPServer) {
        try {
            mockTCPServer.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (this.getStatus() == Status.OPEN && this.getSocket() != null) {
                this.readIncomingStream();
            }
        } catch (final SocketException e) {
            this.logger.warn(e);
        } catch (final IOException | ConfigurationException e) {
            this.logger.error(e.getMessage(), e);
        } finally {
            this.setStatus(Status.CLOSING);

            this.close();
        }
    }

    private void readIncomingStream() {
        try {
            this.setDataStream(null);
            while (this.getDataStream().write(this.getInputStream().read()) != -1) {
                if (Arrays.equals(this.getDataStream().getTail(), this.getTerminator())) {
                    this.incrementMessagesReceivedCount();

                    break;
                }
            }

            if (this.getDataStream().getLastByte() == -1) {
                // The stream has ended so close all streams so that a new ServerSocket is opened and a new connection can be accepted.
                this.closeStreams();
            } else if (this.getDataStream().size() > 0) { // Ignore null (i.e. zero length) in order allow a probing ping e.g. paping.exe
                this.processIncomingMessage();
            }
        } catch (final Exception e) {
            this.logger.error(e.getMessage(), e);
        }
    }

    private void processIncomingMessage() throws IOException {
        this.setAssertionError(null);
        try {
            if (this.getExpectedMessage() != null) {
                Assert.assertThat("Unexpected message from the AM Host Client.", this.getDataStream(), this.getExpectedMessage());
            }
        } catch (final AssertionError e) {
            this.setAssertionError(e);
        }
        this.onMessage(this.getDataStream());
        // If the stream has not ended and a response is required, send one.
        if (this.getDataStream().getLastByte() != -1 && !this.getIsAlwaysNoResponse()) {
            byte[] response;

            if (this.getAssertionError() == null && !this.getIsAlwaysNAKResponse()) {
                response = this.getACK();
            } else {
                response = this.getNAK();
            }

            this.getOutputStream().write(response);

            this.afterResponse(response);
        }
    }

    /**
     * The server will read the stream until these characters are encountered.
     *
     * @return the terminator.
     */
    public byte[] getTerminator() {
        if (this.terminator == null) {
            this.terminator = MockTCPServer.DEFAULT_TERMINATOR;
        }

        return this.terminator;
    }

    /**
     * The server will read the stream until these characters are encountered.
     *
     * @param terminator the terminator.
     */
    public synchronized void setTerminator(final byte[] terminator) {
        this.terminator = terminator;
    }

    /**
     * The <b>positive</b> acknowledgement response.
     *
     * @return positive acknowledgement
     */
    public byte[] getACK() {
        if (this.ack == null) {
            this.ack = MockTCPServer.DEFAULT_ACK;
        }

        return this.ack;
    }

    /**
     * The <b>positive</b> acknowledgement response.
     *
     * @param ack positive acknowledgement
     */
    public synchronized void setACK(final byte[] ack) {
        this.ack = ack;
    }

    /**
     * The <b>negative</b> acknowledgement response.
     *
     * @return negative acknowledgement
     */
    public byte[] getNAK() {
        if (this.nak == null) {
            this.nak = MockTCPServer.DEFAULT_NAK;
        }

        return this.nak;
    }

    /**
     * The <b>negative</b> acknowledgement response.
     *
     * @param nak negative acknowledgement
     */
    public synchronized void setNAK(final byte[] nak) {
        this.nak = nak;
    }

    /**
     * A server callback when a message has been processed, and a response has been sent to the client.
     *
     * @param response the response that has been sent.
     */
    public synchronized void afterResponse(final byte[] response) {
        this.logger.debug(String.format("Sent the response: %s.", new String(response)));

        if (this.getIsCloseAfterNextResponse()) {
            this.setStatus(Status.CLOSED);
        }
    }

    /**
     * A server callback when a message is received.
     *
     * @param message the message received.
     */
    public void onMessage(final DataStream message) {
        this.logger.info(String.format("Received: %s.", message.toString()));
    }

    /**
     * An error is recorded if a message other than that which is expected is received.
     *
     * @return a recorded error.
     */
    public AssertionError getAssertionError() {
        return this.assertionError;
    }

    /**
     * An error will be recorded if a message other than that which is {@link MockTCPServer#getAssertionError() expected} is received.
     *
     * @param assertionError a recorded error.
     */
    private void setAssertionError(final AssertionError assertionError) {
        this.assertionError = assertionError;
    }

    /**
     * Forces the Server to return a NAK in response to the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used to test a clients response to receiving a NAK.
     * <p>
     * Default is false.
     *
     * @return If true, the Servers next response will always be a NAK.
     */
    public boolean getIsAlwaysNAKResponse() {
        return this.setIsAlwaysNAKResponse;
    }

    /**
     * Forces the Server to return a NAK in response to the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used to test a clients response to receiving a NAK.
     * <p>
     * Default is false.
     *
     * @param isAlwaysNAKResponse if true, the Servers next response will always be a NAK.
     */
    public synchronized void setIsAlwaysNAKResponse(final boolean isAlwaysNAKResponse) {
        this.setIsAlwaysNAKResponse = isAlwaysNAKResponse;
    }

    /**
     * The server <b>never</b> return a response, when true.
     *
     * @return true when the server will <b>never</b> return a response. Default is false.
     */
    public boolean getIsAlwaysNoResponse() {
        return this.setIsAlwaysNoResponse;
    }

    /**
     * The server <b>never</b> return a response, when true.
     *
     * @param isAlwaysNoResponse true when the server will <b>never</b> return a response. Default is false.
     */
    public synchronized void setIsAlwaysNoResponse(final boolean isAlwaysNoResponse) {
        this.setIsAlwaysNoResponse = isAlwaysNoResponse;
    }

    /**
     * Forces the Server to close down after processing the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used so that test clients can wait on the server Thread to end.
     * <p>
     * Default is false.
     *
     * @return if true, the Server will close after the message processing is complete. Default is false.
     */
    public boolean getIsCloseAfterNextResponse() {
        return this.isCloseAfterNextResponse;
    }

    /**
     * Forces the Server to close down after processing the next message received (regardless of <u>any</u> other conditions). The next message will first be processed as normal; irrespective of this property.
     * <p>
     * This is intended to be used so that test clients can wait on the server Thread to end.
     * <p>
     * Default is false.
     *
     * @param isCloseAfterNextResponse if true, the Server will close after the message processing is complete. Default is false.
     */
    public synchronized void setIsCloseAfterNextResponse(final boolean isCloseAfterNextResponse) {
        this.isCloseAfterNextResponse = isCloseAfterNextResponse;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error}.
     *
     * @return ignore if null.
     */
    public DataStreamRegexMatcher getExpectedMessage() {
        return this.expectedMessage;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error} and respond with a {@link MockTCPServer#getNAK() NAK}.
     *
     * @param expectedMessage a Regular Expression that describes what the next received message will be.
     */

    public synchronized void setExpectedMessage(final String expectedMessage) {
        this.expectedMessage = new DataStreamRegexMatcher(expectedMessage);
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#setAssertionError(AssertionError) assertion error} and respond with a {@link MockTCPServer#getNAK() NAK}.
     *
     * @param expectedMessage a Regular Expression that describes what the next received message will be.
     */
    public synchronized void setExpectedMessage(final StringBuilder expectedMessage) {
        this.setExpectedMessage(expectedMessage.toString());
    }

    /**
     * The number of messages received by the server since the server was started.
     *
     * @return The number of messages received by the server since the server was started. Default is 0.
     */
    public int getMessagesReceivedCount() {
        return this.messagesReceivedCount;
    }

    /**
     * Add one to the number of messages received by the server since the server was started. see {@link MockTCPServer#getMessagesReceivedCount()}
     */
    private void incrementMessagesReceivedCount() {
        this.messagesReceivedCount++;
    }

    /**
     * Close the socket (if it is open) and any open data streams.
     */
    @Override
    public synchronized void close() {
        this.logger.info("Closing...");

        if (this.getStatus() != Status.CLOSING) {
            this.setStatus(Status.CLOSED);
        }

        this.closeStreams();

        while (super.isAlive() && this.getStatus() != Status.CLOSING) {
            final long maximumTimeToWait = 10000;

            try {
                super.join(maximumTimeToWait);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Intermittently, the server fails to close. Retry it indefinitely until it does close; an improvement of blocking forever with no feedback.
            if (super.isAlive()) {
                this.logger.warn(String.format("Failed to close the Server(%s) in %d milliseconds. Trying again to shutdown the Server...", super.getName(), maximumTimeToWait));
                if (!super.isInterrupted()) {
                    this.logger.trace(String.format("Interrupting the Server Thread(%s)...", super.getName()));

                    this.closeStreams();
                }
            }
        }

        this.logger.info("Closed.");
    }

    private void closeStreams() {
        try {
            if (this.getConnectionSocket() != null && !this.getConnectionSocket().isInputShutdown()) {
                this.getConnectionSocket().shutdownInput();
            }
            if (this.getConnectionSocket() != null && !this.getConnectionSocket().isOutputShutdown()) {
                this.getConnectionSocket().shutdownOutput();
            }
        } catch (final IOException e) {
            this.logger.error(e.getMessage(), e);
        }
        this.setInputStream(null);
        this.setOutputStream(null);
        // Do not set the ServerSocket to null; just close the Stream.
        this.logger.info("Closing the socket...");
        IOUtils.closeQuietly(this.socket);
        this.logger.info("Closed the socket.");
    }

    private Status getStatus() {
        return this.status;
    }

    private synchronized void setStatus(final Status status) {
        this.status = status;
    }

    private int getPort() {
        try {
            if (this.port == null) {
                this.port = this.configurationSettings.getPort();
            }
        } catch (final ConfigurationException e) {
            this.logger.error(e.getMessage(), e);
        }

        return this.port;
    }

    private void setPort(int port) {
        this.port = port;
    }

    /**
     * Open the Server Socket and wait for a connection.
     * <p>
     * The socket is opened on the configured {@link #getPort() port} on localhost.
     *
     * @return a new ServerSocket.
     * @throws IOException
     * @throws ConfigurationException
     */
    private ServerSocket getSocket() throws IOException, ConfigurationException {
        if (this.socket == null || this.socket.isClosed()) {
            this.logger.info(String.format("Opening a socket on port %d...", this.getPort()));
            this.setSocket(new ServerSocket(this.getPort()));
            this.logger.info("Waiting for a connection...");
            this.setConnectionSocket(this.socket.accept());
            this.logger.info("Accepted a connection.");
            this.setInputStream(new BufferedReader(new InputStreamReader(this.getConnectionSocket().getInputStream())));
            this.setOutputStream(new DataOutputStream(this.getConnectionSocket().getOutputStream()));
            this.logger.info("Ready to receive input.");
        }

        return this.socket;
    }

    private void setSocket(final ServerSocket socket) {
        this.socket = socket;
    }

    private Socket getConnectionSocket() {
        return this.connectionSocket;
    }

    private void setConnectionSocket(final Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }

    private DataStream getDataStream() {
        if (this.dataStream == null) {
            this.dataStream = new DataStream(this.getTerminator().length, this.getRootLoggerName());
        }

        return this.dataStream;
    }

    private void setDataStream(final DataStream dataStream) {
        this.logger.debug("Closing the DataStream...");
        IOUtils.closeQuietly(this.dataStream);
        this.logger.debug("Closed the DataStream.");

        this.dataStream = dataStream;
    }

    private BufferedReader getInputStream() {
        return this.inputStream;
    }

    private void setInputStream(final BufferedReader inputStream) {
        this.logger.info("Closing input stream...");
        IOUtils.closeQuietly(this.inputStream);
        this.logger.info("Closed input stream.");

        this.inputStream = inputStream;
    }

    private DataOutputStream getOutputStream() {
        return this.outputStream;
    }

    private void setOutputStream(final DataOutputStream outputStream) {
        this.logger.info("Closing the output stream...");
        IOUtils.closeQuietly(this.outputStream);
        this.logger.info("Closed the output stream.");

        this.outputStream = outputStream;
    }

    private static Options getCommandLineOptions() {
        // create the Options
        final Options options = new Options();

        options.addOption("p", "port", false, "use this port instead of the default one, or the one specified in the configuration file.");
        options.getOption("port").setArgs(1);
        options.addOption("h", "help", false, "print these usage instructions and exit.");
        options.addOption("?", "help", false, "print these usage instructions and exit.");
        options.addOption("v", "version", false, "print product version and exit.");

        return options;
    }

    /**
     * The log4j root logger name that will contain the class name, even if instantiated as an anonymous class.
     *
     * @return a root logger name.
     */
    public String getRootLoggerName() {
        return this.getThreadName().replaceAll("-", ".");
    }

    /**
     * Derives a {@link Thread#getName() Thread name} that includes the class name, even if this object instantiated as an anonymous class.
     *
     * @return a value used as the log4j root logger and the Thread name.
     */
    private String getThreadName() {
        final String delimeter = ".";
        final String regEx = "\\.";

        String name;

        if (StringUtils.isNotBlank(this.getClass().getSimpleName())) {
            name = this.getClass().getSimpleName();
        } else {
            if (this.getClass().getName().contains(delimeter)) {
                final String[] nameSegments = this.getClass().getName().split(regEx);

                name = String.format("%s-%s", this.getClass().getSuperclass().getSimpleName(), nameSegments[nameSegments.length - 1]);
            } else {
                name = this.getClass().getName();
            }
        }

        return name;
    }
}