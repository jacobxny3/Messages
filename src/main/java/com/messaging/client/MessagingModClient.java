package com.messaging.client;

import com.messaging.client.gui.ContactListScreen;
import com.messaging.client.notification.NotificationManager;
import com.messaging.client.notification.NotificationRenderer;
import com.messaging.network.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MessagingModClient implements ClientModInitializer {
    private static KeyBinding openMessagesKey;
    private static final NotificationRenderer notificationRenderer = new NotificationRenderer();

    @Override
    public void onInitializeClient() {
        com.messaging.config.ConfigManager.getInstance();
        // Register packet types (shared between client and server)
        NetworkHandler.registerPackets();

        // Register client-side packet receivers
        NetworkHandler.registerClientReceivers();

        // Register keybinding (default: M key)
        openMessagesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.messaging.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.messaging"
        ));

        // Register tick event to check for key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMessagesKey.wasPressed()) {
                if (client.player != null) {
                    // Open contact list screen
                    client.setScreen(new ContactListScreen());
                }
            }

            // Tick notification manager
            NotificationManager.getInstance().tick(client);
        });

        // Register HUD rendering for notifications
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            notificationRenderer.render(context,
                    (int) context.getMatrices().peek().getPositionMatrix().m30(),
                    (int) context.getMatrices().peek().getPositionMatrix().m31());
        });

        System.out.println("Messaging Mod (Client) initialized!");
    }

    public static NotificationRenderer getNotificationRenderer() {
        return notificationRenderer;
    }
}