package com.messaging.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

public class ConfigManager {
    private static ConfigManager instance;
    private Config config;
    private Path configPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {
        loadConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadConfig() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.gameDirectory != null) {
                configPath = client.gameDirectory.toPath().resolve("config").resolve("messaging-mod.json");
                Files.createDirectories(configPath.getParent());

                if (Files.exists(configPath)) {
                    try (FileReader reader = new FileReader(configPath.toFile())) {
                        config = gson.fromJson(reader, Config.class);
                    }
                } else {
                    config = new Config();
                    saveConfig();
                }
            } else {
                config = new Config();
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            config = new Config();
        }
    }

    public void saveConfig() {
        if (configPath == null) return;

        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            gson.toJson(config, writer);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public Config getConfig() {
        return config;
    }

    public boolean allowServerLogging() {
        return config.allowServerLogging;
    }

    public void setAllowServerLogging(boolean allow) {
        config.allowServerLogging = allow;
        saveConfig();
    }

    public boolean playNotificationSound() {
        return config.playNotificationSound;
    }

    public void setPlayNotificationSound(boolean play) {
        config.playNotificationSound = play;
        saveConfig();
    }

    public boolean showNotifications() {
        return config.showNotifications;
    }

    public void setShowNotifications(boolean show) {
        config.showNotifications = show;
        saveConfig();
    }

    public Theme getTheme() {
        return config.theme;
    }

    public void setTheme(Theme theme) {
        config.theme = theme;
        saveConfig();
    }

    public int getSentBubbleColor() {
        return config.sentBubbleColor;
    }

    public void setSentBubbleColor(int color) {
        config.sentBubbleColor = color;
        saveConfig();
    }

    public int getReceivedBubbleColor() {
        return config.receivedBubbleColor;
    }

    public void setReceivedBubbleColor(int color) {
        config.receivedBubbleColor = color;
        saveConfig();
    }

    public enum Theme {
        DARK("Dark", 0xFF1C1C1E, 0xFF2C2C2E, 0xFF000000),
        LIGHT("Light", 0xFFFFFFFF, 0xFFF5F5F5, 0xFFEEEEEE),
        MIDNIGHT("Midnight", 0xFF0A0A0F, 0xFF1A1A2E, 0xFF000000),
        OCEAN("Ocean", 0xFF0F2027, 0xFF203A43, 0xFF2C5364);

        public final String name;
        public final int primaryBg;
        public final int secondaryBg;
        public final int tertiaryBg;

        Theme(String name, int primaryBg, int secondaryBg, int tertiaryBg) {
            this.name = name;
            this.primaryBg = primaryBg;
            this.secondaryBg = secondaryBg;
            this.tertiaryBg = tertiaryBg;
        }
    }

    public static class Config {
        public boolean allowServerLogging = true;
        public boolean playNotificationSound = true;
        public boolean showNotifications = true;

        // Theme settings
        public Theme theme = Theme.DARK;
        public int sentBubbleColor = 0xFF007AFF; // iMessage blue
        public int receivedBubbleColor = 0xFF3A3A3C; // Dark gray

        // Future config options can be added here
        public int notificationDuration = 5000; // milliseconds
        public int maxNotifications = 3;
    }
}