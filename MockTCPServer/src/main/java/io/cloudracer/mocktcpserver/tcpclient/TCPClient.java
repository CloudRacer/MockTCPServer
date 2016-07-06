package io.cloudracer.mocktcpserver.tcpclient;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.cloudracer.mocktcpserver.datastream.DataStream;

/**
 * A TCP Client provided primarily for demonstration purposes, and for use in test suites.
 * <p>
 * Send messages to a specified {@link TCPClient#TCPClient(String, int) host} (or localhost, if unspecified) on a specified {@link TCPClient#getPort() port}. By default the client will wait for a synchronous response from the {@link TCPClient#getHostName() server} but the response can be ignored (i.e. not waited for) for particular {@link TCPClient#send(String, boolean) send} instructions.
 * <p>
 * If a {@link TCPClient#setResponseTerminator(byte[]) response terminator} is specified, the Client will wait for a synchronous response with that terminator, unless the {@link TCPClient#setACK(byte[]) ACK} or {@link TCPClient#setNAK(byte[]) NAK} response is received first. A custom ACK or NAK can be specified.
 *
 * @author John McDonnell
 */
public class TCPClient implements Closeable {

    private final Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    private static final byte[] DEFAULT_ACK = { 65 };
    private byte[] ack;
    private static final byte[] DEFAULT_NAK = { 78 };
    private byte[] nak;
    private static final byte[] DEFAULT_RESPONSE_TERMINATOR = { 13, 10 };
    private byte[] responseTerminator = TCPClient.DEFAULT_RESPONSE_TERMINATOR;

    private String hostName = null;
    private Integer port = null;

    private Socket socket;

    private DataOutputStream dataOutputStream;

    private DataInputStream dataInputStream;

    private List<String> responses = new ArrayList<>();

    /**
     * Messages will be sent to the specified port. Specify the {@link TCPClient#getPort() port} that the TCP {@link TCPClient#getHostName() server} is listening on.
     *
     * @param port the port that the TCP {@link TCPClient#getHostName() server} is listening on. If null, the default port will be used.
     */
    public TCPClient(final int port) {
        this.setPort(port);
    }

    /**
     * Specify the {@link TCPClient#getHostName() machine} to communication with and the {@link TCPClient#getPort() port} that the machine is listening on.
     *
     * @param hostName the machine name to communicate with.
     * @param port the port number that the machine (specified by hostName) is listening on.
     * @throws IOException see source documentation.
     */
    public TCPClient(final String hostName, final int port) throws IOException {
        this(port);

        this.setHostName(hostName);
    }

    /**
     * Close the socket (if it is open) and any open data streams.
     *
     * @throws IOException see source documentation.
     */
    @Override
    public void close() throws IOException {
        this.setSocket(null);
    }

    /**
     * Connect to the {@link TCPClient#getHostName() Server}.
     *
     * @throws IOException see source documentation.
     */
    public void connect() throws IOException {
        // Connect to the Server.
        this.getSocket();
    }

    /**
     * Send a message to the {@link TCPClient#getHostName() server} and wait for a response.
     *
     * @param message the message to send.
     * @return the response from the {@link TCPClient#getHostName() server}.
     * @throws IOException see source documentation.
     */
    public DataStream send(final String message) throws IOException {
        return this.send(message, true);
    }

    /**
     * Send a message to the {@link TCPClient#getHostName() server} and, <b>optionally</b>, wait for a response.
     *
     * @param message the message to send.
     * @param waitForResponse if true, wait for a response from the {@link TCPClient#getHostName() server}, otherwise null is returned.
     * @return the response from the {@link TCPClient#getHostName() server} or null if waitForResponse is false.
     * @throws IOException see source documentation.
     */
    public DataStream send(final String message, final boolean waitForResponse) throws IOException {
        final String formattedMessage = StringEscapeUtils.unescapeJava(message);

        return this.send(formattedMessage, waitForResponse, this.getResponseTerminator());
    }

    /**
     * Send a message down the socket.
     *
     * @param message the message to send.
     * @param waitForResponse if true, wait for a response from the {@link TCPClient#getHostName() server}, otherwise return null.
     * @param responseTerminator the terminator to wait for on the response. Ignored if null.
     * @return the response from {@link TCPClient#getHostName() server} or null if waitForResponse is false.
     * @throws IOException
     */
    private DataStream send(final String message, final boolean waitForResponse, final byte[] responseTerminator) throws IOException {
        this.logger.info(String.format("Sending the message %s.", message));

        this.getDataOutputStream().write(message.getBytes(), 0, message.getBytes().length);

        if (waitForResponse) {
            try {
                if (responseTerminator == null) {
                    return this.getResponse();
                } else {
                    return this.getResponse(responseTerminator);
                }
            } catch (final TCPClientUnexpectedResponseException e) {
                this.logger.error(e.getMessage(), e);

                this.close();

                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Read and return the response message sent by {@link TCPClient#getHostName() server}.
     *
     * @return the response from the {@link TCPClient#getHostName() server}.
     * @throws IOException see source documentation.
     * @throws TCPClientUnexpectedResponseException see source documentation.
     */
    public DataStream getResponse() throws IOException, TCPClientUnexpectedResponseException {
        return this.getResponse(this.getResponseTerminator());
    }

    /**
     * Read and return the response message sent by {@link TCPClient#getHostName() server}.
     *
     * @param terminator the response terminator. If null, only the {@link TCPClient#getACK() ACK} or {@link TCPClient#getNAK() NAK} will be expected and an exception will be throws if neither are received..
     * @return the response from the {@link TCPClient#getHostName() server}.
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws TCPClientUnexpectedResponseException
     */
    private DataStream getResponse(final byte[] terminator) throws IOException, TCPClientUnexpectedResponseException {
        this.setDataInputStream(new DataInputStream(this.getSocket().getInputStream()));

        final DataStream dataStream;
        if (terminator == null) {
            dataStream = new DataStream(this.getClass().getSimpleName());
        } else {
            dataStream = new DataStream(terminator.length, this.getClass().getSimpleName());
        }

        while (dataStream.write(this.getDataInputStream().read()) != -1) {
            if (this.isTerminated(dataStream, terminator)) {
                break;
            }
        }

        return dataStream;
    }

    private boolean isTerminated(final DataStream dataStream, final byte[] terminator) throws TCPClientUnexpectedResponseException {
        final boolean terminated = Arrays.equals(dataStream.getTail(), terminator)
                || Arrays.equals(dataStream.toByteArray(), this.getACK())
                || Arrays.equals(dataStream.toByteArray(), this.getNAK());

        if (terminator == null && !terminated && (dataStream.size() == this.getACK().length || dataStream.size() == this.getNAK().length)) {
            throw new TCPClientUnexpectedResponseException(dataStream);
        }

        return terminated;
    }

    /**
     * The port that the {@link TCPClient#getHostName() Server} is listening on.
     *
     * @return the port number.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Set the port that the {@link TCPClient#getHostName() Server} is listening on.
     *
     * @param port the port number. If null, the default port will be used.
     */
    private void setPort(final int port) {
        this.port = port;
    }

    /**
     * The Machine Name to send messages too.
     *
     * @return the Machine Name of the server to communicate with.
     * @throws UnknownHostException see source documentation.
     */
    public String getHostName() throws UnknownHostException {
        if (this.hostName == null) {
            final InetAddress host = InetAddress.getLocalHost();
            this.hostName = host.getHostName();
        }
        return this.hostName;
    }

    /**
     * Set the Machine Name to send messages too.
     *
     * @param hostName the Machine Name to send messages too.
     */
    private void setHostName(final String hostName) {
        this.hostName = hostName;
    }

    /**
     * The NAK (i.e. Not Acknowledged) response to expect from the {@link TCPClient#getHostName() Server}.
     *
     * @return the NAK response to expect.
     */
    public byte[] getNAK() {
        if (this.nak == null) {
            this.nak = TCPClient.DEFAULT_NAK;
        }

        return this.nak;
    }

    /**
     * The NAK (i.e. Not Acknowledged) response to expect from the {@link TCPClient#getHostName() Server}.
     *
     * @param nak the NAK response to expect.
     */
    public void setNAK(final byte[] nak) {
        this.nak = nak;
    }

    /**
     * The ACK (i.e. Acknowledged) response to expect from the {@link TCPClient#getHostName() Server}.
     *
     * @return the ACK response to expect.
     */
    public byte[] getACK() {
        if (this.ack == null) {
            this.ack = TCPClient.DEFAULT_ACK;
        }

        return this.ack;
    }

    /**
     * The ACK (i.e. Acknowledged) response to expect from the {@link TCPClient#getHostName() Server}.
     *
     * @param ack the ACK response to expect.
     */
    public void setACK(final byte[] ack) {
        this.ack = ack;
    }

    /**
     * The response terminator to expect from the {@link TCPClient#getHostName() Server}.
     * <p>
     * If null, all responses other than {@link TCPClient#getACK() ACK} or {@link TCPClient#getNAK() NAK} will result in an {@link TCPClientUnexpectedResponseException exception} (assuming responses are being waited for).
     *
     * @return the response terminator.
     */
    public byte[] getResponseTerminator() {
        return this.responseTerminator;
    }

    /**
     * The response terminator to expect from the {@link TCPClient#getHostName() Server}.
     * <p>
     * If null, all responses other than {@link TCPClient#getACK() ACK} or {@link TCPClient#getNAK() NAK} will result in an {@link TCPClientUnexpectedResponseException exception} (assuming responses are being waited for).
     *
     * @param responseTerminator the response terminator.
     */
    public void setResponseTerminator(final byte[] responseTerminator) {
        this.responseTerminator = responseTerminator;
    }

    /**
     * Open a Socket, if not already open.
     *
     * @return an open {@link Socket} to the local machine, on the specified port ({@link TCPClient#getPort()}).
     * @throws IOException
     */
    private Socket getSocket() throws IOException {
        if (this.socket == null) {
            final int delayBetweenRetries = 10;
            final int timeout = 1000;

            int i = 0;
            while (this.socket == null && timeout > (i * delayBetweenRetries)) {
                i++;

                try {
                    this.socket = new Socket(this.getHostName(), this.getPort());
                } catch (final IOException e) {
                    logger.info(String.format("Unable to connect to the Server \"%s\" on the port %d.", this.getHostName(), this.getPort()), e);
                }
            }
        }

        return this.socket;
    }

    private void setSocket(final Socket socket) throws IOException {
        if (socket == null && this.socket != null) {
            this.setDataInputStream(null);
            this.setDataOutputStream(null);
            IOUtils.closeQuietly(this.socket);

            /*
             * If this pause is not done here, a test that *immediately* tries to connect, may get a "connection refused" error.
             */
            final long sleepDuration = 20;
            final long timeoutDuration = 1000;
            int i = 0;
            while (!this.socket.isClosed() && this.socket.isBound() && timeoutDuration > (i * sleepDuration)) {
                i++;

                try {
                    TimeUnit.MILLISECONDS.sleep(sleepDuration);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        this.socket = socket;
    }

    private DataOutputStream getDataOutputStream() throws IOException {
        if (this.dataOutputStream == null) {
            this.setDataOutputStream(new DataOutputStream(this.getSocket().getOutputStream()));
        }

        return this.dataOutputStream;
    }

    private void setDataOutputStream(final DataOutputStream dataOutputStream) throws IOException {
        if (dataOutputStream == null && this.dataOutputStream != null) {
            IOUtils.closeQuietly(this.getDataOutputStream());
        }

        this.dataOutputStream = dataOutputStream;
    }

    private DataInputStream getDataInputStream() throws IOException {
        if (this.dataInputStream == null) {
            this.setDataInputStream(new DataInputStream(this.getSocket().getInputStream()));
        }

        return this.dataInputStream;
    }

    private void setDataInputStream(final DataInputStream dataInputStream) throws IOException {
        if (dataInputStream == null && this.dataInputStream != null) {
            IOUtils.closeQuietly(this.getDataInputStream());
        }

        this.dataInputStream = dataInputStream;
    }

    /**
     * Read-only copy of the {@link java.util.List list} of responses that will be sent by {@link #sendResponses()}.
     *
     * @return response the new response to add.
     */
    public List<String> getResponses() {
        return Collections.unmodifiableList(responses);
    }

    /**
     * Add a message to the {@link java.util.List list} of responses that will be sent by {@link #sendResponses()}.
     *
     * @param response the new response to add.
     */
    public void addResponse(String response) {
        responses.add(response);
    }

    /**
     * Send the responses added with {@link #addResponse(String)}.
     *
     * @throws IOException
     */
    public void sendResponses() throws IOException {
        for (String response : responses) {
            send(response, false);
        }
    }

    @Override
    public String toString() {
        return "TCPClient [hostName=" + hostName + ", port=" + port + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TCPClient other = (TCPClient) obj;
        if (hostName == null) {
            if (other.hostName != null) {
                return false;
            }
        } else if (!hostName.equals(other.hostName)) {
            return false;
        }
        if (port == null) {
            if (other.port != null) {
                return false;
            }
        } else if (!port.equals(other.port)) {
            return false;
        }
        return true;
    }
}