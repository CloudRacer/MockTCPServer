package io.cloudracer.mocktcpserver.standalone;

import io.cloudracer.mocktcpserver.MockTCPServer;

public abstract class Bootstrap {

    private Bootstrap() {
        // Do nothing. This class cannot be instantiated.
    }

    /**
     * Starts a MockTCPServer on a command-line.
     *
     * @param args MockTCPServer parameters
     */
    public static void main(String[] args) {
        MockTCPServer.main(args);
    }
}
