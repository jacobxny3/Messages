package com.messaging.client.gui;

import com.messaging.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
    private final Screen parent;
    private ConfigManager configManager;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Messaging Settings"));
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
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.literal("ON").withStyle(ChatFormatting.GREEN),
                        Component.literal("OFF").withStyle(ChatFormatting.RED)
                ).withInitialValue(configManager.allowServerLogging())
                .create(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight,
                        Component.literal("Allow Server Logging"),
                        (button, value) -> configManager.setAllowServerLogging(value)
                ));

        // Notification Sound Toggle
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.literal("ON").withStyle(ChatFormatting.GREEN),
                        Component.literal("OFF").withStyle(ChatFormatting.RED)
                ).withInitialValue(configManager.playNotificationSound())
                .create(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight,
                        Component.literal("Notification Sound"),
                        (button, value) -> configManager.setPlayNotificationSound(value)
                ));

        // Show Notifications Toggle
        this.addRenderableWidget(CycleButton.booleanBuilder(
                        Component.literal("ON").withStyle(ChatFormatting.GREEN),
                        Component.literal("OFF").withStyle(ChatFormatting.RED)
                ).withInitialValue(configManager.showNotifications())
                .create(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight,
                        Component.literal("Show Notifications"),
                        (button, value) -> configManager.setShowNotifications(value)
                ));
        this.addRenderableWidget(Button.builder(
                Component.literal("Theme & Colors"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ThemeCustomizationScreen(this));
                    }
                }
        ).bounds(centerX - buttonWidth / 2, startY + spacing * 3 + 10, buttonWidth, buttonHeight).build());

        // Done button
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> this.onClose()
        ).bounds(centerX - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Background
        super.render(context, mouseX, mouseY, delta);
        // Title
        Component title = Component.literal("Messaging Settings").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
        context.drawCenteredString(this.font, title, this.width / 2, 20, 0xFFFFFFFF);

        // Descriptions
        int descY = 85;
        int spacing = 30;

        context.drawCenteredString(this.font,
                Component.literal("When OFF, server won't log your messages to console").withStyle(ChatFormatting.GRAY),
                this.width / 2, descY - 35, 0xFF888888);

        context.drawCenteredString(this.font,
                Component.literal("Play a sound when receiving messages").withStyle(ChatFormatting.GRAY),
                this.width / 2, descY - 25 + spacing, 0xFF888888);

        context.drawCenteredString(this.font,
                Component.literal("Show notification pop-ups for new messages").withStyle(ChatFormatting.GRAY),
                this.width / 2, descY - 17 + spacing * 2, 0xFF888888);


    }


    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}