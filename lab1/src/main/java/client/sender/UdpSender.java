package client.sender;

import util.Helper;

import java.io.IOException;
import java.net.*;

public class UdpSender implements Sender {

    //UDP serverSocket
    DatagramSocket socket;

    //Server IP and Port
    String serverIP;
    Integer serverPort;

    /**
     * Creates a new UdpSender instance
     * @param serverIP the IP address of the server
     * @param serverPort the port of the server
     */
    public UdpSender(String serverIP, Integer serverPort) {

        this.serverIP = serverIP;
        this.serverPort = serverPort;

        // Create UDP socket
        try{
            socket = new DatagramSocket();
        }catch (SocketException ex) {
            System.out.println(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Sends the message over UDP
     * @param message the message
     * @return the answer from the server
     * @throws IOException if the message could not be send
     */
    public String send(String message) throws IOException{

        String response = "";

        try{

            // Create message packet
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,InetAddress.getByName(serverIP),serverPort);

            // Send packet
            socket.send(packet);

            //Wait for answer (blocking)
            buffer = new byte[1024];
            packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            //Read out response
            response = Helper.getResponseFromPacket(packet);

        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to host: " + e.getMessage());
        }

        return response;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void close() {

        //Close UDP-Socket
        if(socket != null && !socket.isClosed()){
            socket.close();
        }

    }
}
