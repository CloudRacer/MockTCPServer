package io.cloudracer.mocktcpserver.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.cloudracer.mocktcpserver.MockTCPServer;

/**
 * Manage a pool of MockTCPServer servers where each server listens of a different port.
 *
 * @author John McDonnell
 */
public class MockTCPServerPool {

    private static Map<Integer, MockTCPServer> mockTCPServerSet = new HashMap<>();

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
}