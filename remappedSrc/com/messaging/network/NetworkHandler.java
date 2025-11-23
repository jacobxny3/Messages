package com.messaging.network;

import com.messaging.network.MessageManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class NetworkHandler {
    public static final ResourceLocation SEND_MESSAGE_ID = ResourceLocation.fromNamespaceAndPath("messaging-mod", "send_message");
    public static final ResourceLocation RECEIVE_MESSAGE_ID = ResourceLocation.fromNamespaceAndPath("messaging-mod", "receive_message");
    public static final ResourceLocation REQUEST_HISTORY_ID = ResourceLocation.fromNamespaceAndPath("messaging-mod", "request_history");
    public static final ResourceLocation SYNC_HISTORY_ID = ResourceLocation.fromNamespaceAndPath("messaging-mod", "sync_history");
    public static final ResourceLocation PLAYER_LIST_ID = ResourceLocation.fromNamespaceAndPath("messaging-mod", "player_list");
    public static final ResourceLocation REQUEST_PLAYER_LIST_ID = ResourceLocation.fromNamespaceAndPath("messaging-mod", "request_player_list");

    private static boolean packetsRegistered = false;

    public static void registerPackets() {
        if (packetsRegistered) return;
        packetsRegistered = true;

        PayloadTypeRegistry.playC2S().register(SendMessagePayload.ID, SendMessagePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestHistoryPayload.ID, RequestHistoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestPlayerListPayload.ID, RequestPlayerListPayload.CODEC);

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
    public record SendMessagePayload(String recipient, String message, boolean allowLogging) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SendMessagePayload> ID = new CustomPacketPayload.Type<>(SEND_MESSAGE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SendMessagePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SendMessagePayload::recipient,
                ByteBufCodecs.STRING_UTF8, SendMessagePayload::message,
                ByteBufCodecs.BOOL, SendMessagePayload::allowLogging,
                SendMessagePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ReceiveMessagePayload(String sender, String message, long timestamp) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ReceiveMessagePayload> ID = new CustomPacketPayload.Type<>(RECEIVE_MESSAGE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ReceiveMessagePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ReceiveMessagePayload::sender,
                ByteBufCodecs.STRING_UTF8, ReceiveMessagePayload::message,
                ByteBufCodecs.VAR_LONG, ReceiveMessagePayload::timestamp,
                ReceiveMessagePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record RequestHistoryPayload(String otherPlayer) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestHistoryPayload> ID = new CustomPacketPayload.Type<>(REQUEST_HISTORY_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestHistoryPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RequestHistoryPayload::otherPlayer,
                RequestHistoryPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record SyncHistoryPayload(String recipient, String messages) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncHistoryPayload> ID = new CustomPacketPayload.Type<>(SYNC_HISTORY_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncHistoryPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SyncHistoryPayload::recipient,
                ByteBufCodecs.STRING_UTF8, SyncHistoryPayload::messages,
                SyncHistoryPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record PlayerListPayload(String players) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlayerListPayload> ID = new CustomPacketPayload.Type<>(PLAYER_LIST_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerListPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PlayerListPayload::players,
                PlayerListPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record RequestPlayerListPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestPlayerListPayload> ID = new CustomPacketPayload.Type<>(REQUEST_PLAYER_LIST_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestPlayerListPayload> CODEC = StreamCodec.unit(new RequestPlayerListPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}