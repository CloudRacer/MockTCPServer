package io.cloudracer.tcpclient;

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

import io.cloudracer.datastream.DataStream;

/**
 * @author John McDonnell
 */
public class TCPClient implements Closeable {

    private Logger logger = LogManager.getLogger(this.getClass().getSimpleName());

    public static final byte ACK = 65;
    private static final byte NAK = 78;
    private int port;

    private Socket socket;

    private DataOutputStream dataOutputStream;

    private DataInputStream dataInputStream;

    private byte[] terminatorDefault = new byte[] { 13, 10 };

    private int byteCount;

    public TCPClient(final int port) throws IOException {
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
     * Send a message down the socket.
     *
     * @param message
     *        the message to send.
     * @return the response from server (i.e. the other end of the
     *         {@link Socket}).
     * @throws IOException
     * @throws ClassNotFoundException
     * @see TCPClient#getResponse()
     */
    public DataStream send(final String message) throws IOException, ClassNotFoundException {
        logger.info(String.format("Sending the message: %s.", message));

        getDataOutputStream().write(message.getBytes());

        return getResponse();
    }

    /**
     * Send a message down the socket.
     *
     * @param message
     *        the message to send.
     * @param responseTerminator
     *        the terminator to wait for on the response. Ignored if null.
     * @return the response from server (i.e. the other end of the
     *         {@link Socket}).
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public String send(final String message, final byte[] responseTerminator)
            throws IOException, ClassNotFoundException {
        logger.info(String.format("Sending the message %s.", message));

        getDataOutputStream().write(message.getBytes(), 0, message.getBytes().length);

        DataStream response;
        if (responseTerminator == null) {
            response = getResponse();
        } else {
            response = getResponse(responseTerminator);
        }

        return response.toString();
    }

    public DataStream getResponse() throws IOException {
        return getResponse(getTerminatorDefault());
    }

    /**
     * Read and display the response message sent by server application.
     *
     * @return the response from server (i.e. the other end of the
     *         {@link Socket}).
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public DataStream getResponse(final byte[] terminator) throws IOException {
        setDataInputStream(new DataInputStream(getSocket().getInputStream()));

        final DataStream dataStream = new DataStream(this.getClass().getSimpleName());

        setByteCount(1);
        int nextByte;
        final byte[] potentialTerminator = new byte[terminator.length];
        while ((nextByte = getDataInputStream().read()) != -1) {
            dataStream.write(nextByte);

            if (getByteCount() > terminator.length) {
                for (int i = 0; i < potentialTerminator.length - 1; i++) {
                    potentialTerminator[i] = potentialTerminator[i + 1];
                }
                potentialTerminator[potentialTerminator.length - 1] = (byte) nextByte;
            } else {
                potentialTerminator[getByteCount() - 1] = (byte) nextByte;
            }

            if (isTerminated(potentialTerminator, terminator)) {
                break;
            }

            setByteCount(getByteCount() + 1);
        }
        if (nextByte == -1) {
            dataStream.write(nextByte);
        }

        return dataStream;
    }

    private boolean isTerminated(final byte[] potentialTerminator, final byte[] terminator) {
        return (isEqualByteArray(potentialTerminator, terminator)
                || (getByteCount() == 1 && (potentialTerminator[0] == ACK || potentialTerminator[0] == NAK)));
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
     * @return an open {@link Socket} to the local machine, on the specified
     *         port ({@link TCPClient#getPort()}).
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

    private int getByteCount() {
        return byteCount;
    }

    private void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }
}
