/**
 * A general purpose {@link java.io.OutputStream} which provides operations that are particularly useful in managing large streams of terminated (i.e. a purpose specific series of characters that identify the logical end of a message or stream) large amounts of data e.g. an XML document or binary file.
 * <p>
 * The functionality provided in this package is designed specifically with performance in mind. However, the {@link java.io.ByteArrayOutputStream} is used for the internal store of data, so all data is held <b>in memory</b> and has an inherited maximum capacity of 2GB (to be exact, {@link java.lang.Integer#MAX_VALUE} - 2).
 *
 * @author John McDonnell
 **/
package io.cloudracer.mocktcpserver.datastream;