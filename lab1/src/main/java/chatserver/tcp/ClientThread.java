package chatserver.tcp;

import chatserver.Chatserver;
import model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class ClientThread implements Runnable {

	private Socket clientSocket;
	private Chatserver chatserver;

	public ClientThread(Socket clientSocket, Chatserver chatserver) {
		this.clientSocket = clientSocket;
		this.chatserver = chatserver;
	}

	public void run() {

		try {

			// prepare the input reader for the socket
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			// prepare the writer for responding to clients requests
			PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

			while (true) {

				String request;

				// read client requests
				while ((request = reader.readLine()) != null) {


					String[] parts = request.split("\\s");
					String response = "";

					switch (parts[0]) {
						case "!register_public":
							response = chatserver.registerPublic(clientSocket,parts[1]);
							break;
						case "!login":
							response = chatserver.loginUser(parts[1], parts[2], clientSocket);
							break;
						case "!logout":
							response = chatserver.logoutUser(clientSocket);
							break;
						case "!send":
							String msg = request.substring(parts[0].length() + 1, request.length());
							response = chatserver.sendMessage(clientSocket, msg);
							break;
						case "!register":
							response = chatserver.registerUserAddress(clientSocket, parts[1]);
							break;
						case "!lookup":
							response = chatserver.lookup(parts[1],clientSocket);
							break;
						default:
							response = "Unknown request: " + request;
							break;
					}

					// print request
					writer.println(response);
				}

			}
		} catch (SocketException sex){
			// Socket got closed because server shuts down
			//sex.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error occurred while waiting for/communicating with client: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (clientSocket != null && !clientSocket.isClosed())
				try {
					clientSocket.close();
				} catch (IOException e) {
					// Ignored because we cannot handle it
				}
		}
	}
}
