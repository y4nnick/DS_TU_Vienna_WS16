package at.beachcrew;

import java.io.IOException;

public class Main {

    private static final String MAT_NR = "1229026";
    private static final String TUWEL_ID = "49665";

    private static final String HOST = "dslab.duckdns.org";
    private static final Integer PORT = 9000;

    public static void main(String[] args) {

        //Create client
        TCPClient client = new TCPClient();

        try{

            //Connect to server
            client.connect(HOST,PORT);
            System.out.println("Connection to server established");

            //Send registration message
            client.sendRegistrationMessage(MAT_NR,TUWEL_ID);

        }catch (IOException e) {
            System.out.print("Could not connect to the server or could not send message: " + e.getMessage());
            e.printStackTrace();

        }catch (Exception e){
            System.out.print("Unexpected error: " + e.getMessage());
            e.printStackTrace();

        }finally {

            //Close the client
            try{
                client.close();
            }catch (IOException e){
                System.out.print("Could not close the client: " + e.getMessage());
                e.printStackTrace();
            }

        }
    }
}
