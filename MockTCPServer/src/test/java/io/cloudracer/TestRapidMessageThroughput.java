package io.cloudracer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Send many messages in a tight loop to ensure that "maximum" volume throughput
 * is possible.
 *
 * @author John McDonnell
 */
public class TestRapidMessageThroughput extends AbstractTestTools {

	@Override
	@Before
	public void setUp() throws IOException {
		super.setUp();
	}

	@Override
	@After
	public void cleanUp() throws IOException {
		super.cleanUp();
	}

	/**
	 * Send many messages in a tight loop to ensure that "maximum" volume
	 * throughput is possible.
	 *
	 * @throws IOException
	 * @throws ClassNotFoundException
	 **/
	@Test(timeout = TestConstants.TEST_TIMEOUT_10_MINUTE)
	public void rapidMessageThroughput() throws ClassNotFoundException, IOException {
		final int totalServerRestarts = 1000;
		for (int i = 0; i < totalServerRestarts; i++) {
			final String message = String.format("Test %d%s", i, TestConstants.DEFAULT_TERMINATOR);

			assertEquals("Unexpected server response.", TestConstants.ACK, this.getClient().send(message).toString());
		}

		this.checkLogMonitorForUnexpectedMessages();
	}

}
