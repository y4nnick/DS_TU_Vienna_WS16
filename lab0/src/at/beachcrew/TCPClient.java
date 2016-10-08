package at.beachcrew;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPClient {

    // The socket connection
    private Socket socket;

    /**
     * Generates an TCPClient instance
     */
    public TCPClient(){}

    /**
     * Connects the client to a TCP Server
     * @param host The host adress of the server
     * @param port The port of the server
     */
    public void connect(String host, Integer port) throws IOException{
        socket = new Socket();

        SocketAddress sockaddr = new InetSocketAddress(host, port);

        socket.connect(sockaddr, 1000);

        if (socket.isConnected()){
            return;
        }else{
            throw new IOException("Connection could not be established");
        }
    }

    /**
     * Closes the client
     */
    public void close() throws IOException{
        if(socket != null){
            socket.close();
        }
    }

    /**
     * Sends the registration message to the server
     * @param MatNr The immatriculation number
     * @param TuwelID The TUWEL ID
     */
    public String sendRegistrationMessage(String MatNr, String TuwelID) throws Exception,IOException{

        String result = "";
        String message = "!login "+MatNr+" "+TuwelID;

        //Create Streams
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //Send message
        out.println(message);
        System.out.println("Message send to server, waiting for answer...");

        //Listen to server
        String fromServer;

        while ((fromServer = in.readLine()) != null) {
            System.out.println("Server: " + fromServer);
            result += fromServer + "\n";

            if (fromServer.contains("Your registration was successful! Your DSLab account is")) {
                break;
            } else if (fromServer.contains("Received login command for")) {
                //Wait for next message
            } else {
                throw new Exception("Unknown message from the server: " + fromServer);
            }
        }

        return result;
    }

}
