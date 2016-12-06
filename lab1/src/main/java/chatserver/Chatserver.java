package chatserver;

import chatserver.tcp.ClientListener;
import chatserver.udp.UdpListener;
import cli.Command;
import cli.Shell;
import model.User;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;
	private ExecutorService executor;

	Usermanager usermanager = new Usermanager();

	//TCP Socket
	private ServerSocket serverSocket;

	//TCP Socket for public messages
	private ServerSocket publicMessagesSocket;

	//UDP Socket
	private DatagramSocket udpSocket;

	/**
	 * @param componentName the name of the component - represented in the prompt
	 * @param config the configuration to use
	 * @param userRequestStream the input stream to read user input from
	 * @param userResponseStream the output stream to write the console output to
	 */
	public Chatserver(String componentName, Config config, InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		// Load users from Config file
		usermanager.loadFromConfig();
	}

	@Override
	public void run() {

		try {

			// Create a new TCP ServerSocket
			serverSocket = new ServerSocket(config.getInt("tcp.port"));

			// Create a new TCP ServerSocket
			publicMessagesSocket = new ServerSocket(config.getInt("tcp.port") + 2);

			executor = Executors.newFixedThreadPool(4);

			//Listen for new TCP clients
			Runnable clientListener = new ClientListener(serverSocket,this);
			executor.execute(clientListener);

			//Listen for new TCP clients (public messages)
			Runnable clientListenerPublicMessanger = new ClientListener(publicMessagesSocket,this);
			executor.execute(clientListenerPublicMessanger);

			//Listen for UDP-Data
			udpSocket = new DatagramSocket(config.getInt("udp.port"));
			Runnable udpListener = new UdpListener(udpSocket,this);
			executor.execute(udpListener);

			// Reigster shell-listener for client inputs
			shell = new Shell(componentName, userRequestStream, userResponseStream);
			shell.register(this);

			//Execute the shell listener in a new Thread
			executor.execute(shell);

		} catch (IOException e) {
			throw new RuntimeException("Cannot start the chart server", e);
		}
	}

	@Override
	@Command
	public String users() throws IOException {

		String result = "";

		Integer id = 1;
		for (User u : usermanager.getUsers()) {
			result+= (id++) + ". " + u.getName() + " " + ((u.isLoggedIn())?"online":"offline") + "\n";
		}

		return result;
	}

	@Override
	@Command
	public String exit() throws IOException {

		//Logout all users and disconnect them
		for(User u : usermanager.getUsers()){

			u.setLoggedIn(false);

			if(u.getSocket() != null && !u.getSocket().isClosed()){
				u.getSocket().close();
			}

			if(u.getPublicSocket() != null && !u.getPublicSocket().isClosed()){
				u.getPublicSocket().close();
			}
		}

		//Close resources
		executor.shutdown();
		serverSocket.close();
		if(publicMessagesSocket != null)publicMessagesSocket.close();
		udpSocket.close();

		return null;
	}

	/**
	 * Delivers a list of all online users
	 * @return the online users in string format
     */
	public String list(){

		String result = "Online users:\n";

		for (User u : usermanager.getUsers()) {
			if(u.isLoggedIn()){
				result+= "* " + u.getName() + "\n";
			}
		}

		return result;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0], new Config("chatserver"), System.in, System.out);
		chatserver.run();
	}

	/**
	 * Logs in a user
	 * @param username username
	 * @param password password
     * @return a string respsone
     */
	public synchronized String loginUser(String username, String password, Socket socket){

		User user = usermanager.getByName(username);

		//Check if user exists and password is correct
		if(user == null || !user.getPassword().equals(password)){
			return "Wrong username or password.";
		}

		//Check if user is logged in
		if(user.isLoggedIn()){
			return "Already logged in.";
		}

		//Set the socket and socketAddress to the user
		user.setSocket(socket);

		//Set user to logged in
		user.setLoggedIn(true);

		return "Successfully logged in.";
	}

	/**
	 * Logout a user
	 * @param clientsocket the socketAdress of the user
	 * @return a string response
     */
	public synchronized String logoutUser(Socket clientsocket){

		//Get user by address
		User user = usermanager.getLoggedInUserBySocket(clientsocket);

		//Check if logged in
		if(user == null){
			return "Not logged in.";
		}

		//Logout
		user.setLoggedIn(false);
		user.setAddress(null);

		return "Successfully logged out.";
	}

	/**
	 * Registers the client socket for public messages
	 * @param clientSocket the socket for public messages
	 * @param username the username of the client
     */
	public String registerPublic(Socket clientSocket, String username){
		User u = usermanager.getByName(username);
		u.setPublicSocket(clientSocket);
		return "success";
	}

	/**
	 * Registers the given address for the given client
	 * @param clientSocket
	 * @param address
     * @return
     */
	public synchronized String registerUserAddress(Socket clientSocket, String address){

		User currentUser = usermanager.getLoggedInUserBySocket(clientSocket);

		//Check if logged in
		if(currentUser == null){
			return "Not logged in.";
		}

		currentUser.setAddress(address);

		return "successfully registered address for " + currentUser.getName() + ".";
	}

	/**
	 * Looksup the address from the given user
	 * @param username the username from the user
	 * @return the address
     */
	public String lookup(String username, Socket clientSocket){

		User currentUser = usermanager.getLoggedInUserBySocket(clientSocket);

		//Check if logged in
		if(currentUser == null){
			return "Not logged in.";
		}

		User user = usermanager.getByName(username);

		if(user == null || user.getAddress() == null){
			//System.out.print(users.toString());
			return "Wrong username or user not registered.";
		}

		return user.getAddress();
	}

	/**
	 * Sends a message to all other clients
	 * @param clientSocket the client which sended the message
	 * @param msg the message
     * @return the response to the sender
     */
	public String sendMessage(Socket clientSocket, String msg){

		//Check if user is logged in
		User currentUser = usermanager.getLoggedInUserBySocket(clientSocket);
		if(currentUser == null){
			return "Not logged in.";
		}

		//Write to other clients
		try{
			for(User u : usermanager.getUsers()){
				if(u.getSocket() != null && u.getPublicSocket().isConnected() && u.isLoggedIn()
						&& !u.getSocket().equals(clientSocket)){

					PrintWriter writer = new PrintWriter(u.getPublicSocket().getOutputStream(),true);
					writer.println(currentUser.getName() + ": " + msg);
					writer.flush();
				}
			}
		}catch (IOException ex) {
			ex.printStackTrace();
		}

		return "";
	}


}
