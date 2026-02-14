import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Objects; // Import added

public class AuthParser {

    // Updated Regex: Added \\s* to allow for spaces (e.g., "username": 123 vs "username":123)
    private static final Pattern AUTH_PATTERN = Pattern.compile("\"username\"\\s*:\\s*(\\d+)\\s*,\\s*\"password\"\\s*:\\s*(\\d+)");

    public static class Credentials {
        private final int username;
        private final int password;

        public Credentials(int username, int password) {
            this.username = username;
            this.password = password;
        }

        public int getUsername() { return username; }
        public int getPassword() { return password; }

        // --- NEW CODE: Allows .equals() to work properly ---
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Credentials that = (Credentials) o;
            return username == that.username && password == that.password;
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, password);
        }
        // ----------------------------------------------------
    }

    public static Credentials parse(String input) {
        if (input == null) throw new IllegalArgumentException("Input cannot be null.");

        Matcher matcher = AUTH_PATTERN.matcher(input);
        if (matcher.find()) {
            try {
                return new Credentials(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
                );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ID too large.", e);
            }
        }
        throw new IllegalArgumentException("Invalid format.");
    }
}