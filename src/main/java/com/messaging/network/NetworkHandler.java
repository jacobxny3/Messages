package com.messaging.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class NetworkHandler {
    public static final Identifier SEND_MESSAGE_ID = Identifier.of("messaging-mod", "send_message");
    public static final Identifier RECEIVE_MESSAGE_ID = Identifier.of("messaging-mod", "receive_message");
    public static final Identifier REQUEST_HISTORY_ID = Identifier.of("messaging-mod", "request_history");
    public static final Identifier SYNC_HISTORY_ID = Identifier.of("messaging-mod", "sync_history");
    public static final Identifier PLAYER_LIST_ID = Identifier.of("messaging-mod", "player_list");
    public static final Identifier REQUEST_PLAYER_LIST_ID = Identifier.of("messaging-mod", "request_player_list");

    private static boolean packetsRegistered = false;

    public static void registerPackets() {
        if (packetsRegistered) return;
        packetsRegistered = true;

        // Register C2S (Client to Server) packets
        PayloadTypeRegistry.playC2S().register(SendMessagePayload.ID, SendMessagePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestHistoryPayload.ID, RequestHistoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestPlayerListPayload.ID, RequestPlayerListPayload.CODEC);

        // Register S2C (Server to Client) packets
        PayloadTypeRegistry.playS2C().register(ReceiveMessagePayload.ID, ReceiveMessagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncHistoryPayload.ID, SyncHistoryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerListPayload.ID, PlayerListPayload.CODEC);
    }

    public static void registerClientReceivers() {
        // Handle receiving messages
        ClientPlayNetworking.registerGlobalReceiver(ReceiveMessagePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MessageManager.getInstance().addMessage(
                        payload.sender(),
                        payload.message(),
                        false,
                        payload.timestamp()
                );

                // Show notification
                com.messaging.client.notification.NotificationManager.getInstance()
                        .showNotification(payload.sender(), payload.message());
            });
        });

        // Handle history sync
        ClientPlayNetworking.registerGlobalReceiver(SyncHistoryPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MessageManager.getInstance().loadHistoryFromServer(
                        payload.recipient(),
                        payload.messages()
                );
            });
        });

        // Handle player list updates
        ClientPlayNetworking.registerGlobalReceiver(PlayerListPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MessageManager.getInstance().updateOnlinePlayers(payload.players());
            });
        });
    }

    public static void registerServerReceivers() {
        // Handle sending messages
        ServerPlayNetworking.registerGlobalReceiver(SendMessagePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                com.messaging.server.ServerMessageHandler.handleSendMessage(
                        context.player(),
                        payload.recipient(),
                        payload.message(),
                        payload.allowLogging()
                );
            });
        });

        // Handle history requests
        ServerPlayNetworking.registerGlobalReceiver(RequestHistoryPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                com.messaging.server.ServerMessageHandler.handleHistoryRequest(
                        context.player(),
                        payload.otherPlayer()
                );
            });
        });

        // Handle player list requests
        ServerPlayNetworking.registerGlobalReceiver(RequestPlayerListPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                com.messaging.server.ServerMessageHandler.handlePlayerListRequest(context.player());
            });
        });
    }

    // Payload classes
    public record SendMessagePayload(String recipient, String message, boolean allowLogging) implements CustomPayload {
        public static final Id<SendMessagePayload> ID = new Id<>(SEND_MESSAGE_ID);
        public static final PacketCodec<RegistryByteBuf, SendMessagePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, SendMessagePayload::recipient,
                PacketCodecs.STRING, SendMessagePayload::message,
                PacketCodecs.BOOL, SendMessagePayload::allowLogging,
                SendMessagePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ReceiveMessagePayload(String sender, String message, long timestamp) implements CustomPayload {
        public static final Id<ReceiveMessagePayload> ID = new Id<>(RECEIVE_MESSAGE_ID);
        public static final PacketCodec<RegistryByteBuf, ReceiveMessagePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ReceiveMessagePayload::sender,
                PacketCodecs.STRING, ReceiveMessagePayload::message,
                PacketCodecs.VAR_LONG, ReceiveMessagePayload::timestamp,
                ReceiveMessagePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RequestHistoryPayload(String otherPlayer) implements CustomPayload {
        public static final Id<RequestHistoryPayload> ID = new Id<>(REQUEST_HISTORY_ID);
        public static final PacketCodec<RegistryByteBuf, RequestHistoryPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, RequestHistoryPayload::otherPlayer,
                RequestHistoryPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SyncHistoryPayload(String recipient, String messages) implements CustomPayload {
        public static final Id<SyncHistoryPayload> ID = new Id<>(SYNC_HISTORY_ID);
        public static final PacketCodec<RegistryByteBuf, SyncHistoryPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, SyncHistoryPayload::recipient,
                PacketCodecs.STRING, SyncHistoryPayload::messages,
                SyncHistoryPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PlayerListPayload(String players) implements CustomPayload {
        public static final Id<PlayerListPayload> ID = new Id<>(PLAYER_LIST_ID);
        public static final PacketCodec<RegistryByteBuf, PlayerListPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, PlayerListPayload::players,
                PlayerListPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RequestPlayerListPayload() implements CustomPayload {
        public static final Id<RequestPlayerListPayload> ID = new Id<>(REQUEST_PLAYER_LIST_ID);
        public static final PacketCodec<RegistryByteBuf, RequestPlayerListPayload> CODEC = PacketCodec.unit(new RequestPlayerListPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}