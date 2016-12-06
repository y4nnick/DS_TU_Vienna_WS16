package client;

import cli.Command;
import cli.Shell;
import client.listener.PrivateChannelListener;
import client.listener.TcpListener;
import client.sender.Sender;
import client.sender.TcpSender;
import client.sender.UdpSender;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;

	String username;

	private ExecutorService executor;

	private String lastMsg = "No message received!";

	//TCP socket for Sender
	Socket senderSocket;

	//TCP socket for public message
	Socket publicMessagesSocket;

	//TCP sender
	Sender tcpSender;

	//UDP sender
	Sender udpSender;

	//TCP Listener
	TcpListener tcpListener;

	//Tcp sender, only for the registration process for the public socket
	TcpSender publicSender;

	Integer serverPortPublicMessage;
	String serverIP;

	//Server socket for private messages
	ServerSocket privateServerSocket;

	//Listener for new Clients for private messages
	PrivateChannelListener privateChannelListener;

	/**
	 * @param componentName the name of the component - represented in the prompt
	 * @param config the configuration to use
	 * @param userRequestStream the input stream to read user input from
	 * @param userResponseStream the output stream to write the console output to
	 */
	public Client(String componentName, Config config,InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
	}

	@Override
	public void run() {

		try {

			// Read server configuration
			serverIP = config.getString("chatserver.host");
			Integer serverPortTcp = config.getInt("chatserver.tcp.port");
			serverPortPublicMessage = serverPortTcp + 2;
			Integer serverPortUdp = config.getInt("chatserver.udp.port");

			// Create TCP socket
			senderSocket = new Socket(serverIP, serverPortTcp);

			// Create UDP Sender
			udpSender = new UdpSender(serverIP,serverPortUdp);

			// Reigster shell-listener for client inputs
			shell = new Shell(componentName, userRequestStream, userResponseStream);
			shell.register(this);

			//Execute the shell listener in a new Thread
			executor = Executors.newFixedThreadPool(3);
			executor.execute(shell);

			// Create TCP Sender
			tcpSender = new TcpSender(senderSocket);


		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to host: " + e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": "+ e.getMessage());
		}
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {

		String response = tcpSender.send("!login " + username + " " + password);

		if(response.equals("Successfully logged in.")){

			//Store username for the private messages
			this.username = username;

			//register Socket for public messages
			if(publicMessagesSocket == null){
				publicMessagesSocket = new Socket(serverIP, serverPortPublicMessage);
				publicSender = new TcpSender(publicMessagesSocket);
				publicSender.send("!register_public " + username);

				//Listen for public messages from the server
				tcpListener = new TcpListener(publicMessagesSocket,this,new PrintWriter(userResponseStream));
				executor.execute(tcpListener);
			}
		}

		return response;
	}

	@Override
	@Command
	public String logout() throws IOException {
		return tcpSender.send("!logout");
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		return tcpSender.send("!send " + message);
	}

	@Override
	@Command
	public String list() throws IOException {
		return udpSender.send("!list");
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {

		// Steps:
		// 1) Lookup IP for username
		// 2) Make new TCP socket connection
		// 3) Send the message over TCP to the client
		// 4) Wait for !ack message
		// 5) Close socket

		// 1) Lookup IP
		String lookupResponse = lookup(username);

		// Check if look up was successfull

		if(lookupResponse.equals("Not logged in.")){
			return lookupResponse;
		}

		if(lookupResponse.equals("Wrong username or user not registered.")){
			return "Wrong username or user not reachable.";
		}

		//Read out IP and Port
		String[] values = lookupResponse.split(":");
		String ip = values[0];
		Integer port = Integer.valueOf(values[1]);

		// 2) Make new TCP socket connection
		Socket privateSocket = new Socket(ip, port);

		// 3) Send message & 4) Wait for !ack and print response

		TcpSender privateMessageSender = new TcpSender(privateSocket);
		String response = privateMessageSender.send(this.username + ": " + message);

		// 5) Close all
		privateMessageSender.close();
		privateMessageSender.close();

		return username + " replied with " + response + ".";
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		return tcpSender.send("!lookup " + username);
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {

		String registerResponse = tcpSender.send("!register " + privateAddress);

		// Listen on given address for TCP messages
 		if(registerResponse.startsWith("successfully registered address for")){

			//Read out port
			Integer port = Integer.valueOf(privateAddress.split(":")[1]);

			privateServerSocket = new ServerSocket(port);

			privateChannelListener = new PrivateChannelListener(privateServerSocket,this, new PrintWriter(userResponseStream));
			executor.execute(privateChannelListener);
		}

		return registerResponse;
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		return lastMsg;
	}

	@Override
	@Command
	public String exit() throws IOException {

		//Logout the user
		try{
			String logoutResponse = logout();
		}catch (IOException ex){
			//Server could allready be down
		}

		//Close sender socket
		if (senderSocket != null && !senderSocket.isClosed()){
			try {
				senderSocket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}

		//Close private socket server
		if (privateServerSocket != null && !senderSocket.isClosed()){
			try {
				privateServerSocket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}

		//Close socket for public messages
		if (publicMessagesSocket != null && !publicMessagesSocket.isClosed()){
			try {
				publicMessagesSocket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}

		//Stop shell listener
		shell.close();

		//Close senders
		tcpSender.close();
		udpSender.close();

		tcpListener.close();

		//Shutdown executer
		executor.shutdown();

		if(privateChannelListener != null)privateChannelListener.close();

		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,System.out);
		client.run();
	}

	public void setLastMsg(String lastMsg) {
		this.lastMsg = lastMsg;
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
