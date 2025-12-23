# IPBlock - Plugin Bảo Vệ Server Minecraft

Plugin PaperMC bảo vệ server Minecraft khỏi các kết nối không mong muốn dựa trên quốc gia, tích hợp chống DDoS.

## Tính năng

| Tính năng | Mô tả |
|-----------|-------|
| 🌍 **Lọc Quốc Gia** | Whitelist/Blacklist quốc gia bằng GeoIP |
| 🛡️ **Anti-DDoS** | Giới hạn kết nối, tự động block IP spam |
| ⚡ **Rate Limiting** | Chặn IP kết nối quá nhiều lần |
| 📋 **Blacklist** | Danh sách IP cấm vĩnh viễn |
| 📥 **Auto-Download** | Tự động tải GeoLite2 database |
| 🔔 **Webhooks** | Thông báo Discord & Telegram |

## Cài đặt

1. Tải `IPBlock-0.1.jar` từ [Releases](../../releases)
2. Bỏ vào thư mục `plugins/`
3. Khởi động server → Database tự động tải!

## Commands

| Command | Mô tả |
|---------|-------|
| `/ipblock reload` | Reload config và services |
| `/ipblock status` | Xem trạng thái plugin |
| `/ipblock check <ip>` | Kiểm tra quốc gia của IP |
| `/ipblock whitelist add/remove <ip>` | Quản lý whitelist |
| `/ipblock blacklist add/remove <ip>` | Quản lý blacklist |
| `/ipblock ddos` | Xem trạng thái Anti-DDoS |
| `/ipblock webhook` | Cấu hình Discord/Telegram |
| `/ipblock update` | Cập nhật GeoLite2 database |

## Cấu hình

### Config cơ bản (config.yml)
```yaml
# Mode: whitelist (chỉ cho phép) hoặc blacklist (chặn)
mode: whitelist

# Quốc gia (ISO 3166-1 alpha-2)
countries:
  - VN  # Vietnam

# Anti-DDoS
anti-ddos:
  enabled: true
  max-connections-per-second: 50
  max-connections-per-ip: 3
```

### Cấu hình Webhook
```bash
# Discord
/ipblock webhook discord https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN

# Telegram
/ipblock webhook telegram YOUR_BOT_TOKEN YOUR_CHAT_ID

# Test & Áp dụng
/ipblock webhook test
/ipblock reload
```

## Mã Quốc Gia (ISO 3166-1 alpha-2)

### Châu Á
| Mã | Quốc gia | Mã | Quốc gia |
|----|----------|----|---------| 
| VN | 🇻🇳 Việt Nam | PH | 🇵🇭 Philippines |
| TH | 🇹🇭 Thái Lan | MY | 🇲🇾 Malaysia |
| SG | 🇸🇬 Singapore | ID | 🇮🇩 Indonesia |
| JP | 🇯🇵 Nhật Bản | KR | 🇰🇷 Hàn Quốc |
| TW | 🇹🇼 Đài Loan | CN | 🇨🇳 Trung Quốc |
| HK | 🇭🇰 Hồng Kông | IN | 🇮🇳 Ấn Độ |

### Châu Âu
| Mã | Quốc gia | Mã | Quốc gia |
|----|----------|----|---------| 
| GB | 🇬🇧 Anh | DE | 🇩🇪 Đức |
| FR | 🇫🇷 Pháp | IT | 🇮🇹 Ý |
| ES | 🇪🇸 Tây Ban Nha | NL | 🇳🇱 Hà Lan |
| PL | 🇵🇱 Ba Lan | RU | 🇷🇺 Nga |
| UA | 🇺🇦 Ukraine | SE | 🇸🇪 Thụy Điển |

### Châu Mỹ
| Mã | Quốc gia | Mã | Quốc gia |
|----|----------|----|---------| 
| US | 🇺🇸 Mỹ | CA | 🇨🇦 Canada |
| MX | 🇲🇽 Mexico | BR | 🇧🇷 Brazil |
| AR | 🇦🇷 Argentina | CL | 🇨🇱 Chile |

### Châu Đại Dương
| Mã | Quốc gia | Mã | Quốc gia |
|----|----------|----|---------| 
| AU | 🇦🇺 Úc | NZ | 🇳🇿 New Zealand |

> 📖 Danh sách đầy đủ: [ISO 3166-1 alpha-2](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2)

## Quyền hạn

| Permission | Mô tả |
|------------|-------|
| `ipblock.admin` | Truy cập tất cả commands |

## Yêu cầu

- PaperMC 1.20+ (hoặc fork tương thích)
- Java 21+

## Build

```bash
./gradlew shadowJar
```

Output: `build/libs/IPBlock-0.1.jar`

## License

MIT License

---

[🇬🇧 English](README.md)
