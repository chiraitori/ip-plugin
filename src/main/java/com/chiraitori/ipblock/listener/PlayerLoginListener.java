package com.chiraitori.ipblock.listener;

import com.chiraitori.ipblock.IPBlockPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.InetAddress;

public class PlayerLoginListener implements Listener {

    private final IPBlockPlugin plugin;

    public PlayerLoginListener(IPBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        InetAddress address = event.getAddress();
        String ip = address.getHostAddress();
        String countryCode = null;

        // Check whitelist first
        if (plugin.getWhitelistedIPs().contains(ip)) {
            return;
        }

        // Check if local/private IP
        if (plugin.isAllowLocal() && isLocalAddress(address)) {
            return;
        }

        // Anti-DDoS check
        String ddosBlockReason = plugin.getAntiDDoSService().checkConnection(ip);
        if (ddosBlockReason != null) {
            if (ddosBlockReason.contains("blacklist")) {
                kick(event, plugin.getMessage("kick-blacklist"), "Blacklist: " + ddosBlockReason, null);
            } else {
                kick(event, plugin.getMessage("kick-ddos"), "Anti-DDoS: " + ddosBlockReason, null);
            }
            return;
        }

        // Check rate limit
        if (plugin.getRateLimitService().isRateLimited(ip)) {
            kick(event, plugin.getMessage("kick-ratelimit"), "Rate limited", null);
            return;
        }
        plugin.getRateLimitService().recordAttempt(ip);

        // Check GeoIP
        if (!plugin.getGeoIPService().isAvailable()) {
            return;
        }

        countryCode = plugin.getGeoIPService().getCountryCode(address);
        
        // Handle unknown country
        if (countryCode == null) {
            if (!plugin.isAllowUnknownCountry()) {
                kick(event, plugin.getMessage("kick-unknown"), "Unknown country", null);
            }
            return;
        }

        // Check country based on mode
        boolean allowed;
        if (plugin.getMode().equals("whitelist")) {
            allowed = plugin.getCountries().contains(countryCode);
        } else {
            allowed = !plugin.getCountries().contains(countryCode);
        }

        if (!allowed) {
            String countryName = plugin.getGeoIPService().getCountryName(address);
            kick(event, plugin.getMessage("kick-country"), 
                "Country: " + countryCode + " (" + countryName + ")", countryCode);
        } else {
            // Log allowed connection (if enabled)
            plugin.getLoggingService().logAllowed(ip, countryCode);
        }
    }

    private void kick(AsyncPlayerPreLoginEvent event, String message, String reason, String country) {
        String ip = event.getAddress().getHostAddress();
        
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, 
            net.kyori.adventure.text.Component.text(message));
        
        // Log to file and console
        plugin.getLoggingService().logBlocked(ip, reason, country);
    }

    private boolean isLocalAddress(InetAddress address) {
        return address.isLoopbackAddress() 
            || address.isSiteLocalAddress() 
            || address.isLinkLocalAddress()
            || address.getHostAddress().startsWith("192.168.")
            || address.getHostAddress().startsWith("10.")
            || address.getHostAddress().startsWith("172.");
    }
}
