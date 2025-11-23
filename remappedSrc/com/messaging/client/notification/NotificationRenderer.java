package com.messaging.client.notification;

import com.messaging.client.notification.NotificationManager.MessageNotification;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class NotificationRenderer {
    private static final int NOTIFICATION_WIDTH = 300;
    private static final int NOTIFICATION_HEIGHT = 60;
    private static final int NOTIFICATION_PADDING = 10;
    private static final int NOTIFICATION_SPACING = 5;
    private static final int CLOSE_BUTTON_SIZE = 16;

    private MessageNotification hoveredNotification = null;
    private MessageNotification hoveredCloseButton = null;

    public void render(GuiGraphics context, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        NotificationManager manager = NotificationManager.getInstance();
        List<MessageNotification> notifications = manager.getNotifications();

        if (notifications.isEmpty()) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Position notifications in top-right corner
        int x = screenWidth - NOTIFICATION_WIDTH - 20;
        int startY = 20;

        hoveredNotification = null;
        hoveredCloseButton = null;

        for (int i = 0; i < notifications.size(); i++) {
            MessageNotification notification = notifications.get(i);
            int y = startY + (i * (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING));

            boolean isHovered = mouseX >= x && mouseX <= x + NOTIFICATION_WIDTH &&
                    mouseY >= y && mouseY <= y + NOTIFICATION_HEIGHT;

            boolean isCloseHovered = mouseX >= x + NOTIFICATION_WIDTH - CLOSE_BUTTON_SIZE - 5 &&
                    mouseX <= x + NOTIFICATION_WIDTH - 5 &&
                    mouseY >= y + 5 &&
                    mouseY <= y + 5 + CLOSE_BUTTON_SIZE;

            if (isHovered && !isCloseHovered) {
                hoveredNotification = notification;
            }

            if (isCloseHovered) {
                hoveredCloseButton = notification;
            }

            renderNotification(context, notification, x, y, isHovered, isCloseHovered, client.font);
        }
    }

    private void renderNotification(GuiGraphics context, MessageNotification notification,
                                    int x, int y, boolean isHovered, boolean isCloseHovered,
                                    Font textRenderer) {
        float alpha = notification.getAlpha();
        if (alpha <= 0) return;

        // Calculate colors with alpha
        int bgColor = applyAlpha(isHovered ? 0xFF2C2C2E : 0xFF1C1C1E, alpha);
        int borderColor = applyAlpha(0xFF007AFF, alpha);
        int textColor = applyAlpha(0xFFFFFFFF, alpha);
        int subTextColor = applyAlpha(0xFF888888, alpha);

        // Background with shadow
        context.fill(x + 2, y + 2, x + NOTIFICATION_WIDTH + 2, y + NOTIFICATION_HEIGHT + 2, applyAlpha(0xFF000000, alpha * 0.3f));
        context.fill(x, y, x + NOTIFICATION_WIDTH, y + NOTIFICATION_HEIGHT, bgColor);

        // Border (left side accent)
        context.fill(x, y, x + 4, y + NOTIFICATION_HEIGHT, borderColor);

        // Sender name
        Component senderText = Component.literal(notification.senderName).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
        context.drawString(textRenderer, senderText, x + NOTIFICATION_PADDING, y + NOTIFICATION_PADDING, textColor, false);

        // Message preview
        String messagePreview = notification.message;
        if (messagePreview.length() > 40) {
            messagePreview = messagePreview.substring(0, 37) + "...";
        }

        Component messageText = Component.literal(messagePreview).withStyle(ChatFormatting.GRAY);
        context.drawString(textRenderer, messageText, x + NOTIFICATION_PADDING, y + NOTIFICATION_PADDING + 15, subTextColor, false);

        // "New Message" label
        Component newMsgText = Component.literal("New Message").withStyle(ChatFormatting.DARK_GRAY);
        context.drawString(textRenderer, newMsgText, x + NOTIFICATION_PADDING, y + NOTIFICATION_HEIGHT - 20, subTextColor, false);

        // Close button (X)
        int closeX = x + NOTIFICATION_WIDTH - CLOSE_BUTTON_SIZE - 5;
        int closeY = y + 5;
        int closeBg = applyAlpha(isCloseHovered ? 0xFF444444 : 0xFF2C2C2E, alpha);

        context.fill(closeX, closeY, closeX + CLOSE_BUTTON_SIZE, closeY + CLOSE_BUTTON_SIZE, closeBg);

        Component closeText = Component.literal("✕");
        int closeTextX = closeX + (CLOSE_BUTTON_SIZE - textRenderer.width(closeText)) / 2;
        int closeTextY = closeY + (CLOSE_BUTTON_SIZE - 8) / 2;
        context.drawString(textRenderer, closeText, closeTextX, closeTextY, textColor, false);

        // Hover indicator
        if (isHovered && !isCloseHovered) {
            Component clickText = Component.literal("Click to view").withStyle(ChatFormatting.AQUA);
            int clickX = x + NOTIFICATION_WIDTH - textRenderer.width(clickText) - NOTIFICATION_PADDING;
            context.drawString(textRenderer, clickText, clickX, y + NOTIFICATION_HEIGHT - 20, applyAlpha(0xFF00FFFF, alpha), false);
        }
    }

    private int applyAlpha(int color, float alpha) {
        int a = (int) (alpha * 255);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false; // Only left click

        NotificationManager manager = NotificationManager.getInstance();

        // Check if close button was clicked
        if (hoveredCloseButton != null) {
            manager.removeNotification(hoveredCloseButton);
            return true;
        }

        // Check if notification was clicked
        if (hoveredNotification != null) {
            manager.openNotification(hoveredNotification);
            return true;
        }

        return false;
    }
}