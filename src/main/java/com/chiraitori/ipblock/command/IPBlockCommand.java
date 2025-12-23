package com.chiraitori.ipblock.command;

import com.chiraitori.ipblock.IPBlockPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IPBlockCommand implements CommandExecutor, TabCompleter {

    private final IPBlockPlugin plugin;

    public IPBlockCommand(IPBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ipblock.admin")) {
            sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadAll();
                sender.sendMessage("§aĐã reload config và services!");
                sender.sendMessage("§7Mode: §f" + plugin.getMode());
                sender.sendMessage("§7Countries: §f" + plugin.getCountries());
                sender.sendMessage("§7Webhook Discord: " + (plugin.getWebhookService().isDiscordEnabled() ? "§aEnabled" : "§cDisabled"));
                sender.sendMessage("§7Webhook Telegram: " + (plugin.getWebhookService().isTelegramEnabled() ? "§aEnabled" : "§cDisabled"));
                break;

            case "check":
                if (args.length < 2) {
                    sender.sendMessage("§cSử dụng: /ipblock check <ip>");
                    return true;
                }
                checkIP(sender, args[1]);
                break;

            case "whitelist":
                if (args.length < 3) {
                    sender.sendMessage("§cSử dụng: /ipblock whitelist <add|remove> <ip>");
                    return true;
                }
                handleWhitelist(sender, args[1], args[2]);
                break;

            case "status":
                showStatus(sender);
                break;

            case "update":
                updateDatabase(sender);
                break;

            case "blacklist":
                if (args.length < 3) {
                    sender.sendMessage("§cSử dụng: /ipblock blacklist <add|remove> <ip>");
                    return true;
                }
                handleBlacklist(sender, args[1], args[2]);
                break;

            case "ddos":
                showDDoSStatus(sender);
                break;

            case "webhook":
                if (args.length < 2) {
                    showWebhookHelp(sender);
                    return true;
                }
                handleWebhook(sender, args);
                break;

            default:
                showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6=== IPBlock Commands ===");
        sender.sendMessage("§e/ipblock reload §7- Reload config");
        sender.sendMessage("§e/ipblock check <ip> §7- Kiểm tra quốc gia của IP");
        sender.sendMessage("§e/ipblock whitelist add/remove <ip> §7- Quản lý whitelist");
        sender.sendMessage("§e/ipblock blacklist add/remove <ip> §7- Quản lý blacklist");
        sender.sendMessage("§e/ipblock status §7- Xem trạng thái plugin");
        sender.sendMessage("§e/ipblock ddos §7- Xem trạng thái Anti-DDoS");
        sender.sendMessage("§e/ipblock webhook §7- Cấu hình Discord/Telegram webhook");
        sender.sendMessage("§e/ipblock update §7- Tải/cập nhật GeoLite2 database");
    }

    private void updateDatabase(CommandSender sender) {
        sender.sendMessage("§eĐang tải GeoLite2 database từ GitHub...");
        plugin.getDownloadService().downloadAsync(true, () -> {
            plugin.reloadGeoIPService();
            sender.sendMessage("§aĐã cập nhật GeoLite2 database thành công!");
        });
    }

    private void checkIP(CommandSender sender, String ipString) {
        try {
            InetAddress address = InetAddress.getByName(ipString);
            String countryCode = plugin.getGeoIPService().getCountryCode(address);
            String countryName = plugin.getGeoIPService().getCountryName(address);

            if (countryCode == null) {
                sender.sendMessage("§eIP: §f" + ipString);
                sender.sendMessage("§eQuốc gia: §7Không xác định (private/local IP)");
            } else {
                sender.sendMessage("§eIP: §f" + ipString);
                sender.sendMessage("§eQuốc gia: §f" + countryName + " §7(" + countryCode + ")");
                
                boolean allowed;
                if (plugin.getMode().equals("whitelist")) {
                    allowed = plugin.getCountries().contains(countryCode);
                } else {
                    allowed = !plugin.getCountries().contains(countryCode);
                }
                
                sender.sendMessage("§eTrạng thái: " + (allowed ? "§aCho phép" : "§cBị chặn"));
            }
        } catch (UnknownHostException e) {
            sender.sendMessage("§cIP không hợp lệ: " + ipString);
        }
    }

    private void handleWhitelist(CommandSender sender, String action, String ip) {
        List<String> whitelist = plugin.getConfig().getStringList("whitelisted-ips");
        
        switch (action.toLowerCase()) {
            case "add":
                if (!whitelist.contains(ip)) {
                    whitelist.add(ip);
                    plugin.getConfig().set("whitelisted-ips", whitelist);
                    plugin.saveConfig();
                    plugin.loadConfiguration();
                    sender.sendMessage("§aĐã thêm §f" + ip + " §avào whitelist!");
                } else {
                    sender.sendMessage("§eIP đã có trong whitelist!");
                }
                break;
                
            case "remove":
                if (whitelist.contains(ip)) {
                    whitelist.remove(ip);
                    plugin.getConfig().set("whitelisted-ips", whitelist);
                    plugin.saveConfig();
                    plugin.loadConfiguration();
                    sender.sendMessage("§aĐã xóa §f" + ip + " §akhỏi whitelist!");
                } else {
                    sender.sendMessage("§eIP không có trong whitelist!");
                }
                break;
                
            default:
                sender.sendMessage("§cSử dụng: /ipblock whitelist <add|remove> <ip>");
        }
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage("§6=== IPBlock Status ===");
        sender.sendMessage("§eGeoIP Database: " + 
            (plugin.getGeoIPService().isAvailable() ? "§aLoaded" : "§cNot found"));
        sender.sendMessage("§eMode: §f" + plugin.getMode());
        sender.sendMessage("§eCountries: §f" + plugin.getCountries());
        sender.sendMessage("§eWhitelisted IPs: §f" + plugin.getWhitelistedIPs().size());
        sender.sendMessage("§eBlacklisted IPs: §f" + plugin.getAntiDDoSService().getPermBlockedCount());
        sender.sendMessage("§eAllow Local: §f" + plugin.isAllowLocal());
        sender.sendMessage("§eAnti-DDoS: " + (plugin.getAntiDDoSService().isEnabled() ? "§aEnabled" : "§cDisabled"));
    }

    private void handleBlacklist(CommandSender sender, String action, String ip) {
        switch (action.toLowerCase()) {
            case "add":
                plugin.getAntiDDoSService().addPermBlock(ip);
                sender.sendMessage("§aĐã thêm §f" + ip + " §avào blacklist vĩnh viễn!");
                break;
                
            case "remove":
                if (plugin.getAntiDDoSService().removePermBlock(ip)) {
                    sender.sendMessage("§aĐã xóa §f" + ip + " §akhỏi blacklist!");
                } else {
                    sender.sendMessage("§eIP không có trong blacklist!");
                }
                break;
                
            default:
                sender.sendMessage("§cSử dụng: /ipblock blacklist <add|remove> <ip>");
        }
    }

    private void showDDoSStatus(CommandSender sender) {
        sender.sendMessage("§6=== Anti-DDoS Status ===");
        sender.sendMessage("§eEnabled: " + (plugin.getAntiDDoSService().isEnabled() ? "§aYes" : "§cNo"));
        sender.sendMessage("§eConnections/sec: §f" + plugin.getAntiDDoSService().getConnectionsPerSecond());
        sender.sendMessage("§eTemp Blocked IPs: §f" + plugin.getAntiDDoSService().getTempBlockedCount());
        sender.sendMessage("§ePerm Blocked IPs: §f" + plugin.getAntiDDoSService().getPermBlockedCount());
        
        if (plugin.getAntiDDoSService().getPermBlockedCount() > 0) {
            sender.sendMessage("§7Blacklist: " + plugin.getAntiDDoSService().getPermBlacklist());
        }
    }

    private void showWebhookHelp(CommandSender sender) {
        sender.sendMessage("§6=== Webhook Commands ===");
        sender.sendMessage("§e/ipblock webhook status §7- Xem trạng thái webhook");
        sender.sendMessage("§e/ipblock webhook discord <url> §7- Cấu hình Discord webhook");
        sender.sendMessage("§e/ipblock webhook telegram <bot-token> <chat-id> §7- Cấu hình Telegram");
        sender.sendMessage("§e/ipblock webhook test §7- Test webhook");
        sender.sendMessage("§e/ipblock webhook disable <discord|telegram> §7- Tắt webhook");
    }

    private void handleWebhook(CommandSender sender, String[] args) {
        switch (args[1].toLowerCase()) {
            case "status":
                sender.sendMessage("§6=== Webhook Status ===");
                sender.sendMessage("§eDiscord: " + (plugin.getWebhookService().isDiscordEnabled() ? "§aEnabled" : "§cDisabled"));
                sender.sendMessage("§eTelegram: " + (plugin.getWebhookService().isTelegramEnabled() ? "§aEnabled" : "§cDisabled"));
                break;
                
            case "discord":
                if (args.length < 3) {
                    sender.sendMessage("§cSử dụng: /ipblock webhook discord <webhook-url>");
                    return;
                }
                plugin.getConfig().set("webhook.discord.enabled", true);
                plugin.getConfig().set("webhook.discord.url", args[2]);
                plugin.saveConfig();
                sender.sendMessage("§aĐã cấu hình Discord webhook! Reload plugin để áp dụng.");
                break;
                
            case "telegram":
                if (args.length < 4) {
                    sender.sendMessage("§cSử dụng: /ipblock webhook telegram <bot-token> <chat-id>");
                    return;
                }
                plugin.getConfig().set("webhook.telegram.enabled", true);
                plugin.getConfig().set("webhook.telegram.bot-token", args[2]);
                plugin.getConfig().set("webhook.telegram.chat-id", args[3]);
                plugin.saveConfig();
                sender.sendMessage("§aĐã cấu hình Telegram webhook! Reload plugin để áp dụng.");
                break;
                
            case "test":
                sender.sendMessage("§eĐang gửi test webhook...");
                plugin.getWebhookService().notifyPermBlock("TEST-IP-123.456.789.0");
                sender.sendMessage("§aĐã gửi test! Kiểm tra Discord/Telegram của bạn.");
                break;
                
            case "disable":
                if (args.length < 3) {
                    sender.sendMessage("§cSử dụng: /ipblock webhook disable <discord|telegram>");
                    return;
                }
                if (args[2].equalsIgnoreCase("discord")) {
                    plugin.getConfig().set("webhook.discord.enabled", false);
                    sender.sendMessage("§aĐã tắt Discord webhook!");
                } else if (args[2].equalsIgnoreCase("telegram")) {
                    plugin.getConfig().set("webhook.telegram.enabled", false);
                    sender.sendMessage("§aĐã tắt Telegram webhook!");
                }
                plugin.saveConfig();
                break;
                
            default:
                showWebhookHelp(sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "check", "whitelist", "blacklist", "status", "ddos", "webhook", "update"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("whitelist") || args[0].equalsIgnoreCase("blacklist")) {
                completions.addAll(Arrays.asList("add", "remove"));
            } else if (args[0].equalsIgnoreCase("webhook")) {
                completions.addAll(Arrays.asList("status", "discord", "telegram", "test", "disable"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("webhook") && args[1].equalsIgnoreCase("disable")) {
            completions.addAll(Arrays.asList("discord", "telegram"));
        }
        
        return completions;
    }
}
