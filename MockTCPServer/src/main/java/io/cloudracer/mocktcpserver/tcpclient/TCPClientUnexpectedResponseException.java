package io.cloudracer.mocktcpserver.tcpclient;

import io.cloudracer.mocktcpserver.datastream.DataStream;

/**
 * Indicated that the TCP Server has responded, to a message that was sent to it, with an unexpected response.
 * <p>
 * When the {@link TCPClient#getResponseTerminator() Response Terminator} is null, the response must equal either the {@link TCPClient#setACK(byte[]) ACK} or {@link TCPClient#setNAK(byte[]) NAK}; when it does not, this exception is thrown.
 *
 * @author John McDonnell
 */
public class TCPClientUnexpectedResponseException extends Exception {

    private static final long serialVersionUID = -1249843985630767953L;

    /**
     * The specified response is unexpected.
     * <p>
     * When the {@link TCPClient#getResponseTerminator() Response Terminator} is <code>null</code>, the response must equal either the {@link TCPClient#setACK(byte[]) ACK} or {@link TCPClient#setNAK(byte[]) NAK}; when it does not, this exception is thrown.
     *
     * @param response the unexpected response.
     */
    public TCPClientUnexpectedResponseException(final DataStream response) {
        super(String.format("Unexpected response received from server: %s.", response.toString()));
    }
}