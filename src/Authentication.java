import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;

public class Authentication {
	public boolean isCorrect(String requestedAccount) {
	    Gson gson = new Gson();
	    try (JsonReader reader = new JsonReader(new FileReader("./assets/passwords.json"))) {
	        
	        // 1. Read the JSON as an Array of users (UserFile[].class), not a single object
	        UserFile[] allUsers = gson.fromJson(reader, UserFile[].class);
	        
	        // 2. Parse the incoming web request once
	        AuthParser.Credentials requestCreds = AuthParser.parse(requestedAccount);
	        
	        // 3. Loop through EVERY user in the file to check for a match
	        for (UserFile user : allUsers) {
	            AuthParser.Credentials fileCreds = new AuthParser.Credentials(user.getUsername(), user.getPassword());
	            
	            if (requestCreds.equals(fileCreds)) {
	                System.out.println("Yay, you're in (Matched User: " + user.getUsername() + ")");
	                return true; // Found a match! Stop looking.
	            }
	        }
	        
	        // 4. If the loop finishes and we haven't returned true, no match was found
	        System.out.println("Get good at typing, and remembering");
	        return false;

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return false;
	}

    // Renamed to match the JSON keys: "username" and "password"
    public class UserFile {
        private int username;
        private int password;

        public int getUsername() { return username; }
        public int getPassword() { return password; }
    }
}