
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.sun.net.httpserver.HttpServer;

public class Main {

    /**
     * The port number for the HTTP and WebSocket server.
     */
    private static final int HTTP_PORT = 8082;
    private static final int WS_PORT = 8081;

    /**
     * A set to store all active WebSocket connections.
     * This is thread-safe.
     */
    private static final Set<WebSocket> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws IOException{
    	// My Auth Backend
    	Authentication auth = new Authentication();
    	
        // --- HTTP Server Setup ---
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);

        // Context for the login handler
        httpServer.createContext("/login", exchange -> {
            //
            // THIS IS A VERY IMPORTANT NOTE
            //
            // The following line is CRUCIAL for handling Cross-Origin Resource Sharing (CORS) requests.
            // Modern web browsers, for security reasons, block web pages from making requests
            // to a different domain than the one that served the page. This is known as the
            // Same-Origin Policy.
            //
            // By adding the 'Access-Control-Allow-Origin' header with a value of "*", we are
            // telling the browser that any origin is allowed to access this resource. This is
            // generally fine for development purposes, but in a production environment, you
            // would want to restrict this to the specific domain of your website,
            // for example: "https://your-website.com".
            //
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Read the request body
                InputStream inputStream = exchange.getRequestBody();
                String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(requestBody);

                // For simplicity, we'll just check if the request body contains a username and password.
                // In a real application, you would parse the JSON and check against a database.
                if (requestBody.contains("username") && requestBody.contains("password")) {
                	if (auth.isCorrect(requestBody)) {
                	    // Success Case
                	    String response = "{\"success\": true}";
                	    exchange.sendResponseHeaders(200, response.length());
                	    OutputStream os = exchange.getResponseBody();
                	    os.write(response.getBytes());
                	    os.close();
                	} else {
                	    // --- NEW CODE: Failure Case ---
                	    // You MUST send a response here, or the browser will hang!
                	    System.out.println("Wrong Password, or Other Error");
                	    String response = "{\"success\": false, \"message\": \"Wrong username or password\"}";
                	    exchange.sendResponseHeaders(401, response.length()); // 401 = Unauthorized
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
                // The OPTIONS method is sent by the browser as a "preflight" request
                // to check if the server will allow the actual request (e.g., a POST request).
                // We need to respond with the allowed methods and headers.
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1); // 204 No Content
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        });

        // Set the executor for the HTTP server
        httpServer.setExecutor(null); // Use the default executor

        // --- WebSocket Server Setup ---
        WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(WS_PORT)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                clients.add(conn);
                System.out.println("New client connected: " + conn.getRemoteSocketAddress());
                broadcast("New client connected: " + conn.getRemoteSocketAddress());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                clients.remove(conn);
                System.out.println("Client disconnected: " + conn.getRemoteSocketAddress());
                broadcast("Client disconnected: " + conn.getRemoteSocketAddress());
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                System.out.println("Message from client " + conn.getRemoteSocketAddress() + ": " + message);
                // Broadcast the message to all other clients
                broadcast(conn.getRemoteSocketAddress() + ": " + message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                ex.printStackTrace();
                if (conn != null) {
                    clients.remove(conn);
                    // some errors like port binding failed may not be assignable to a specific websocket
                }
            }

            @Override
            public void onStart() {
                System.out.println("WebSocket server started successfully");
                setConnectionLostTimeout(0);
                setConnectionLostTimeout(100);
            }
        };

        // Start both servers
        httpServer.start();
        webSocketServer.start();

        System.out.println("HTTP and WebSocket server started on port " + HTTP_PORT + " and " + WS_PORT);
    }

    /**
     * Broadcasts a message to all connected clients.
     * @param message The message to broadcast.
     */
    private static void broadcast(String message) {
        synchronized (clients) {
            for (WebSocket client : clients) {
                client.send(message);
            }
        }
    }
}
