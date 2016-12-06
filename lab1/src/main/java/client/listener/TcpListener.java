package client.listener;

import client.Client;
import client.sender.TcpSender;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class TcpListener implements Runnable {

	private Socket serverSocket;
	private Client client;
	private PrintWriter outputStream;

	private TcpSender tcpSender = null;

	public TcpListener(Socket serverSocket, Client client, PrintWriter outputStream) {
		this.serverSocket = serverSocket;
		this.client = client;
		this.outputStream = outputStream;
	}

	public void registerSender(TcpSender sender){
		this.tcpSender = sender;
	}

	public void unregisterSender(){
		this.tcpSender = null;
	}

	public void run() {

		try {

			BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

			while (true) {

				String message = serverReader.readLine();

				if(message == null) continue;

				client.setLastMsg(message);

				outputStream.println(message);
				outputStream.flush();
			}

		} catch (SocketException sex){
			// Socket got closed because server shuts down
			//sex.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error occurred while waiting for public message from server: " + e.getMessage());
			e.printStackTrace();
		} finally {

		}

	}

	public void close(){


		//Close socket if not allready closed
		/*try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			// Ignored because we cannot handle it
		}

		//Close server Writer
		if (outputStream != null) {
			outputStream.close();
		}*/

	}
}
