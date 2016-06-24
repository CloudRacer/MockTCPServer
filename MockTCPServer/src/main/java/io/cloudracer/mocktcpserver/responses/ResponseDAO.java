package io.cloudracer.mocktcpserver.responses;

/**
 * A {@link #getResponse() message} to send, as described by its destination {@link #getMachineName() machine} and {@link #getPort() port}.
 *
 * @author John McDonnell
 *
 */
public class ResponseDAO {

    private String machineName;
    private int port;
    private String response;

    /**
     * A {@link #getResponse() message} to send to a {@link #getPort() port} on a {@link #getMachineName() machine}.
     *
     * @param machineName the machine name to send the response to
     * @param port the port on the machine that the response is to be sent to
     * @param response the response message to send
     */
    public ResponseDAO(String machineName, int port, String response) {
        setMachineName(machineName);
        setPort(port);
        setResponse(response);
    }

    /**
     * The machine that the {@link ResponseDAO#getResponse() response} message is to be sent to.
     *
     * @return the machine name
     */
    public String getMachineName() {
        return machineName;
    }

    private void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    /**
     * The port, on the {@link ResponseDAO#getMachineName() machine}, that the {@link ResponseDAO#getMessage() message} is to be sent to.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    private void setPort(int port) {
        this.port = port;
    }

    /**
     * The message to send to the specified {@link ResponseDAO#getMachineName() machine name} and {@link ResponseDAO#getPort() port}.
     *
     * @return the response message
     */
    public String getResponse() {
        return response;
    }

    private void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "ResponseDAO [machineName=" + machineName + ", port=" + port + ", response=" + response + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((machineName == null) ? 0 : machineName.hashCode());
        result = prime * result + port;
        result = prime * result + ((response == null) ? 0 : response.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResponseDAO other = (ResponseDAO) obj;
        if (machineName == null) {
            if (other.machineName != null) {
                return false;
            }
        } else if (!machineName.equals(other.machineName)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (response == null) {
            if (other.response != null) {
                return false;
            }
        } else if (!response.equals(other.response)) {
            return false;
        }
        return true;
    }
}