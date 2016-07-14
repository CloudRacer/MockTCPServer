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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.xml.sax.SAXException;

import io.cloudracer.mocktcpserver.datastream.DataStream;
import io.cloudracer.mocktcpserver.datastream.DataStreamRegexMatcher;
import io.cloudracer.mocktcpserver.responses.ResponseDAO;
import io.cloudracer.mocktcpserver.tcpclient.TCPClient;
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
    private boolean isSendResponses = true;
    private int messagesReceivedCount = 0;

    private Status status = Status.OPEN;
    private final ConfigurationSettings configurationSettings = new ConfigurationSettings();

    private final List<ResponseDAO> responsesSent = new ArrayList<>();

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
            CommandLine commandLine = new DefaultParser().parse(getCommandLineOptions(), args);
            // If no options have been provided, output the help text
            if (commandLine.getOptions().length <= 0) {
                commandLine = new DefaultParser().parse(getCommandLineOptions(), new String[] { "--help" });
            }
            // Version information only.
            if (commandLine.hasOption("version")) {
                Print.printVersion();
            } else if (commandLine.hasOption("help")) {
                Print.printHelp();
            } else {
                final MockTCPServer mockTCPServer;
                final int port = Integer.parseInt(commandLine.getOptionValue("port"));
                mockTCPServer = new MockTCPServer(port);

                // When the Operating System interrupts the thread (kill or CTRL-C), stop the server.
                Runtime.getRuntime().addShutdownHook(new Thread() {

                    @Override
                    public void run() {
                        logger.info("Operating System interrupt detected.");

                        mockTCPServer.close();
                    }
                });

                try {
                    waitForThread(logger, mockTCPServer, 0);
                } finally {
                    IOUtils.closeQuietly(mockTCPServer);
                }
            }
        } catch (final ParseException e1) {
            Print.printHelp(e1);
        }
    }

    private static void waitForThread(final Logger logger, final Thread thread, final long maximumDurationToWait) {
        try {
            thread.join(maximumDurationToWait);
        } catch (final InterruptedException e) {
            logger.warn(e);
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
        } catch (final Exception e) {
            this.logger.error(e.getMessage(), e);
        } finally {
            this.setStatus(Status.CLOSING);

            this.close();
        }
    }

    private void readIncomingStream() throws IOException, XPathExpressionException, ConfigurationException, ParserConfigurationException, SAXException {
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

        final List<ResponseDAO> responses = sendResponses();
        responsesSent.addAll(responses);
    }

    private List<ResponseDAO> sendResponses() throws XPathExpressionException, ConfigurationException, ParserConfigurationException, SAXException, IOException {
        final List<ResponseDAO> responses = new ArrayList<>();

        if (isSendResponses()) {
            final String message = this.getDataStream().toString().substring(0, this.getDataStream().toString().length() - this.getDataStream().getTail().length);
            Set<TCPClient> clients = getResponses().get(message);
            if (clients != null) {
                for (TCPClient tcpClient : clients) {
                    logger.debug("Sending responses from \"{}\".", tcpClient.toString());
                    responses.addAll(tcpClient.sendResponses());
                }
            }
        }

        return responses;
    }

    /**
     * A read-only {@link List} of responses already sent.
     *
     * @return a {@link List} of responses already sent
     */
    public List<ResponseDAO> getResponsesSent() {
        return Collections.unmodifiableList(responsesSent);
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
     * The server will send the responses described by {@link #getResponses()}.
     * <p>
     * Default is true.
     *
     * @return true, if the {@link #getResponses() responses} are to be sent.
     */
    public boolean isSendResponses() {
        return isSendResponses;
    }

    /**
     * The server will send the responses described by {@link #getResponses()}.
     * <p>
     * Default is true.
     *
     * @param isSendResponses true, if the {@link #getResponses() responses} are to be sent
     *
     */
    public void setIsSendResponses(boolean isSendResponses) {
        this.isSendResponses = isSendResponses;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#getAssertionError() assertion error}.
     *
     * @return ignore if null.
     */
    public DataStreamRegexMatcher getExpectedMessage() {
        return this.expectedMessage;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#getAssertionError() assertion error} and respond with a {@link MockTCPServer#getNAK() NAK}.
     *
     * @param expectedMessage a Regular Expression that describes what the next received message will be.
     */

    public synchronized void setExpectedMessage(final String expectedMessage) {
        this.expectedMessage = new DataStreamRegexMatcher(expectedMessage);
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#getAssertionError() assertion error} and respond with a {@link MockTCPServer#getNAK() NAK}.
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
     * Get the messages, initialised from the configuration file, that will be sent when specified messages are received.
     *
     * @return the server responses.
     * @throws ConfigurationException error reading the configuration file
     */
    public Map<String, Set<TCPClient>> getResponses() throws ConfigurationException {
        final Map<String, Set<TCPClient>> tcpClients = new HashMap<>();
        final Map<String, List<ResponseDAO>> responsesDAOs = this.configurationSettings.getResponses(getPort()).getResponses();

        for (Map.Entry<String, List<ResponseDAO>> incommingMessage : responsesDAOs.entrySet()) {
            for (ResponseDAO responseDAO : incommingMessage.getValue()) {
                final TCPClient tcpClient = new TCPClient(responseDAO.getMachineName(), responseDAO.getPort());
                tcpClient.addResponse(responseDAO.getResponse());
                final Set<TCPClient> client = new HashSet<>(Arrays.asList(tcpClient));
                if (tcpClients.containsKey(incommingMessage.getKey())) {
                    Set<TCPClient> currentClients = tcpClients.get(incommingMessage.getKey());
                    updateTCPClientList(currentClients, tcpClient);
                } else {
                    tcpClients.put(incommingMessage.getKey(), client);
                }
            }
        }

        return tcpClients;
    }

    private void updateTCPClientList(Set<TCPClient> currentClients, final TCPClient tcpClient) {
        if (currentClients.contains(tcpClient)) {
            for (Iterator<TCPClient> it = currentClients.iterator(); it.hasNext();) {
                TCPClient currentClient = it.next();
                if (currentClient.equals(tcpClient)) {
                    currentClient.addResponse(tcpClient.getResponses().get(0));
                }
            }
        } else {
            currentClients.add(tcpClient);
        }
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

        while ((socket != null && !socket.isClosed() && socket.isBound()) || (super.isAlive() && this.getStatus() != Status.CLOSING)) {
            final long maximumTimeToWait = 10;

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
            if (this.getConnectionSocket() != null && !this.getConnectionSocket().isClosed() && this.getConnectionSocket().isConnected()) {
                if (!this.getConnectionSocket().isInputShutdown()) {
                    this.getConnectionSocket().shutdownInput();
                }
                if (!this.getConnectionSocket().isOutputShutdown()) {
                    this.getConnectionSocket().shutdownOutput();
                }
            }
        } catch (final IOException e) {
            this.logger.error(e.getMessage(), e);
        }
        this.setInputStream(null);
        this.setOutputStream(null);
        // Do not set the ServerSocket to null; just close the Stream.
        this.logger.debug("Closing the socket...");
        IOUtils.closeQuietly(this.socket);
        this.logger.debug("Closed the socket.");
    }

    private Status getStatus() {
        return this.status;
    }

    private synchronized void setStatus(final Status status) {
        this.status = status;
    }

    /**
     * The port that this server is listening on.
     *
     * @return the port that this server is listening on.
     */
    public int getPort() {
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
            this.logger.debug(String.format("Opening a socket on port %d...", this.getPort()));
            this.setSocket(new ServerSocket(this.getPort()));
            this.logger.info(String.format("Waiting for a connection on port %d...", this.getPort()));
            this.setConnectionSocket(this.socket.accept());
            this.logger.debug(String.format("Accepted a connection on port %d...", this.getPort()));
            this.setInputStream(new BufferedReader(new InputStreamReader(this.getConnectionSocket().getInputStream())));
            this.setOutputStream(new DataOutputStream(this.getConnectionSocket().getOutputStream()));
            this.logger.debug("Ready to receive input.");
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
        this.logger.debug("Closing input stream...");
        IOUtils.closeQuietly(this.inputStream);
        this.logger.debug("Closed input stream.");

        this.inputStream = inputStream;
    }

    private DataOutputStream getOutputStream() {
        return this.outputStream;
    }

    private void setOutputStream(final DataOutputStream outputStream) {
        this.logger.debug("Closing the output stream...");
        IOUtils.closeQuietly(this.outputStream);
        this.logger.debug("Closed the output stream.");

        this.outputStream = outputStream;
    }

    private static Options getCommandLineOptions() {
        // create the Options
        final Options options = new Options();

        final OptionGroup startup = new OptionGroup();
        final Option port = Option.builder("p")
                .longOpt("port")
                .desc("the port that the server will listen on.")
                .type(Integer.class)
                .numberOfArgs(1)
                .build();
        startup.addOption(port);
        options.addOptionGroup(startup);
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