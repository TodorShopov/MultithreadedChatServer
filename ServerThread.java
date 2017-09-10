import java.io.*;
import java.net.*;

public class ServerThread extends Thread {
	private Server server;
	private Socket socket;

	private DataInputStream inputStream;
	private DataOutputStream outputStream;

	String username;
	String chatRoom;
	int ID;

	public ServerThread(Server server, Socket socket) {
		super();

		this.server = server;
		this.socket = socket;
		chatRoom = "Global";
		username = "Unnamed";
		ID = socket.getPort();

		open();
		start();
	}

	@Override
	public void run() {
		System.out.println("Client #" + ID + " is connected.");
		getUser();

		String input = "";
		while (!input.equals("/exit")) {

			try {
				input = inputStream.readUTF();
			} catch (IOException ioe) {
				System.out.println("Error reading from client #" + ID + ": " + ioe.getMessage());
				server.handleMessage(chatRoom, ID, "/exit");
				close();
				break;
			}

			System.out.println("Client #" + ID + "(" + username + "): " + input);
			switch (input) {
			case "/exit":
				send("/exit");

				server.removeUser(username, chatRoom);
				server.handleMessage(chatRoom, ID, input);

				close();
				break;

			case "/logout":
				send("You have logged out.");
				send("Please login or register...");
				System.out.println("Client #" + ID + "(" + username + ") has logged out.");

				send("/logout");
				getUser();
				break;

			case "/faq":
				send("/faq");
				break;

			case "/list-rooms":
				System.out.println(username + " has required chatroom list.");
				send(server.listRooms());
				break;

			case "/leave":
				if (chatRoom == "Global") {
					System.out.println(username + " has failed to leave chatroom '" + chatRoom + "'.");
					send("You can't leave the global room.");
					break;
				}
				server.leave(username, chatRoom);
				server.handleMessage(chatRoom, ID, username + " has left the room.");

				System.out.println(username + " has left chatroom '" + chatRoom + "'.");
				chatRoom = "Global";

				send("/leave");
				break;

			case "/delete":
				if (chatRoom == "Global") {
					System.out.println(username + "has failed to delete chatroom '" + chatRoom + "'.");
					send("You can't delete Global channel.");
					break;
				}

				if (server.delete(username, chatRoom)) {
					System.out.println(username + " has deleted chatroom '" + chatRoom + "'.");
					send("/delete");
					break;
				} else {
					System.out.println(username + "has failed to delete chatroom '" + chatRoom + "'.");
					send("ERROR: Only the creator of the chatroom can delete it.");
					break;
				}
			default:
				if (input.startsWith("/list-users")) {

					if (input.equals("/list-users")) {
						System.out.println(username + " has required user list of chatroom '" + chatRoom + "'.");
						send(server.listUsers(chatRoom));
					} else {

						if (input.length() <= 12 || !input.startsWith("/list-users ")) {
							send("Invalid command. To see online users in a room use /list-users <room-name> "
									+ "or just /list-users to see online users in your room.");
						} else {

							if (server.listUsers(input.substring(12)).equals(""))
								send("There is no such room.");
							else
								send(server.listUsers(input.substring(12)));

							System.out.println(
									username + " has required user list of chatroom '" + input.substring(12) + "'.");
						}
					}

				} else if (input.startsWith("/msg")) {

					try {
						String receiverUsername = input.split(" ")[1];
						server.handlePrivateMessage(ID, username + " to " + receiverUsername + ": "
								+ input.substring(receiverUsername.length() + 5), receiverUsername);
					} catch (ArrayIndexOutOfBoundsException aioob) {
						send("Invalid command. To send a private message use /msg <username> <message>.");
					}

				} else if (input.startsWith("/join")) {

					if (input.length() <= 6 || !input.startsWith("/join ")) {
						send("Invalid command. To join a room use /join <room-name>");
						break;
					}

					if (chatRoom != "Global") {
						System.out.println(username + " has failed to join chatroom '" + input.substring(6) + "'.");
						send("You are already in chatroom '" + chatRoom + "'.");
						break;
					}

					if (server.join(username, input.substring(6))) {
						server.handleMessage(input.substring(6), ID, username + " has joined the room.");
						chatRoom = input.substring(6);
						System.out.println(username + " has joined chatroom '" + chatRoom + "'.");
						send(input);
					} else {
						System.out.println(username + " has failed to join chatroom '" + input.substring(6) + "'.");
						send("/failedjoin " + input.substring(6));
					}

				} else if (input.startsWith("/create")) {

					if (input.length() <= 8 || !input.startsWith("/create ")) {
						send("Invalid command. To create a room use /create <room-name>");
						break;
					}

					if (chatRoom != "Global") {
						System.out.println(username + " has failed to create chatroom '" + input.substring(8) + "'.");
						send("You are already in chatroom '" + chatRoom + "'.");
						break;
					}

					if (server.create(username, input.substring(8))) {
						chatRoom = input.substring(8);
						System.out.println(username + " has created chatroom '" + chatRoom + "'.");
						send(input);
					} else {
						System.out.println(username + " has failed to create chatroom '" + input.substring(8) + "'.");
						send("/failedcreate " + input.substring(8));
					}

				} else if (input.startsWith("/")) {

					if (input.contains("/login") || input.contains("/register"))
						send("You are already logged in.");
					else
						send("There is no such command.");

				} else
					server.handleMessage(chatRoom, ID, (username + ": " + input));
			}
		}
		close();
		Server.cnt--;
		System.out.println("Client #" + ID + "(" + username + ") has disconnected.");
		System.out.println("Total users online: " + Server.cnt);
	}

	public void send(String message) {
		try {
			outputStream.writeUTF(message);
			outputStream.flush();
		} catch (IOException ioe) {
			System.out.println("Error while client #" + ID + " is sending: " + ioe.getMessage());
			server.removeUser(username, chatRoom);
			close();
		}
	}

	private void getUser() {
		server.removeUser(username, chatRoom);

		username = "Unnamed";
		String input = "";
		String[] parts = new String[3];
		String user = "";
		String command = "";
		String newUsername = "";

		while (true) {
			try {
				input = inputStream.readUTF();

				if (input.equals("/exit")) {
					close();
					return;
				}

				server.userData.readUserData(); // updates server's registration
												// list

				parts = input.split(" ");
				command = parts[0];
				newUsername = parts[1];
				user = newUsername + " " + parts[2];

				if (!command.equals("/login") && !command.equals("/register")) {
					send("InvalidCommand");
					System.out.println("Invalid command from Client #" + ID + ". ");
				} else if (command.equals("/login")) {

					if (server.userData.hasUser(user)) {

						if (server.isLoggedIn(newUsername))
							send("AlreadyLogged");
						else {
							send("LoggedIn");
							System.out.println(newUsername + " successfully logged in.");
							username = newUsername;
							break;
						}

					} else if (server.userData.freeUsername(newUsername)) {
						send("MissingUsername");
					} else if (!server.userData.freeUsername(newUsername))
						send("WrongPassword");

				} else if (command.equals("/register")) {

					if (server.userData.freeUsername(newUsername) && !newUsername.equals("Unnamed")) {
						send("Registered");
						server.userData.registerUser(user);
						System.out.println(newUsername + " successfully registered.");
						username = newUsername;
						break;
					} else
						send("AlreadyTaken");
				}

			} catch (IOException e) {
				System.out.println("Error reading  data. ");
				return;
			} catch (Exception e) {
				send("InvalidCommand");
				System.out.println("Invalid command from Client #" + ID + ". ");
			}
		}

		server.addUser(username, chatRoom);
	}

	public void open() {
		try {
			inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		} catch (IOException ioe) {
			System.out.println("Error opening input stream of server thread #" + ID);
			server.remove(ID);
		}

		try {
			outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		} catch (IOException e) {
			System.out.println("Error opening output stream of server thread #" + ID);
			server.remove(ID);
		}
	}

	public void close() {
		if (inputStream != null)
			try {
				inputStream.close();
			} catch (IOException e) {
				System.out.println("Error closing input stream of server thread #" + ID + ": ");
			}

		if (outputStream != null)
			try {
				outputStream.close();
			} catch (IOException e) {
				System.out.println("Error closing output stream of server thread #" + ID + ": ");
			}

		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Error closing socket of server thread #" + ID + ": ");
			}

		server.remove(ID);
	}
}
