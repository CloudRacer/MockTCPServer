package io.cloudracer.mocktcpserver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

import io.cloudracer.mocktcpserver.bootstrap.Bootstrap;
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

    private byte[] terminator = null;
    private AssertionError assertionError;

    private ServerSocket socket;
    private DataStreamRegexMatcher expectedMessage;

    private Integer port;
    private boolean setIsAlwaysNAKResponse = false;
    private boolean setIsAlwaysNoResponse = false;
    private boolean isSendResponses = true;

    private Status status = Status.OPEN;
    private final ConfigurationSettings configurationSettings = new ConfigurationSettings();

    private Map<String, Set<TCPClient>> tcpClients = new HashMap<>();;

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
     *
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     */
    public MockTCPServer(final Integer port) throws ConfigurationException, InterruptedException {
        this(port, true);
    }

    /**
     * Start the server on the specified port.
     *
     * @param port the TCP Server will listen on this port. If null, the default port will be used.
     * @param startServer if true, start the server
     *
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     */
    public MockTCPServer(final Integer port, boolean startServer) throws ConfigurationException, InterruptedException {
        // If the port is specified as -1, creating a connection pool containing a separate server to listen on each port specified in the configuration file.
        if (port == -1) {
            this.logger.info("Starting a connection pool...");

            new Bootstrap().startup();
        } else {
            this.logger.info(String.format("Starting to listen on port %d only...", port));

            if (port != null) {
                this.setPort(port);
            }

            super.setName(String.format("%s-%d", this.getThreadName(), this.getPort()));

            if (startServer) {
                this.start();
            }
        }

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
     *
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     */
    public static void main(String[] args) throws ConfigurationException, InterruptedException {
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
            try {
                while (this.getStatus() == Status.OPEN && this.getSocket() != null) {
                    handleConnection();
                }
            } catch (final IOException | ConfigurationException e) {
                this.logger.error(e.getMessage(), e);
            }
        } finally {
            this.setStatus(Status.CLOSING);

            this.close();
        }
    }

    private void handleConnection() throws IOException, ConfigurationException {
        try {
            acceptNewConnection().start();
        } catch (final SocketException e) {
            this.logger.warn(e);
        }
    }

    /**
     * A server callback when a message has been processed, and a response has been sent to the client.
     *
     * @param clientMachine the name of the client machine that made the connection
     * @param clientPort the port the client is transmitting on
     * @param serverMachine the name of the server machine
     * @param serverPort the port the machine is listening on
     */
    public synchronized void afterConnection(final String clientMachine, final int clientPort, final String serverMachine, final int serverPort) {
        this.logger.info(String.format("Accepted a connection on machine %s:%d, from the client %s:%d.", serverMachine, serverPort, clientMachine, clientPort));
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
    public void setTerminator(final byte[] terminator) {
        this.terminator = terminator;
    }

    /**
     * A server callback when a message has been processed, and a response has been sent to the client.
     *
     * @param response the response that has been sent.
     */
    public synchronized void afterResponse(final byte[] response) {
        this.logger.debug(String.format("Sent the response: %s.", new String(response)));
    }

    /**
     * A server callback when a message is received.
     *
     * @param message the message received.
     */
    public synchronized void onMessage(final DataStream message) {
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
     * An error will be recorded if a message other than that which is {@link ClientConnection#getAssertionError() expected} is received. This property must be <b>set before a client connection is established</b>.
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
     * This is intended to be used to test a clients response to receiving a NAK. This property must be <b>set before a client connection is established</b>.
     * <p>
     * Default is false.
     *
     * @param isAlwaysNAKResponse if true, the Servers next response will always be a NAK.
     */
    public void setIsAlwaysNAKResponse(final boolean isAlwaysNAKResponse) {
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
     * The server <b>never</b> return a response, when true. This property must be <b>set before a client connection is established</b>.
     *
     * @param isAlwaysNoResponse true when the server will <b>never</b> return a response. Default is false.
     */
    public void setIsAlwaysNoResponse(final boolean isAlwaysNoResponse) {
        this.setIsAlwaysNoResponse = isAlwaysNoResponse;
    }

    /**
     * The server will send the responses described by {@link #getResponses()}.
     * <p>
     * Default is true.
     *
     * @return true, if the {@link #getResponses() responses} are to be sent.
     */
    public boolean getIsSendResponses() {
        return isSendResponses;
    }

    /**
     * The server will send the responses described by {@link #getResponses()}. This property must be <b>set before a client connection is established</b>.
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
     * If any message, other that this one, is the next message to be received, record it as an {@link MockTCPServer#getAssertionError() assertion error} and respond with a NAK. This property must be <b>set before a client connection is established</b>.
     *
     * @param expectedMessage a Regular Expression that describes what the next received message will be.
     */

    public void setExpectedMessage(final String expectedMessage) {
        this.expectedMessage = new DataStreamRegexMatcher(expectedMessage);
    }

    /**
     * Get the messages, initialised from the configuration file, that will be sent when specified messages are received.
     *
     * @return the server responses.
     * @throws ConfigurationException error reading the configuration file
     */
    public Map<String, Set<TCPClient>> getResponses() throws ConfigurationException {
        if (getIsSendResponses()) {
            if (tcpClients.isEmpty()) {
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
            }
        } else {
            tcpClients = new HashMap<>();
        }

        return Collections.unmodifiableMap(tcpClients);

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
            final long maximumTimeToWait = 1000;

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
        // Do not set the ServerSocket to null; just close the Stream.
        this.logger.debug("Closing the socket...");
        IOUtils.closeQuietly(this.socket);
        this.logger.debug("Closed the socket.");
    }

    private Status getStatus() {
        return this.status;
    }

    private void setStatus(final Status status) {
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
     */
    private ServerSocket getSocket() throws IOException {
        if (this.socket == null || this.socket.isClosed()) {
            this.logger.debug(String.format("Opening a socket on port %d...", this.getPort()));
            this.setSocket(new ServerSocket(this.getPort()));
        }

        return this.socket;
    }

    private ClientConnection acceptNewConnection() throws IOException, ConfigurationException {
        this.logger.info(String.format("Waiting for a connection on port %d...", this.getPort()));
        final Socket client = this.socket.accept();
        @SuppressWarnings("static-access")
        final InetAddress inetAddress = client.getInetAddress().getLocalHost(); // NOSONAR
        this.afterConnection(inetAddress.getHostName(), client.getPort(), InetAddress.getLocalHost().getHostName(), this.getPort());
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(client.getInputStream()));
        final DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
        final ClientConnection clientConnection = new ClientConnection(inputStream, outputStream, getIsAlwaysNAKResponse(), getIsAlwaysNoResponse(), getExpectedMessage(), getTerminator(), getResponses()) {

            @Override
            public void onMessage(DataStream message) {
                super.onMessage(message);
                MockTCPServer.this.onMessage(message);
            }

            @Override
            public void setAssertionError(AssertionError assertionError) {
                super.setAssertionError(assertionError);
                MockTCPServer.this.setAssertionError(assertionError);
            }

            @Override
            public synchronized void afterResponse(byte[] response) throws IOException {
                super.afterResponse(response);
                MockTCPServer.this.afterResponse(response);
            }
        };
        this.logger.debug("Ready to receive input.");

        return clientConnection;
    }

    private void setSocket(final ServerSocket socket) {
        this.socket = socket;
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
    private String getRootLoggerName() {
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