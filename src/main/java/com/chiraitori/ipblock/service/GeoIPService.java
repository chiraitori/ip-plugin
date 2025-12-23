package com.chiraitori.ipblock.service;

import com.chiraitori.ipblock.IPBlockPlugin;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class GeoIPService {

    private final IPBlockPlugin plugin;
    private DatabaseReader reader;
    private boolean available = false;

    public GeoIPService(IPBlockPlugin plugin, File databaseFile) {
        this.plugin = plugin;
        
        if (databaseFile.exists()) {
            try {
                reader = new DatabaseReader.Builder(databaseFile).build();
                available = true;
                plugin.getLogger().info("GeoIP database loaded successfully!");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load GeoIP database: " + e.getMessage());
            }
        }
    }

    /**
     * Get country code (ISO 3166-1 alpha-2) from IP address
     * @return Country code (e.g., "VN", "US") or null if not found
     */
    public String getCountryCode(InetAddress address) {
        if (!available || reader == null) {
            return null;
        }

        try {
            CountryResponse response = reader.country(address);
            return response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception e) {
            // IP not found in database (private IP, etc.)
            return null;
        }
    }

    /**
     * Get country name from IP address
     */
    public String getCountryName(InetAddress address) {
        if (!available || reader == null) {
            return "Unknown";
        }

        try {
            CountryResponse response = reader.country(address);
            return response.getCountry().getName();
        } catch (IOException | GeoIp2Exception e) {
            return "Unknown";
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                plugin.getLogger().warning("Error closing GeoIP database: " + e.getMessage());
            }
        }
    }
}
