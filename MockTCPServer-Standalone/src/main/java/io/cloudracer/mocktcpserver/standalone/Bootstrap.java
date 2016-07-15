package io.cloudracer.mocktcpserver.standalone;

import java.io.IOException;

import org.apache.commons.configuration2.ex.ConfigurationException;

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
     *
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     * @throws IOException the server pool
     */
    public static void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
        final String createPool = "-1";

        if (args.length > 0 && args[0].equals(createPool)) {
            try (final io.cloudracer.mocktcpserver.bootstrap.Bootstrap bootstrap = new io.cloudracer.mocktcpserver.bootstrap.Bootstrap();) {
                bootstrap.startup();
                bootstrap.join();
            }
        } else {
            MockTCPServer.main(args);
        }
    }
}
