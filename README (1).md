# AuroraPunishNotify

**AuroraPunishNotify** is a lightweight Paper plugin that automatically sends richly formatted Discord notifications whenever a punishment is issued on your server via CMI. It supports two delivery modes — a raw Discord Webhook (no extra plugins required) or DiscordSRV (bot-powered delivery).

---

## ✨ Features

- 🔔 Instant Discord notifications for: **Ban, Temp-ban, Mute, Temp-mute, Warn, Kick, Unban, Unmute**
- 🎨 Fully customizable **embed** per punishment type — color, title, fields, footer, avatar thumbnail
- 🖼️ Displays the punished player's **avatar/skin** in the embed
- ⚡ **Asynchronous delivery** — notifications never lag the server thread
- 🔀 Two delivery modes:
  - `webhook` — sends directly to a Discord Webhook URL (no extra plugin needed)
  - `discordsrv` — sends via DiscordSRV's JDA bot (supports DiscordSRV legacy)
- 🔄 **Hot-reload** support via `/apn reload`
- 🏷️ Rich placeholder system: player name, executor, reason, duration, server name, timestamp

---

## 📋 Requirements

| Requirement | Version |
|---|---|
| Server Software | Paper (or forks) |
| Minecraft Version | 1.21+ |
| Java | 21+ |
| CMI | Required (hard dependency) |
| DiscordSRV | Optional (only if using `mode: discordsrv`) |

---

## 📦 Installation

1. Download `AuroraPunishNotify-1.0.0.jar` and place it in your server's `plugins/` folder.
2. Make sure **CMI** is already installed and loaded.
3. *(Optional)* If you plan to use `discordsrv` mode, install **DiscordSRV** as well.
4. Start or restart your server.
5. A default `config.yml` will be generated inside `plugins/AuroraPunishNotify/`.
6. Edit `config.yml` to configure your webhook URL or DiscordSRV channel ID.
7. Run `/apn reload` to apply changes without restarting.

---

## ⚙️ Configuration

The full `config.yml` is documented below (see the separate translated config file). Key sections:

### Notification Mode

```yaml
mode: webhook       # Use 'webhook' or 'discordsrv'
```

### Webhook Mode Setup

```yaml
webhook:
  url: "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
  username: "Punishment Bot"
  avatar_url: ""     # Optional: override webhook avatar
```

To get a webhook URL: go to your Discord channel → **Edit Channel** → **Integrations** → **Webhooks** → **New Webhook** → Copy URL.

### DiscordSRV Mode Setup

```yaml
discordsrv:
  channel_id: "000000000000000000"
```

To get a channel ID: enable **Developer Mode** in Discord (**Settings → Advanced → Developer Mode**), then right-click the channel and select **Copy Channel ID**.

### Player Avatar URL

```yaml
avatar_url: "https://minotar.net/avatar/{username}"
```

Supports `{uuid}` and `{name}` as placeholders. You can use any skin rendering service such as:
- `https://crafatar.com/avatars/{uuid}?size=64&overlay` *(recommended, renders with skin overlay)*
- `https://minotar.net/avatar/{username}`
- `https://mc-heads.net/avatar/{uuid}`

### Notification Embeds

Each punishment type (`ban`, `tempban`, `mute`, `warn`, `kick`, `unban`, `unmute`) can be independently configured:

```yaml
notifications:
  ban:
    enabled: true
    color: 15158332        # Embed color in decimal (Red)
    title: "🗡️ [{server}] BAN 🗡️"
    fields:
      - name: "Player"
        value: "{player}"
        inline: true
      - name: "Issued By"
        value: "{executor}"
        inline: true
      - name: "Duration"
        value: "{duration}"
        inline: true
      - name: "Reason"
        value: "{reason}"
        inline: false
      - name: "Server"
        value: "{server}"
        inline: true
    footer: "AuroraSurvival Punishment System"
    show_avatar: true
```

---

## 🏷️ Placeholders

The following placeholders are available in `title`, field `name`, and field `value`:

| Placeholder | Description |
|---|---|
| `{player}` | Name of the punished player |
| `{executor}` | Name of the staff member who issued the punishment |
| `{reason}` | Reason for the punishment |
| `{duration}` | Duration of the punishment (or the `permanent_label` value if permanent) |
| `{server}` | Server name (from `server_name` in config) |
| `{timestamp}` | Unix timestamp (usable in Discord as `<t:{timestamp}:R>`) |

---

## 🎨 Embed Color Reference

Colors are specified as **decimal integers**. Common values:

| Color | Decimal |
|---|---|
| Red | `15158332` |
| Orange | `15105570` |
| Yellow | `16776960` |
| Blue | `3447003` |
| Green | `3066993` |
| Grey | `9807270` |

Use any [HEX to Decimal converter](https://www.binaryhexconverter.com/hex-to-decimal-converter) to create custom colors.

---

## 🛠️ Commands

| Command | Description | Permission |
|---|---|---|
| `/apn` | Show plugin version | `aurorapunishnotify.admin` |
| `/apn reload` | Reload config.yml | `aurorapunishnotify.admin` |

**Alias:** `/aurorapunish`

**Default permission:** OP only

---

## 🔍 How It Works

The plugin uses **two event listeners** to detect punishments:

1. **CMIPunishListener** — listens to native CMI API events (`CMIPlayerBanEvent`, `CMIPlayerWarnEvent`, `CMIPlayerKickEvent`) to capture bans, warns, and kicks at `MONITOR` priority.

2. **PunishCommandListener** — intercepts player and console commands using regex pattern matching to detect `mute`, `tempmute`, `unmute`, and `unban` commands (both in `/cmi <cmd>` and direct shorthand forms).

All notifications are dispatched **asynchronously** via a dedicated single-thread executor so the server thread is never blocked.

---

## 🔒 Supported Punishment Types

| Type | Trigger |
|---|---|
| `ban` | CMI permanent ban |
| `tempban` | CMI temporary ban |
| `mute` | CMI permanent or temporary mute (via command) |
| `warn` | CMI warn |
| `kick` | CMI kick |
| `unban` | `/unban` or `/cmi unban` command |
| `unmute` | `/unmute` or `/cmi unmute` command |

---

## 🐛 Troubleshooting

**Notifications not sending (webhook mode)**
- Double-check that `webhook.url` is correctly set and is a valid Discord webhook URL.
- Make sure the webhook URL does not contain `YOUR_WEBHOOK`.

**Notifications not sending (discordsrv mode)**
- Confirm DiscordSRV is installed and the bot is connected.
- Ensure `discordsrv.channel_id` is set to a valid channel ID the bot has access to.
- Enable Developer Mode in Discord to copy channel IDs.

**"channel not found" warning in console**
- The bot may not be a member of the server that owns the channel, or the channel ID is wrong.

**Mute/Unmute/Unban not triggering**
- These are command-based. Make sure the command typed matches the expected format: `/mute <player> [time] [reason]`, `/tempmute <player> <time> [reason]`, `/unmute <player>`, `/unban <player>`.

---

## 📁 File Structure

```
plugins/AuroraPunishNotify/
└── config.yml       ← Main configuration file
```

---

## 📜 License

Developed by **Aurora_VN** for [AuroraCraft](https://auroracraft.online).
