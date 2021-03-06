package io.cloudracer.mocktcpserver.bootstrap;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.configuration2.ex.ConfigurationException;

import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.properties.ConfigurationSettings;

/**
 * Start the Server by initialising the server Thread pool with each Thread listening to a different port.
 *
 * @author John McDonnell
 *
 */
public final class Bootstrap extends MockTCPServerPool {

    private static ConfigurationSettings configurationSettings;
    private MockTCPServerPool serverPool;

    /**
     * Start the Server and listen on all ports specified in the configured file.
     *
     * @throws ConfigurationException error reading the configuration file
     * @throws InterruptedException the MockTCPServer was unexpectedly interrupted
     */
    public void startup() throws ConfigurationException, InterruptedException {
        final Set<Integer> ports = getConfigurationSettings().getPorts();
        for (Iterator<Integer> iterator = ports.iterator(); iterator.hasNext();) {
            final Integer port = iterator.next();

            getServerPool().add(new MockTCPServer(port));
        }

        super.start();
    }

    /**
     * Stop listening to any ports i.e. stop all threads.
     */
    @Override
    public void shutdown() {
        getServerPool().shutdown();
    }

    static ConfigurationSettings getConfigurationSettings() {
        if (Bootstrap.configurationSettings == null) {
            Bootstrap.configurationSettings = new ConfigurationSettings();
        }
        return Bootstrap.configurationSettings;
    }

    static void setConfigurationSettings(ConfigurationSettings configurationSettings) {
        Bootstrap.configurationSettings = configurationSettings;
    }

    /**
     * Get the {@link MockTCPServerPool server pool}; one for each port in the configuration file.
     *
     * @return the {@link MockTCPServerPool server pool}
     */
    public MockTCPServerPool getServerPool() {
        if (serverPool == null) {
            this.serverPool = new MockTCPServerPool();
        }

        return serverPool;
    }
}
