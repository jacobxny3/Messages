package com.messaging.client.gui;

import com.messaging.network.MessageManager;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ContactListScreen extends Screen {
    private static final int CONTACT_HEIGHT = 60;
    private static final int PADDING = 10;
    private int scrollOffset = 0;
    private List<ContactEntry> contacts = new ArrayList<>();

    com.messaging.config.ConfigManager config = com.messaging.config.ConfigManager.getInstance();
    com.messaging.config.ConfigManager.Theme theme = config.getTheme();

    public ContactListScreen() {
        super(Component.literal("Messages"));
    }

    @Override
    protected void init() {
        MessageManager.getInstance().requestPlayerList();
        loadContacts();
        super.init();

        // Close button
        this.addRenderableWidget(Button.builder(
                Component.literal("✕"),
                button -> this.onClose()
        ).bounds(this.width - 30, 5, 20, 20).build());

        // Refresh button
        this.addRenderableWidget(Button.builder(
                Component.literal("↻"),
                button -> {
                    loadContacts();
                }
        ).bounds(this.width - 55, 5, 20, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("⚙"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new com.messaging.client.gui.SettingsScreen(this));
                    }
                }
        ).bounds(this.width - 80, 5, 20, 20).build());

    }

    private void loadContacts() {
        contacts.clear();
        MessageManager manager = MessageManager.getInstance();

        // Get current player name to exclude self
        String selfName = null;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            selfName = client.player.getName().getString().toLowerCase();
        }

        // Get all unique contacts (people we've messaged or who are online)
        Set<String> allContactNames = new HashSet<>();

        // Add people we have message history with
        Set<String> historyContacts = manager.getAllContacts();

        for (String contact : historyContacts) {
            if (selfName == null || !contact.equalsIgnoreCase(selfName)) {
                allContactNames.add(contact);
            }
        }

        // Add online players (excluding self)
        Set<String> onlinePlayers = manager.getOnlinePlayers();
        for (String player : onlinePlayers) {
            if (selfName == null || !player.equalsIgnoreCase(selfName)) {
                allContactNames.add(player);
            }
        }


        for (String contact : allContactNames) {
            List<MessageManager.ChatMessage> messages = manager.getMessages(contact);
            boolean isOnline = manager.isPlayerOnline(contact);

            String lastMessage = "";
            long lastTimestamp = 0;
            int unreadCount = 0; // TODO: Implement unread tracking

            if (!messages.isEmpty()) {
                MessageManager.ChatMessage last = messages.get(messages.size() - 1);
                lastMessage = last.content;
                lastTimestamp = last.timestamp;
            }

            contacts.add(new ContactEntry(contact, lastMessage, lastTimestamp, isOnline, unreadCount));
        }

        // Sort by most recent message first, then by online status
        contacts.sort((a, b) -> {
            // If one has messages and the other doesn't, prioritize the one with messages
            if (a.lastTimestamp > 0 && b.lastTimestamp == 0) return -1;
            if (b.lastTimestamp > 0 && a.lastTimestamp == 0) return 1;

            // If both have messages, sort by timestamp
            if (a.lastTimestamp > 0 && b.lastTimestamp > 0) {
                return Long.compare(b.lastTimestamp, a.lastTimestamp);
            }

            // If neither has messages, sort by online status then name
            if (a.isOnline != b.isOnline) {
                return a.isOnline ? -1 : 1;
            }
            return a.name.compareToIgnoreCase(b.name);
        });
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        MessageManager.getInstance().requestPlayerList();
        loadContacts();

        // Reload contacts each frame to ensure online status is current
        super.render(context, mouseX, mouseY, delta);

        // Background
        context.fill(0, 0, this.width, this.height, theme.primaryBg);

        // Header
        this.renderHeader(context);

        // Contacts list
        this.renderContacts(context, mouseX, mouseY);
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
        }
        else if (name == "Dark") {
            textColor = lightTextColor;
        }
        context.drawString(this.font, "↻",width - 55,  5, textColor, false);
        context.drawString(this.font, "✕",width - 30,  5, textColor, false);
        context.drawString(this.font, "⚙",width - 80,  5, textColor, false);

        // Render widgets
    }

    private void renderHeader(GuiGraphics context) {
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        ChatFormatting formattingColor = ChatFormatting.WHITE;
        ChatFormatting formattingBold = ChatFormatting.BOLD;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = ChatFormatting.BLACK;
            formattingBold = null;

        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = ChatFormatting.WHITE;
            formattingBold = ChatFormatting.BOLD;
        }
        int headerHeight = 50;
        context.fill(0, 0, this.width, headerHeight, theme.secondaryBg);

        // Title
        Component title = Component.literal("Messages").withStyle(formattingColor);
        int titleX = PADDING;
        context.drawString(this.font, title, titleX, 15, textColor, false);

        // Subtitle
        Component subtitle = Component.literal(contacts.size() + " conversation" + (contacts.size() != 1 ? "s" : ""))
                .withStyle(ChatFormatting.GRAY);
        context.drawString(this.font, subtitle, titleX, 30, textColor, false);
    }

    private void renderContacts(GuiGraphics context, int mouseX, int mouseY) {
        int headerHeight = 50;
        int startY = headerHeight + PADDING;
        int endY = this.height;

        context.enableScissor(0, startY, this.width, endY);

        int currentY = startY - scrollOffset;

        for (ContactEntry contact : contacts) {
            if (currentY + CONTACT_HEIGHT >= startY && currentY <= endY) {
                boolean hovered = mouseX >= PADDING && mouseX <= this.width - PADDING &&
                        mouseY >= currentY && mouseY <= currentY + CONTACT_HEIGHT;

                this.renderContact(context, contact, currentY, hovered);
            }
            currentY += CONTACT_HEIGHT + 5;
        }

        context.disableScissor();

        // No contacts message
        if (contacts.isEmpty()) {
            Component noContacts = Component.literal("No messages yet").withStyle(ChatFormatting.GRAY);
            int x = (this.width - this.font.width(noContacts)) / 2;
            int y = (this.height - 50) / 2;
            context.drawString(this.font, noContacts, x, y, 0xFF888888, false);

            Component hint = Component.literal("Start a conversation by messaging an online player").withStyle(ChatFormatting.DARK_GRAY);
            int hintX = (this.width - this.font.width(hint)) / 2;
            context.drawString(this.font, hint, hintX, y + 15, 0xFF555555, false);
        }
    }

    private void renderContact(GuiGraphics context, ContactEntry contact, int y, boolean hovered) {
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        ChatFormatting formattingColor = ChatFormatting.WHITE;
        ChatFormatting formattingBold = ChatFormatting.BOLD;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = ChatFormatting.BLACK;
            formattingBold = null;
            Component title = Component.literal("Messages").withStyle(formattingColor);

        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = ChatFormatting.WHITE;
            formattingBold = ChatFormatting.BOLD;
        }
        int bgColor = hovered ? theme.primaryBg : theme.secondaryBg;
        context.fill(PADDING, y, this.width - PADDING, y + CONTACT_HEIGHT, bgColor);

        // Contact name
        Component nameText = Component.literal(contact.name).withStyle(formattingColor);
        context.drawString(this.font, nameText, PADDING + 10, y + 10, textColor, false);

        // Online status
        if (contact.isOnline) {
            context.fill(this.width - PADDING - 20, y + 12, this.width - PADDING - 14, y + 18, 0xFF00FF00);
            Component onlineText = Component.literal("Online").withStyle(ChatFormatting.GREEN);
            context.drawString(this.font, onlineText, this.width - PADDING - 60, y + 10, 0xFF00FF00, false);
        } else {
            Component offlineText = Component.literal("Offline").withStyle(ChatFormatting.GRAY);
            context.drawString(this.font, offlineText, this.width - PADDING - 60, y + 10, 0xFF888888, false);
        }

        // Last message preview
        if (!contact.lastMessage.isEmpty()) {
            String preview = contact.lastMessage;
            if (preview.length() > 50) {
                preview = preview.substring(0, 47) + "...";
            }
            Component previewText = Component.literal(preview).withStyle(ChatFormatting.GRAY);
            context.drawString(this.font, previewText, PADDING + 10, y + 28, 0xFF888888, false);
        }

        // Timestamp
        if (contact.lastTimestamp > 0) {
            String timeStr = formatTimestamp(contact.lastTimestamp);
            Component timeText = Component.literal(timeStr).withStyle(ChatFormatting.DARK_GRAY);
            int timeX = this.width - PADDING - 10 - this.font.width(timeText);
            context.drawString(this.font, timeText, timeX, y + 45, 0xFF555555, false);
        }

        // Unread badge
        if (contact.unreadCount > 0) {
            int badgeSize = 20;
            int badgeX = this.width - PADDING - 30;
            int badgeY = y + 40;

            context.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, 0xFF007AFF);

            String count = String.valueOf(contact.unreadCount);
            Component countText = Component.literal(count);
            int countX = badgeX + (badgeSize - this.font.width(countText)) / 2;
            int countY = badgeY + (badgeSize - 8) / 2;
            context.drawString(this.font, countText, countX, countY, 0xFFFFFFFF, false);
        }

        // Divider line
        context.fill(PADDING + 10, y + CONTACT_HEIGHT + 4, this.width - PADDING - 10, y + CONTACT_HEIGHT + 5, 0xFF333333);
    }

    private String formatTimestamp(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "Just now";
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        int headerHeight = 50;
        int startY = headerHeight + PADDING;
        int currentY = startY - scrollOffset;

        for (ContactEntry contact : contacts) {
            if (click.x() >= PADDING && click.x() <= this.width - PADDING &&
                    click.y() >= currentY && click.y() <= currentY + CONTACT_HEIGHT) {

                // Open message screen with this contact
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new MessageScreen(contact.name));
                }
                return true;
            }
            currentY += CONTACT_HEIGHT + 5;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, contacts.size() * (CONTACT_HEIGHT + 5) - (this.height - 60));
        scrollOffset -= (int) (verticalAmount * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class ContactEntry {
        String name;
        String lastMessage;
        long lastTimestamp;
        boolean isOnline;
        int unreadCount;

        ContactEntry(String name, String lastMessage, long lastTimestamp, boolean isOnline, int unreadCount) {
            this.name = name;
            this.lastMessage = lastMessage;
            this.lastTimestamp = lastTimestamp;
            this.isOnline = isOnline;
            this.unreadCount = unreadCount;
        }
    }
}