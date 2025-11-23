package com.messaging;

import com.messaging.network.NetworkHandler;
import com.messaging.server.ServerMessageHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagingMod implements ModInitializer {
    public static Logger ServerLogger = LoggerFactory.getLogger("Messaging Server");
    

    @Override
    public void onInitialize() {
        // Register packet types (shared between client and server)
        NetworkHandler.registerPackets();

        // Register server-side packet receivers
        NetworkHandler.registerServerReceivers();


        // Initialize server storage when world loads
        // Initialize server storage when world loads
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Use the root directory of the world save
            ServerMessageHandler.initialize(server.getWorldPath(LevelResource.ROOT));
        });

        // Broadcast player list when players join/leave
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                ServerMessageHandler.broadcastPlayerList(
                        server.getPlayerList().getPlayers()
                );
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            server.execute(() -> {
                ServerMessageHandler.broadcastPlayerList(
                        server.getPlayerList().getPlayers()
                );
            });
        });

        System.out.println("Messaging Mod (Server) initialized!");
    }
}