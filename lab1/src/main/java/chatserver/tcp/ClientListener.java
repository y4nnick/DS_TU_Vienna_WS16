package chatserver.tcp;

import chatserver.Chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class ClientListener implements Runnable {

	private ServerSocket serverSocket;
	private Chatserver chatserver;

	private ExecutorService executorService;

	public ClientListener(ServerSocket serverSocket, Chatserver chatserver) {
		this.serverSocket = serverSocket;
		this.chatserver = chatserver;

		executorService = Executors.newFixedThreadPool(999);
	}

	public void run() {

		while (true) {
			Socket socket = null;
			try {

				// wait for Client to connect
				socket = serverSocket.accept();

				// Create new Thread for the new client
				executorService.execute(new ClientThread(socket,chatserver));

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
}
