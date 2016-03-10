package io.cloudracer.mocktcpserver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.cloudracer.TestConstants;
import io.cloudracer.tcpclient.TCPClient;

public class MockTCPServerST {

	private io.cloudracer.tcpclient.TCPClient client;
	private MockTCPServer server;

	@Before
	public void setUp() throws Exception {
		server = new MockTCPServer(TestConstants.MOCK_SERVER_PORT);
		client = new TCPClient(TestConstants.MOCK_SERVER_PORT);
	}

	@After
	public void tearDown() throws Exception {
		client.close();
		server.close();
	}

	@Test
	public void testMockTCPServer() throws ClassNotFoundException, IOException {
		final String message = String.format("%s%s", "Test message!!", TestConstants.DEFAULT_TERMINATOR);

		assertEquals("Unexpected server response.", TestConstants.ACK, client.send(message).toString());
	}

	@Test
	public void testMockTCPServerWithCustomTerminator() throws ClassNotFoundException, IOException {
		final String message = String.format("%s%s", "Test message!!", TestConstants.CUSTOM_TERMINATOR);

		server.setTerminator(TestConstants.CUSTOM_TERMINATOR.getBytes());

		assertEquals("Unexpected server response.", TestConstants.ACK, client.send(message).toString());
	}
}