package com.chiraitori.ipblock.service;

import com.chiraitori.ipblock.IPBlockPlugin;
import com.chiraitori.ipblock.util.SchedulerUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class GeoIPDownloadService {

    private static final String DOWNLOAD_URL = 
        "https://github.com/P3TERX/GeoLite.mmdb/releases/latest/download/GeoLite2-Country.mmdb";
    
    private final IPBlockPlugin plugin;
    private final File targetFile;

    public GeoIPDownloadService(IPBlockPlugin plugin, File targetFile) {
        this.plugin = plugin;
        this.targetFile = targetFile;
    }

    /**
     * Download database if not exists or if force update
     * @param force Force re-download even if file exists
     * @return true if download successful or file already exists
     */
    public boolean downloadIfNeeded(boolean force) {
        if (!force && targetFile.exists()) {
            plugin.getLogger().info("GeoLite2 database already exists, skipping download.");
            return true;
        }

        plugin.getLogger().info("Downloading GeoLite2-Country.mmdb from GitHub...");
        
        try {
            return downloadFile();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to download GeoLite2 database: " + e.getMessage());
            return false;
        }
    }

    private boolean downloadFile() throws IOException {
        URL url = new URL(DOWNLOAD_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "IPBlock-Plugin/1.0");
        
        // Follow redirects (GitHub uses redirects)
        connection.setInstanceFollowRedirects(true);
        
        int responseCode = connection.getResponseCode();
        
        // Handle GitHub redirect (302 -> actual file)
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = connection.getHeaderField("Location");
            connection.disconnect();
            connection = (HttpURLConnection) new URL(newUrl).openConnection();
            connection.setRequestProperty("User-Agent", "IPBlock-Plugin/1.0");
            responseCode = connection.getResponseCode();
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            plugin.getLogger().severe("HTTP Error: " + responseCode);
            return false;
        }

        long contentLength = connection.getContentLengthLong();
        plugin.getLogger().info("Downloading... (Size: " + formatSize(contentLength) + ")");

        // Create parent directory if not exists
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        // Download to temp file first, then move
        File tempFile = new File(targetFile.getParentFile(), "GeoLite2-Country.mmdb.tmp");
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;
            int lastPercent = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                
                if (contentLength > 0) {
                    int percent = (int) ((downloaded * 100) / contentLength);
                    if (percent >= lastPercent + 20) {
                        plugin.getLogger().info("Download progress: " + percent + "%");
                        lastPercent = percent;
                    }
                }
            }
        }

        // Move temp file to target
        Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        plugin.getLogger().info("GeoLite2 database downloaded successfully!");
        return true;
    }

    /**
     * Download async on another thread (Folia compatible)
     */
    public void downloadAsync(boolean force, Runnable onComplete) {
        SchedulerUtil.runAsync(plugin, () -> {
            boolean success = downloadIfNeeded(force);
            if (onComplete != null && success) {
                SchedulerUtil.runSync(plugin, onComplete);
            }
        });
    }

    private String formatSize(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
