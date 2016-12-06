package chatserver.udp;

import chatserver.Chatserver;
import util.Helper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Thread to listen for incoming data packets on the given socket.
 */
public class UdpListener implements Runnable {

	private DatagramSocket datagramSocket;
	private Chatserver chatserver;

	public UdpListener(DatagramSocket datagramSocket, Chatserver chatserver) {
		this.datagramSocket = datagramSocket;
		this.chatserver = chatserver;
	}

	@Override
	public void run() {

		byte[] buffer;
		DatagramPacket packet;
		try {
			while (true) {

				buffer = new byte[1024];

				// Create a datagram packet of specified length (buffer.length)
				packet = new DatagramPacket(buffer, buffer.length);

				// Wait for incoming packets from client (blocking)
				datagramSocket.receive(packet);

				// Get the data from the packet
				String request = Helper.getResponseFromPacket(packet);

				// Split request
				String[] parts = request.split("\\s");

				String response = "";

				switch (parts[0]) {
					case "!list":
						response = chatserver.list();
						break;
					default:
						response = "Unknown UDPrequest: " + request;
						break;
				}

				// get the address of the sender (client) from the received packet
				InetAddress address = packet.getAddress();

				// get the port of the sender from the received packet
				int port = packet.getPort();

				//Create response Buffer
				buffer = response.getBytes();

				// Create response packet
				packet = new DatagramPacket(buffer, buffer.length, address, port);

				// Send the packet
				datagramSocket.send(packet);
			}

		}catch (SocketException soe) {
			// Socket got closed because server shuts down
		}catch (IOException e) {
			System.err.println("Error occurred while waiting for/handling packets: "+ e.getMessage());
		} finally {
			if (datagramSocket != null && !datagramSocket.isClosed())
				datagramSocket.close();
		}

	}
}
