package io.cloudracer.mocktcpserver.tcpclient;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
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
    private int port;

    private Socket socket;

    private DataOutputStream dataOutputStream;

    private DataInputStream dataInputStream;

    /**
     * Specify the {@link TCPClient#getPort() port} that the TCP {@link TCPClient#getHostName() server} is listening on.
     *
     * @param port the port that the TCP {@link TCPClient#getHostName() server} is listening on.
     */
    public TCPClient(final int port) {
        this.setPort(port);
    }

    /**
     * Specify the {@link TCPClient#getHostName() machine} to communication with and the {@link TCPClient#getPort() port} that the machine is listening on.
     *
     * @param hostName the machine name to communicate with.
     * @param port the port number that the machine (specified by hostName) is listening on.
     * @throws IOException
     */
    public TCPClient(final String hostName, final int port) throws IOException {
        this(port);

        this.setHostName(hostName);
    }

    /**
     * Close the socket (if it is open) and any open data streams.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        this.setSocket(null);
    }

    /**
     * Connect to the {@link TCPClient#getHostName() Server}.
     *
     * @throws IOException
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
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public DataStream send(final String message) throws IOException, ClassNotFoundException {
        return this.send(message, true);
    }

    /**
     * Send a message to the {@link TCPClient#getHostName() server} and, <b>optionally</b>, wait for a response.
     *
     * @param message the message to send.
     * @param waitForResponse if true, wait for a response from the {@link TCPClient#getHostName() server}, otherwise null is returned.
     * @return the response from the {@link TCPClient#getHostName() server} or null if waitForResponse is false.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public DataStream send(final String message, final boolean waitForResponse) throws IOException, ClassNotFoundException {
        return this.send(message, waitForResponse, this.getResponseTerminator());
    }

    /**
     * Send a message down the socket.
     *
     * @param message the message to send.
     * @param waitForResponse if true, wait for a response from the {@link TCPClient#getHostName() server}, otherwise return null.
     * @param responseTerminator the terminator to wait for on the response. Ignored if null.
     * @return the response from {@link TCPClient#getHostName() server} or null if waitForResponse is false.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private DataStream send(final String message, final boolean waitForResponse, final byte[] responseTerminator) throws IOException, ClassNotFoundException {
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

                return new DataStream();
            }
        } else {
            return null;
        }
    }

    /**
     * Read and return the response message sent by {@link TCPClient#getHostName() server}.
     *
     * @return the response from the {@link TCPClient#getHostName() server}.
     * @throws IOException
     * @throws TCPClientUnexpectedResponseException
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
     * @throws ClassNotFoundException
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
        boolean terminated;
        terminated = (Arrays.equals(dataStream.getTail(), terminator)
                || ((dataStream.size() == this.getACK().length) && Arrays.equals(dataStream.toByteArray(), this.getACK()))
                || ((dataStream.size() == this.getNAK().length) && Arrays.equals(dataStream.toByteArray(), this.getNAK())));

        if ((terminator == null) && !terminated && ((dataStream.size() == this.getACK().length) || (dataStream.size() == this.getNAK().length))) {
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
     * @param port the port number.
     */
    private void setPort(final int port) {
        this.port = port;
    }

    /**
     * The Machine Name to send messages too.
     *
     * @return the Machine Name of the server to communicate with.
     * @throws UnknownHostException
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
            this.socket = new Socket(this.getHostName(), this.getPort());
        }

        return this.socket;
    }

    private void setSocket(final Socket socket) throws IOException {
        if ((socket == null) && (this.socket != null)) {
            this.setDataInputStream(null);
            this.setDataOutputStream(null);
            IOUtils.closeQuietly(this.socket);
        }

        this.socket = socket;
    }

    private DataOutputStream getDataOutputStream() throws IOException {
        if (this.dataOutputStream == null) {
            setDataOutputStream(new DataOutputStream(getSocket().getOutputStream()));
        }

        return this.dataOutputStream;
    }

    private void setDataOutputStream(final DataOutputStream dataOutputStream) throws IOException {
        if ((dataOutputStream == null) && (this.dataOutputStream != null)) {
            IOUtils.closeQuietly(getDataOutputStream());
        }

        this.dataOutputStream = dataOutputStream;
    }

    private DataInputStream getDataInputStream() throws IOException {
        if (this.dataInputStream == null) {
            setDataInputStream(new DataInputStream(getSocket().getInputStream()));
        }

        return this.dataInputStream;
    }

    private void setDataInputStream(final DataInputStream dataInputStream) throws IOException {
        if ((dataInputStream == null) && (this.dataInputStream != null)) {
            IOUtils.closeQuietly(getDataInputStream());
        }

        this.dataInputStream = dataInputStream;
    }
}