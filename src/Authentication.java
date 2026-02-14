import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;

public class Authentication {
	
	public boolean isCorrect(String requestName, String requestPassword, int hashName, int hashPassword ) {
		if (requestName.hashCode() == hashName) {
			if (requestPassword.hashCode() == hashPassword) {
				return true;
			}
			else
				return false;
		}
		else
			return false;
	}
	
	public static void authentication(String[] args) {
		Gson gson = new Gson();
		 try (JsonReader reader = new JsonReader(new FileReader(".\\assets\\passwords.json"))) {
	            MyData data = gson.fromJson(reader, MyData.class);

	            // Access the data
	            System.out.println("Name: " + data.getName());
	            System.out.println("ID: " + data.getId());
	        }
		 catch (IOException e) {
        	e.printStackTrace();
	 	}
	}
	
	public class MyData {
	    private String name;
	    private int id;
	    // Getters and setters (or use a Java record)
	    public String getName() { return name; }
	    public void setName(String name) { this.name = name; }
	    public int getId() { return id; }
	    public void setId(int id) { this.id = id; }
	}
}
