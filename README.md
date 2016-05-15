# MockTCPServer

MockTCPServer simulates a TCP Server in environments where the *real* server is not available, or it connects to other systems that are not available. MockTCPServer has a number of features specifically designed for Unit/Integration/System testing. No particular Test Framework is preferred or required.

Sometimes, installing and configuring one or more enterprise application in a System Test environment is simply not worth it. Of course, the *real* TCP Server may still be in development but work on dependant systems must continue.

## Getting Started

MockTCPServer JAR binaries are distributed via the Maven Central Repository, where there is details of how to add dependency information for all major build systems e.g. Maven, Gradle, Ivy, etc. For example, this dependency information can be added to a Maven POM configuration file : -
```xml
<dependency>
    <groupId>io.cloudracer</groupId>
    <artifactId>MockTCPServer</artifactId>
    <version>1.1.0</version>
</dependency>
```
[JUnit](http://junit.org/) will be used here to provide code snippets that describe "best practice" when using MockTCPServer. The MockTCPServer can be easily started during the test setup and, purely for the purposes of this tutorial, a TCPClient will also be started: -
```javascript
private MockTCPServer server;
private TCPClient client;

@Before
public void setUp() throws IOException {
    this.server = new MockTCPServer();
    this.client = new TCPClient(6789);
}
```
MockTCPServer started on port `6789` by default; consequently, the TCPClient is initialised to communicate on that port.
 
Of course, it is good practice to release resources at the end of the test: -
```javascript
@After
public void cleanUp() throws IOException {
    this.client.close();
    this.server.close();
}
```
All the following code snippets are written with the assumption that the Before and After routines above are in place.

### A Simple Message

This is the most basic demonstration of the MockTCPServer. The message `Hello World\r\n\n` is sent and the Acknowledgement `A` is returned, as expected.
```javascript
/**
 * Server returns the expected ACK to a properly terminated message.
 *
 * @throws ClassNotFoundException see source documentation.
 * @throws IOException see source documentation.
 */
@Test(timeout = TIMEOUT)
public void ack() throws ClassNotFoundException, IOException {
    assertArrayEquals("A".getBytes(), this.client.send("Hello World\r\n\n").toByteArray());
}
```
### Custom Message Terminator

Specify a different message terminator using the MockTCPServer <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#setTerminator(byte[])" target="_blank">setTerminator()</a> method.
```javascript
/**
 * Having set a customised terminator, the server returns the expected ACK to a message terminated with the custom terminator.
 *
 * @throws ClassNotFoundException see source documentation.
 * @throws IOException see source documentation.
 * @throws InterruptedException
 */
@Test
public void docTest() throws ClassNotFoundException, IOException, InterruptedException {
    final byte[] customTerminator = new byte[] { 88, 89, 90 }; // XYZ
    final String message = String.format("%s%s", "Hello World", new String(customTerminator));
    
    // Set the custom terminator.
    this.server.setTerminator(customTerminator);
    
    // Send a message with the correct terminator (i.e. the custom on we set at the start of this method) and wait for the response.
    assertArrayEquals("A".getBytes(), this.client.send(message).toByteArray());
}
```
### Force NAK

Force the MockTCPServer to **always** return a NAK (not acknowledged) when ```true``` is passed to the <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#setIsAlwaysNAKResponse(boolean)" target="_blank">setIsAlwaysNAKResponse()</a> method.
```javascript
/**
 * Having set the Server to always return a NAK, the Server returns the expected NAK when an ACK would normally be expected.
 *
 * @throws ClassNotFoundException see source documentation.
 * @throws IOException see source documentation.
 */
@Test
public void forceNAK() throws ClassNotFoundException, IOException {
    this.server.setIsAlwaysNAKResponse(true);

    assertArrayEquals("N".getBytes(), this.client.send("Hello World\r\n\n").toByteArray());
}
```
### Force No Response

Force the MockTCPServer to **never** return a response when ```true``` is passed to the <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#setIsAlwaysNoResponse(boolean)" target="_blank">setIsAlwaysNoResponse()</a> method.
```javascript
/**
 * Having set the Server to never respond, wait for the Server {@link Thread} to die. If the server has not responded after 5 seconds, assume that it never will.
 *
 * @throws InterruptedException see source documentation.
 */
@Test
public void forceNoResponse() throws InterruptedException {
    this.server.setIsAlwaysNoResponse(true);

    final Thread waitForResponse = new Thread("WaitForResponse") {
        @Override
        public void run() {
            try {
                TestMockTCPServerST.this.client.send("Hello World\r\n\n");
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    };
    waitForResponse.start();

    final int timeout = 5000; // 5 seconds.
    waitForResponse.join(timeout);
    assertTrue(waitForResponse.isAlive());
}
```
### Force Close After the Next Response

Pass ```true``` to the <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#setIsCloseAfterNextResponse(boolean)" target="_blank">setIsCloseAfterNextResponse()</a> method, to instruct the server to close down after it responds to the next message it is sent.
```javascript
/**
 * Having set the Server to close after the next response, wait for the Server {@link Thread} to die after sending one message.
 *
 * @throws ClassNotFoundException see source documentation.
 * @throws IOException see source documentation.
 * @throws InterruptedException see source documentation.
 */
@Test
public void forceCloseAfterNextResponse() throws ClassNotFoundException, IOException, InterruptedException {
    this.server.setIsCloseAfterNextResponse(true);

    assertArrayEquals("A".getBytes(), this.client.send("Hello World\r\n\n").toByteArray());

    // Wait for the MockTCPServer to Thread to die.
    final int timeout = 5000; // 5 seconds.
    this.server.join(timeout);

    assertFalse(this.server.isAlive());
}
```
### Expect a Specific Message

The MockTCPServer can be instructed to expect **only** messages that match the Regular Expression passed to the <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#setExpectedMessage(java.lang.String)" target="_blank">setExpectedMessage()</a> method. MockTCPServer will respond with a <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#getNAK()" target="_blank">NAK</a>, and records and <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#getAssertionError()" target="_blank">Assertion Error</a>, if it receives a message that does not mathch the Regular Expression, and <a href="http://www.cloudracer.org/mocktcpserver/docs/api/latest/io/cloudracer/mocktcpserver/MockTCPServer.html#getACK()" target="_blank">ACK</a> if it does.
```javascript
/**
 * Having set the Server to expect only messages that match a specified Regular Expression, ensure that a NAK is returned for messages that do not match and an ACK for messages that do match.
 *
 * @throws ClassNotFoundException see source documentation.
 * @throws IOException see source documentation.
 */
@Test
public void expectSpecificMessage() throws ClassNotFoundException, IOException {
    this.server.setExpectedMessage("Hello.*\r\n\n");

    assertArrayEquals("A".getBytes(), this.client.send("Hello World\r\n\n").toByteArray());
    assertNull(this.server.getAssertionError());
    assertArrayEquals("N".getBytes(), this.client.send("Invalid\r\n\n").toByteArray());
    assertNotNull(this.server.getAssertionError());
}
```
