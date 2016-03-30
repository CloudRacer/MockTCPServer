package io.cloudracer.mocktcpserver.datastream;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataStream implements Closeable {

    private Logger logger;

    public final static int DEFAULT_TAIL_MAXIMUM_LENGTH = 3;

    private ByteArrayOutputStream output;
    private Deque<Byte> tailQueue = null;
    private Byte lastByte;
    private Integer tailMaximumLength = null;
    private String rootLoggerName;

    /**
     * Default constructor.
     * <p>
     * Use a default {@link DataStream#getTailMaximumLength() tail length} of {@link DataStream#DEFAULT_TAIL_MAXIMUM_LENGTH} and a default {@link LogManager#getLogger(Class) log4j root logger} of the Class name.
     */
    public DataStream() {}

    /**
     * Specify a log4j root logger.
     *
     * @param rootLoggerName
     *        log4j root logger
     */
    public DataStream(final String rootLoggerName) {
        setRootLoggerName(rootLoggerName);
    }

    /**
     * Specify a {@link DataStream#getTailMaximumLength() maximum length of the stream tail}.
     *
     * @param tailMaximumLength
     *        tail length.
     */
    public DataStream(final int tailMaximumLength) {
        setTailMaximumLength(tailMaximumLength);
    }

    /**
     * Specify a {@link DataStream#getRootLoggerName() log4j root logger} and the {@link DataStream#getTailMaximumLength() maximum length of the stream tail}.
     *
     * @param rootLoggerName
     *        log4j root logger.
     * @param tailMaximumLength
     *        tail length.
     */
    public DataStream(final int tailMaximumLength, final String rootLoggerName) {
        setTailMaximumLength(tailMaximumLength);
        setRootLoggerName(rootLoggerName);
    }

    /**
     * Write byte to the {@link ByteArrayOutputStream#write(int)}.
     *
     * @param data
     *        written to the {@link ByteArrayOutputStream}.
     * @return the data written (equal to the data parameter).
     * @throws IOException
     *         see source documentation
     */
    public synchronized int write(final int data) throws IOException {
        getOutput().write(data);

        return data;
    }

    /**
     * {@link PipedOutputStream#close() close} the {@link PipedOutputStream output stream}.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        setOutput(null);
    }

    /**
     * Delegate of {@link ByteArrayOutputStream#size()}.
     *
     * @return the current size of the stream. see source documentation
     */
    public synchronized int size() {
        return getOutput().size();
    }

    private synchronized ByteArrayOutputStream getOutput() {
        if (this.output == null) {
            this.output = new ByteArrayOutputStream() {
                @Override
                public synchronized void write(final int b) {
                    super.write(b);
                    setLastByte((byte) b);
                    addToTailList();
                }
            };
        }

        return this.output;
    }

    /**
     * Delegate of {@link ByteArrayOutputStream#toByteArray()}.
     *
     * @return a byte array, containing a copy of the DataStream.
     */
    public synchronized byte[] toByteArray() {
        return getOutput().toByteArray();
    }

    /**
     * Copies the stream to a newly created {@link PipedInputStream input stream}. The {@link PipedInputStream input stream} is cached internally and only reinitialised when new bytes are written to the stream.
     * <p>
     * The stream is copied without making one or more intermediary copies of the content (i.e. it is memory efficient).
     *
     * @return {@link PipedInputStream input stream} populated with all available data.
     * @throws IOException
     */
    public PipedInputStream toInputStream() throws IOException {
        PipedInputStream inputStream = null;

        if (getOutput().size() == 0) {
            inputStream = new PipedInputStream();
        } else {
            inputStream = new PipedInputStream(getOutput().size());
            final PipedOutputStream outputStream = new PipedOutputStream(inputStream);

            final Thread copy = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getOutput().writeTo(outputStream);
                    } catch (final IOException e) {
                        DataStream.this.logger.error(e.getMessage(), e);
                    }
                }
            });
            copy.setName(String.format("%s-InputStream-Creator", getThreadName()));
            copy.start();
            try {
                copy.join();
            } catch (final InterruptedException e) {
                this.logger.error(e.getCause(), e);
            }
        }

        return inputStream;

    }

    /**
     * {@link ByteArrayOutputStream#close() close} the {@link ByteArrayOutputStream output stream}.
     *
     * @param output
     *        if null, the current {@link ByteArrayOutputStream output stream} is closed before being reinitialised.
     * @throws IOException
     */
    private void setOutput(final ByteArrayOutputStream output) throws IOException {
        if ((output == null) && (this.output != null)) {
            IOUtils.closeQuietly(this.output);
        }

        this.output = output;

    }

    /**
     * The length of the tail.
     *
     * @return the length of the tail. Default of {@link DataStream#DEFAULT_TAIL_MAXIMUM_LENGTH}.
     */
    public int getTailMaximumLength() {
        if (this.tailMaximumLength == null) {
            this.tailMaximumLength = DataStream.DEFAULT_TAIL_MAXIMUM_LENGTH;
        }

        return this.tailMaximumLength;
    }

    private void setTailMaximumLength(final int tailLength) {
        this.tailMaximumLength = tailLength;
    }

    /**
     * The tail of the specified {@link DataStream#getTailMaximumLength() length}.
     *
     * @return the {@link Bytes} that represent the tail of the DataStrean.
     */
    private Deque<Byte> getTailQueue() {
        if (this.tailQueue == null) {
            this.tailQueue = new ArrayDeque<Byte>(getTailMaximumLength());
        }

        return this.tailQueue;
    }

    /**
     * The tail of the specified {@link DataStream#getTailMaximumLength() length}.
     *
     * @return a byte[] containing the tail data, of specified maximum length.
     */
    public byte[] getTail() {
        // Convert the list to an array.
        final byte[] tail = new byte[getTailQueue().size()];
        int i = 0;
        for (final Iterator<Byte> iterator = getTailQueue().iterator(); iterator.hasNext();) {
            final Byte nextByte = iterator.next();
            tail[i] = nextByte;

            i++;
        }

        return tail;
    }

    private void addToTailList() {
        getTailQueue().addLast(getLastByte());
        if (getTailQueue().size() > getTailMaximumLength()) {
            getTailQueue().removeFirst();
        }
    }

    /**
     * Get the most recently received byte of the stream.
     *
     * @return the first byte in the stream.
     */
    public Byte getLastByte() {
        return this.lastByte;
    }

    private void setLastByte(final byte lastByte) {
        this.lastByte = lastByte;
    }

    /**
     * Obtain the log4j root logger name.
     *
     * @return String the log4j root logger name
     */
    private String getRootLoggerName() {
        if (this.rootLoggerName == null) {
            this.rootLoggerName = getClssName();
        }

        return (new String(this.rootLoggerName));
    }

    /**
     * Set the log4j {@link Logger#getLogger root logger} name.
     *
     * @param rootLoggerName
     *        the name of the log4j root logger
     */
    private void setRootLoggerName(final String rootLoggerName) {
        this.rootLoggerName = new String(rootLoggerName);
        this.logger = LogManager.getLogger(String.format("%s.%s", this.rootLoggerName, getClssName()));
    }

    private Logger getLogger() {
        if (this.logger == null) {
            this.logger = LogManager.getLogger(String.format("%s.%s", getRootLoggerName(), getClssName()));
        }

        return this.logger;
    }

    private String getThreadName() {
        final String delimeter = ".";
        final String regEx = "\\.";

        String name = null;

        if ((this.getClass().getSimpleName() != null) && (this.getClass().getSimpleName().length() > 0)) {
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
     * @return a Class name.
     */
    private String getClssName() {
        final String delimeter = ".";
        final String regEx = "\\.";

        String name = null;

        if ((this.getClass().getSimpleName() != null) && (this.getClass().getSimpleName().length() != 0)) {
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
        } catch (final UnsupportedEncodingException e) {
            getLogger().error(e.getMessage(), e);
        }

        return returnValue;
    }
}