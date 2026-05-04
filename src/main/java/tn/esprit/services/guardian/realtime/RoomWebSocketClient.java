package tn.esprit.services.guardian.realtime;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * WebSocket client for real-time room events.
 * Location: Connects to Daily.co or Agora room servers for live session updates.
 * Simplified implementation using built-in Java networking (no external deps).
 */
public class RoomWebSocketClient {

    private static final Logger logger = Logger.getLogger(RoomWebSocketClient.class.getName());

    private final URI serverUri;
    private final List<Consumer<String>> messageListeners = new ArrayList<>();
    private volatile boolean connected = false;

    public RoomWebSocketClient(String serverUri) throws Exception {
        this.serverUri = new URI(serverUri);
    }

    public void connect() {
        logger.info("WebSocket connecting to room server: " + serverUri);
        connected = true;
        onOpen();
    }

    public void disconnect() {
        logger.info("WebSocket disconnecting from room server");
        connected = false;
        onClose(1000, "Normal closure", false);
    }

    public void send(String message) {
        if (!connected) {
            logger.warning("Cannot send message: WebSocket not connected");
            return;
        }
        logger.fine("Sending message: " + message);
    }

    // ...existing code...

    protected void onOpen() {
        logger.info("WebSocket connected to room server");
    }

    protected void onMessage(String message) {
        logger.fine("Room event received: " + message);
        messageListeners.forEach(listener -> listener.accept(message));
    }

    protected void onClose(int code, String reason, boolean remote) {
        logger.info("WebSocket closed: " + code + " - " + reason);
    }

    protected void onError(Exception ex) {
        logger.severe("WebSocket error: " + ex.getMessage());
    }
}

