package online.auroracraft.aurorapunishnotify.listeners;

import com.Zrips.CMI.events.CMIPlayerBanEvent;
import com.Zrips.CMI.events.CMIPlayerKickEvent;
import com.Zrips.CMI.events.CMIPlayerWarnEvent;
import online.auroracraft.aurorapunishnotify.AuroraPunishNotify;
import online.auroracraft.aurorapunishnotify.PunishData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class CMIPunishListener implements Listener {

    private final AuroraPunishNotify plugin;

    public CMIPunishListener(AuroraPunishNotify plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBan(CMIPlayerBanEvent event) {
        String playerUUID = event.getBanned().toString().replace("-", "");
        String playerName = nameFromUUID(event.getBanned());
        String executor   = senderName(event.getBannedBy());
        String reason     = event.getReason() != null ? event.getReason() : "No reason";

        Long until = event.getUntil();
        PunishData.Type type;
        Long durationMs;
        if (until == null || until <= 0) {
            type = PunishData.Type.BAN;
            durationMs = null;
        } else {
            type = PunishData.Type.TEMPBAN;
            durationMs = until - System.currentTimeMillis();
            if (durationMs < 0) durationMs = null;
        }

        plugin.getNotifier().sendAsync(
                new PunishData(type, playerName, playerUUID, executor, reason, durationMs));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarn(CMIPlayerWarnEvent event) {
        String playerUUID = event.getUser().getUniqueId().toString().replace("-", "");
        String playerName = event.getUser().getName();
        String executor   = event.getWarning().getGivenBy() != null
                ? event.getWarning().getGivenBy() : "Console";
        String reason     = event.getWarning().getReason() != null
                ? event.getWarning().getReason() : "No reason";

        plugin.getNotifier().sendAsync(
                new PunishData(PunishData.Type.WARN, playerName, playerUUID, executor, reason, null));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(CMIPlayerKickEvent event) {
        String playerUUID = event.getBanned().toString().replace("-", "");
        String playerName = nameFromUUID(event.getBanned());
        String executor   = senderName(event.getBannedBy());
        String reason     = event.getReason() != null ? stripColor(event.getReason()) : "No reason";

        plugin.getNotifier().sendAsync(
                new PunishData(PunishData.Type.KICK, playerName, playerUUID, executor, reason, null));
    }


    private String senderName(CommandSender sender) {
        return sender == null ? "Console" : sender.getName();
    }

    private String nameFromUUID(UUID uuid) {
        try {
            var p = Bukkit.getOfflinePlayer(uuid);
            return p.getName() != null ? p.getName() : uuid.toString().substring(0, 8);
        } catch (Exception e) {
            return uuid.toString().substring(0, 8);
        }
    }

    private String stripColor(String s) {
        return s.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
    }
}
