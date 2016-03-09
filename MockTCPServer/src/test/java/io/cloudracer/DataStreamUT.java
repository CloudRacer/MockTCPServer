package io.cloudracer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class DataStreamUT {

    @Test
    public void dataStreamTest() throws IOException {
        DataStream dataStream = new DataStream(this.getClass().getSimpleName());

        writeStringToStream(HostMessageTypeConstants.MESSAGE_8, dataStream);
        assertArrayEquals("Tail contains an unexpected value.", dataStream.getTail(), HostMessageTypeConstants.VALID_TERMINATOR.getBytes());
        assertEquals("getTailLength() of returns an unexpected value.", dataStream.getTailLength(), dataStream.getTail().length, 0);

        assertEquals("getLastByte() of returns an unexpected value.", dataStream.getLastByte(), HostMessageTypeConstants.MESSAGE_8.getBytes()[HostMessageTypeConstants.MESSAGE_8.length() - 1]);
        assertEquals("getLength() of returns an unexpected value.", dataStream.size(), HostMessageTypeConstants.MESSAGE_8.length(), 0);

        // Ensure that the stream can be successfully read more than once (i.e. the stream can be reset).
        assertEquals("toString() of returns an unexpected value.", dataStream.toString(), HostMessageTypeConstants.MESSAGE_8);
        assertEquals("toString() of returns an unexpected value.", dataStream.toString(), HostMessageTypeConstants.MESSAGE_8);

        dataStream.close();
    }

    @Test
    public void OneHundredMBDataStreamTest() throws IOException {
        final int numberOfBytesToWrite = 102400000; // 100Mb
        final byte testCharacter = 65; // 65 = A.
        final byte testTail[] = new byte[] { 66, 67, 68 }; // 66 = B, 67 = C, 68 = D.
        final int totalLength = numberOfBytesToWrite + testTail.length;
        final byte testStream[] = new byte[totalLength];

        DataStream dataStream = new DataStream(this.getClass().getSimpleName());
        checkDataStreamInternalConsistency(dataStream);

        // Write the test data to the stream.
        for (int i = 0; i < numberOfBytesToWrite; i++) {
            // Create an array using the test data, which can be compared to the stream in order to assert that the stream is as expected.
            testStream[i] = testCharacter;
            // write the test data to the stream.
            dataStream.write(testCharacter);

            checkDataStreamInternalConsistency(dataStream);
        }
        // Write the test tail to the stream.
        for (int i = 0; i < testTail.length; i++) {
            dataStream.write(testTail[i]);

            checkDataStreamInternalConsistency(dataStream);
        }
        // Create add the tail data to the test array.
        System.arraycopy(testTail, 0, testStream, testStream.length - testTail.length, testTail.length);
        final String testString = new String(testStream);

        // Check the size.
        assertEquals("size() of returns an unexpected value.", totalLength, dataStream.size(), 0);

        // Check the tail.
        assertEquals("getTailLength() of has an unexpected value.", testTail.length, dataStream.getTailLength(), 0);
        assertArrayEquals("getTail() contains an unexpected value.", testTail, dataStream.getTail());

        // Ensure that the stream can be successfully read more than once (i.e. the stream can be reset).
        assertEquals("toString() of has an unexpected value.", testString, dataStream.toString());
        assertEquals("toString() of has an unexpected value.", testString, dataStream.toString());

        // Ensure that the stream can be successfully copied more than once (i.e. the stream can be reset).
        assertEquals("copyToInputStream() of has an unexpected value.", testString, convertCopiedStreamToString(dataStream));
        assertEquals("copyToInputStream() of has an unexpected value.", testString, convertCopiedStreamToString(dataStream));

        dataStream.close();
    }

    private String convertCopiedStreamToString(final DataStream dataStream) throws IOException {
        final InputStream inputStream = dataStream.copyToInputStream();
        final byte rawData[] = new byte[dataStream.size()];
        inputStream.read(rawData, 0, rawData.length);

        return new String(rawData);
    }

    private void writeStringToStream(final String data, final DataStream dataStream) throws IOException {
        final byte[] dataBytes = data.getBytes();

        for (int i = 0; i < dataBytes.length; i++) {
            dataStream.write(dataBytes[i]);

            checkDataStreamInternalConsistency(dataStream);
        }
    }

    /**
     * Conduct a series of checks that must always pass on <b>every</b> {@link DataStream} at <b>any</b> time.
     *
     * @param dataStream the {@link DataStream} to check.
     */
    private void checkDataStreamInternalConsistency(final DataStream dataStream) {
        assertTrue("Tail data longer than required tail length.", dataStream.getTail().length <= dataStream.getTailLength());
    }
}