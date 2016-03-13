package io.cloudracer.datastream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class DataStream {

	private Logger logger;

	public final static int TAIL_LENGTH_DEFAULT = 3;

	private ByteArrayOutputStream output;
	private Deque<Byte> tailQueue = null;
	private int lastByte;
	private Integer tailLength = null;
	private String rootLoggerName;

	/**
	 * Default constructor.
	 */
	public DataStream(final String rootLoggerName) {
		setTailLength(TAIL_LENGTH_DEFAULT);
		setRootLoggerName(rootLoggerName);
	}

	/**
	 * The PipeStream will maintain a tail (most recently read bytes) of the
	 * specified length.
	 *
	 * @param tailLength
	 *            tail length. Default of {@link DataStream#TAIL_LENGTH_DEFAULT}
	 */
	public DataStream(final String rootLoggerName, final int tailLength) {
		setTailLength(tailLength);
		setRootLoggerName(rootLoggerName);
	}

	/**
	 * Delegate of {@link PipedOutputStream#write(int)}.
	 *
	 * @param data
	 *            data to write to the {@link PipedInputStream}.
	 * @throws IOException
	 */
	public synchronized void write(final int data) throws IOException {
		getOutput().write(data);
	}

	/**
	 * {@link PipedOutputStream#close() close} the {@link PipedOutputStream
	 * output stream}.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		setOutput(null);
	}

	/**
	 * Delegate of {@link ByteArrayOutputStream#size()}.
	 */
	public synchronized int size() {
		return getOutput().size();
	}

	public synchronized ByteArrayOutputStream getOutput() {
		if (output == null) {
			output = new ByteArrayOutputStream() {
				@Override
				public synchronized void write(int b) {
					super.write(b);
					setLastByte(b);
					addToTailList();
				}
			};
		}

		return output;
	}

	/**
	 * Copies the stream to a newly created {@link PipedInputStream input
	 * stream}. The {@link PipedInputStream input stream} is cached internally
	 * and only reinitialised when new bytes are written to the stream.
	 * <p>
	 * The stream is copied without making one or more intermediary copies of
	 * the content (i.e. it is memory efficient).
	 *
	 * @return {@link PipedInputStream input stream} populated with all
	 *         available data.
	 * @throws IOException
	 */
	public PipedInputStream copyToInputStream() throws IOException {
		PipedInputStream inputStream = new PipedInputStream(getOutput().size());
		final PipedOutputStream outputStream = new PipedOutputStream(inputStream);

		Thread copy = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					getOutput().writeTo(outputStream);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
		copy.setName(String.format("%s-InputStream-Creator", getThreadName()));
		copy.start();
		try {
			copy.join();
		} catch (InterruptedException e) {
			logger.error(e.getCause(), e);
		}

		return inputStream;
	}

	/**
	 * Instantiate the {@link ByteArrayOutputStream output stream}.
	 *
	 * @param outputStream
	 *            if null, the current {@link PipedOutputStream output stream}
	 *            is {@link PipedOutputStream#close() closed} before being
	 *            reinitialised.
	 * @throws IOException
	 */
	private void setOutput(final ByteArrayOutputStream output) throws IOException {
		if (output == null && this.output != null) {
			this.output.close();
		}

		this.output = output;

	}

	/**
	 * The length of the tail.
	 *
	 * @return the length of the tail. Default of
	 *         {@link DataStream#TAIL_LENGTH_DEFAULT length}.
	 */
	public int getTailLength() {
		return tailLength;
	}

	private void setTailLength(final int tailLength) {
		this.tailLength = tailLength;
	}

	/**
	 * The tail of the specified {@link DataStream#getTailLength() length}.
	 *
	 * @return
	 */
	private Deque<Byte> getTailQueue() {
		if (tailQueue == null) {
			tailQueue = new ArrayDeque<Byte>();
		}

		return tailQueue;
	}

	/**
	 * The tail of the specified {@link DataStream#getTailLength() length}.
	 *
	 * @return
	 */
	public byte[] getTail() {
		// Convert the list to an array.
		final byte[] tail = new byte[getTailQueue().size()];
		int i = 0;
		for (Iterator<Byte> iterator = getTailQueue().iterator(); iterator.hasNext();) {
			final Byte nextByte = iterator.next();
			tail[i] = nextByte;

			i++;
		}

		return tail;
	}

	private void addToTailList() {
		getTailQueue().addLast(getLastByte());
		if (getTailQueue().size() > getTailLength()) {
			getTailQueue().removeFirst();
		}
	}

	/**
	 * Get the most recently received byte of the stream.
	 *
	 * @return the first byte in the stream.
	 */
	public byte getLastByte() {
		return (byte) lastByte;
	}

	private void setLastByte(final int lastByte) {
		this.lastByte = lastByte;
	}

	/**
	 * Obtain the log4j root logger name.
	 *
	 * @return String the log4j root logger name
	 */
	private String getRootLoggerName() {
		return (new String(rootLoggerName));
	}

	/**
	 * Set the log4j root logger name.
	 *
	 * @param rootLoggerName
	 *            The name of the log4j root logger
	 */
	private void setRootLoggerName(String rootLoggerName) {
		this.rootLoggerName = new String(rootLoggerName);
		logger = Logger.getLogger(String.format("%s.%s", this.rootLoggerName, getClazzName()));
	}

	private Logger getLogger() {
		if (logger == null) {
			logger = Logger.getLogger(String.format("%s.%s", getRootLoggerName(), getClazzName()));
		}

		return logger;
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

		if (this.getClass().getSimpleName() != null && this.getClass().getSimpleName().length() != 0) {
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

	/**
	 * Convert the content of the stream to a UTF-8 character set String.
	 */
	@Override
	public String toString() {
		String returnValue = null;

		try {
			returnValue = getOutput().toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			getLogger().error(e.getMessage(), e);
		}

		return returnValue;
	}
}