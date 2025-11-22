package com.messaging.client.gui;

import com.messaging.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ThemeCustomizationScreen extends Screen {
    private final Screen parent;
    private ConfigManager configManager;

    private static final int[][] PRESET_COLORS = {
            {0xFF007AFF, 0xFF3A3A3C}, // iMessage (Blue/Gray)
            {0xFF34B7F1, 0xFF2C2C2E}, // Twitter Blue
            {0xFF25D366, 0xFF1F1F1F}, // WhatsApp Green
            {0xFF7289DA, 0xFF2C2F33}, // Discord Blurple
            {0xFFE01E5A, 0xFF1A1D21}, // Slack Magenta
            {0xFFFF6B6B, 0xFF4A4A4A}, // Red
            {0xFFFFD93D, 0xFF3A3A3A}, // Yellow
            {0xFF6BCF7F, 0xFF2F2F2F}, // Green
            {0xFFAD5EFF, 0xFF2B2B2B}, // Purple
            {0xFFFF6B9D, 0xFF353535}, // Pink
    };

    private static final String[] PRESET_NAMES = {
            "iMessage", "Twitter", "WhatsApp", "Discord", "Slack",
            "Red", "Yellow", "Green", "Purple", "Pink"
    };

    private int previewTab = 0; // 0 = Messages, 1 = Contacts

    public ThemeCustomizationScreen(Screen parent) {
        super(Text.literal("Theme & Colors"));
        this.parent = parent;
        this.configManager = ConfigManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int leftPanelX = 20;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int startY = 40;

        // Theme selection buttons
        int themeY = startY;
        for (ConfigManager.Theme theme : ConfigManager.Theme.values()) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(theme.name),
                    button -> {
                        configManager.setTheme(theme);
                    }
            ).dimensions(leftPanelX, themeY, buttonWidth, buttonHeight).build());
            themeY += 25;
        }

        // Color preset buttons
        int presetStartY = themeY + 20;
        int presetX = leftPanelX;
        int presetY = presetStartY;
        int presetSize = 30;
        int presetSpacing = 35;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int index = i;
            int buttonX = presetX + (i % 5) * presetSpacing;
            int buttonY = presetY + (i / 5) * presetSpacing;

            this.addDrawableChild(new ColorPresetButton(
                    buttonX, buttonY, presetSize, presetSize,
                    PRESET_COLORS[index][0], PRESET_COLORS[index][1],
                    PRESET_NAMES[index],
                    button -> {
                        configManager.setSentBubbleColor(PRESET_COLORS[index][0]);
                        configManager.setReceivedBubbleColor(PRESET_COLORS[index][1]);
                    }
            ));
        }

        // Preview tab buttons
        int previewButtonY = startY;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Messages Preview"),
                button -> previewTab = 0
        ).dimensions(this.width - 230, previewButtonY, 110, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Contacts Preview"),
                button -> previewTab = 1
        ).dimensions(this.width - 115, previewButtonY, 110, buttonHeight).build());

        // Done button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> this.close()
        ).dimensions((this.width / 2) + 140, 10, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ConfigManager.Theme theme = configManager.getTheme();
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        Formatting formattingColor = Formatting.WHITE;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = Formatting.BLACK;
        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = Formatting.WHITE;
        }

        // Background
        context.fill(0, 0, this.width, this.height, theme.primaryBg);

        // Title

        // Left panel background
        context.fill(10, 15, 190, this.height - 20, theme.primaryBg);
        super.render(context, mouseX, mouseY, delta);
        Text title = Text.literal("Theme & Color Customization").formatted(formattingColor);
        context.drawText(this.textRenderer, title, (this.width / 2) - 20, 10, textColor, false);

        // Section labels
        context.drawText(this.textRenderer, Text.literal("Themes"), 20, 25, textColor, false);

        int presetLabelY = 40 + (ConfigManager.Theme.values().length * 25) + 5;
        context.drawText(this.textRenderer, Text.literal("Text Bubble Presets"), 20, presetLabelY, textColor, false);

        // Preview panel
        renderPreview(context, theme);

    }

    private void renderPreview(DrawContext context, ConfigManager.Theme theme) {
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        Formatting formattingColor = Formatting.WHITE;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = Formatting.BLACK;
        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = Formatting.WHITE;
        }
        int previewX = 200;
        int previewY = 70;
        int previewWidth = this.width - previewX - 10;
        int previewHeight = this.height - previewY - 50;

        // Preview background
        context.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight + 35, theme.tertiaryBg);

        if (previewTab == 0) {
            renderMessagesPreview(context, previewX, previewY, previewWidth, previewHeight, theme);
        } else {
            renderContactsPreview(context, previewX, previewY, previewWidth, previewHeight, theme);
        }

        // Preview label
        context.drawText(this.textRenderer,
                Text.literal("Preview").formatted(),
                previewX + 5, previewY - 15, textColor, false);
    }

    private void renderMessagesPreview(DrawContext context, int x, int y, int width, int height, ConfigManager.Theme theme) {
        // Header
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        Formatting formattingColor = Formatting.WHITE;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = Formatting.BLACK;
        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = Formatting.WHITE;
        }
        int headerHeight = 40;
        int contactY = y + headerHeight + 10;
        context.fill(x, y, x + width, y + headerHeight, theme.secondaryBg);
        context.drawText(this.textRenderer,
                Text.literal("Preview User").formatted(Formatting.BOLD),
                x + width / 2, y + 10, textColor, false);
        context.drawText(this.textRenderer,
                Text.literal("● Online").formatted(Formatting.GREEN),
                x + width / 2 - 40, y + 10, 0xFF00FF00, false);

        // Messages area
        int msgY = y + headerHeight + 10;

        // Received message
        int receivedBubbleWidth = 200;
        int receivedBubbleHeight = 30;
        drawRoundedRect(context, x + 10, msgY, receivedBubbleWidth, receivedBubbleHeight,
                configManager.getReceivedBubbleColor());
        context.drawText(this.textRenderer, "Hey! How are you?", x + 18, msgY + 10, 0xFFFFFFFF, false);

        // Sent message
        msgY += 40;
        int sentBubbleWidth = 180;
        int sentBubbleHeight = 30;
        drawRoundedRect(context, x + width - sentBubbleWidth - 10, msgY, sentBubbleWidth, sentBubbleHeight,
                configManager.getSentBubbleColor());
        context.drawText(this.textRenderer, "I'm doing great!", x + width - sentBubbleWidth - 2, msgY + 10, 0xFFFFFFFF, false);

        // Input area
        int inputY = y + height - 35;
        context.fill(x + 5, inputY + 40, x + width - 5, inputY + 67, theme.secondaryBg);
        context.drawText(this.textRenderer,
                Text.literal("Enter Text...").formatted(Formatting.GRAY),
                x + 10, inputY + 50, 0xFF888888, false);
    }

    private void renderContactsPreview(DrawContext context, int x, int y, int width, int height, ConfigManager.Theme theme) {
        // Header
        int darkTextColor = 0xFF000000;
        int lightTextColor = 0xFFFFFFFF;
        int textColor = lightTextColor;
        Formatting formattingColor = Formatting.WHITE;
        String name = theme.name;
        if (name == "Light") {
            textColor = darkTextColor;
            formattingColor = Formatting.BLACK;
        }
        else if (name == "Dark") {
            textColor = lightTextColor;
            formattingColor = Formatting.WHITE;
        }
        int headerHeight = 40;
        context.fill(x, y, x + width, y + headerHeight, theme.primaryBg);
        context.drawText(this.textRenderer,
                Text.literal("Messages"),
                x + 10, y + 10, textColor, false);
        context.drawText(this.textRenderer,
                Text.literal("2 conversations").formatted(Formatting.GRAY),
                x + 10, y + 25, 0xFF888888, false);

        // Contact entries
        int contactY = y + headerHeight + 10;
        int contactHeight = 50;

        // Contact 1
        context.fill(x + 10, contactY, x + width - 10, contactY + contactHeight, theme.primaryBg);
        context.drawText(this.textRenderer,
                Text.literal("Steve").formatted(),
                x + 15, contactY + 8, textColor, false);
        context.fill(x + width - 25, contactY + 10, x + width - 19, contactY + 16, 0xFF00FF00);
        context.drawText(this.textRenderer,
                Text.literal("See you later!").formatted(Formatting.GRAY),
                x + 15, contactY + 23, 0xFF888888, false);
        context.drawText(this.textRenderer,
                Text.literal("Online").formatted(Formatting.GREEN),
                x + 215, contactY + 10, 0xFF00FF00, false);
        context.drawText(this.textRenderer,
                Text.literal("2m ago").formatted(Formatting.DARK_GRAY),
                x + width - 60, contactY + 38, 0xFF555555, false);

        // Contact 2
        contactY += contactHeight + 5;

        context.fill(x + 10, contactY, x + width - 10, contactY + contactHeight, theme.primaryBg);
        context.drawText(this.textRenderer,
                Text.literal("Bob"),
                x + 15, contactY + 8, textColor, false);
        context.drawText(this.textRenderer,
                Text.literal("Offline").formatted(Formatting.GRAY),
                x + width - 60, contactY + 8, 0xFF888888, false);
        context.drawText(this.textRenderer,
                Text.literal("Thanks for the help").formatted(Formatting.GRAY),
                x + 15, contactY + 23, 0xFF888888, false);
        context.drawText(this.textRenderer,
                Text.literal("1h ago").formatted(Formatting.DARK_GRAY),
                x + width - 60, contactY + 38, 0xFF555555, false);
    }

    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int color) {
        int radius = 4;
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);
        context.fill(x + 1, y + 1, x + radius, y + radius, color);
        context.fill(x + width - radius, y + 1, x + width - 1, y + radius, color);
        context.fill(x + 1, y + height - radius, x + radius, y + height - 1, color);
        context.fill(x + width - radius, y + height - radius, x + width - 1, y + height - 1, color);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    // Custom button for color presets
    private class ColorPresetButton extends ButtonWidget {
        private final int sentColor;
        private final int receivedColor;
        private final String presetName;

        public ColorPresetButton(int x, int y, int width, int height, int sentColor, int receivedColor, String presetName, PressAction onPress) {
            super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.sentColor = sentColor;
            this.receivedColor = receivedColor;
            this.presetName = presetName;
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            // Border if hovered
            if (this.isHovered()) {
                context.fill(this.getX() - 2, this.getY() - 2, this.getX() + this.width + 2, this.getY() + this.height + 2, 0xFFFFFFFF);
            }

            // Split button in half
            context.fill(this.getX(), this.getY(), this.getX() + this.width / 2, this.getY() + this.height, sentColor);
            context.fill(this.getX() + this.width / 2, this.getY(), this.getX() + this.width, this.getY() + this.height, receivedColor);

            // Tooltip on hover
            if (this.isHovered() && client != null) {
                context.drawTooltip(client.textRenderer, Text.literal(presetName), mouseX, mouseY);
            }
        }
    }
}