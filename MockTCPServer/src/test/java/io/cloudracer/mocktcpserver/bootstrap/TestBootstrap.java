package io.cloudracer.mocktcpserver.bootstrap;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;

/**
 * Bootstrap multiple servers using details from the configuration file.
 *
 * @author John McDonnell
 */
public class TestBootstrap {

    private static final int TIMEOUT = 10000;

    /**
     * Bootstrap multiple servers using details from the configuration file.
     *
     * @throws ConfigurationException
     * @throws InterruptedException
     */
    @Test // (timeout = TIMEOUT)
    public void startup() throws ConfigurationException, InterruptedException {
        final Bootstrap bootstrap = new Bootstrap();

        bootstrap.startup();
        bootstrap.shutdown();
    }
}
