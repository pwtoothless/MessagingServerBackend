import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginHttpHandler implements HttpHandler {

    private final Authentication auth;

    public LoginHttpHandler(Authentication auth) {
        this.auth = auth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            if (requestBody.contains("username") && requestBody.contains("password")) {
                if (auth.isCorrect(requestBody)) {
                    String response = "{\"success\": true}";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    String response = "{\"success\": false, \"message\": \"Wrong username or password\"}";
                    exchange.sendResponseHeaders(401, response.length()); 
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close(); 
                }
            } else {
                String response = "{\"success\": false, \"message\": \"Invalid request\"}";
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } else if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1); 
        } else {
            exchange.sendResponseHeaders(405, -1); 
        }
    }
}
