import java.io.*;
import java.util.*;

public class RoomClass {
	String name;
	String owner;
	List<String> users;

	int online;
	int port;

	String fileName;
	File history;

	RoomClass(String name, String owner, int port) {
		this.name = name;
		this.owner = owner;
		fileName = "name" + ".txt";

		this.port = port;
		online = 1;

		users = new ArrayList<String>();
		users.add(owner);

		history = new File(fileName);
		history.mkdirs();
		try {
			history.delete();
			history.createNewFile();
		} catch (IOException e) {
			System.out.println("Failed to create chatroom " + name + "'s history file. ");
		}
	}

	@Override
	public String toString() {
		return "Name: " + name + ". Users online: " + online + "\n";
	}

	public void add(String username) {
		users.add(username);
		online++;
	}

	public void remove(String username) {
		for (int i = 0; i < users.size(); i++)
			if (users.get(i).equals(username)) {
				users.remove(i);
				online--;
				break;
			}
	}

	public int onlineCount() {
		return online;
	}

	public String getOnlineUsers() {
		String usersList = "";
		for (int i = 0; i < users.size(); i++)
			usersList += users.get(i) + "\n";

		return usersList;
	}

	public void write(String message) {
		try {
			
			FileWriter fw = new FileWriter(fileName, true);
			BufferedWriter bw = new BufferedWriter(fw);

			bw.write(message + "\n");
			bw.flush();

			bw.close();
			fw.close();

		} catch (IOException ioe) {
			System.out.println("IO exception writing to chatroom " + name + " history.");
		}
	}

	public String getHistory() {
		String history = "History of " + name + "...\n\n";
		String line = "";

		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				history += line + "\n";
			}

			bufferedReader.close();
			fileReader.close();

		} catch (FileNotFoundException fnfe) {
			System.out.println("Unable to find chatroom " + name + " history file.");
		} catch (IOException ioe) {
			System.out.println("Error reading chatroom " + name + " history.");
		}

		return history + "\n...\n\n";
	}
}
