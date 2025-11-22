package com.messaging.client.gui;

import com.messaging.network.MessageManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

public class ContactListScreen extends Screen {
    private static final int CONTACT_HEIGHT = 60;
    private static final int PADDING = 10;
    private int scrollOffset = 0;
    private List<ContactEntry> contacts = new ArrayList<>();

    com.messaging.config.ConfigManager config = com.messaging.config.ConfigManager.getInstance();
    com.messaging.config.ConfigManager.Theme theme = config.getTheme();

    public ContactListScreen() {
        super(Text.literal("Messages"));
    }

    @Override
    protected void init() {
        MessageManager.getInstance().requestPlayerList();
        loadContacts();
        super.init();

        // Close button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("✕"),
                button -> this.close()
        ).dimensions(this.width - 30, 5, 20, 20).build());

        // Refresh button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("↻"),
                button -> {
                    loadContacts();
                }
        ).dimensions(this.width - 55, 5, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("⚙"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new com.messaging.client.gui.SettingsScreen(this));
                    }
                }
        ).dimensions(this.width - 80, 5, 20, 20).build());

    }

    private void loadContacts() {
        contacts.clear();
        MessageManager manager = MessageManager.getInstance();

        // Get current player name to exclude self
        String selfName = null;
        MinecraftClient client = MinecraftClient.getInstance();
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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
        context.drawText(this.textRenderer, "↻",width - 55,  5, textColor, false);
        context.drawText(this.textRenderer, "✕",width - 30,  5, textColor, false);
        context.drawText(this.textRenderer, "⚙",width - 80,  5, textColor, false);

        // Render widgets
    }

    private void renderHeader(DrawContext context) {
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        Formatting formattingColor = Formatting.WHITE;
        Formatting formattingBold = Formatting.BOLD;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = Formatting.BLACK;
            formattingBold = null;

        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = Formatting.WHITE;
            formattingBold = Formatting.BOLD;
        }
        int headerHeight = 50;
        context.fill(0, 0, this.width, headerHeight, theme.secondaryBg);

        // Title
        Text title = Text.literal("Messages").formatted(formattingColor);
        int titleX = PADDING;
        context.drawText(this.textRenderer, title, titleX, 15, textColor, false);

        // Subtitle
        Text subtitle = Text.literal(contacts.size() + " conversation" + (contacts.size() != 1 ? "s" : ""))
                .formatted(Formatting.GRAY);
        context.drawText(this.textRenderer, subtitle, titleX, 30, textColor, false);
    }

    private void renderContacts(DrawContext context, int mouseX, int mouseY) {
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
            Text noContacts = Text.literal("No messages yet").formatted(Formatting.GRAY);
            int x = (this.width - this.textRenderer.getWidth(noContacts)) / 2;
            int y = (this.height - 50) / 2;
            context.drawText(this.textRenderer, noContacts, x, y, 0xFF888888, false);

            Text hint = Text.literal("Start a conversation by messaging an online player").formatted(Formatting.DARK_GRAY);
            int hintX = (this.width - this.textRenderer.getWidth(hint)) / 2;
            context.drawText(this.textRenderer, hint, hintX, y + 15, 0xFF555555, false);
        }
    }

    private void renderContact(DrawContext context, ContactEntry contact, int y, boolean hovered) {
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        Formatting formattingColor = Formatting.WHITE;
        Formatting formattingBold = Formatting.BOLD;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = Formatting.BLACK;
            formattingBold = null;
            Text title = Text.literal("Messages").formatted(formattingColor);

        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = Formatting.WHITE;
            formattingBold = Formatting.BOLD;
        }
        int bgColor = hovered ? theme.primaryBg : theme.secondaryBg;
        context.fill(PADDING, y, this.width - PADDING, y + CONTACT_HEIGHT, bgColor);

        // Contact name
        Text nameText = Text.literal(contact.name).formatted(formattingColor);
        context.drawText(this.textRenderer, nameText, PADDING + 10, y + 10, textColor, false);

        // Online status
        if (contact.isOnline) {
            context.fill(this.width - PADDING - 20, y + 12, this.width - PADDING - 14, y + 18, 0xFF00FF00);
            Text onlineText = Text.literal("Online").formatted(Formatting.GREEN);
            context.drawText(this.textRenderer, onlineText, this.width - PADDING - 60, y + 10, 0xFF00FF00, false);
        } else {
            Text offlineText = Text.literal("Offline").formatted(Formatting.GRAY);
            context.drawText(this.textRenderer, offlineText, this.width - PADDING - 60, y + 10, 0xFF888888, false);
        }

        // Last message preview
        if (!contact.lastMessage.isEmpty()) {
            String preview = contact.lastMessage;
            if (preview.length() > 50) {
                preview = preview.substring(0, 47) + "...";
            }
            Text previewText = Text.literal(preview).formatted(Formatting.GRAY);
            context.drawText(this.textRenderer, previewText, PADDING + 10, y + 28, 0xFF888888, false);
        }

        // Timestamp
        if (contact.lastTimestamp > 0) {
            String timeStr = formatTimestamp(contact.lastTimestamp);
            Text timeText = Text.literal(timeStr).formatted(Formatting.DARK_GRAY);
            int timeX = this.width - PADDING - 10 - this.textRenderer.getWidth(timeText);
            context.drawText(this.textRenderer, timeText, timeX, y + 45, 0xFF555555, false);
        }

        // Unread badge
        if (contact.unreadCount > 0) {
            int badgeSize = 20;
            int badgeX = this.width - PADDING - 30;
            int badgeY = y + 40;

            context.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, 0xFF007AFF);

            String count = String.valueOf(contact.unreadCount);
            Text countText = Text.literal(count);
            int countX = badgeX + (badgeSize - this.textRenderer.getWidth(countText)) / 2;
            int countY = badgeY + (badgeSize - 8) / 2;
            context.drawText(this.textRenderer, countText, countX, countY, 0xFFFFFFFF, false);
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
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int headerHeight = 50;
        int startY = headerHeight + PADDING;
        int currentY = startY - scrollOffset;

        for (ContactEntry contact : contacts) {
            if (click.x() >= PADDING && click.x() <= this.width - PADDING &&
                    click.y() >= currentY && click.y() <= currentY + CONTACT_HEIGHT) {

                // Open message screen with this contact
                if (this.client != null) {
                    this.client.setScreen(new MessageScreen(contact.name));
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
    public boolean shouldPause() {
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