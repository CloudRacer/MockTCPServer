package io.cloudracer.datastream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import io.cloudracer.TestConstants;

public class DataStreamUT {

    @Test
    public void dataStream() throws IOException {
        DataStream dataStream = new DataStream(this.getClass().getSimpleName());

        writeStringToStream(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR, dataStream);

        dataStream.close();
    }

    @Test
    public void dataStream100MB() throws IOException {
        final int numberOfBytesToWrite = 102400000; // 10Mb
        final byte testCharacter = 65; // 65 = A.
        final byte testTail[] = new byte[] { 66, 67, 68 }; // 66 = B, 67 = C, 68
                                                           // = D.
        int totalLength = numberOfBytesToWrite + testTail.length;
        byte testStream[] = new byte[totalLength];

        DataStream dataStream = new DataStream(this.getClass().getSimpleName());
        checkDataStreamInternalConsistency(dataStream);

        // Write the test data to the stream.
        for (int i = 0; i < numberOfBytesToWrite; i++) {
            // Create an array using the test data, which can be compared to the stream in order to assert that the stream is as expected.
            testStream[i] = testCharacter;
            // write the test data to the stream.
            dataStream.write(testCharacter);
        }

        checkDataStreamInternalConsistency(dataStream, new String(Arrays.copyOf(testStream, testStream.length - testTail.length), "UTF-8"));

        // Write the test tail to the stream.
        for (int i = 0; i < testTail.length; i++) {
            dataStream.write(testTail[i]);
        }
        // Create add the tail data to the test array.
        System.arraycopy(testTail, 0, testStream, testStream.length - testTail.length, testTail.length);
        final String testString = new String(testStream, "UTF-8");
        checkDataStreamInternalConsistency(dataStream, testString);

        dataStream.close();
    }

    @Test
    public void dataStreamWithCustomTailLength() throws IOException {
        final int maximumTailLength = 10;
        DataStream dataStream = new DataStream(maximumTailLength);

        writeStringToStream(TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR, dataStream);

        checkDataStreamInternalConsistency(dataStream, TestConstants.WELLFORMED_XML_WITH_VALID_TERMINATOR);
    }

    private void writeStringToStream(final String data, final DataStream dataStream) throws IOException {
        final byte[] dataBytes = data.getBytes();
        final StringBuilder dataWritten = new StringBuilder();

        for (int i = 0; i < dataBytes.length; i++) {
            dataStream.write(dataBytes[i]);
            byte[] nextByte = new byte[] { dataBytes[i] };
            dataWritten.append(new String(nextByte, "UTF-8"));

            checkDataStreamInternalConsistency(dataStream, dataWritten.toString());
        }
    }

    private void checkDataStreamInternalConsistency(final DataStream dataStream) throws IOException {
        checkDataStreamInternalConsistency(dataStream, null);
    }

    /**
     * Conduct a series of checks that must always pass on <b>every</b> {@link DataStream} at <b>any</b> time.
     *
     * @param dataStream the {@link DataStream} to check.
     * @throws IOException
     */
    private void checkDataStreamInternalConsistency(final DataStream dataStream, final String data) throws IOException {
        assertTrue("Tail data longer than required tail length.", dataStream.getTail().length <= dataStream.getTailMaximumLength());
        assertTrue("Tail data longer than stream size.", dataStream.getTail().length <= dataStream.size());

        // toInputStream() conversion; the converted size equals the DataStrem size.
        final byte[] input = new byte[dataStream.size()];
        dataStream.toInputStream().read(input);
        assertEquals("Input stream of unexpected size.", dataStream.size(), input.length);
        assertArrayEquals("Input stream of unexpected value.", input, dataStream.toString().getBytes());

        // toString() conversion; the converted size equals the DataStrem size.
        byte[] expectedString = dataStream.toString().getBytes();
        assertEquals("toString() conversion of unexpected size.", dataStream.size(), expectedString.length);
        assertArrayEquals("toString() compared to toInputStream().", input, expectedString);

        // getTail() size, and content, equals the DataStrem.
        final byte[] expectedTail = Arrays.copyOfRange(expectedString, expectedString.length - dataStream.getTail().length, expectedString.length);
        assertEquals("Tail of unexpected size.", expectedTail.length, dataStream.getTail().length);
        assertArrayEquals("Tail contains an unexpected value.", expectedTail, dataStream.getTail());

        if (dataStream.size() == 0) {
            assertEquals("getTailLength() of returns an unexpected value.", 0, dataStream.size());
            assertEquals("size() returns an unexpected value.", 0, dataStream.getOutput().size());
            assertEquals("getTailLength() of returns an unexpected value.", 0, dataStream.getTail().length);
            assertNull("getLastByte() of returns an unexpected value.", dataStream.getLastByte());
        } else if (dataStream.size() > dataStream.getTail().length) {
            assertEquals("getTailLength() of returns an unexpected value.", dataStream.getTailMaximumLength(), dataStream.getTail().length);
        } else if (dataStream.size() < dataStream.getTail().length) {
            assertEquals("getTailLength() of returns an unexpected value.", dataStream.size(), dataStream.getTail().length);
        } else if (dataStream.size() == dataStream.getTail().length) {
            assertEquals("getTailLength() of returns an unexpected value.", dataStream.size(), dataStream.getTail().length);
        }

        if (data != null) {
            assertEquals("getLastByte() of returns an unexpected value.", data.getBytes()[data.length() - 1], dataStream.getLastByte(), 0);
            assertEquals("DataStream size different to data size.", data.length(), dataStream.size());
        }
    }
}