package online.auroracraft.aurorapunishnotify.notifier;

import online.auroracraft.aurorapunishnotify.AuroraPunishNotify;
import online.auroracraft.aurorapunishnotify.PunishData;
import org.bukkit.configuration.ConfigurationSection;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class DiscordNotifier {

    private final AuroraPunishNotify plugin;
    private final ExecutorService executor;
    private final String mode;

    private static final String[] EMBED_BUILDER_CLASSES = {
            "github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder",
            "net.dv8tion.jda.api.EmbedBuilder"
    };
    private static final String[] MESSAGE_EMBED_CLASSES = {
            "github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed",
            "net.dv8tion.jda.api.entities.MessageEmbed"
    };

    public DiscordNotifier(AuroraPunishNotify plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AuroraPunishNotify-Discord");
            t.setDaemon(true);
            return t;
        });
        this.mode = plugin.getConfig().getString("mode", "webhook").toLowerCase();
    }

    public void sendAsync(PunishData data) {
        executor.submit(() -> {
            try {
                send(data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error sending Discord notification: " + e.getMessage(), e);
            }
        });
    }

    private void send(PunishData data) throws Exception {
        String typeKey = typeToKey(data.getType());
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("notifications." + typeKey);
        if (cfg == null || !cfg.getBoolean("enabled", true)) return;

        String serverName     = plugin.getConfig().getString("server_name", "Server");
        String permanentLabel = plugin.getConfig().getString("permanent_label", "Permanent");
        String avatarTemplate = plugin.getConfig().getString("avatar_url",
                "https://crafatar.com/avatars/{uuid}?size=64&overlay");

        String duration  = data.getFormattedDuration(permanentLabel);
        String avatarUrl = avatarTemplate
                .replace("{uuid}", data.getPlayerUUID())
                .replace("{name}", data.getPlayerName());

        int    color       = cfg.getInt("color", 15158332);
        String title       = applyPlaceholders(cfg.getString("title", typeKey.toUpperCase()), data, serverName, duration);
        String footer      = cfg.getString("footer", "");
        boolean showAvatar = cfg.getBoolean("show_avatar", true);

        List<java.util.Map<?, ?>> fieldsDef = cfg.getMapList("fields");

        if ("discordsrv".equals(mode)) {
            sendViaDiscordSRV(data, title, color, fieldsDef, footer,
                    showAvatar ? avatarUrl : null, serverName, duration);
        } else {
            StringBuilder fieldsJson = new StringBuilder();
            for (var field : fieldsDef) {
                String fName   = applyPlaceholders(String.valueOf(field.get("name")),  data, serverName, duration);
                String fValue  = applyPlaceholders(String.valueOf(field.get("value")), data, serverName, duration);
                boolean inline = field.get("inline") != null && Boolean.parseBoolean(String.valueOf(field.get("inline")));
                if (!fieldsJson.isEmpty()) fieldsJson.append(",");
                fieldsJson.append("{\"name\":").append(jsonString(fName))
                        .append(",\"value\":").append(jsonString(fValue.isEmpty() ? "\u200b" : fValue))
                        .append(",\"inline\":").append(inline).append("}");
            }
            String embedJson = buildEmbedJson(title, color, fieldsJson.toString(), footer,
                    showAvatar ? avatarUrl : null, System.currentTimeMillis() / 1000);
            sendViaWebhook(embedJson);
        }
    }


    private void sendViaWebhook(String embedJson) throws Exception {
        String webhookUrl = plugin.getConfig().getString("webhook.url", "");
        if (webhookUrl.isBlank() || webhookUrl.contains("YOUR_WEBHOOK")) {
            plugin.getLogger().warning("webhook.url is not configured in config.yml!");
            return;
        }
        String botName   = plugin.getConfig().getString("webhook.username", "Punishment System");
        String botAvatar = plugin.getConfig().getString("webhook.avatar_url", "");

        StringBuilder body = new StringBuilder("{");
        body.append("\"username\":").append(jsonString(botName)).append(",");
        if (!botAvatar.isBlank()) {
            body.append("\"avatar_url\":").append(jsonString(botAvatar)).append(",");
        }
        body.append("\"embeds\":[").append(embedJson).append("]}");
        post(webhookUrl, body.toString());
    }


    private void sendViaDiscordSRV(PunishData data, String title, int color,
                                    List<java.util.Map<?, ?>> fieldsDef,
                                    String footer, String thumbnailUrl,
                                    String serverName, String duration) {
        try {
            Class<?> dsrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object   dsrv      = dsrvClass.getMethod("getPlugin").invoke(null);

            Object jda = dsrvClass.getMethod("getJda").invoke(dsrv);
            if (jda == null) {
                plugin.getLogger().warning("DiscordSRV: JDA is not ready yet.");
                return;
            }

            String channelId = plugin.getConfig().getString("discordsrv.channel_id", "").strip();
            if (channelId.isEmpty()) {
                plugin.getLogger().warning("discordsrv.channel_id is not set in config.yml!");
                return;
            }
            Object channel = jda.getClass()
                    .getMethod("getTextChannelById", String.class)
                    .invoke(jda, channelId);
            if (channel == null) {
                plugin.getLogger().warning("DiscordSRV: channel ID '" + channelId + "' not found. " +
                        "Make sure the bot is in the correct server and the ID is right.");
                return;
            }

            Class<?> ebClass = resolveClass(EMBED_BUILDER_CLASSES);
            if (ebClass == null) {
                plugin.getLogger().warning("DiscordSRV: cannot find EmbedBuilder class.");
                return;
            }
            Object eb = ebClass.getDeclaredConstructor().newInstance();

            ebClass.getMethod("setTitle", String.class, String.class).invoke(eb, title, (Object) null);

            ebClass.getMethod("setColor", int.class).invoke(eb, color);

            Method addField = ebClass.getMethod("addField", String.class, String.class, boolean.class);
            for (var field : fieldsDef) {
                String fName   = applyPlaceholders(String.valueOf(field.get("name")),  data, serverName, duration);
                String fValue  = applyPlaceholders(String.valueOf(field.get("value")), data, serverName, duration);
                boolean inline = field.get("inline") != null && Boolean.parseBoolean(String.valueOf(field.get("inline")));
                if (fValue.isEmpty()) fValue = "\u200b";
                addField.invoke(eb, fName, fValue, inline);
            }

            if (footer != null && !footer.isBlank()) {
                ebClass.getMethod("setFooter", String.class).invoke(eb, footer);
            }

            if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                ebClass.getMethod("setThumbnail", String.class).invoke(eb, thumbnailUrl);
            }

            try {
                ebClass.getMethod("setTimestamp", java.time.temporal.TemporalAccessor.class)
                        .invoke(eb, Instant.now());
            } catch (Exception ignored) { /* optional */ }

            Object embed = ebClass.getMethod("build").invoke(eb);

            Class<?> embedClass = resolveClass(MESSAGE_EMBED_CLASSES);
            boolean sent = false;
            if (embedClass != null) {
                try {
                    Object emptyArr = java.lang.reflect.Array.newInstance(embedClass, 0);
                    Object action = channel.getClass()
                            .getMethod("sendMessageEmbeds", embedClass, emptyArr.getClass())
                            .invoke(channel, embed, emptyArr);
                    action.getClass().getMethod("queue").invoke(action);
                    sent = true;
                } catch (NoSuchMethodException ignored) {}
            }
            if (!sent) {
                Object action = channel.getClass()
                        .getMethod("sendMessage", embed.getClass())
                        .invoke(channel, embed);
                action.getClass().getMethod("queue").invoke(action);
            }

        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("DiscordSRV not found. Is it installed?");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "DiscordSRV send error: " + e.getMessage(), e);
        }
    }

    private Class<?> resolveClass(String[] classNames) {
        for (String name : classNames) {
            try { return Class.forName(name); } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }


    private void post(String urlStr, String jsonBody) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("User-Agent", "AuroraPunishNotify/1.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            plugin.getLogger().warning("Discord returned HTTP " + code + " when sending notification.");
        }
        conn.disconnect();
    }


    private String buildEmbedJson(String title, int color, String fieldsJson,
                                   String footer, String thumbnailUrl, long timestamp) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"title\":").append(jsonString(title)).append(",");
        sb.append("\"color\":").append(color).append(",");
        if (fieldsJson != null && !fieldsJson.isBlank()) {
            sb.append("\"fields\":[").append(fieldsJson).append("],");
        }
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            sb.append("\"thumbnail\":{\"url\":").append(jsonString(thumbnailUrl)).append("},");
        }
        if (footer != null && !footer.isBlank()) {
            sb.append("\"footer\":{\"text\":").append(jsonString(footer)).append("},");
        }
        sb.append("\"timestamp\":\"").append(Instant.ofEpochSecond(timestamp)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String applyPlaceholders(String template, PunishData data, String serverName, String duration) {
        if (template == null) return "";
        return template
                .replace("{player}",    data.getPlayerName())
                .replace("{executor}",  data.getExecutor())
                .replace("{reason}",    data.getReason().isEmpty() ? "No reason" : data.getReason())
                .replace("{duration}",  duration)
                .replace("{server}",    serverName)
                .replace("{timestamp}", String.valueOf(System.currentTimeMillis() / 1000));
    }

    private String typeToKey(PunishData.Type type) {
        return switch (type) {
            case BAN    -> "ban";
            case TEMPBAN -> "tempban";
            case MUTE   -> "mute";
            case WARN   -> "warn";
            case KICK   -> "kick";
            case UNBAN  -> "unban";
            case UNMUTE -> "unmute";
        };
    }

    private String jsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
