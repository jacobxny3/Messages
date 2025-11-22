package com.messaging.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.messaging.network.NetworkHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMessageHandler {
    private static final Map<String, List<StoredMessage>> serverMessageHistory = new ConcurrentHashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Path serverStoragePath;

    public static void initialize(Path worldPath) {
        try {
            serverStoragePath = worldPath.resolve("messaging-mod");
            Files.createDirectories(serverStoragePath);
        } catch (IOException e) {
            System.err.println("Failed to create server messaging storage: " + e.getMessage());
        }
    }

    public static void handleSendMessage(ServerPlayerEntity sender, String recipientName, String message, boolean allowLogging) {
        String senderName = sender.getName().getString();
        long timestamp = System.currentTimeMillis();

        // Only log if sender allows it
        if (allowLogging) {
            System.out.println("[Messaging] " + senderName + " -> " + recipientName + ": " + message);
        }

        // Store message on server
        storeMessage(senderName, recipientName, message, timestamp);

        // Find recipient and send if online
        ServerPlayerEntity recipient = sender.getServer().getPlayerManager().getPlayer(recipientName);
        if (recipient != null) {
            if (allowLogging) {
                System.out.println("[Messaging] Sending message to online recipient: " + recipientName);
            }
            ServerPlayNetworking.send(
                    recipient,
                    new NetworkHandler.ReceiveMessagePayload(senderName, message, timestamp)
            );
        } else {
            if (allowLogging) {
                System.out.println("[Messaging] Recipient offline, message stored: " + recipientName);
            }
        }

        // Send player list update to all players
        broadcastPlayerList(sender.getServer().getPlayerManager().getPlayerList());
    }

    public static void handleHistoryRequest(ServerPlayerEntity requester, String otherPlayer) {
        String requesterName = requester.getName().getString();
        List<StoredMessage> messages = loadConversation(requesterName, otherPlayer);

        // Convert to client format
        List<ClientMessage> clientMessages = new ArrayList<>();
        for (StoredMessage msg : messages) {
            boolean sentByPlayer = msg.sender.equals(requesterName);
            clientMessages.add(new ClientMessage(msg.message, sentByPlayer, msg.timestamp));
        }

        String messagesJson = gson.toJson(clientMessages);
        ServerPlayNetworking.send(
                requester,
                new NetworkHandler.SyncHistoryPayload(otherPlayer, messagesJson)
        );
    }

    public static void handlePlayerListRequest(ServerPlayerEntity requester) {
        List<ServerPlayerEntity> players = requester.getServer().getPlayerManager().getPlayerList();

        List<String> playerNames = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            playerNames.add(player.getName().getString());
        }

        String playersJson = gson.toJson(playerNames);
        ServerPlayNetworking.send(
                requester,
                new NetworkHandler.PlayerListPayload(playersJson)
        );
    }

    public static void broadcastPlayerList(List<ServerPlayerEntity> players) {
        List<String> playerNames = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
            playerNames.add(player.getName().getString());
        }


        String playersJson = gson.toJson(playerNames);
        NetworkHandler.PlayerListPayload payload = new NetworkHandler.PlayerListPayload(playersJson);

        for (ServerPlayerEntity player : players) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void storeMessage(String sender, String recipient, String message, long timestamp) {
        String conversationKey = getConversationKey(sender, recipient);

        List<StoredMessage> messages = serverMessageHistory.computeIfAbsent(
                conversationKey,
                k -> loadMessagesFromFile(conversationKey)
        );

        messages.add(new StoredMessage(sender, recipient, message, timestamp));
        saveMessagesToFile(conversationKey, messages);
    }

    private static List<StoredMessage> loadConversation(String player1, String player2) {
        String conversationKey = getConversationKey(player1, player2);

        if (!serverMessageHistory.containsKey(conversationKey)) {
            serverMessageHistory.put(conversationKey, loadMessagesFromFile(conversationKey));
        }

        return new ArrayList<>(serverMessageHistory.get(conversationKey));
    }

    private static String getConversationKey(String player1, String player2) {
        // Create consistent key regardless of order
        List<String> names = Arrays.asList(player1.toLowerCase(), player2.toLowerCase());
        Collections.sort(names);
        return names.get(0) + "_" + names.get(1);
    }

    private static List<StoredMessage> loadMessagesFromFile(String conversationKey) {
        if (serverStoragePath == null) return new ArrayList<>();

        try {
            File file = serverStoragePath.resolve(conversationKey + ".json").toFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    List<StoredMessage> messages = gson.fromJson(
                            reader,
                            new TypeToken<List<StoredMessage>>(){}.getType()
                    );
                    return messages != null ? messages : new ArrayList<>();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load conversation " + conversationKey + ": " + e.getMessage());
        }

        return new ArrayList<>();
    }

    private static void saveMessagesToFile(String conversationKey, List<StoredMessage> messages) {
        if (serverStoragePath == null) return;

        try {
            File file = serverStoragePath.resolve(conversationKey + ".json").toFile();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(messages, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save conversation " + conversationKey + ": " + e.getMessage());
        }
    }

    private static class StoredMessage {
        String sender;
        String recipient;
        String message;
        long timestamp;

        StoredMessage(String sender, String recipient, String message, long timestamp) {
            this.sender = sender;
            this.recipient = recipient;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    private static class ClientMessage {
        String content;
        boolean isSentByPlayer;
        long timestamp;

        ClientMessage(String content, boolean isSentByPlayer, long timestamp) {
            this.content = content;
            this.isSentByPlayer = isSentByPlayer;
            this.timestamp = timestamp;
        }
    }
}