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
     * Close all {@link MockTCPServer servers} in the pool.
     */
    public void shutdown() {
        for (Entry<Integer, MockTCPServer> entry : mockTCPServerSet.entrySet()) {
            entry.getValue().close();
        }
    }
}