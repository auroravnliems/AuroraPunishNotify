package online.auroracraft.aurorapunishnotify.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import online.auroracraft.aurorapunishnotify.AuroraPunishNotify;
import online.auroracraft.aurorapunishnotify.PunishData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishCommandListener implements Listener {

    private final AuroraPunishNotify plugin;

    private static final Pattern MUTE_PATTERN = Pattern.compile(
            "^(?:cmi )?(?:(temp)mute|mute)\\s+(\\S+)(?:\\s+(\\S+))?(?:\\s+(.+))?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UNMUTE_PATTERN = Pattern.compile(
            "^(?:cmi )?unmute\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UNBAN_PATTERN = Pattern.compile(
            "^(?:cmi )?unban\\s+(\\S+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TIME_ARG = Pattern.compile(
            "^(?:\\d+[smhdwMy])+$"
    );
    private static final Pattern TIME_UNIT = Pattern.compile(
            "(\\d+)([smhdwMy])"
    );

    public PunishCommandListener(AuroraPunishNotify plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        processCommand(event.getPlayer(), event.getMessage().substring(1));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        processCommand(event.getSender(), event.getCommand());
    }

    private void processCommand(CommandSender executor, String fullCmd) {
        if (fullCmd.startsWith("/")) fullCmd = fullCmd.substring(1);

        Matcher muteMatcher = MUTE_PATTERN.matcher(fullCmd);
        if (muteMatcher.matches()) {
            boolean isTemp     = muteMatcher.group(1) != null;
            String  targetName = muteMatcher.group(2);
            String  thirdArg   = muteMatcher.group(3);
            String  fourthArg  = muteMatcher.group(4);

            String timeArg, reasonArg;
            if (isTemp) {
                timeArg   = thirdArg;
                reasonArg = fourthArg;
            } else if (isTimeArg(thirdArg)) {
                timeArg   = thirdArg;
                reasonArg = fourthArg;
            } else {
                timeArg   = null;
                reasonArg = thirdArg;
            }

            long  durationMs   = parseTimeMs(timeArg);
            String executorName = executor.getName();
            String fallback     = reasonArg != null && !reasonArg.isBlank() ? reasonArg : "No reason";
            Long   duration     = durationMs > 0 ? durationMs : null;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                OfflinePlayer target = findPlayer(targetName);
                if (target == null) return;

                UUID    uuid    = target.getUniqueId();
                CMIUser cmiUser = CMI.getInstance().getPlayerManager().getUser(uuid);
                if (cmiUser == null || !cmiUser.isMuted()) return;

                plugin.getNotifier().sendAsync(new PunishData(
                        PunishData.Type.MUTE,
                        target.getName() != null ? target.getName() : targetName,
                        uuid.toString().replace("-", ""),
                        executorName,
                        fallback,
                        duration
                ));
            }, 2L);
            return;
        }

        Matcher unmuteMatcher = UNMUTE_PATTERN.matcher(fullCmd);
        if (unmuteMatcher.matches()) {
            String targetName   = unmuteMatcher.group(1);
            String executorName = executor.getName();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                OfflinePlayer target = findPlayer(targetName);
                if (target == null) return;
                UUID uuid = target.getUniqueId();
                plugin.getNotifier().sendAsync(new PunishData(
                        PunishData.Type.UNMUTE,
                        target.getName() != null ? target.getName() : targetName,
                        uuid.toString().replace("-", ""),
                        executorName, "", null
                ));
            }, 2L);
            return;
        }

        Matcher unbanMatcher = UNBAN_PATTERN.matcher(fullCmd);
        if (unbanMatcher.matches()) {
            String targetName   = unbanMatcher.group(1);
            String executorName = executor.getName();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                OfflinePlayer target = findPlayer(targetName);
                if (target == null) return;
                UUID uuid = target.getUniqueId();
                plugin.getNotifier().sendAsync(new PunishData(
                        PunishData.Type.UNBAN,
                        target.getName() != null ? target.getName() : targetName,
                        uuid.toString().replace("-", ""),
                        executorName, "", null
                ));
            }, 2L);
        }
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer findPlayer(String name) {
        OfflinePlayer online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private boolean isTimeArg(String s) {
        return s != null && TIME_ARG.matcher(s).matches();
    }

    private long parseTimeMs(String s) {
        if (s == null || s.isBlank()) return 0;
        Matcher m = TIME_UNIT.matcher(s);
        long ms = 0;
        while (m.find()) {
            long val = Long.parseLong(m.group(1));
            ms += switch (m.group(2)) {
                case "s" -> val * 1_000L;
                case "m" -> val * 60_000L;
                case "h" -> val * 3_600_000L;
                case "d" -> val * 86_400_000L;
                case "w" -> val * 7   * 86_400_000L;
                case "M" -> val * 30  * 86_400_000L;
                case "y" -> val * 365 * 86_400_000L;
                default  -> 0L;
            };
        }
        return ms;
    }
}