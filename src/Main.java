import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class Main {

    private static final int HTTP_PORT = 8082;
    private static final int WS_PORT = 8081;

    public static void main(String[] args) throws IOException {
        ServerState state = new ServerState();
        Authentication auth = new Authentication();
        
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        httpServer.createContext("/login", new LoginHttpHandler(auth));
        httpServer.setExecutor(null); 

        ChatWebSocketServer webSocketServer = new ChatWebSocketServer(WS_PORT, state);

        httpServer.start();
        webSocketServer.start();
        
        System.out.println("HTTP Server started on port " + HTTP_PORT);
        System.out.println("WebSocket Server started on port " + WS_PORT);
    }
}