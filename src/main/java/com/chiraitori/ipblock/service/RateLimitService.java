package com.chiraitori.ipblock.service;

import com.chiraitori.ipblock.IPBlockPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimitService {

    private final IPBlockPlugin plugin;
    private final Map<String, ConnectionAttempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedIPs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    
    private final boolean enabled;
    private final int maxAttempts;
    private final int timeWindowSeconds;
    private final int blockDurationMinutes;

    public RateLimitService(IPBlockPlugin plugin) {
        this.plugin = plugin;
        
        this.enabled = plugin.getConfig().getBoolean("rate-limit.enabled", true);
        this.maxAttempts = plugin.getConfig().getInt("rate-limit.max-attempts", 5);
        this.timeWindowSeconds = plugin.getConfig().getInt("rate-limit.time-window-seconds", 60);
        this.blockDurationMinutes = plugin.getConfig().getInt("rate-limit.block-duration-minutes", 30);
        
        // Cleanup scheduler - run every minute
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Check if IP is rate limited
     * @return true if blocked, false if allowed
     */
    public boolean isRateLimited(String ip) {
        if (!enabled) {
            return false;
        }

        // Check if IP is currently blocked
        Long blockedUntil = blockedIPs.get(ip);
        if (blockedUntil != null) {
            if (System.currentTimeMillis() < blockedUntil) {
                return true;
            } else {
                blockedIPs.remove(ip);
            }
        }

        return false;
    }

    /**
     * Record a connection attempt
     * @return true if IP should be blocked after this attempt
     */
    public boolean recordAttempt(String ip) {
        if (!enabled) {
            return false;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - (timeWindowSeconds * 1000L);

        ConnectionAttempt attempt = attempts.compute(ip, (key, existing) -> {
            if (existing == null || existing.windowStart < windowStart) {
                return new ConnectionAttempt(now, 1);
            } else {
                existing.count++;
                return existing;
            }
        });

        if (attempt.count > maxAttempts) {
            // Block this IP
            blockedIPs.put(ip, now + (blockDurationMinutes * 60 * 1000L));
            attempts.remove(ip);
            plugin.getLogger().warning("Rate limit exceeded for IP: " + ip + " - blocked for " + blockDurationMinutes + " minutes");
            return true;
        }

        return false;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        long windowStart = now - (timeWindowSeconds * 1000L);

        // Remove old attempts
        attempts.entrySet().removeIf(entry -> entry.getValue().windowStart < windowStart);
        
        // Remove expired blocks
        blockedIPs.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    public void close() {
        scheduler.shutdown();
    }

    private static class ConnectionAttempt {
        long windowStart;
        int count;

        ConnectionAttempt(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
