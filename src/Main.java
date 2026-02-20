import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {

    private static final int HTTP_PORT = 8082;
    private static final int WS_PORT = 8081;

    private static final Map<WebSocket, String> activeUsers = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> voiceChannels = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        Authentication auth = new Authentication();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);

        httpServer.createContext("/login", exchange -> {
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
        });

        httpServer.setExecutor(null); 

        WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(WS_PORT)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {}

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                String username = activeUsers.remove(conn);
                if (username != null) {
                    for (Set<String> channel : voiceChannels.values()) {
                        channel.remove(username);
                    }
                }
                broadcastUserList();
                broadcastVoiceList();
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                try {
                    JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                    String type = json.get("type").getAsString();

                    if ("join".equals(type)) {
                        activeUsers.put(conn, json.get("username").getAsString());
                        broadcastUserList();
                        broadcastVoiceList();
                    } else if ("chat".equals(type)) {
                        String sender = activeUsers.getOrDefault(conn, "Unknown");
                        JsonObject out = new JsonObject();
                        out.addProperty("type", "chat");
                        out.addProperty("username", sender);
                        out.addProperty("message", json.get("message").getAsString());
                        broadcast(out.toString());
                    } else if ("join-voice".equals(type)) {
                        String channelName = json.get("channel").getAsString();
                        String username = activeUsers.get(conn);
                        
                        for (Set<String> usersInChannel : voiceChannels.values()) {
                            usersInChannel.remove(username);
                        }
                        
                        Set<String> channel = voiceChannels.computeIfAbsent(channelName, k -> ConcurrentHashMap.newKeySet());
                        
                        // Notify existing users so they can initiate WebRTC Offer
                        JsonObject notify = new JsonObject();
                        notify.addProperty("type", "user-joined-voice");
                        notify.addProperty("username", username);
                        for (Map.Entry<WebSocket, String> entry : activeUsers.entrySet()) {
                            if (channel.contains(entry.getValue())) {
                                entry.getKey().send(notify.toString());
                            }
                        }
                        
                        channel.add(username);
                        broadcastVoiceList();
                        
                    } else if (type.equals("webrtc-offer") || type.equals("webrtc-answer") || type.equals("webrtc-ice")) {
                        String target = json.get("target").getAsString();
                        String sender = activeUsers.get(conn);
                        json.addProperty("sender", sender); 
                        
                        for (Map.Entry<WebSocket, String> entry : activeUsers.entrySet()) {
                            if (entry.getValue().equals(target)) {
                                entry.getKey().send(json.toString());
                                break;
                            }
                        }
                    }
                } catch (Exception e) {}
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {}

            @Override
            public void onStart() {}
        };

        httpServer.start();
        webSocketServer.start();
    }

    private static void broadcastUserList() {
        JsonObject out = new JsonObject();
        out.addProperty("type", "users");
        out.add("list", gson.toJsonTree(activeUsers.values()));
        broadcast(out.toString());
    }

    private static void broadcastVoiceList() {
        JsonObject out = new JsonObject();
        out.addProperty("type", "voice-users");
        out.add("channels", gson.toJsonTree(voiceChannels));
        broadcast(out.toString());
    }

    private static void broadcast(String message) {
        for (WebSocket client : activeUsers.keySet()) {
            client.send(message);
        }
    }
}