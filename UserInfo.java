
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class UserInfo {
	private ArrayList<String> userData;
	private String fileName;

	UserInfo(int port) {
		fileName = "E:\\file" + port + ".txt";
		userData = new ArrayList<String>();

		readUserData();

		System.out.println("Registered users: ");
		for (int i = 0; i < userData.size(); i++)
			System.out.println(userData.get(i));
	}

	boolean registerUser(String user) {
		if (!freeUsername(user.split(" ")[0])) {
			return false;
		}

		try {
			FileWriter fileWriter = new FileWriter(fileName, true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			bufferedWriter.write(user + "\n");
			bufferedWriter.flush();

			bufferedWriter.close();
			fileWriter.close();

			userData.add(user);
		} catch (IOException ioe) {
			System.out.println("IO exception registering user. ");
			return false;
		}

		return true;
	}

	void readUserData() {
		String line;

		File f = new File(fileName);

		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				System.out.println("Failed to create user data file. ");
			}
		} else {
			try {
				FileReader fileReader = new FileReader(fileName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);

				while ((line = bufferedReader.readLine()) != null) {
					userData.add(line);
				}

				bufferedReader.close();

			} catch (Exception e) {
				System.out.println("Unable to find user data.");
			}
		}
	}

	boolean freeUsername(String username) {
		for (int i = 0; i < userData.size(); i++)
			if (username.equals(userData.get(i).split(" ")[0]))
				return false;

		return true;
	}

	boolean hasUser(String user) {
		for (int i = 0; i < userData.size(); i++)
			if (user.equals(userData.get(i)))
				return true;

		return false;
	}

}