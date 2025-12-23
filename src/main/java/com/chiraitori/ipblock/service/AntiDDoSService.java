package com.chiraitori.ipblock.service;

import com.chiraitori.ipblock.IPBlockPlugin;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AntiDDoSService {

    private final IPBlockPlugin plugin;
    
    // Global connection tracking
    private final AtomicInteger connectionsPerSecond = new AtomicInteger(0);
    private final AtomicLong lastSecondReset = new AtomicLong(System.currentTimeMillis());
    
    // Per-IP tracking
    private final Map<String, ConnectionInfo> ipConnections = new ConcurrentHashMap<>();
    
    // Temporary blacklist (auto-expires)
    private final Map<String, Long> tempBlacklist = new ConcurrentHashMap<>();
    
    // Permanent blacklist (persisted to file)
    private final Set<String> permBlacklist = ConcurrentHashMap.newKeySet();
    
    private final ScheduledExecutorService scheduler;
    private final File blacklistFile;
    
    // Config
    private final boolean enabled;
    private final int maxConnectionsPerSecond;      // Global max connections/sec
    private final int maxConnectionsPerIP;           // Per IP max connections in time window
    private final int ipTimeWindowSeconds;           // Time window for per-IP tracking
    private final int tempBlockMinutes;              // Temp block duration
    private final int permBlockThreshold;            // Times to temp block before perm block
    private final int ddosThreshold;                 // Connections/sec to trigger DDoS mode

    public AntiDDoSService(IPBlockPlugin plugin) {
        this.plugin = plugin;
        this.blacklistFile = new File(plugin.getDataFolder(), "blacklist.txt");
        
        // Load config
        this.enabled = plugin.getConfig().getBoolean("anti-ddos.enabled", true);
        this.maxConnectionsPerSecond = plugin.getConfig().getInt("anti-ddos.max-connections-per-second", 50);
        this.maxConnectionsPerIP = plugin.getConfig().getInt("anti-ddos.max-connections-per-ip", 3);
        this.ipTimeWindowSeconds = plugin.getConfig().getInt("anti-ddos.ip-time-window-seconds", 10);
        this.tempBlockMinutes = plugin.getConfig().getInt("anti-ddos.temp-block-minutes", 60);
        this.permBlockThreshold = plugin.getConfig().getInt("anti-ddos.perm-block-threshold", 5);
        this.ddosThreshold = plugin.getConfig().getInt("anti-ddos.ddos-threshold", 100);
        
        // Load permanent blacklist
        loadBlacklist();
        
        // Cleanup scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.SECONDS);
        
        if (enabled) {
            plugin.getLogger().info("Anti-DDoS enabled! Max " + maxConnectionsPerSecond + " connections/sec");
        }
    }

    /**
     * Check if connection should be allowed
     * @return Block reason or null if allowed
     */
    public String checkConnection(String ip) {
        if (!enabled) {
            return null;
        }

        // Check permanent blacklist
        if (permBlacklist.contains(ip)) {
            return "Permanent blacklist";
        }

        // Check temporary blacklist
        Long blockedUntil = tempBlacklist.get(ip);
        if (blockedUntil != null) {
            if (System.currentTimeMillis() < blockedUntil) {
                return "Temporary blacklist";
            } else {
                tempBlacklist.remove(ip);
            }
        }

        // Update global connections count
        long now = System.currentTimeMillis();
        if (now - lastSecondReset.get() >= 1000) {
            int currentCPS = connectionsPerSecond.getAndSet(0);
            lastSecondReset.set(now);
            
            // Check for DDoS
            if (currentCPS >= ddosThreshold) {
                plugin.getLogger().warning("⚠️ DDoS DETECTED! " + currentCPS + " connections/sec");
            }
        }
        
        int currentConnections = connectionsPerSecond.incrementAndGet();
        
        // Check global rate limit
        if (currentConnections > maxConnectionsPerSecond) {
            plugin.getLogger().warning("Global rate limit exceeded: " + currentConnections + " connections/sec");
            return "Server overloaded";
        }

        // Check per-IP rate limit
        ConnectionInfo info = ipConnections.compute(ip, (key, existing) -> {
            if (existing == null) {
                return new ConnectionInfo();
            }
            existing.addConnection();
            return existing;
        });

        if (info.getConnectionCount() > maxConnectionsPerIP) {
            // Temp block this IP
            addTempBlock(ip);
            return "Too many connections";
        }

        return null; // Allow
    }

    private void addTempBlock(String ip) {
        long blockUntil = System.currentTimeMillis() + (tempBlockMinutes * 60 * 1000L);
        tempBlacklist.put(ip, blockUntil);
        
        // Track temp blocks for this IP
        ConnectionInfo info = ipConnections.get(ip);
        if (info != null) {
            info.incrementTempBlocks();
            
            // Check if should be permanently blocked
            if (info.getTempBlockCount() >= permBlockThreshold) {
                addPermBlock(ip);
            }
        }
        
        plugin.getLogger().warning("Temp blocked IP: " + ip + " for " + tempBlockMinutes + " minutes");
    }

    public void addPermBlock(String ip) {
        permBlacklist.add(ip);
        tempBlacklist.remove(ip);
        saveBlacklist();
        plugin.getLogger().warning("Permanently blocked IP: " + ip);
    }

    public boolean removePermBlock(String ip) {
        if (permBlacklist.remove(ip)) {
            saveBlacklist();
            return true;
        }
        return false;
    }

    public void removeTempBlock(String ip) {
        tempBlacklist.remove(ip);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        long windowStart = now - (ipTimeWindowSeconds * 1000L);
        
        // Cleanup old connection tracking
        ipConnections.entrySet().removeIf(entry -> 
            entry.getValue().getLastConnection() < windowStart);
        
        // Cleanup expired temp blocks
        tempBlacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private void loadBlacklist() {
        if (!blacklistFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(blacklistFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    permBlacklist.add(line);
                }
            }
            plugin.getLogger().info("Loaded " + permBlacklist.size() + " IPs from blacklist");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load blacklist: " + e.getMessage());
        }
    }

    private void saveBlacklist() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(blacklistFile))) {
            writer.println("# IPBlock Permanent Blacklist");
            writer.println("# IPs in this file are permanently blocked");
            for (String ip : permBlacklist) {
                writer.println(ip);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save blacklist: " + e.getMessage());
        }
    }

    public void close() {
        scheduler.shutdown();
    }

    // Stats
    public int getConnectionsPerSecond() { return connectionsPerSecond.get(); }
    public int getTempBlockedCount() { return tempBlacklist.size(); }
    public int getPermBlockedCount() { return permBlacklist.size(); }
    public Set<String> getPermBlacklist() { return Collections.unmodifiableSet(permBlacklist); }
    public boolean isEnabled() { return enabled; }

    private static class ConnectionInfo {
        private int connectionCount = 1;
        private long lastConnection = System.currentTimeMillis();
        private int tempBlockCount = 0;

        void addConnection() {
            connectionCount++;
            lastConnection = System.currentTimeMillis();
        }

        void incrementTempBlocks() {
            tempBlockCount++;
        }

        int getConnectionCount() { return connectionCount; }
        long getLastConnection() { return lastConnection; }
        int getTempBlockCount() { return tempBlockCount; }
    }
}
