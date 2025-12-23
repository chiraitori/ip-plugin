# IPBlock - PaperMC IP Protection Plugin

A powerful PaperMC plugin to protect your Minecraft server from unwanted connections based on country, with built-in Anti-DDoS protection.

## Features

| Feature | Description |
|---------|-------------|
| 🌍 **Country Filter** | Whitelist/Blacklist countries using GeoIP |
| 🛡️ **Anti-DDoS** | Connection throttling, auto-block spam IPs |
| ⚡ **Rate Limiting** | Block IPs with excessive connection attempts |
| 📋 **Blacklist** | Permanent IP blacklist (auto-saved) |
| 📥 **Auto-Download** | Automatically downloads GeoLite2 database |
| 🔔 **Webhooks** | Discord & Telegram notifications |

## Installation

1. Download `IPBlock-0.1.jar` from [Releases](../../releases)
2. Place in your server's `plugins/` folder
3. Start server → GeoIP database auto-downloads!

## Commands

| Command | Description |
|---------|-------------|
| `/ipblock reload` | Reload config and services |
| `/ipblock status` | View plugin status |
| `/ipblock check <ip>` | Check country of an IP |
| `/ipblock whitelist add/remove <ip>` | Manage IP whitelist |
| `/ipblock blacklist add/remove <ip>` | Manage IP blacklist |
| `/ipblock ddos` | View Anti-DDoS status |
| `/ipblock webhook` | Configure Discord/Telegram webhooks |
| `/ipblock update` | Update GeoLite2 database |

## Configuration

### Basic Setup (config.yml)
```yaml
# Mode: whitelist (only allow) or blacklist (block)
mode: whitelist

# Countries (ISO 3166-1 alpha-2)
countries:
  - VN  # Vietnam

# Anti-DDoS
anti-ddos:
  enabled: true
  max-connections-per-second: 50
  max-connections-per-ip: 3
```

### Webhook Setup
```bash
# Discord
/ipblock webhook discord https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN

# Telegram
/ipblock webhook telegram YOUR_BOT_TOKEN YOUR_CHAT_ID

# Test & Apply
/ipblock webhook test
/ipblock reload
```

## Country Codes (ISO 3166-1 alpha-2)

### Asia
| Code | Country | Code | Country |
|------|---------|------|---------|
| VN | 🇻🇳 Vietnam | PH | 🇵🇭 Philippines |
| TH | 🇹🇭 Thailand | MY | 🇲🇾 Malaysia |
| SG | 🇸🇬 Singapore | ID | 🇮🇩 Indonesia |
| JP | 🇯🇵 Japan | KR | 🇰🇷 South Korea |
| TW | 🇹🇼 Taiwan | CN | 🇨🇳 China |
| HK | 🇭🇰 Hong Kong | IN | 🇮🇳 India |

### Europe
| Code | Country | Code | Country |
|------|---------|------|---------|
| GB | 🇬🇧 United Kingdom | DE | 🇩🇪 Germany |
| FR | 🇫🇷 France | IT | 🇮🇹 Italy |
| ES | 🇪🇸 Spain | NL | 🇳🇱 Netherlands |
| PL | 🇵🇱 Poland | RU | 🇷🇺 Russia |
| UA | 🇺🇦 Ukraine | SE | 🇸🇪 Sweden |

### Americas
| Code | Country | Code | Country |
|------|---------|------|---------|
| US | 🇺🇸 United States | CA | 🇨🇦 Canada |
| MX | 🇲🇽 Mexico | BR | 🇧🇷 Brazil |
| AR | 🇦🇷 Argentina | CL | 🇨🇱 Chile |

### Oceania
| Code | Country | Code | Country |
|------|---------|------|---------|
| AU | 🇦🇺 Australia | NZ | 🇳🇿 New Zealand |

> 📖 Full list: [ISO 3166-1 alpha-2](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2)

## Permissions

| Permission | Description |
|------------|-------------|
| `ipblock.admin` | Access to all commands |

## Requirements

- PaperMC 1.20+ (or compatible forks)
- Java 21+

## Building

```bash
./gradlew shadowJar
```

Output: `build/libs/IPBlock-0.1.jar`

## License

MIT License

---

[🇻🇳 Tiếng Việt](README_VI.md)
