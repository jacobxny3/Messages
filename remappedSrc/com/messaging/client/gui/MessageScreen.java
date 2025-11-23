package com.messaging.client.gui;

import com.messaging.config.ConfigManager;
import com.messaging.network.MessageManager;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;


public class MessageScreen extends Screen {
    private EditBox messageInput;
    private List<MessageManager.ChatMessage> messages = new ArrayList<>();
    private String recipientName;
    private int scrollOffset = 0;
    private static final int PADDING = 10;
    private static final int MESSAGE_SPACING = 8;
    private static final int BUBBLE_PADDING = 8;
    private MessageManager messageManager;
    private int lastMessageCount = 0;
    private int totalContentHeight = 0;
    ConfigManager config = ConfigManager.getInstance();
    ConfigManager.Theme theme = config.getTheme();

    public MessageScreen(String recipientName) {
        super(Component.literal("Messages"));
        this.recipientName = recipientName;
        this.messageManager = MessageManager.getInstance();

        // Load message history
        this.messages = messageManager.getMessages(recipientName);
        this.lastMessageCount = messages.size();

        // Request history from server
        messageManager.requestHistory(recipientName);
    }

    public String getRecipientName() {
        return recipientName;
    }

    @Override
    protected void init() {
        super.init();

        // Message input field at the bottom
        int inputHeight = 30;
        int inputY = this.height - inputHeight - PADDING;

        this.messageInput = new EditBox(
                this.font,
                PADDING + 5,
                inputY + 5,
                this.width - 80 - PADDING,
                20,
                Component.literal("Message")
        );
        this.messageInput.setMaxLength(256);
        this.messageInput.setHint(Component.literal("Enter Text...").withStyle(ChatFormatting.GRAY));
        this.addWidget(this.messageInput);
        this.setInitialFocus(this.messageInput);

        // Send button
        this.addRenderableWidget(Button.builder(
                Component.literal("Send"),
                button -> this.sendMessage()
        ).bounds(this.width - 70, inputY + 10, 60, 20).build());

        // Back button (top left)
        this.addRenderableWidget(Button.builder(
                Component.literal("←"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ContactListScreen());
                    }
                }
        ).bounds(5, 5, 20, 20).build());

        // Close button (top right)
        this.addRenderableWidget(Button.builder(
                Component.literal("✕"),
                button -> this.onClose()
        ).bounds(this.width - 30, 5, 20, 20).build());
    }

    private void sendMessage() {
        String message = this.messageInput.getValue().trim();
        if (!message.isEmpty()) {
            messageManager.sendMessage(recipientName, message);
            messages = messageManager.getMessages(recipientName);
            this.messageInput.setValue("");

            // Auto-scroll to bottom when sending
            scrollOffset = 0;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Refresh messages in case we received new ones
        List<MessageManager.ChatMessage> currentMessages = messageManager.getMessages(recipientName);

        // Check if we got new messages and auto-scroll to bottom
        if (currentMessages.size() != lastMessageCount) {
            messages = currentMessages;
            lastMessageCount = messages.size();
            scrollOffset = 0; // Auto-scroll to bottom on new messages
        } else {
            messages = currentMessages;
        }
        // Render widgets (buttons and text field)
        super.render(context, mouseX, mouseY, delta);
        // Background
        context.fill(0, 0, this.width, this.height, theme.primaryBg);

        // Header bar
        this.renderHeader(context);

        // Messages area
        this.renderMessages(context);

        // Input area background
        int inputY = this.height - 40;
        context.fill(PADDING, inputY, this.width - PADDING, this.height - PADDING, theme.secondaryBg);


        // Render text field text on top
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
        this.messageInput.render(context, mouseX, mouseY, delta);
        context.drawString(this.font, "✕",width - 30,  5, textColor, false);
        context.drawString(this.font, "Send", width - 70, inputY + 10, textColor, false);
        context.drawString(this.font, "←", 5, 5, textColor, false);
    }

    private void renderHeader(GuiGraphics context) {
        int headerHeight = 50;
        context.fill(0, 0, this.width, headerHeight, theme.secondaryBg);

        // Recipient name
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        ChatFormatting formattingColor = ChatFormatting.WHITE;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = ChatFormatting.BLACK;
        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = ChatFormatting.WHITE;
        }
        Component nameText = Component.literal(recipientName).withStyle(formattingColor);
        int nameX = (this.width - this.font.width(nameText)) / 2;
        context.drawString(this.font, nameText, nameX, 15, textColor, false);

        // Status (online/offline)
        boolean isOnline = messageManager.isPlayerOnline(recipientName);
        Component statusText = isOnline ?
                Component.literal("● Online").withStyle(ChatFormatting.GREEN) :
                Component.literal("● Offline").withStyle(ChatFormatting.GRAY);
        int statusColor = isOnline ? 0xFF00FF00 : 0xFF888888;
        int statusX = (this.width - this.font.width(statusText)) / 2;
        context.drawString(this.font, statusText, statusX, 30, statusColor, false);
    }

    private void renderMessages(GuiGraphics context) {
        int headerHeight = 50;
        int inputAreaHeight = 50;
        int messagesAreaTop = headerHeight + PADDING;
        int messagesAreaBottom = this.height - inputAreaHeight - PADDING;
        int messagesAreaHeight = messagesAreaBottom - messagesAreaTop;

        // Calculate total content height for scrolling
        totalContentHeight = 0;
        for (MessageManager.ChatMessage msg : messages) {
            List<String> wrappedLines = this.wrapText(msg.content, this.width - 120);
            int bubbleHeight = wrappedLines.size() * 10 + BUBBLE_PADDING * 2;
            totalContentHeight += bubbleHeight + MESSAGE_SPACING;
        }

        // Enable scissor for scrolling
        context.enableScissor(0, messagesAreaTop, this.width, messagesAreaBottom);

        // Start from top if content fits, otherwise from bottom with scroll offset
        int startY;
        if (totalContentHeight <= messagesAreaHeight) {
            // Content fits - render from top
            startY = messagesAreaTop + PADDING;
        } else {
            // Content doesn't fit - render from bottom with scroll
            startY = messagesAreaBottom - totalContentHeight + scrollOffset;
        }

        int currentY = startY;

        // Render messages from top to bottom (oldest to newest)
        for (int i = 0; i < messages.size(); i++) {
            MessageManager.ChatMessage msg = messages.get(i);
            currentY = this.renderMessage(context, msg, currentY);
            currentY += MESSAGE_SPACING;
        }

        context.disableScissor();

        // No messages indicator
        if (messages.isEmpty()) {
            Component noMessages = Component.literal("No messages yet").withStyle(ChatFormatting.GRAY);
            int x = (this.width - this.font.width(noMessages)) / 2;
            int y = (messagesAreaTop + messagesAreaBottom) / 2;
            context.drawString(this.font, noMessages, x, y, 0xFF888888, false);
        }
    }

    private int renderMessage(GuiGraphics context, MessageManager.ChatMessage message, int topY) {
        List<String> wrappedLines = this.wrapText(message.content, this.width - 120);
        int bubbleHeight = wrappedLines.size() * 10 + BUBBLE_PADDING * 2;

        int maxBubbleWidth = this.width - 100;
        int bubbleWidth = Math.min(maxBubbleWidth, this.calculateTextWidth(wrappedLines) + BUBBLE_PADDING * 2);

        int bubbleX;
        int bubbleColor;
        int textColor;

        if (message.isSentByPlayer) {
            // Sent messages (right side, blue)
            bubbleX = this.width - bubbleWidth - PADDING * 2;
            bubbleColor = config.getSentBubbleColor(); // iMessage blue
            textColor = 0xFFFFFFFF;
        } else {
            // Received messages (left side, gray)
            bubbleX = PADDING * 2;
            bubbleColor = config.getReceivedBubbleColor(); // Dark gray
            textColor = 0xFFFFFFFF;
        }

        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColors = lightTextColor;
        String name = theme.name;
        if (name == "Light") {
            textColors = darkTextColor;
        }
        else if (name == "Dark") {
            textColors = lightTextColor;
        }

        // Draw rounded bubble (approximated with multiple fills)
        this.drawRoundedRect(context, bubbleX, topY, bubbleWidth, bubbleHeight, bubbleColor);

        // Draw text
        int textY = topY + BUBBLE_PADDING;
        for (String line : wrappedLines) {
            context.drawString(this.font, line, bubbleX + BUBBLE_PADDING, textY, textColors, false);
            textY += 10;
        }

        return topY + bubbleHeight;
    }

    private void drawRoundedRect(GuiGraphics context, int x, int y, int width, int height, int color) {
        // Simple rounded corners approximation
        int radius = 4;

        // Main body
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);

        // Corners (simple approximation)
        context.fill(x + 1, y + 1, x + radius, y + radius, color);
        context.fill(x + width - radius, y + 1, x + width - 1, y + radius, color);
        context.fill(x + 1, y + height - radius, x + radius, y + height - 1, color);
        context.fill(x + width - radius, y + height - radius, x + width - 1, y + height - 1, color);
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.isEmpty() ? List.of(text) : lines;
    }

    private int calculateTextWidth(List<String> lines) {
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, this.font.width(line));
        }
        return maxWidth;
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.isConfirmation() || keyInput.key() == 335) { // Enter or Numpad Enter
            this.sendMessage();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int headerHeight = 50;
        int inputAreaHeight = 50;
        int messagesAreaHeight = this.height - headerHeight - inputAreaHeight - PADDING * 2;

        // Only allow scrolling if content is taller than the viewable area
        if (totalContentHeight > messagesAreaHeight) {
            // Scroll up = positive verticalAmount = increase scrollOffset (show older messages)
            // Scroll down = negative verticalAmount = decrease scrollOffset (show newer messages)
            scrollOffset += (int) (verticalAmount * 20);

            // Limit scroll range
            int maxScroll = totalContentHeight - messagesAreaHeight;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}