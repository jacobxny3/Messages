package com.messaging.client.gui;

import com.messaging.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SettingsScreen extends Screen {
    private final Screen parent;
    private ConfigManager configManager;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Messaging Settings"));
        this.parent = parent;
        this.configManager = ConfigManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 60;
        int buttonWidth = 300;
        int buttonHeight = 20;
        int spacing = 40;

        // Server Logging Toggle
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(
                        Text.literal("ON").formatted(Formatting.GREEN),
                        Text.literal("OFF").formatted(Formatting.RED)
                ).initially(configManager.allowServerLogging())
                .build(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight,
                        Text.literal("Allow Server Logging"),
                        (button, value) -> configManager.setAllowServerLogging(value)
                ));

        // Notification Sound Toggle
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(
                        Text.literal("ON").formatted(Formatting.GREEN),
                        Text.literal("OFF").formatted(Formatting.RED)
                ).initially(configManager.playNotificationSound())
                .build(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight,
                        Text.literal("Notification Sound"),
                        (button, value) -> configManager.setPlayNotificationSound(value)
                ));

        // Show Notifications Toggle
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(
                        Text.literal("ON").formatted(Formatting.GREEN),
                        Text.literal("OFF").formatted(Formatting.RED)
                ).initially(configManager.showNotifications())
                .build(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight,
                        Text.literal("Show Notifications"),
                        (button, value) -> configManager.setShowNotifications(value)
                ));
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Theme & Colors"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new ThemeCustomizationScreen(this));
                    }
                }
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 3 + 10, buttonWidth, buttonHeight).build());

        // Done button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> this.close()
        ).dimensions(centerX - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        super.render(context, mouseX, mouseY, delta);
        // Title
        Text title = Text.literal("Messaging Settings").formatted(Formatting.WHITE, Formatting.BOLD);
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, 20, 0xFFFFFFFF);

        // Descriptions
        int descY = 85;
        int spacing = 30;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("When OFF, server won't log your messages to console").formatted(Formatting.GRAY),
                this.width / 2, descY - 35, 0xFF888888);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Play a sound when receiving messages").formatted(Formatting.GRAY),
                this.width / 2, descY - 25 + spacing, 0xFF888888);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Show notification pop-ups for new messages").formatted(Formatting.GRAY),
                this.width / 2, descY - 17 + spacing * 2, 0xFF888888);


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
}