package io.cloudracer.mocktcpserver.tcpclient;

import io.cloudracer.mocktcpserver.datastream.DataStream;

public class TCPClientUnexpectedResponseException extends Exception {

    private static final long serialVersionUID = -1249843985630767953L;

    public TCPClientUnexpectedResponseException(DataStream response) {
        super(String.format("Unexpected response received from server: %s.", response.toString()));
    }
}
