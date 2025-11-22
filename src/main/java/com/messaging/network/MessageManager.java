package com.messaging.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager {
    private static MessageManager instance;
    private final Map<String, List<ChatMessage>> messageHistory = new ConcurrentHashMap<>();
    private final Set<String> onlinePlayers = ConcurrentHashMap.newKeySet();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path messageStoragePath;

    private MessageManager() {
        initializeStorage();
    }

    public static MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }

    private void initializeStorage() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.runDirectory != null) {
                messageStoragePath = client.runDirectory.toPath().resolve("messaging-mod");
                Files.createDirectories(messageStoragePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create messaging storage directory: " + e.getMessage());
        }
    }

    public void addMessage(String otherPlayer, String content, boolean sentByPlayer, long timestamp) {
        String key = normalizePlayerName(otherPlayer);
        List<ChatMessage> messages = messageHistory.computeIfAbsent(key, k -> {
            // Load from disk if not in memory
            loadMessages(k);
            return messageHistory.getOrDefault(k, new ArrayList<>());
        });

        // Check if this exact message already exists (prevent duplicates)
        boolean exists = messages.stream().anyMatch(msg ->
                msg.content.equals(content) &&
                        Math.abs(msg.timestamp - timestamp) < 1000 && // Within 1 second tolerance
                        msg.isSentByPlayer == sentByPlayer
        );

        if (!exists) {
            messages.add(new ChatMessage(content, sentByPlayer, timestamp));
            // Sort by timestamp to keep chronological order
            messages.sort(Comparator.comparingLong(m -> m.timestamp));
            saveMessages(key);
        }
    }

    public List<ChatMessage> getMessages(String otherPlayer) {
        String key = normalizePlayerName(otherPlayer);
        if (!messageHistory.containsKey(key)) {
            loadMessages(key);
        }
        return new ArrayList<>(messageHistory.getOrDefault(key, new ArrayList<>()));
    }

    public void sendMessage(String recipient, String message) {
        long timestamp = System.currentTimeMillis();

        // Add message locally
        addMessage(recipient, message, true, timestamp);

        // Send to server with logging preference
        if (ClientPlayNetworking.canSend(NetworkHandler.SendMessagePayload.ID)) {
            boolean allowLogging = com.messaging.config.ConfigManager.getInstance().allowServerLogging();
            ClientPlayNetworking.send(new NetworkHandler.SendMessagePayload(recipient, message, allowLogging));
        }
    }

    public void requestHistory(String otherPlayer) {
        if (ClientPlayNetworking.canSend(NetworkHandler.RequestHistoryPayload.ID)) {
            ClientPlayNetworking.send(new NetworkHandler.RequestHistoryPayload(otherPlayer));
        }
    }

    public void requestPlayerList() {
        if (ClientPlayNetworking.canSend(NetworkHandler.RequestPlayerListPayload.ID)) {
            ClientPlayNetworking.send(new NetworkHandler.RequestPlayerListPayload());

        }
    }

    public void loadHistoryFromServer(String recipient, String messagesJson) {
        try {
            List<ChatMessage> serverMessages = gson.fromJson(
                    messagesJson,
                    new TypeToken<List<ChatMessage>>(){}.getType()
            );
            if (serverMessages != null && !serverMessages.isEmpty()) {
                String key = normalizePlayerName(recipient);

                // Get existing messages
                List<ChatMessage> existingMessages = messageHistory.getOrDefault(key, new ArrayList<>());

                // Create a map for faster lookup using timestamp ranges
                Map<String, ChatMessage> existingMap = new HashMap<>();
                for (ChatMessage msg : existingMessages) {
                    String msgKey = msg.content + "_" + (msg.timestamp / 1000) + "_" + msg.isSentByPlayer;
                    existingMap.put(msgKey, msg);
                }

                // Add only new messages from server
                for (ChatMessage msg : serverMessages) {
                    String msgKey = msg.content + "_" + (msg.timestamp / 1000) + "_" + msg.isSentByPlayer;
                    if (!existingMap.containsKey(msgKey)) {
                        existingMessages.add(msg);
                    }
                }

                // Sort by timestamp
                existingMessages.sort(Comparator.comparingLong(m -> m.timestamp));

                messageHistory.put(key, existingMessages);
                saveMessages(key);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse message history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateOnlinePlayers(String playersJson) {
        try {
            List<String> players = gson.fromJson(
                    playersJson,
                    new TypeToken<List<String>>(){}.getType()
            );
            if (players != null) {
                onlinePlayers.clear();
                // Normalize all player names
                for (String player : players) {
                    String normalized = normalizePlayerName(player);
                    onlinePlayers.add(normalized);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse player list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Set<String> getOnlinePlayers() {
        return new HashSet<>(onlinePlayers);
    }

    public boolean isPlayerOnline(String playerName) {
        return onlinePlayers.contains(normalizePlayerName(playerName));
    }

    public Set<String> getAllContacts() {
        Set<String> contacts = new HashSet<>();

        // Add all contacts from message history
        for (String key : messageHistory.keySet()) {
            contacts.add(key);
        }

        // Remove self from contacts
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String selfName = normalizePlayerName(client.player.getName().getString());
            contacts.remove(selfName);
        }

        return contacts;
    }

    private String normalizePlayerName(String playerName) {
        // Always store and compare player names in lowercase to avoid duplicates
        return playerName.toLowerCase();
    }

    private void saveMessages(String playerKey) {
        if (messageStoragePath == null) return;

        try {
            File file = messageStoragePath.resolve(playerKey + ".json").toFile();
            List<ChatMessage> messages = messageHistory.get(playerKey);

            if (messages != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(messages, writer);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save messages for " + playerKey + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMessages(String playerKey) {
        if (messageStoragePath == null) return;

        // Don't reload if already in memory
        if (messageHistory.containsKey(playerKey)) {
            return;
        }

        try {
            File file = messageStoragePath.resolve(playerKey + ".json").toFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    List<ChatMessage> messages = gson.fromJson(
                            reader,
                            new TypeToken<List<ChatMessage>>(){}.getType()
                    );
                    if (messages != null) {
                        messageHistory.put(playerKey, messages);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load messages for " + playerKey + ": " + e.getMessage());
        }
    }

    public void clearHistory(String otherPlayer) {
        String key = normalizePlayerName(otherPlayer);
        messageHistory.remove(key);

        if (messageStoragePath != null) {
            try {
                File file = messageStoragePath.resolve(key + ".json").toFile();
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                System.err.println("Failed to delete message file: " + e.getMessage());
            }
        }
    }

    public static class ChatMessage {
        public String content;
        public boolean isSentByPlayer;
        public long timestamp;

        public ChatMessage(String content, boolean isSentByPlayer, long timestamp) {
            this.content = content;
            this.isSentByPlayer = isSentByPlayer;
            this.timestamp = timestamp;
        }
    }
}