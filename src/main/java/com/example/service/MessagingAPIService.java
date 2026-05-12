package com.example.service;

import com.pusher.rest.Pusher;

public class MessagingAPIService {

    // Pusher credentials loaded from config.properties
    private static final String APP_ID = ConfigLoader.get("pusher.app.id");
    private static final String APP_KEY = ConfigLoader.get("pusher.app.key");
    private static final String APP_SECRET = ConfigLoader.get("pusher.app.secret");
    private static final String CLUSTER = ConfigLoader.get("pusher.cluster");

    private final   Pusher pusherRest;

    public MessagingAPIService() {
        if (APP_ID != null && !APP_ID.equals("YOUR_PUSHER_APP_ID")) {
            this.pusherRest = new Pusher(APP_ID, APP_KEY, APP_SECRET);
            this.pusherRest.setCluster(CLUSTER);
        } else {
            this.pusherRest = null;
        }
    }

    public void sendMessage(Integer roomId, String sender, String content) {
        if (pusherRest != null) {
            pusherRest.trigger("room-" + roomId, "new-message", content);
        }
    }
    
    public void disconnect() {
        // Disconnect logic if needed
    }
}
