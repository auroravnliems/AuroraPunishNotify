package online.auroracraft.aurorapunishnotify;

public class PunishData {

    public enum Type {
        BAN, TEMPBAN, MUTE, WARN, KICK, UNBAN, UNMUTE
    }

    private final Type type;
    private final String playerName;
    private final String playerUUID;
    private final String executor;
    private final String reason;
    private final Long durationMillis;

    public PunishData(Type type, String playerName, String playerUUID,
                      String executor, String reason, Long durationMillis) {
        this.type = type;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.executor = executor;
        this.reason = reason;
        this.durationMillis = durationMillis;
    }

    public Type getType() { return type; }
    public String getPlayerName() { return playerName; }
    public String getPlayerUUID() { return playerUUID; }
    public String getExecutor() { return executor; }
    public String getReason() { return reason; }
    public Long getDurationMillis() { return durationMillis; }

    public String getFormattedDuration(String permanentLabel) {
        if (durationMillis == null || durationMillis <= 0) return permanentLabel;
        long seconds = durationMillis / 1000;
        if (seconds < 60) return seconds + " second";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour";
        long days = hours / 24;
        return days + " day";
    }
}
