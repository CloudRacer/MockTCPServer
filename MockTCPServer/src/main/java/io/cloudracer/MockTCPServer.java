package io.cloudracer;

import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * A mock server used for testing purposes only.
 *
 * @author John McDonnell
 */
public class MockTCPServer extends Thread {

	private Logger logger = Logger.getLogger(String.format("%s.%s", getRootLoggerName(), getClazzName()));

	public final static byte[] DEFAULT_TERMINATOR = { 13, 10, 10 };
	public final static byte[] DEFAULT_ACK = { 65 };
	public final static byte[] DEFAULT_NAK = { 78 };

	public byte[] terminator = null;
	public byte[] ACK = null;
	public byte[] NAK = null;

	private AssertionError assertionError;

	private ServerSocket socket;
	private BufferedReader inputStream;
	private DataOutputStream outputStream;
	private RegexMatcher expectedMessage;

	private int port;
	private boolean setIsAlwaysNAKResponse = false;
	private boolean setIsAlwaysNoResponse = false;
	private boolean isCloseAfterNextResponse = false;
	private int messagesReceivedCount = 0;

	public MockTCPServer(final int port) {
		logger.info("Mock Host Server starting...");

		super.setName(String.format("%s-%d", getThreadName(), port));

		setPort(port);
		start();
	}

	@Override
	public void run() {
		try {
			setSocket(new ServerSocket(getPort()));
			while (getSocket() != null && !getSocket().isClosed()) {
				logger.info("Mock Host Server waiting for a connection...");
				final Socket connectionSocket = getSocket().accept();
				logger.info("Mock Host Server connected.");
				setInputStream(new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())));
				setOutputStream(new DataOutputStream(connectionSocket.getOutputStream()));
				final DataStream dataStream = new DataStream(getRootLoggerName(), getTerminator().length);
				int c;
				logger.info("Mock Host Server ready to receive input.");
				while ((c = getInputStream().read()) != -1) {
					dataStream.write(c);
					if (Arrays.equals(dataStream.getTail(), getTerminator())) {
						incrementMessagesReceivedCount();

						break;
					}
				}
				// Ignore null in order allow a probing ping e.g. paping.exe
				if (dataStream != null && dataStream.size() > 0) {
					setAssertionError(null);
					try {
						if (getExpectedMessage() != null) {
							assertThat("Unexpected message from the AM Host Client.", dataStream, getExpectedMessage());
						}
					} catch (AssertionError e) {
						setAssertionError(e);
					}
					onMessage(dataStream);
					byte[] response = null;
					if (!getIsAlwaysNoResponse()) {
						if (getAssertionError() == null && !getIsAlwaysNAKResponse()) {
							response = getACK();
						} else {
							response = getNAK();
						}

						getOutputStream().write(response);

						afterResponse(response);
					}

					setOutputStream(null);
				}
			}
		} catch (SocketException e) {
			logger.warn(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * The server will read the stream until these characters are encountered.
	 *
	 * @return the terminator.
	 */
	public byte[] getTerminator() {
		if (terminator == null) {
			terminator = DEFAULT_TERMINATOR;
		}

		return terminator;
	}

	/**
	 * The server will read the stream until these characters are encountered.
	 *
	 * @param terminator
	 *            the terminator.
	 */
	public void setTerminator(byte[] terminator) {
		this.terminator = terminator;
	}

	/**
	 * The <b>positive</b> acknowledgement response.
	 *
	 * @return positive acknowledgement
	 */
	public byte[] getACK() {
		if (this.ACK == null) {
			this.ACK = DEFAULT_ACK;
		}

		return ACK;
	}

	/**
	 * The <b>positive</b> acknowledgement response.
	 *
	 * @param ACK
	 *            positive acknowledgement
	 */
	public void setACK(byte[] ACK) {
		this.ACK = ACK;
	}

	/**
	 * The <b>negative</b> acknowledgement response.
	 *
	 * @return negative acknowledgement
	 */
	public byte[] getNAK() {
		if (this.NAK == null) {
			this.NAK = DEFAULT_NAK;
		}

		return NAK;
	}

	/**
	 * The <b>negative</b> acknowledgement response.
	 *
	 * @param NAK
	 *            negative acknowledgement
	 */
	public void setNAK(byte[] NAK) {
		this.NAK = NAK;
	}

	/**
	 * A server callback when a message has been processed, and a response has
	 * been sent to the client.
	 *
	 * @param response
	 *            the response that has been sent.
	 * @throws IOException
	 */
	public void afterResponse(final byte[] response) throws IOException {
		logger.info(String.format("Mock Host Server sent the response: %s.", new String(response)));

		if (getIsCloseAfterNextResponse()) {
			close();
		}
	}

	/**
	 * A server callback when a message is received.
	 *
	 * @param message
	 *            the message received.
	 */
	public void onMessage(final DataStream message) {
		logger.info(String.format("Mock Host Server received: %s.", message.toString()));
	}

	/**
	 * An error is recorded if a message other than that which is expected is
	 * received.
	 *
	 * @return a recorded error.
	 */
	public AssertionError getAssertionError() {
		return this.assertionError;
	}

	/**
	 * An error will be recorded if a message other than that which is
	 * {@link MockTCPServer#getAssertionError() expected} is received.
	 *
	 * @param assertionError
	 *            a recorded error.
	 */
	public void setAssertionError(AssertionError assertionError) {
		this.assertionError = assertionError;
	}

	/**
	 * Forces the Server to return a NAK in response to the next message
	 * received (regardless of <u>any</u> other conditions). The next message
	 * will first be processed as normal; irrespective of this property.
	 * <p>
	 * This is intended to be used to test a clients response to receiving a
	 * NAK.
	 * <p>
	 * Default is false.
	 *
	 * @return If true, the Servers next response will always be a NAK.
	 */
	public boolean getIsAlwaysNAKResponse() {
		return this.setIsAlwaysNAKResponse;
	}

	/**
	 * Forces the Server to return a NAK in response to the next message
	 * received (regardless of <u>any</u> other conditions). The next message
	 * will first be processed as normal; irrespective of this property.
	 * <p>
	 * This is intended to be used to test a clients response to receiving a
	 * NAK.
	 * <p>
	 * Default is false.
	 *
	 * @param isCloseAfterNextResponse
	 *            if true, the Servers next response will always be a NAK.
	 */
	public void setIsAlwaysNAKResponse(final boolean isAlwaysNAKResponse) {
		this.setIsAlwaysNAKResponse = isAlwaysNAKResponse;
	}

	public boolean getIsAlwaysNoResponse() {
		return setIsAlwaysNoResponse;
	}

	public void setIsAlwaysNoResponse(final boolean isAlwaysNoResponse) {
		this.setIsAlwaysNoResponse = isAlwaysNoResponse;
	}

	/**
	 * Forces the Server to close down after processing the next message
	 * received (regardless of <u>any</u> other conditions). The next message
	 * will first be processed as normal; irrespective of this property.
	 * <p>
	 * This is intended to be used so that test clients can wait on the server
	 * Thread to end.
	 * <p>
	 * Default is false.
	 *
	 * @return if true, the Server will close after the message processing is
	 *         complete.
	 */
	public boolean getIsCloseAfterNextResponse() {
		return isCloseAfterNextResponse;
	}

	/**
	 * Forces the Server to close down after processing the next message
	 * received (regardless of <u>any</u> other conditions). The next message
	 * will first be processed as normal; irrespective of this property.
	 * <p>
	 * This is intended to be used so that test clients can wait on the server
	 * Thread to end.
	 * <p>
	 * Default is false.
	 *
	 * @param isCloseAfterNextResponse
	 *            if true, the Server will close after the message processing is
	 *            complete.
	 */
	public void setIsCloseAfterNextResponse(boolean isCloseAfterNextResponse) {
		this.isCloseAfterNextResponse = isCloseAfterNextResponse;
	}

	/**
	 * If any message, other that this one, is the next message to be received,
	 * record it as an {@link MockTCPServer#setAssertionError(AssertionError)
	 * assertion error}.
	 *
	 * @return ignore if null.
	 */
	public RegexMatcher getExpectedMessage() {
		return expectedMessage;
	}

	/**
	 * If any message, other that this one, is the next message to be received,
	 * record it as an {@link MockTCPServer#setAssertionError(AssertionError)
	 * assertion error}.
	 *
	 * @param expectedMessage
	 *            a Regular Expression that describes what the next received
	 *            message will be.
	 */
	public void setExpectedMessage(final String expectedMessage) {
		this.expectedMessage = new RegexMatcher(expectedMessage);
	}

	/**
	 * If any message, other that this one, is the next message to be received,
	 * record it as an {@link MockTCPServer#setAssertionError(AssertionError)
	 * assertion error}.
	 *
	 * @param expectedMessage
	 *            a Regular Expression that describes what the next received
	 *            message will be.
	 */
	public void setExpectedMessage(final StringBuffer expectedMessage) {
		setExpectedMessage(expectedMessage.toString());
	}

	public int getMessagesReceivedCount() {
		return messagesReceivedCount;
	}

	private void incrementMessagesReceivedCount() {
		this.messagesReceivedCount++;
	}

	/**
	 * Close the socket (if it is open) and any open data streams.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		logger.info("Mock Host Server stopping...");

		setOutputStream(null);
		setInputStream(null);
		setSocket(null);
	}

	private int getPort() {
		return port;
	}

	private void setPort(int port) {
		this.port = port;
	}

	private ServerSocket getSocket() {
		return socket;
	}

	private void setSocket(ServerSocket socket) {
		if (socket == null && this.socket != null) {
			try {
				logger.info("Mock Host closing the server socket...");
				this.socket.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		this.socket = socket;
	}

	private BufferedReader getInputStream() {
		return inputStream;
	}

	private void setInputStream(BufferedReader inputStream) throws IOException {
		if (inputStream == null && this.inputStream != null) {
			logger.info("Mock Host closing the input stream...");
			this.inputStream.close();
		}

		this.inputStream = inputStream;
	}

	private DataOutputStream getOutputStream() {
		return outputStream;
	}

	private void setOutputStream(DataOutputStream outputStream) throws IOException {
		if (outputStream == null && this.outputStream != null) {
			logger.info("Mock Host flushing the output stream...");
			this.outputStream.flush();
			logger.info("Mock Host closing the output stream...");
			this.outputStream.close();
		}

		this.outputStream = outputStream;
	}

	/**
	 * The log4j root logger name that will contain the class name, even if
	 * instantiated as an anonymous class.
	 *
	 * @return a root logger name.
	 */
	public String getRootLoggerName() {
		final String delimeter = ".";
		final String regEx = "\\.";

		String name = null;

		if (this.getClass().getSimpleName() != null && this.getClass().getSimpleName().length() != 0) {
			name = this.getClass().getSimpleName();
		} else {
			if (this.getClass().getName().contains(delimeter)) {
				final String nameSegments[] = this.getClass().getName().split(regEx);

				name = String.format("%s-%s", this.getClass().getSuperclass().getSimpleName(),
						nameSegments[nameSegments.length - 1]);
			} else {
				name = this.getClass().getName();
			}
		}

		return name;
	}

	private String getThreadName() {
		final String delimeter = ".";
		final String regEx = "\\.";

		String name = null;

		if (this.getClass().getSimpleName() != null && this.getClass().getSimpleName().length() > 0) {
			name = this.getClass().getSimpleName();
		} else {
			if (this.getClass().getName().contains(delimeter)) {
				final String nameSegments[] = this.getClass().getName().split(regEx);

				name = String.format("%s-%s", this.getClass().getSuperclass().getSimpleName(),
						nameSegments[nameSegments.length - 1]);
			} else {
				name = this.getClass().getName();
			}
		}

		return name;
	}

	/**
	 * This class name, even if instantiated as an anonymous class.
	 *
	 * @return a root logger name.
	 */
	private String getClazzName() {
		final String delimeter = ".";
		final String regEx = "\\.";

		String name = null;

		if (StringUtils.isNotBlank(this.getClass().getSimpleName())) {
			name = this.getClass().getSimpleName();
		} else {
			if (this.getClass().getName().contains(delimeter)) {
				final String nameSegments[] = this.getClass().getName().split(regEx);

				name = nameSegments[nameSegments.length - 1];
			} else {
				name = this.getClass().getName();
			}
		}

		return name;
	}
}