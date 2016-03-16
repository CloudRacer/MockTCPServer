package io.cloudracer.datastream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class DataStream {

    private Logger logger;

    public final static int TAIL_MAXIMUM_LENGTH_DEFAULT = 3;

    private ByteArrayOutputStream output;
    private Deque<Byte> tailQueue = null;
    private Byte lastByte;
    private Integer tailMaximumLength = null;
    private String rootLoggerName;

    /**
     * Default constructor.
     * <p>
     * Use a default {@link DataStream#getTailMaximumLength() tail length} of {@link DataStream#TAIL_MAXIMUM_LENGTH_DEFAULT} and a default {@link Logger#getLogger(Class) log4j root logger} of the Class name.
     *
     * @param rootLoggerName log4j root logger
     */
    public DataStream() {
        setRootLoggerName(getClssName());
    }

    /**
     * Specify a log4j root logger.
     *
     * @param rootLoggerName log4j root logger
     */
    public DataStream(final String rootLoggerName) {
        setRootLoggerName(rootLoggerName);
    }

    /**
     * Specify a {@link DataStream#getTailMaximumLength() maximum length of the stream tail}.
     *
     * @param tailMaximumLength tail length.
     */
    public DataStream(final int tailMaximumLength) {
        setTailMaximumLength(tailMaximumLength);
    }

    /**
     * Specify a {@link DataStream#getRootLoggerName() log4j root logger} and the {@link DataStream#getTailMaximumLength() maximum length of the stream tail}.
     *
     * @param rootLoggerName log4j root logger.
     * @param tailMaximumLength tail length.
     */
    public DataStream(final String rootLoggerName, final int tailMaximumLength) {
        setTailMaximumLength(tailMaximumLength);
        setRootLoggerName(rootLoggerName);
    }

    /**
     * Write byte to the {@link ByteArrayOutputStream#write(int)}.
     *
     * @param data written to the {@link ByteArrayOutputStream}.
     * @return the data written (equal to the data parameter).
     * @throws IOException
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
                    setLastByte((byte) b);
                    addToTailList();
                }
            };
        }

        return output;
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
        }

        return inputStream;

    }

    /**
     * {@link ByteArrayOutputStream#close() close} the {@link ByteArrayOutputStream output stream}.
     *
     * @param output if null, the current {@link ByteArrayOutputStream output stream} is closed before being reinitialised.
     * @throws IOException
     */
    private void setOutput(final ByteArrayOutputStream output) throws IOException {
        if (output == null && this.output != null) {
            IOUtils.closeQuietly(this.output);
        }

        this.output = output;

    }

    /**
     * The length of the tail.
     *
     * @return the length of the tail. Default of {@link DataStream#TAIL_MAXIMUM_LENGTH_DEFAULT}.
     */
    public int getTailMaximumLength() {
        if (tailMaximumLength == null) {
            tailMaximumLength = TAIL_MAXIMUM_LENGTH_DEFAULT;
        }

        return tailMaximumLength;
    }

    private void setTailMaximumLength(final int tailLength) {
        this.tailMaximumLength = tailLength;
    }

    /**
     * The tail of the specified {@link DataStream#getTailMaximumLength() length}.
     *
     * @return
     */
    private Deque<Byte> getTailQueue() {
        if (tailQueue == null) {
            tailQueue = new ArrayDeque<Byte>(getTailMaximumLength());
        }

        return tailQueue;
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
        for (Iterator<Byte> iterator = getTailQueue().iterator(); iterator.hasNext();) {
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
        return lastByte;
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
        return (new String(rootLoggerName));
    }

    /**
     * Set the log4j {@link Logger#getLogger root logger} name.
     *
     * @param rootLoggerName the name of the log4j root logger
     */
    private void setRootLoggerName(String rootLoggerName) {
        this.rootLoggerName = new String(rootLoggerName);
        logger = Logger.getLogger(String.format("%s.%s", this.rootLoggerName, getClssName()));
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(String.format("%s.%s", getRootLoggerName(), getClssName()));
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
     * @return a Class name.
     */
    private String getClssName() {
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