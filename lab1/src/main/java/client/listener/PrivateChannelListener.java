package client.listener;

import client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class PrivateChannelListener implements Runnable {

	private ServerSocket serverSocket;
	private Client client;

	PrintWriter userOutputStream;

	public PrivateChannelListener(ServerSocket serverSocket, Client client, PrintWriter userOutputStream) {
		this.serverSocket = serverSocket;
		this.client = client;
		this.userOutputStream = userOutputStream;
	}

	public void run() {

		while (true) {
			Socket socket = null;
			try {

				// wait for Client to connect
				socket = serverSocket.accept();

				// Create Reader on socket
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				// Prepare the writer for responding to clients requests
				PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

				// Read the message
				String message = reader.readLine();
				userOutputStream.println(message);

				writer.println("!ack");

				socket.close();

			} catch (SocketException sex){
				// Socket got closed because server shuts down
				//sex.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
				System.err.println("Error occurred while waiting for/communicating with client: "+ e.getMessage());

				break;

			}
		}
	}

	public void close(){

		if(serverSocket != null && !serverSocket.isClosed()){
			try {
				serverSocket.close();
			} catch (IOException e) {
				//Can not be handeled here
			}
		}



	}
}
