import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChatWebSocketServer extends WebSocketServer {

    private final ServerState state;

    public ChatWebSocketServer(int port, ServerState state) {
        super(new InetSocketAddress(port));
        this.state = state;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {}

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String username = state.getActiveUsers().remove(conn);
        if (username != null) {
            for (Set<String> channel : state.getVoiceChannels().values()) {
                channel.remove(username);
            }
        }
        state.broadcastUserList();
        state.broadcastVoiceList();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();

            if ("join".equals(type)) {
                state.getActiveUsers().put(conn, json.get("username").getAsString());
                state.broadcastUserList();
                state.broadcastVoiceList();
            } else if ("chat".equals(type)) {
                String sender = state.getActiveUsers().getOrDefault(conn, "Unknown");
                JsonObject out = new JsonObject();
                out.addProperty("type", "chat");
                out.addProperty("username", sender);
                out.addProperty("message", json.get("message").getAsString());
                state.broadcast(out.toString());
            } else if ("join-voice".equals(type)) {
                String channelName = json.get("channel").getAsString();
                String username = state.getActiveUsers().get(conn);
                
                for (Set<String> usersInChannel : state.getVoiceChannels().values()) {
                    usersInChannel.remove(username);
                }
                
                Set<String> channel = state.getVoiceChannels().computeIfAbsent(channelName, k -> ConcurrentHashMap.newKeySet());
                
                // Notify existing users so they can initiate WebRTC Offer
                JsonObject notify = new JsonObject();
                notify.addProperty("type", "user-joined-voice");
                notify.addProperty("username", username);
                for (Map.Entry<WebSocket, String> entry : state.getActiveUsers().entrySet()) {
                    if (channel.contains(entry.getValue())) {
                        entry.getKey().send(notify.toString());
                    }
                }
                
                channel.add(username);
                state.broadcastVoiceList();
                
            } else if (type.equals("webrtc-offer") || type.equals("webrtc-answer") || type.equals("webrtc-ice")) {
                String target = json.get("target").getAsString();
                String sender = state.getActiveUsers().get(conn);
                json.addProperty("sender", sender); 
                
                for (Map.Entry<WebSocket, String> entry : state.getActiveUsers().entrySet()) {
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
}
