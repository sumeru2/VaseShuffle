package com.sumeru.vaseshuffle.listener;

import com.sumeru.vaseshuffle.VaseShuffle;
import com.sumeru.vaseshuffle.game.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

public class EventListener implements Listener  {
    @EventHandler
    public void onVaseSelect(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Minigame game = VaseShuffle.minigame;
        if (game.players.isEmpty()) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.DECORATED_POT) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (player.getPersistentDataContainer().getOrDefault(
                    new NamespacedKey(VaseShuffle.instance, "pressingTime"),
                    PersistentDataType.BOOLEAN,
                    false
            ) && player.getUniqueId().equals(game.players.getFirst().getUniqueId())) {
                if (game.vases.indexOf(clickedBlock) == game.emeraldIndex.get()) {
                    game.round++;
                    player.sendTitle(ChatColor.GOLD + "Round " + game.round + "!", null, 10, 70, 20);

                    if (game.round == 5) {
                        Block newBlock1 = game.vases.get(2).getLocation().clone().add(0.0, 0.0, 2.0).getBlock();
                        game.setTypeAndData(newBlock1);
                        game.vases.add(newBlock1);

                        Block newBlock2 = game.vases.getFirst().getLocation().clone().add(0.0, 0.0, -2.0).getBlock();
                        game.setTypeAndData(newBlock2);
                        game.vases.add(newBlock2);
                    }

                    switch (game.round) {
                        case 4 -> game.n = 2;
                        case 8 -> game.n = 4;
                        case 12 -> game.n = 5;
                    }

                    if (game.round % 3 == 0 && game.velocity < 4) {
                        game.velocity++;
                    }

                    player.getPersistentDataContainer().set(
                            new NamespacedKey(VaseShuffle.instance, "pressingTime"),
                            PersistentDataType.BOOLEAN,
                            false
                    );

                    game.animateEmerald(game.vases.get(game.emeraldIndex.get()));

                    Bukkit.getScheduler().runTaskLater(
                            VaseShuffle.instance,
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (player.isOnline()) {
                                        game.iterations++;
                                        game.round(game.vases, player, 1, game.emeraldIndex.get());
                                    }
                                }
                            },
                            (game.cooldown * 20L)
                    );
                } else {
                    player.sendTitle(ChatColor.DARK_BLUE + "You have lost the game!", null, 10, 70, 20);
                    game.cancelGame(player);
                }
            }
            event.setCancelled(true);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null && event.getClickedBlock().hasMetadata("custom_pot")) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (VaseShuffle.minigame.players.contains(event.getPlayer())) {
            VaseShuffle.minigame.cancelGame(event.getPlayer());
        }
    }
}
