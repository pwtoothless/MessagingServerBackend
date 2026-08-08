import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ServerState {
    private final Map<WebSocket, String> activeUsers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> voiceChannels = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public Map<WebSocket, String> getActiveUsers() {
        return activeUsers;
    }

    public Map<String, Set<String>> getVoiceChannels() {
        return voiceChannels;
    }

    public void broadcastUserList() {
        JsonObject out = new JsonObject();
        out.addProperty("type", "users");
        out.add("list", gson.toJsonTree(activeUsers.values()));
        broadcast(out.toString());
    }

    public void broadcastVoiceList() {
        JsonObject out = new JsonObject();
        out.addProperty("type", "voice-users");
        out.add("channels", gson.toJsonTree(voiceChannels));
        broadcast(out.toString());
    }

    public void broadcast(String message) {
        for (WebSocket client : activeUsers.keySet()) {
            client.send(message);
        }
    }
}
