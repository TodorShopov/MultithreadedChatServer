import java.io.*;
import java.net.*;
import java.util.*;

public class Server implements Runnable {
	private ServerSocket serverSocket;
	private Thread thread;
	private ServerThread[] clientsThread;
	private List<RoomClass> roomsList;

	static int cnt;

	UserInfo userData;

	static int capacity = 100;

	public Server(int port) {
		clientsThread = new ServerThread[capacity];

		userData = new UserInfo(port);

		roomsList = new ArrayList<RoomClass>();
		roomsList.add(new RoomClass("Global", "Unnamed", port));
		roomsList.get(0).remove("Unnamed");

		cnt = 0;

		try {
			serverSocket = new ServerSocket(port);

			System.out.println("Server started: " + serverSocket);
			System.out.println("Waiting for a connection");

			start();
		} catch (IOException e) {
			System.out.println("Error starting server: ");
			e.printStackTrace();

			stop();
		}
	}

	public void removeUser(String username, String chatroom) {
		roomsList.get(findRoom(chatroom)).remove(username);
	}

	public void addUser(String username, String chatroom) {
		roomsList.get(findRoom(chatroom)).add(username);
	}

	public String listRooms() {
		String chatRoomList = "";
		for (int i = 0; i < roomsList.size(); i++)
			chatRoomList += roomsList.get(i).toString();

		return chatRoomList;
	}

	public String listUsers(String chatroom) {
		for (int i = 0; i < roomsList.size(); i++)
			if (roomsList.get(i).name.equals(chatroom))
				return "Online users in " + chatroom + ":\n" + roomsList.get(i).getOnlineUsers();

		return "";
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
	}

	public void run() {
		while (true) {
			try {
				userData.readUserData();
				addClient(serverSocket.accept());

			} catch (IOException e) {
				System.out.println("Error accepting connection: ");
			}
		}
	}

	public void addClient(Socket socket) {
		int freePosition = getFreePosition();

		if (freePosition != -1) {

			System.out.println("Client accepted: " + socket);
			System.out.println("Total clients: " + cnt);

			clientsThread[freePosition] = new ServerThread(this, socket);
			cnt++;

		} else {
			System.out.println("Server is full. Failed to accept new client.");
		}
	}

	public synchronized void handleMessage(String chatRoom, int ID, String input) {
		if (input.equals("/exit")) {

			for (int i = 0; i < capacity; i++)
				if (clientsThread[i] != null && clientsThread[i].isAlive() && i != findClient(ID)
						&& clientsThread[i].chatRoom.equals(chatRoom))
					clientsThread[i]
							.send(chatRoom + " - " + clientsThread[findClient(ID)].username + " has disconnected.");

			if (findClient(ID) != -1) {
				clientsThread[findClient(ID)].send(input);
				remove(ID);
			}

		} else {

			for (int i = 0; i < capacity; i++)
				if (clientsThread[i] != null && clientsThread[i].isAlive()
						&& clientsThread[i].chatRoom.equals(chatRoom))
					clientsThread[i].send(chatRoom + " - " + input);

			if (!input.endsWith(" has joined the room.") && !input.endsWith(" has left the room."))
				roomsList.get(findRoom(chatRoom)).write(input);

		}
	}

	public boolean create(String username, String chatroom) {
		if (findRoom(chatroom) != -1)
			return false;

		roomsList.add(new RoomClass(chatroom, username, roomsList.get(0).port));
		roomsList.get(0).remove(username);

		return true;
	}

	public boolean join(String username, String chatroom) {
		int x = findRoom(chatroom);
		if (x == -1)
			return false;

		roomsList.get(x).add(username);
		roomsList.get(0).remove(username);
		clientsThread[findClient(getID(username))].send(roomsList.get(x).getHistory());

		return true;
	}

	public void leave(String username, String chatroom) {
		roomsList.get(findRoom(chatroom)).remove(username);
		roomsList.get(0).add(username);
	}

	public boolean delete(String username, String chatroom) {
		int x = findRoom(chatroom);
		if (!roomsList.get(x).owner.equals(username))
			return false;

		for (int i = 0; i < roomsList.get(x).users.size(); i++) {

			clientsThread[findClient(getID(roomsList.get(x).users.get(i)))].chatRoom = "Global";
			roomsList.get(0).add(roomsList.get(x).users.get(i));

			if (!roomsList.get(x).users.get(i).equals(username))
				clientsThread[findClient(getID(roomsList.get(x).users.get(i)))]
						.send("Chatroom '" + chatroom + "' has been deleted.\nYou have been moved to Global channel.");
		}

		roomsList.remove(x);
		return true;
	}

	public synchronized void handlePrivateMessage(int ID, String input, String username) {
		int receiverID = getID(username);

		if (receiverID == -1)
			clientsThread[findClient(ID)].send("There is no such user online.");
		else {
			clientsThread[findClient(receiverID)].send(input);
			clientsThread[findClient(ID)].send(input);
		}
	}

	public int getID(String username) {
		for (int i = 0; i < clientsThread.length; i++)
			if (clientsThread[i] != null && clientsThread[i].isAlive() && clientsThread[i].username.equals(username))
				return clientsThread[i].ID;

		return -1;
	}

	public synchronized boolean isLoggedIn(String username) {
		for (int i = 0; i < clientsThread.length; i++)
			if (clientsThread[i] != null && clientsThread[i].isAlive() && clientsThread[i].username.equals(username))
				return true;

		return false;
	}

	void remove(int ID) {
		if (findClient(ID) != -1)
			clientsThread[findClient(ID)] = null;
	}

	private int findClient(int ID) {
		for (int i = 0; i < capacity; i++)
			if (clientsThread[i] != null && clientsThread[i].isAlive() && clientsThread[i].ID == ID)
				return i;

		return -1;
	}

	private int getFreePosition() {
		for (int i = 0; i < capacity; i++)
			if (clientsThread[i] == null)
				return i;

		return -1;
	}

	private int findRoom(String chatroom) {
		for (int i = 0; i < roomsList.size(); i++)
			if (roomsList.get(i).name.equals(chatroom))
				return i;

		return -1;
	}

	public static void main(String[] args) {
		System.out.println("Which port would you like to use to host a server?");

		Scanner in = new Scanner(System.in);
		int port = in.nextInt();
		in.close();

		System.out.println("Creating chat server on localhost, port " + port);

		@SuppressWarnings("unused")
		Server chatServer = new Server(port);
	}
}