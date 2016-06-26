package io.cloudracer.mocktcpserver.standalone;

import io.cloudracer.mocktcpserver.MockTCPServer;

/**
 * Control the startup of the MockTCPServer.
 *
 * @author John McDonnell
 *
 */
public interface Bootstrap {

    /**
     * Starts a MockTCPServer on a command-line.
     *
     * @param args MockTCPServer parameters
     */
    public static void main(String[] args) {
        MockTCPServer.main(args);
    }
}
