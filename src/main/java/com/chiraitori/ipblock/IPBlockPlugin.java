package com.chiraitori.ipblock;

import com.chiraitori.ipblock.command.IPBlockCommand;
import com.chiraitori.ipblock.listener.PlayerLoginListener;
import com.chiraitori.ipblock.service.AntiDDoSService;
import com.chiraitori.ipblock.service.GeoIPDownloadService;
import com.chiraitori.ipblock.service.GeoIPService;
import com.chiraitori.ipblock.service.LoggingService;
import com.chiraitori.ipblock.service.RateLimitService;
import com.chiraitori.ipblock.service.WebhookService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class IPBlockPlugin extends JavaPlugin {

    private GeoIPService geoIPService;
    private RateLimitService rateLimitService;
    private AntiDDoSService antiDDoSService;
    private GeoIPDownloadService downloadService;
    private LoggingService loggingService;
    private WebhookService webhookService;
    private File geoipFile;
    
    // Messages
    private FileConfiguration messages;
    
    private String mode;
    private Set<String> countries;
    private Set<String> whitelistedIPs;
    private boolean allowLocal;
    private boolean logBlocked;
    private boolean allowUnknownCountry;
    private boolean autoDownload;

    @Override
    public void onEnable() {
        // Save default config and messages
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        // Load configuration and messages
        loadConfiguration();
        loadMessages();
        
        // Initialize GeoIP file path
        geoipFile = new File(getDataFolder(), "GeoLite2-Country.mmdb");
        downloadService = new GeoIPDownloadService(this, geoipFile);
        
        // Auto-download if enabled and file doesn't exist
        if (autoDownload && !geoipFile.exists()) {
            getLogger().info("Auto-downloading GeoLite2 database...");
            downloadService.downloadAsync(false, () -> {
                initGeoIPService();
                getLogger().info("GeoIP database loaded after download!");
            });
        }
        
        // Initialize GeoIP service
        initGeoIPService();
        
        // Initialize services
        rateLimitService = new RateLimitService(this);
        antiDDoSService = new AntiDDoSService(this);
        loggingService = new LoggingService(this);
        webhookService = new WebhookService(this);
        
        // Register listener
        getServer().getPluginManager().registerEvents(
            new PlayerLoginListener(this), this
        );
        
        // Register command
        IPBlockCommand command = new IPBlockCommand(this);
        getCommand("ipblock").setExecutor(command);
        getCommand("ipblock").setTabCompleter(command);
        
        getLogger().info("IPBlock enabled! Mode: " + mode + ", Countries: " + countries);
    }

    @Override
    public void onDisable() {
        if (geoIPService != null) geoIPService.close();
        if (rateLimitService != null) rateLimitService.close();
        if (antiDDoSService != null) antiDDoSService.close();
        if (loggingService != null) loggingService.close();
        if (webhookService != null) webhookService.close();
        getLogger().info("IPBlock disabled!");
    }

    private void initGeoIPService() {
        if (geoIPService != null) geoIPService.close();
        if (!geoipFile.exists()) {
            getLogger().warning("GeoLite2-Country.mmdb not found! Use /ipblock update to download.");
        }
        geoIPService = new GeoIPService(this, geoipFile);
    }

    public void reloadGeoIPService() {
        initGeoIPService();
    }

    /**
     * Reload all configuration and services
     */
    public void reloadAll() {
        // Reload config
        loadConfiguration();
        loadMessages();
        
        // Reinitialize services that depend on config
        if (webhookService != null) webhookService.close();
        webhookService = new WebhookService(this);
        
        getLogger().info("All configurations and services reloaded!");
    }

    public void loadConfiguration() {
        reloadConfig();
        mode = getConfig().getString("mode", "whitelist").toLowerCase();
        countries = new HashSet<>(getConfig().getStringList("countries"));
        whitelistedIPs = new HashSet<>(getConfig().getStringList("whitelisted-ips"));
        allowLocal = getConfig().getBoolean("allow-local", true);
        logBlocked = getConfig().getBoolean("log-blocked", true);
        allowUnknownCountry = getConfig().getBoolean("allow-unknown-country", false);
        autoDownload = getConfig().getBoolean("auto-download", true);
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults from jar
        InputStream defaultStream = getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream));
            messages.setDefaults(defaultMessages);
        }
    }

    public String getMessage(String key) {
        return messages.getString("messages." + key, "&cMessage not found: " + key)
                .replace("&", "§");
    }

    public String getMessage(String key, String placeholder, String value) {
        return getMessage(key).replace("{" + placeholder + "}", value);
    }

    // Getters
    public GeoIPService getGeoIPService() { return geoIPService; }
    public RateLimitService getRateLimitService() { return rateLimitService; }
    public AntiDDoSService getAntiDDoSService() { return antiDDoSService; }
    public GeoIPDownloadService getDownloadService() { return downloadService; }
    public LoggingService getLoggingService() { return loggingService; }
    public WebhookService getWebhookService() { return webhookService; }
    public String getMode() { return mode; }
    public Set<String> getCountries() { return countries; }
    public Set<String> getWhitelistedIPs() { return whitelistedIPs; }
    public boolean isAllowLocal() { return allowLocal; }
    public boolean isLogBlocked() { return logBlocked; }
    public boolean isAllowUnknownCountry() { return allowUnknownCountry; }
}
