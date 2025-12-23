package com.chiraitori.ipblock.service;

import com.chiraitori.ipblock.IPBlockPlugin;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebhookService {

    private final IPBlockPlugin plugin;
    private final ExecutorService executor;
    private final SimpleDateFormat dateFormat;
    
    // Config
    private final boolean discordEnabled;
    private final String discordWebhookUrl;
    private final boolean telegramEnabled;
    private final String telegramBotToken;
    private final String telegramChatId;
    
    // Notification settings
    private final boolean notifyOnBlock;
    private final boolean notifyOnDDoS;
    private final boolean notifyOnPermBlock;

    public WebhookService(IPBlockPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Load Discord config
        this.discordEnabled = plugin.getConfig().getBoolean("webhook.discord.enabled", false);
        this.discordWebhookUrl = plugin.getConfig().getString("webhook.discord.url", "");
        
        // Load Telegram config
        this.telegramEnabled = plugin.getConfig().getBoolean("webhook.telegram.enabled", false);
        this.telegramBotToken = plugin.getConfig().getString("webhook.telegram.bot-token", "");
        this.telegramChatId = plugin.getConfig().getString("webhook.telegram.chat-id", "");
        
        // Notification settings
        this.notifyOnBlock = plugin.getConfig().getBoolean("webhook.notify-on-block", true);
        this.notifyOnDDoS = plugin.getConfig().getBoolean("webhook.notify-on-ddos", true);
        this.notifyOnPermBlock = plugin.getConfig().getBoolean("webhook.notify-on-perm-block", true);
        
        if (discordEnabled) {
            plugin.getLogger().info("Discord webhook enabled!");
        }
        if (telegramEnabled) {
            plugin.getLogger().info("Telegram webhook enabled!");
        }
    }

    /**
     * Send notification when IP is blocked
     */
    public void notifyBlock(String ip, String reason, String country) {
        if (!notifyOnBlock) return;
        
        String title = "🚫 IP Blocked";
        String message = String.format(
            "**IP:** `%s`\n**Reason:** %s\n**Country:** %s\n**Time:** %s",
            ip, reason, country != null ? country : "Unknown", dateFormat.format(new Date())
        );
        
        sendNotification(title, message, 0xFF6B6B); // Red color
    }

    /**
     * Send notification when DDoS is detected
     */
    public void notifyDDoS(int connectionsPerSecond) {
        if (!notifyOnDDoS) return;
        
        String title = "⚠️ DDoS DETECTED!";
        String message = String.format(
            "**Connections/sec:** %d\n**Time:** %s\n\n**Action:** Server is under attack!",
            connectionsPerSecond, dateFormat.format(new Date())
        );
        
        sendNotification(title, message, 0xFFD93D); // Yellow color
    }

    /**
     * Send notification when IP is permanently blocked
     */
    public void notifyPermBlock(String ip) {
        if (!notifyOnPermBlock) return;
        
        String title = "🔒 IP Permanently Blocked";
        String message = String.format(
            "**IP:** `%s`\n**Time:** %s\n\nThis IP has been added to permanent blacklist.",
            ip, dateFormat.format(new Date())
        );
        
        sendNotification(title, message, 0x6BCB77); // Green color
    }

    private void sendNotification(String title, String message, int color) {
        if (discordEnabled) {
            sendDiscord(title, message, color);
        }
        if (telegramEnabled) {
            sendTelegram(title, message);
        }
    }

    private void sendDiscord(String title, String message, int color) {
        if (discordWebhookUrl.isEmpty()) return;
        
        executor.submit(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                embed.addProperty("description", message.replace("**", "").replace("`", ""));
                embed.addProperty("color", color);
                
                JsonObject payload = new JsonObject();
                payload.addProperty("username", "IPBlock");
                payload.add("embeds", new com.google.gson.JsonArray());
                payload.getAsJsonArray("embeds").add(embed);
                
                sendHttpPost(discordWebhookUrl, payload.toString());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private void sendTelegram(String title, String message) {
        if (telegramBotToken.isEmpty() || telegramChatId.isEmpty()) return;
        
        executor.submit(() -> {
            try {
                String telegramUrl = String.format(
                    "https://api.telegram.org/bot%s/sendMessage",
                    telegramBotToken
                );
                
                // Convert markdown for Telegram
                String text = title + "\n\n" + message
                    .replace("**", "*")
                    .replace("`", "`");
                
                JsonObject payload = new JsonObject();
                payload.addProperty("chat_id", telegramChatId);
                payload.addProperty("text", text);
                payload.addProperty("parse_mode", "Markdown");
                
                sendHttpPost(telegramUrl, payload.toString());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Telegram message: " + e.getMessage());
            }
        });
    }

    private void sendHttpPost(String urlString, String jsonPayload) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            plugin.getLogger().warning("Webhook returned: " + responseCode);
        }
        
        conn.disconnect();
    }

    public void close() {
        executor.shutdown();
    }
    
    public boolean isDiscordEnabled() { return discordEnabled; }
    public boolean isTelegramEnabled() { return telegramEnabled; }
}
