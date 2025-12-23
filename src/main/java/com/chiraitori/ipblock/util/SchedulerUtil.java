package com.chiraitori.ipblock.util;

import com.chiraitori.ipblock.IPBlockPlugin;
import org.bukkit.Bukkit;

/**
 * Scheduler wrapper for Paper/Folia compatibility
 */
public class SchedulerUtil {

    private static Boolean isFolia = null;

    /**
     * Check if running on Folia
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * Run task asynchronously (works on both Paper and Folia)
     */
    public static void runAsync(IPBlockPlugin plugin, Runnable task) {
        if (isFolia()) {
            // Folia: use AsyncScheduler
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            // Paper: use BukkitScheduler
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Run task on main thread (works on both Paper and Folia)
     */
    public static void runSync(IPBlockPlugin plugin, Runnable task) {
        if (isFolia()) {
            // Folia: use GlobalRegionScheduler
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            // Paper: use BukkitScheduler
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run task later asynchronously
     */
    public static void runAsyncLater(IPBlockPlugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            // Folia: use AsyncScheduler with delay
            long delayMs = delayTicks * 50; // Convert ticks to ms
            Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), 
                delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            // Paper: use BukkitScheduler
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }
}
