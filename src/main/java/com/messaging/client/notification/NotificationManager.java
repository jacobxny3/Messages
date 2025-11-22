package com.messaging.client.notification;

import com.messaging.client.gui.MessageScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {
    private static NotificationManager instance;
    private final List<MessageNotification> notifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 3;
    private static final long NOTIFICATION_DURATION = 5000; // 5 seconds

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void showNotification(String senderName, String message) {
        // Don't show notification if already on message screen with this person
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof MessageScreen) {
            MessageScreen screen = (MessageScreen) client.currentScreen;
            if (screen.getRecipientName().equalsIgnoreCase(senderName)) {
                return; // Already viewing this conversation
            }
        }

        // Remove oldest notification if at max
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }

        // Add new notification
        MessageNotification notification = new MessageNotification(
                senderName,
                message,
                System.currentTimeMillis()
        );
        notifications.add(notification);

        // Play notification sound
        if (client.player != null) {
            client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f)
            );
        }
    }

    public void tick(MinecraftClient client) {
        long currentTime = System.currentTimeMillis();
        Iterator<MessageNotification> iterator = notifications.iterator();

        while (iterator.hasNext()) {
            MessageNotification notification = iterator.next();
            if (currentTime - notification.timestamp > NOTIFICATION_DURATION) {
                iterator.remove();
            }
        }
    }

    public List<MessageNotification> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public void removeNotification(MessageNotification notification) {
        notifications.remove(notification);
    }

    public void openNotification(MessageNotification notification) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new MessageScreen(notification.senderName));
        removeNotification(notification);
    }

    public static class MessageNotification {
        public final String senderName;
        public final String message;
        public final long timestamp;

        public MessageNotification(String senderName, String message, long timestamp) {
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
        }

        public float getAlpha() {
            long age = System.currentTimeMillis() - timestamp;
            if (age < 300) {
                // Fade in
                return age / 300f;
            } else if (age > NOTIFICATION_DURATION - 500) {
                // Fade out
                return (NOTIFICATION_DURATION - age) / 500f;
            }
            return 1.0f;
        }
    }
}