package com.chiraitori.ipblock.service;

import com.chiraitori.ipblock.IPBlockPlugin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoggingService {

    private final IPBlockPlugin plugin;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    private final ExecutorService executor;
    private final boolean enabled;

    public LoggingService(IPBlockPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "blocked.log");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.executor = Executors.newSingleThreadExecutor();
        this.enabled = plugin.getConfig().getBoolean("logging.file-enabled", true);
        
        if (enabled) {
            plugin.getLogger().info("File logging enabled: " + logFile.getAbsolutePath());
        }
    }

    /**
     * Log a blocked connection
     */
    public void logBlocked(String ip, String reason, String country) {
        String timestamp = dateFormat.format(new Date());
        String logLine = String.format("[%s] BLOCKED | IP: %s | Reason: %s | Country: %s",
                timestamp, ip, reason, country != null ? country : "Unknown");
        
        if (enabled) {
            writeAsync(logLine);
        }
        
        // Console log
        if (plugin.isLogBlocked()) {
            plugin.getLogger().info("Blocked: " + ip + " - " + reason);
        }
        
        // Webhook notification
        if (plugin.getWebhookService() != null) {
            plugin.getWebhookService().notifyBlock(ip, reason, country);
        }
    }

    /**
     * Log DDoS detection
     */
    public void logDDoS(int connectionsPerSecond) {
        String timestamp = dateFormat.format(new Date());
        String logLine = String.format("[%s] DDOS_ALERT | Connections/sec: %d",
                timestamp, connectionsPerSecond);
        
        if (enabled) {
            writeAsync(logLine);
        }
        plugin.getLogger().warning("⚠️ DDoS DETECTED! " + connectionsPerSecond + " connections/sec");
        
        // Webhook notification
        if (plugin.getWebhookService() != null) {
            plugin.getWebhookService().notifyDDoS(connectionsPerSecond);
        }
    }

    /**
     * Log temp block
     */
    public void logTempBlock(String ip, int durationMinutes) {
        if (!enabled) return;
        
        String timestamp = dateFormat.format(new Date());
        String logLine = String.format("[%s] TEMP_BLOCK | IP: %s | Duration: %d minutes",
                timestamp, ip, durationMinutes);
        
        writeAsync(logLine);
    }

    /**
     * Log permanent block
     */
    public void logPermBlock(String ip) {
        String timestamp = dateFormat.format(new Date());
        String logLine = String.format("[%s] PERM_BLOCK | IP: %s",
                timestamp, ip);
        
        if (enabled) {
            writeAsync(logLine);
        }
        
        // Webhook notification
        if (plugin.getWebhookService() != null) {
            plugin.getWebhookService().notifyPermBlock(ip);
        }
    }

    /**
     * Log allowed connection (optional, for debugging)
     */
    public void logAllowed(String ip, String country) {
        if (!enabled || !plugin.getConfig().getBoolean("logging.log-allowed", false)) return;
        
        String timestamp = dateFormat.format(new Date());
        String logLine = String.format("[%s] ALLOWED | IP: %s | Country: %s",
                timestamp, ip, country != null ? country : "Unknown");
        
        writeAsync(logLine);
    }

    private void writeAsync(String line) {
        executor.submit(() -> {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(line);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write to log file: " + e.getMessage());
            }
        });
    }

    public void close() {
        executor.shutdown();
    }
}
