package client.sender;

import client.listener.TcpListener;

import java.io.*;
import java.net.Socket;

public class TcpSender implements Sender{

    Socket serverSocket;

    //Reader and Writer to the server
    BufferedReader serverReader = null;
    PrintWriter serverWriter = null;

    //Message which was readed from the TCP listener
    private String msg = "";


    /**
     * Creates a new TcpSender instance
     * @param socket the server Socket
     */
    public TcpSender(Socket socket) {
        this.serverSocket = socket;

        try{

            // create a reader to retrieve messages send by the server
            serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            // create a writer to send messages to the server
            serverWriter = new PrintWriter(serverSocket.getOutputStream(), true);

        }catch (IOException e){
            System.out.println(e.getClass().getSimpleName() + ": "+ e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Sends the message over TCP
     * @param message the message
     * @return allways null because the answer will get printed from the TcpListener
     * @throws IOException if the message could not be send
     */
    @Override
    public synchronized String send(String message) throws IOException{

        // Write message to the serverWriter
        serverWriter.println(message);
        serverWriter.flush();

        return serverReader.readLine();
      //
    }

    /**
     * Sends the response for a call
     * @param message the response from the server
     */
    public void sendMessage(String message){
        this.msg = message;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void close() {

        //Close server Reader
        try {
            if (serverReader != null) {
                serverReader.close();
            }
        } catch (IOException ex) {
            // Ignored because we cannot handle it
        }

        //Close server Writer
        if (serverWriter != null) {
            serverWriter.close();
        }

        //Close socket if not allready closed
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignored because we cannot handle it
        }

    }
}
