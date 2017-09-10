import java.io.*;
import java.net.*;
import java.util.*;

public class ClientThread extends Thread {
	Socket s;
	Client client;
	DataInputStream inputStream;

	ClientThread(Client client, Socket s) {
		super();

		this.client = client;
		this.s = s;

		try {
			inputStream = new DataInputStream(new BufferedInputStream(s.getInputStream()));
		} catch (IOException e) {
			System.out.println("Error with input stream: " + e);

			client.close();
			client.stop();
		}
		start();
	}
	
	public void close() {
		if (inputStream != null)
			try {
				inputStream.close();
			} catch (IOException ioe) {
				System.out.println("Error closing input stream: " + ioe);
			}
	}

	public void run() {
		String input = "";
		boolean authentication = false;

		client.authenticate();

		while (!authentication) {
			try {
				input = inputStream.readUTF();

				switch (input) {
				case "/exit":
					System.out.println("You have been disconnected.");
					close();
					break;

				case "/logout":
					System.out.println("Press a button to proceed");
					client.authenticate();
					break;

				case "/faq":
					client.faq();
					break;

				case "/leave":
					System.out.println("You have left.");
					client.chatRoom = "Global";
					
					break;

				case "/delete":
					System.out.println("You have deleted the chatroom '");
					System.out.println("Getting redirected to Global chatroom...");
					client.chatRoom = "Global";
					break;

				default:
					if (input.startsWith("/join")) {
						System.out.println("You have joined chatroom '" + input.substring(6) + "'.");
						client.chatRoom = input.substring(6);

					} else if (input.startsWith("/failedjoin")) {
						System.out.println("There is no such room.");

					} else if (input.startsWith("/create")) {
						System.out.println("You have created chatroom '" + input.substring(8) + "'.");
						client.chatRoom = input.substring(8);

					} else if (input.startsWith("/failedcreate")) {
						System.out.println("There is already a room with that name.");

					} else if (input.startsWith("/"))
						System.out.println("Unknown command.");

					else
						System.out.println(input);

				}

			} catch (IOException e) {
				System.out.println("Error with output");

				close();
				client.close();
				client.stop();
				break;
			}
		}

		close();
	}

	

	
}