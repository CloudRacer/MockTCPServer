package io.cloudracer.mocktcpserver.bootstrap;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import io.cloudracer.mocktcpserver.MockTCPServer;

/**
 * Manage a pool of MockTCPServer servers where each server listens of a different port.
 *
 * @author John McDonnell
 */
public class MockTCPServerPool extends Thread implements Closeable {

    /**
     * The status of the server pool.
     */
    public enum Status {
        /**
         * The server pool is started.
         */
        STARTED,
        /**
         * The server pool is in the process of being stopped.
         */
        STOPPING,
        /**
         * The server pool is stopped.
         */
        STOPPED,
        /**
         * Failed to stop.
         */
        STOPPING_FAILED
    }

    private static Status status = Status.STARTED;

    private static Map<Integer, MockTCPServer> mockTCPServerSet = new HashMap<>();

    /**
     * The pool will remain active until it is {@link #shutdown() shutdown)} (i.e. the {@link #getStatus() status)} is STOPPED).
     */
    @Override
    public void run() {
        super.run();

        final int interval = 1;

        while (!getStatus().equals(Status.STOPPED)) {
            try {
                TimeUnit.SECONDS.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Add a {@link MockTCPServer server} to the pool.
     *
     * @param mockTCPServer the server to add to the pool.
     */
    public void add(MockTCPServer mockTCPServer) {
        mockTCPServerSet.put(mockTCPServer.getPort(), mockTCPServer);
    }

    /**
     * Get a specific {@link MockTCPServer server} from the {@link MockTCPServerPool server pool}.
     *
     * @param port the port being listened too by the required {@link MockTCPServer server}
     *
     * @return the requested {@link MockTCPServer server}
     */
    public MockTCPServer get(final int port) {
        return mockTCPServerSet.get(port);
    }

    /**
     * Close all {@link MockTCPServer servers} in the pool.
     */
    public void shutdown() {
        for (Entry<Integer, MockTCPServer> entry : mockTCPServerSet.entrySet()) {
            entry.getValue().close();
        }
    }

    /**
     * The {@link Status} of the connection pool.
     *
     * @return the {@link Status} of the connection pool
     */
    public static Status getStatus() {
        return status;
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }
}