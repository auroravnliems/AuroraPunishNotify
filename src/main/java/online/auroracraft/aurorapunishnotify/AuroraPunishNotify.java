package online.auroracraft.aurorapunishnotify;

import online.auroracraft.aurorapunishnotify.listeners.CMIPunishListener;
import online.auroracraft.aurorapunishnotify.listeners.PunishCommandListener;
import online.auroracraft.aurorapunishnotify.notifier.DiscordNotifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class AuroraPunishNotify extends JavaPlugin {

    private static AuroraPunishNotify instance;
    private DiscordNotifier notifier;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        notifier = new DiscordNotifier(this);

        getServer().getPluginManager().registerEvents(new CMIPunishListener(this), this);
        getServer().getPluginManager().registerEvents(new PunishCommandListener(this), this);

        getLogger().info("AuroraPunishNotify enabled! Mode: " + getConfig().getString("mode", "webhook"));
    }

    @Override
    public void onDisable() {
        notifier.shutdown();
        getLogger().info("AuroraPunishNotify disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("aurorapunish")) return false;
        if (!sender.hasPermission("aurorapunishnotify.admin")) {
            sender.sendMessage("§cNo Permission!");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            notifier = new DiscordNotifier(this);
            sender.sendMessage("§a[AuroraPunishNotify] §fConfig Reloaded!");
        } else {
            sender.sendMessage("§a[AuroraPunishNotify] §fv" + getDescription().getVersion());
            sender.sendMessage("§7Dung: /apn reload");
        }
        return true;
    }

    public static AuroraPunishNotify getInstance() {
        return instance;
    }

    public DiscordNotifier getNotifier() {
        return notifier;
    }
}
