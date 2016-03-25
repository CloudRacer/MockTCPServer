package io.cloudracer.mocktcpserver.tcpclient;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.cloudracer.mocktcpserver.datastream.DataStream;

/**
 * A TCP Client provided primarily for demonstration purposes, and for use in test suites.
 *
 * @author John McDonnell
 */
public class TCPClient implements Closeable {

    private Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    public static final byte ACK_DEFAULT = 65;
    private final byte ACK;
    public static final byte NAK_DEFAULT = 78;
    private final byte NAK;
    private int port;

    private Socket socket;

    private DataOutputStream dataOutputStream;

    private DataInputStream dataInputStream;

    private byte[] terminatorDefault = new byte[] { 13, 10 };

    public TCPClient(final int port, final byte ACK, final byte NAK) throws IOException {
        this.ACK = ACK;
        this.NAK = NAK;

        setPort(port);
        // Open a socket;
        getSocket();
    }

    public TCPClient(final int port) throws IOException {
        this.ACK = ACK_DEFAULT;
        this.NAK = NAK_DEFAULT;

        setPort(port);
        // Open a socket;
        getSocket();
    }

    /**
     * Close the socket (if it is open) and any open data streams.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        setSocket(null);
    }

    /**
     * Send a message down the socket and wait for a response.
     *
     * @param message
     *        the message to send.
     * @return the response from server (i.e. the other end of the {@link Socket}).
     * @throws IOException
     * @throws ClassNotFoundException
     * @see TCPClient#getResponse()
     */
    public DataStream send(final String message) throws IOException, ClassNotFoundException {
        return send(message, true);
    }

    /**
     * Send a message down the socket.
     *
     * @param message
     *        the message to send.
     * @param waitForResponse
     *        if true, wait for a response from the server, otherwise return null.
     * @return the response from server (i.e. the other end of the {@link Socket}) or null if waitForResponse is false.
     * @throws IOException
     * @throws ClassNotFoundException
     * @see TCPClient#getResponse()
     */
    public DataStream send(final String message, final boolean waitForResponse) throws IOException, ClassNotFoundException {
        return send(message, true, null);
    }

    /**
     * Send a message down the socket.
     *
     * @param message
     *        the message to send.
     * @param waitForResponse
     *        if true, wait for a response from the server, otherwise return null.
     * @param responseTerminator
     *        the terminator to wait for on the response. Ignored if null.
     * @return the response from server (i.e. the other end of the {@link Socket}) or null if waitForResponse is false.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public DataStream send(final String message, final boolean waitForResponse, final byte[] responseTerminator) throws IOException, ClassNotFoundException {
        logger.info(String.format("Sending the message %s.", message));

        getDataOutputStream().write(message.getBytes(), 0, message.getBytes().length);

        if (waitForResponse) {
            if (responseTerminator == null) {
                return getResponse();
            } else {
                return getResponse(responseTerminator);
            }
        } else {
            return null;
        }
    }

    public DataStream getResponse() throws IOException {
        return getResponse(getTerminatorDefault());
    }

    /**
     * Read and display the response message sent by server application.
     *
     * @return the response from server (i.e. the other end of the {@link Socket}).
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public DataStream getResponse(final byte[] terminator) throws IOException {
        setDataInputStream(new DataInputStream(getSocket().getInputStream()));

        final DataStream dataStream = new DataStream(this.getClass().getSimpleName(), terminator.length);

        while (dataStream.write(getDataInputStream().read()) != -1) {
            if (isTerminated(dataStream, terminator)) {
                break;
            }
        }

        return dataStream;
    }

    private boolean isTerminated(final DataStream dataStream, final byte[] terminator) {
        return (isEqualByteArray(dataStream.getTail(), terminator)
                || (dataStream.size() == 1 && (dataStream.getTail()[0] == ACK || dataStream.getTail()[0] == NAK)));
    }

    private boolean isEqualByteArray(final byte[] byteByteArrayA, final byte[] byteByteArrayB) {
        return Arrays.equals(byteByteArrayA, byteByteArrayB);
    }

    private byte[] getTerminatorDefault() {
        return terminatorDefault;
    }

    private int getPort() {
        return this.port;
    }

    private void setPort(final int port) {
        this.port = port;
    }

    /**
     * Open a Socket, if not already open.
     *
     * @return an open {@link Socket} to the local machine, on the specified port ({@link TCPClient#getPort()}).
     * @throws IOException
     */
    public Socket getSocket() throws IOException {
        if (socket == null) {
            final InetAddress host = InetAddress.getLocalHost();
            socket = new Socket(host.getHostName(), getPort());
        }

        return socket;
    }

    private void setSocket(Socket socket) throws IOException {
        if (socket == null && this.socket != null) {
            setDataInputStream(null);
            setDataOutputStream(null);
            IOUtils.closeQuietly(this.socket);
        }

        this.socket = socket;
    }

    private DataOutputStream getDataOutputStream() throws IOException {
        if (this.dataOutputStream == null) {
            setDataOutputStream(new DataOutputStream(getSocket().getOutputStream()));
        }

        return dataOutputStream;
    }

    private void setDataOutputStream(DataOutputStream dataOutputStream) throws IOException {
        if (dataOutputStream == null && this.dataOutputStream != null) {
            IOUtils.closeQuietly(getDataOutputStream());
        }

        this.dataOutputStream = dataOutputStream;
    }

    private DataInputStream getDataInputStream() throws IOException {
        if (this.dataInputStream == null) {
            setDataInputStream(new DataInputStream(getSocket().getInputStream()));
        }

        return dataInputStream;
    }

    private void setDataInputStream(DataInputStream dataInputStream) throws IOException {
        if (dataInputStream == null && this.dataInputStream != null) {
            IOUtils.closeQuietly(getDataInputStream());
        }

        this.dataInputStream = dataInputStream;
    }
}
