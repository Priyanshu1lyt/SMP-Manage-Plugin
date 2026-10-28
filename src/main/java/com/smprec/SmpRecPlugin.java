package com.smprec;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SmpRecPlugin extends JavaPlugin implements Listener, TabExecutor {

    private final Map<String, String> bans = new HashMap<>();
    private final Set<String> muted = new HashSet<>();
    private final Set<UUID> immortal = new HashSet<>();
    private boolean chatEnabled = true;
    private boolean joinLeaveEnabled = true;
    private File dataFile;

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "data.yml");
        load();
        for (String c : List.of("ban", "unban", "mute", "unmute", "chat", "immortal", "joinleave")) {
            var cmd = getCommand(c);
            if (cmd != null) {
                cmd.setExecutor(this);
                cmd.setTabCompleter(this);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("SmpRecording enabled!");
    }

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(dataFile);
        chatEnabled = y.getBoolean("chat", true);
        joinLeaveEnabled = y.getBoolean("joinleave", true);
        muted.addAll(y.getStringList("muted"));
        var sec = y.getConfigurationSection("bans");
        if (sec != null) {
            for (String k : sec.getKeys(false)) bans.put(k, sec.getString(k, "Banned"));
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("chat", chatEnabled);
        y.set("joinleave", joinLeaveEnabled);
        y.set("muted", new ArrayList<>(muted));
        for (var e : bans.entrySet()) y.set("bans." + e.getKey(), e.getValue());
        try {
            getDataFolder().mkdirs();
            y.save(dataFile);
        } catch (IOException ex) {
            getLogger().warning("Could not save data: " + ex.getMessage());
        }
    }

    private void msg(CommandSender s, String text, NamedTextColor color) {
        s.sendMessage(Component.text(text, color));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        switch (cmd.getName().toLowerCase()) {
            case "ban" -> {
                if (a.length < 1) { msg(s, "Usage: /ban <player> [reason]", NamedTextColor.RED); return true; }
                String reason = a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : "You have been banned.";
                bans.put(a[0].toLowerCase(), reason);
                save();
                Player p = Bukkit.getPlayerExact(a[0]);
                if (p != null) p.kick(Component.text("Banned: " + reason, NamedTextColor.RED));
                msg(s, a[0] + " banned. Reason: " + reason, NamedTextColor.GREEN);
            }
            case "unban" -> {
                if (a.length < 1) { msg(s, "Usage: /unban <player>", NamedTextColor.RED); return true; }
                if (bans.remove(a[0].toLowerCase()) != null) {
                    save();
                    msg(s, a[0] + " unbanned.", NamedTextColor.GREEN);
                } else msg(s, a[0] + " is not banned.", NamedTextColor.RED);
            }
            case "mute" -> {
                if (a.length < 1) { msg(s, "Usage: /mute <player>", NamedTextColor.RED); return true; }
                muted.add(a[0].toLowerCase());
                save();
                msg(s, a[0] + " muted.", NamedTextColor.GREEN);
                Player p = Bukkit.getPlayerExact(a[0]);
                if (p != null) msg(p, "You have been muted.", NamedTextColor.RED);
            }
            case "unmute" -> {
                if (a.length < 1) { msg(s, "Usage: /unmute <player>", NamedTextColor.RED); return true; }
                if (muted.remove(a[0].toLowerCase())) {
                    save();
                    msg(s, a[0] + " unmuted.", NamedTextColor.GREEN);
                    Player p = Bukkit.getPlayerExact(a[0]);
                    if (p != null) msg(p, "You have been unmuted.", NamedTextColor.GREEN);
                } else msg(s, a[0] + " is not muted.", NamedTextColor.RED);
            }
            case "chat" -> {
                if (a.length < 1 || !(a[0].equalsIgnoreCase("on") || a[0].equalsIgnoreCase("off"))) {
                    msg(s, "Usage: /chat <on|off>", NamedTextColor.RED); return true;
                }
                chatEnabled = a[0].equalsIgnoreCase("on");
                save();
                Bukkit.broadcast(Component.text("Chat has been turned " + (chatEnabled ? "ON" : "OFF") + ".",
                        chatEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            case "immortal" -> {
                if (a.length < 1) { msg(s, "Usage: /immortal <player>", NamedTextColor.RED); return true; }
                Player p = Bukkit.getPlayerExact(a[0]);
                if (p == null) { msg(s, "Player not online.", NamedTextColor.RED); return true; }
                if (immortal.remove(p.getUniqueId())) {
                    msg(s, p.getName() + " is no longer immortal.", NamedTextColor.YELLOW);
                    msg(p, "Immortal mode OFF.", NamedTextColor.YELLOW);
                } else {
                    immortal.add(p.getUniqueId());
                    msg(s, p.getName() + " is now IMMORTAL.", NamedTextColor.GREEN);
                    msg(p, "Immortal mode ON - you cannot die (minimum half heart).", NamedTextColor.GOLD);
                }
            }
            case "joinleave" -> {
                if (a.length < 1 || !(a[0].equalsIgnoreCase("on") || a[0].equalsIgnoreCase("off"))) {
                    msg(s, "Usage: /joinleave <on|off>", NamedTextColor.RED); return true;
                }
                joinLeaveEnabled = a[0].equalsIgnoreCase("on");
                save();
                msg(s, "Join/Leave messages " + (joinLeaveEnabled ? "ON" : "OFF") + ".", NamedTextColor.GREEN);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length != 1) return List.of();
        String n = cmd.getName().toLowerCase();
        String prefix = a[0].toLowerCase();
        List<String> out = new ArrayList<>();
        if (n.equals("chat") || n.equals("joinleave")) {
            out.addAll(List.of("on", "off"));
        } else if (n.equals("unban")) {
            out.addAll(bans.keySet());
        } else if (n.equals("unmute")) {
            out.addAll(muted);
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        }
        out.removeIf(x -> !x.toLowerCase().startsWith(prefix));
        return out;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        String reason = bans.get(e.getName().toLowerCase());
        if (reason != null) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    Component.text("You are banned.\nReason: " + reason, NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (muted.contains(p.getName().toLowerCase())) {
            e.setCancelled(true);
            msg(p, "You are muted.", NamedTextColor.RED);
        } else if (!chatEnabled && !p.hasPermission("smprec.admin")) {
            e.setCancelled(true);
            msg(p, "Chat is currently turned off.", NamedTextColor.RED);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!immortal.contains(p.getUniqueId())) return;
        // Half heart = 1.0 HP. Damage allowed until 1 HP, never below.
        if (p.getHealth() - e.getFinalDamage() < 1.0) {
            e.setCancelled(true);
            if (p.getHealth() > 1.0) p.setHealth(1.0);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!joinLeaveEnabled) e.joinMessage(null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!joinLeaveEnabled) e.quitMessage(null);
        immortal.remove(e.getPlayer().getUniqueId());
    }
                  }
