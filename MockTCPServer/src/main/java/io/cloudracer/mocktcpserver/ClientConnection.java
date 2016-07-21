package io.cloudracer.mocktcpserver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

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

/**
 * A TCP Server that is designed to simulate success <b>and</b> failure conditions in System/Integration test environments.
 *
 * @author John McDonnell
 */
public class ClientConnection extends Thread implements Closeable {

    private final Logger logger = LogManager.getLogger(this.getRootLoggerName());

    private enum Status {
        OPEN, CLOSED
    }

    private static final byte[] DEFAULT_TERMINATOR = { 13, 10, 10 };
    private static final byte[] DEFAULT_ACK = { 65 };
    private static final byte[] DEFAULT_NAK = { 78 };

    private byte[] terminator = null;
    private byte[] ack = null;
    private byte[] nak = null;

    private AssertionError assertionError;

    private BufferedReader inputStream;
    private DataOutputStream outputStream;
    private DataStreamRegexMatcher expectedMessage;

    private DataStream dataStream;

    private boolean setIsAlwaysNAKResponse = false;
    private boolean setIsAlwaysNoResponse = false;

    private int messagesReceivedCount = 0;

    private Status status = Status.OPEN;

    private final List<ResponseDAO> responsesSent = new ArrayList<>();

    private Map<String, Set<TCPClient>> responses;

    /**
     * Start the server on the specified port.
     *
     * @param inputStream the incoming stream from the client
     * @param outputStream the output stream to reply to the server
     * @param isAlwaysNAKResponse if true, the Servers next response will always be a NAK
     * @param isAlwaysNoResponse true when the server will <b>never</b> return a response. Default is false
     * @param expectedMessage a Regular Expression that describes what the next received message will be
     * @param terminator the terminator
     * @param responses the messages that will be sent when specified messages are received
     */
    public ClientConnection(final BufferedReader inputStream, final DataOutputStream outputStream, final boolean isAlwaysNAKResponse, final boolean isAlwaysNoResponse, final DataStreamRegexMatcher expectedMessage, final byte[] terminator, final Map<String, Set<TCPClient>> responses) {
        setInputStream(inputStream);
        setOutputStream(outputStream);
        setIsAlwaysNAKResponse(isAlwaysNAKResponse);
        setIsAlwaysNoResponse(isAlwaysNoResponse);
        setExpectedMessage(expectedMessage);
        setTerminator(terminator);
        setResponses(responses);
    }

    @Override
    public void run() {
        try {
            while (this.getStatus().equals(Status.OPEN)) {
                this.readIncomingStream();
            }
        } catch (final SocketException e) {
            this.logger.warn(e);
        } catch (final Exception e) {
            this.logger.error(e.getMessage(), e);
        } finally {
            this.setStatus(Status.CLOSED);

            this.close();

            this.logger.debug("Closed.");
        }
    }

    /**
     * Read and process the incoming stream.
     *
     * @throws IOException
     * @throws XPathExpressionException
     * @throws ConfigurationException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public void readIncomingStream() throws IOException, XPathExpressionException, ConfigurationException, ParserConfigurationException, SAXException {
        this.setDataStream(null);
        try {
            while (this.getDataStream().write(this.getInputStream().read()) != -1) {
                if (Arrays.equals(this.getDataStream().getTail(), this.getTerminator())) {
                    this.incrementMessagesReceivedCount();

                    break;
                }
            }

            if (this.getDataStream().getLastByte() == -1) {
                // The stream has ended so close all streams so that a new ServerSocket is opened and a new connection can be accepted.
                this.close();
            } else if (this.getDataStream().size() > 0) { // Ignore null (i.e. zero length) in order allow a probing ping e.g. paping.exe
                this.processIncomingMessage();
            }

            responsesSent.addAll(sendResponses());
        } catch (SocketTimeoutException e) {
            // Do nothing. This occurs because a client was not closed and the read timeout on the locked stream (i.e. blocked thread) is 60 seconds.
            this.logger.warn(e);
        }
    }

    private List<ResponseDAO> sendResponses() throws XPathExpressionException, ConfigurationException, ParserConfigurationException, SAXException, IOException {
        if (getIsResponses()) {
            final String message = this.getDataStream().toString().substring(0, this.getDataStream().toString().length() - this.getDataStream().getTail().length);
            Set<TCPClient> clients = getResponses().get(message);
            if (clients != null) {
                for (TCPClient tcpClient : clients) {
                    logger.debug("Sending responses from \"{}\".", tcpClient.toString());
                    responsesSent.addAll(tcpClient.sendResponses());

                    tcpClient.close();
                }
            }
        }

        return responsesSent;
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
            this.terminator = ClientConnection.DEFAULT_TERMINATOR;
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
    private byte[] getACK() {
        if (this.ack == null) {
            this.ack = ClientConnection.DEFAULT_ACK;
        }

        return this.ack;
    }

    /**
     * The <b>negative</b> acknowledgement response.
     *
     * @return negative acknowledgement
     */
    private byte[] getNAK() {
        if (this.nak == null) {
            this.nak = ClientConnection.DEFAULT_NAK;
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
     * @throws IOException error while responding to the server.
     */
    public synchronized void afterResponse(final byte[] response) throws IOException {
        this.logger.debug(String.format("Sent the response: %s.", new String(response)));
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
     * An error will be recorded if a message other than that which is {@link ClientConnection#getAssertionError() expected} is received.
     *
     * @param assertionError a recorded error.
     */
    public void setAssertionError(final AssertionError assertionError) {
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

    private boolean getIsResponses() {
        return getResponses() != null && !getResponses().isEmpty();
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link ClientConnection#getAssertionError() assertion error}.
     *
     * @return ignore if null.
     */
    public DataStreamRegexMatcher getExpectedMessage() {
        return this.expectedMessage;
    }

    /**
     * If any message, other that this one, is the next message to be received, record it as an {@link ClientConnection#getAssertionError() assertion error} and respond with a NAK.
     *
     * @param expectedMessage a Regular Expression that describes what the next received message will be.
     */
    public synchronized void setExpectedMessage(final DataStreamRegexMatcher expectedMessage) {
        this.expectedMessage = expectedMessage;
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
     * Add one to the number of messages received by the server since the server was started. see {@link ClientConnection#getMessagesReceivedCount()}
     */
    private void incrementMessagesReceivedCount() {
        this.messagesReceivedCount++;
    }

    Map<String, Set<TCPClient>> getResponses() {
        return Collections.unmodifiableMap(responses);
    }

    private void setResponses(final Map<String, Set<TCPClient>> responses) {
        this.responses = responses;
    }

    /**
     * Close the socket (if it is open) and any open data streams.
     */
    @Override
    public synchronized void close() {
        this.logger.debug("Closing...");

        this.setStatus(Status.CLOSED);
    }

    private Status getStatus() {
        return this.status;
    }

    private void setStatus(final Status status) {
        this.status = status;
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