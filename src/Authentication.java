import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;

public class Authentication {
    public boolean isCorrect(String requestedAccount) {
        Gson gson = new Gson();
        // NOTE: Ensure this path is correct relative to where you run the Java command
        try (JsonReader reader = new JsonReader(new FileReader("./assets/passwords.json"))) {
            
            // 1. Read the file into the corrected data class
            UserFile data = gson.fromJson(reader, UserFile.class);
            
            // 2. Parse the web request
            AuthParser.Credentials requestCreds = AuthParser.parse(requestedAccount);
            
            // 3. Create credentials from the file data for comparison
            AuthParser.Credentials fileCreds = new AuthParser.Credentials(data.getUsername(), data.getPassword());

            // 4. Compare
            if (requestCreds.equals(fileCreds)) {
                System.out.println("Yay, you're in");
                return true;
            } else {
                System.out.println("Get good at typing, and remembering");
                return false;
            }
        } catch (Exception e) { // Catching generic Exception to see parsing errors too
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