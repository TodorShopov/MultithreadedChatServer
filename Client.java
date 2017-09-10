import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable {
	private Scanner consoleInput;
	private DataInputStream serverInput;
	private DataOutputStream clientOutput;
	private Socket s;
	private Thread t;

	String username;
	String chatRoom;

	public Client(String host, int port) {

		try {
			s = new Socket(host, port);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error while creating socket");
		}

		consoleInput = new Scanner((System.in));

		try {
			clientOutput = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
			serverInput = new DataInputStream(new BufferedInputStream(s.getInputStream()));
		} catch (IOException e) {

			System.out.println("Error opening/closing io stream");
			e.printStackTrace();
		}

		System.out.println("Connected.");
		System.out.println("Please login or register...");
		System.out.println("Use /login <username> <password>' or '/register <username> <password>");
		System.out.println("If you seek more commands, type: /faq");

		username = "Unnamed";
		chatRoom = "#Global";

		new ClientThread(this, s);

		start();
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() {
		if (t != null) {
			t.stop();
			t = null;
		}
	}

	void close() {
		if (consoleInput != null)
			try {
				consoleInput.close();
			} catch (Exception e) {

				System.out.println("Error closing console input stream");
				e.printStackTrace();
			}

		if (clientOutput != null)
			try {
				clientOutput.close();
			} catch (IOException e) {
				System.out.println("Error closing os");
				e.printStackTrace();
			}

		if (s != null)
			try {
				s.close();
			} catch (IOException e) {
				System.out.println("Error closing socket");

			}

		if (serverInput != null)
			try {
				serverInput.close();
			} catch (IOException e) {

				System.out.println("Error closing server answer stream");
				e.printStackTrace();
			}
	}

	@Override
	public void run() {
		String input = "";

		while (!input.equals("/exit")) {

			try {
				input = consoleInput.nextLine();
			} catch (Exception e) {
				System.out.println("Error with console input ");
				System.out.println("Logging out...");
				System.exit(0);
			}

			try {
				clientOutput.writeUTF(input);
				clientOutput.flush();
			} catch (IOException e) {
				System.out.println("Error sending input: ");
				System.out.println("Logging out...");
				System.exit(0);
			}
		}

		close();
		stop();
	}

	public void authenticate() {
		stop();
		try {
			String input = "";
			String response = "";
			String[] parts = new String[3];

			chatRoom = "Global";
			username = "Unknown";

			boolean auth = false;
			while (!auth) {
				input = consoleInput.nextLine();

				if (input.equals("/faq"))
					faq();

				clientOutput.writeUTF(input);
				clientOutput.flush();

				parts = input.split(" ");

				if (input.equals("/exit")) {
					System.out.println("You have left the channel...");
					close();
					return;
				}

				response = serverInput.readUTF();

				switch (response) {
				case "LoggedIn":
					System.out.println("Successfuly logged in.");
					username = parts[1];

					auth = true;
					break;

				case "Registered":
					System.out.println("Successfuly registered.");
					username = parts[1];

					auth = true;
					break;

				case "WrongPassword":
					System.out.println("Password is wrong.");

					break;

				case "MissingUsername":
					System.out.println("Username not found.");

					break;

				case "AlreadyLogged":
					System.out.println("User is already logged in.");

					break;

				case "AlreadyTaken":
					System.out.println("Username already taken.");

					break;

				case "InvalidCommand":
					System.out.println("Invalid command.");

					break;
				}
			}
		} catch (IOException e) {
			System.out.println("Invalid authentication command.");

			authenticate();

			return;
		}
		System.out.println("Authentication completed. ");
		System.out.println("Username: " + username);

		start();
	}

	void faq() {
		System.out.println("Available user commands: ");
		System.out.println("1. To register a new user, type: /register <username> <password>");
		System.out.println("2. To log into an existing account, type: /login <username> <password>");
		System.out.println("3. To send a private message, type: /msg <username> <message> ");
		System.out.println("4. To see all available rooms, type: /list-rooms");
		System.out.println("5. To see online users in your chatroom, type: /list-users ");
		System.out.println("7. To join available room, type: /join <name>");
		System.out.println("8. To create a chatroom, type: /create <name>");
		System.out.println("9. To leave your chatroom, type: /leave");
		System.out.println("10. To delete a chatroom, type: /delete");
		System.out.println("11. To logout, type: /logout ");
		System.out.println("12. To exit, type: /exit ");
	}

	public static void main(String[] args) {
		System.out.println("Hello, please enter the name host of your server that you'd like to connect to.");
		System.out.println("Default server name is 'localhost'...");

		Scanner scan = new Scanner(System.in);
		String hostName = scan.nextLine();

		System.out.println("Please enter the chosen port... (Ports above the number 1024 are without root access.");

		int portNumber = scan.nextInt();

		System.out.println("Connecting to server...");

		Client chatClient = new Client(hostName, portNumber);

	}
}
