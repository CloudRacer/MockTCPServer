package io.cloudracer.mocktcpserver.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A DAO class that contains a {@link Map} of incoming messages with corresponding responses i.e. message sent when a specific message is received.
 *
 * @author John McDonnell
 *
 */
public class Responses {

    private final HashMap<String, List<ResponseDAO>> messageResponses = new HashMap<>();

    /**
     * Add a {@link ResponseDAO response} to a received message
     *
     * @param receivedMessage the received message.
     * @param response the {@link ResponseDAO response} to send
     */
    public void add(String receivedMessage, ResponseDAO response) {
        if (messageResponses.containsKey(receivedMessage)) {
            messageResponses.get(receivedMessage).add(response);
        } else {
            final List<ResponseDAO> responseDAOList = new ArrayList<>();
            responseDAOList.add(response);
            messageResponses.put(receivedMessage, responseDAOList);
        }
    }

    /**
     * Create a read-only copy of the responses
     *
     * @return a read-only copy of the responses.
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<ResponseDAO>> getResponses() {
        return (Map<String, List<ResponseDAO>>) messageResponses.clone();
    }

    @Override
    public String toString() {
        return "Responses [messageResponses=" + messageResponses + "]";
    }

}
