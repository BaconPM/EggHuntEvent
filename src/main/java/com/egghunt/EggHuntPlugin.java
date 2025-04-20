package com.egghunt;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

public class EggHuntPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> eggCounts = new HashMap<>();
    private final Map<Item, UUID> spawnedEggs = new HashMap<>();
    private boolean eventRunning = false;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        startEggHuntTimer();
    }

    private void startEggHuntTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                startEvent();
            }
        }.runTaskTimer(this, 0L, 20L * 60 * 60); // раз в час (60*60 секунд)
    }

    private void startEvent() {
        if (eventRunning) return;
        eventRunning = true;
        eggCounts.clear();
        spawnedEggs.clear();
        Bukkit.broadcastMessage(ChatColor.GOLD + "[EggHunt] Ивент начался! Собирай яйца в течение 2 минут!");

        new BukkitRunnable() {
            int time = 120;

            @Override
            public void run() {
                if (time <= 0) {
                    endEvent();
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    spawnEggNear(player);
                    updateScoreboard(player);
                }

                time--;
            }
        }.runTaskTimer(this, 0L, 20L); // каждую секунду
    }

    private void endEvent() {
        eventRunning = false;
        Bukkit.broadcastMessage(ChatColor.GREEN + "[EggHunt] Ивент завершён!");

        UUID topPlayer = null;
        int max = 0;
        for (var entry : eggCounts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                topPlayer = entry.getKey();
            }
        }

        if (topPlayer != null) {
            Player winner = Bukkit.getPlayer(topPlayer);
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.AQUA + "Победитель: " + winner.getName() + " с " + max + " яйцами!");
                winner.sendMessage(ChatColor.GOLD + "Ты получил приз за первое место!");

            }
        }

        for (Item item : spawnedEggs.keySet()) {
            item.remove();
        }
        spawnedEggs.clear();
    }

    private void spawnEggNear(Player player) {
        Location base = player.getLocation();
        Random rand = new Random();
        double x = base.getX() + rand.nextInt(60) - 30;
        double y = base.getY();
        double z = base.getZ() + rand.nextInt(60) - 30;
        Location loc = new Location(player.getWorld(), x, y, z);

        ItemStack egg = new ItemStack(Material.EGG);
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Ивентовое яйцо");
        egg.setItemMeta(meta);

        Item dropped = player.getWorld().dropItemNaturally(loc, egg);
        spawnedEggs.put(dropped, null);
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("eggscore", "dummy", ChatColor.YELLOW + "Собрано яиц:");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        eggCounts.forEach((uuid, count) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                obj.getScore(p.getName()).setScore(count);
            }
        });
        player.setScoreboard(board);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (!eventRunning) return;

        if (event.getItem().getItemStack().getType() != Material.EGG) {
            return;
        }

        event.setCancelled(false);
        UUID uuid = event.getPlayer().getUniqueId();
        eggCounts.put(uuid, eggCounts.getOrDefault(uuid, 0) + 1);
        spawnedEggs.remove(event.getItem());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("startegg")) {
            if (!(sender instanceof Player) || sender.isOp()) {
                startEvent();
                sender.sendMessage(ChatColor.GREEN + "Ивент начался вручную!");
            } else {
                sender.sendMessage(ChatColor.RED + "У тебя нет прав.");
            }
            return true;
        }
        return false;
    }
}