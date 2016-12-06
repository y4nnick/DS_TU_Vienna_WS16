package client.sender;

import java.io.IOException;

public interface Sender {

    /**
     * Sends the message to the server
     * @param message the message
     * @return the answer from the server
     * @throws IOException if the message could not be send
     */
    String send(String message) throws IOException;

    /**
     * Closes all used resources
     */
    void close();
}
