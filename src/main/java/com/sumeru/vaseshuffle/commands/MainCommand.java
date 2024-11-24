package com.sumeru.vaseshuffle.commands;

import com.destroystokyo.paper.Title;
import com.sumeru.vaseshuffle.VaseShuffle;
import com.sumeru.vaseshuffle.game.Minigame;
import com.sumeru.vaseshuffle.utils.Utils;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    public int getNumber(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException err) {
            return 0;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command!").color(NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Commands:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/vshuffle start").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("/vshuffle stop").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("/vshuffle setspawn (Admin Command)").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("start")) {
                if (player.hasPermission("vshuffle.start")) {
                    Minigame game = VaseShuffle.minigame;
                    if (game==null) {
                        player.sendMessage(ChatColor.RED+"The game is not configured!");
                        return false;
                    }
                    if (VaseShuffle.usingViaAPI && Via.getAPI().getPlayerProtocolVersion(player.getUniqueId()).olderThan(ProtocolVersion.v1_20_5)) {
                        sender.sendMessage(ChatColor.RED + "You can't play 'Vase Shuffle' on versions lower than 1.20.6/1.20.5, as legacy versions don't even have a decorated pot!");
                        return false;
                    }

                    if (!game.players.isEmpty()) {
                        sender.sendMessage(Component.text("Another player is already playing this game!").color(NamedTextColor.RED));
                        return false;
                    }

                    player.teleport(game.location);
                    game.players.add(player);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (game.players.contains(player)) {
                                player.sendActionBar(Component.text("Round: " + game.round).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                            } else {
                                cancel();
                            }
                        }
                    }.runTaskTimer(VaseShuffle.instance, 20L, 20L);

                    player.sendMessage(Component.text("Game started!").color(NamedTextColor.DARK_PURPLE));

                    for (int i = 0; i < 3; i++) {
                        Block block = game.location.clone().add(0.0, 0.0, (i - 1) * 2).getBlock();
                        game.setTypeAndData(block);

                        Bukkit.getScheduler().runTaskTimer(VaseShuffle.instance, () -> {
                            ClientboundBlockEventPacket packet = new ClientboundBlockEventPacket(new BlockPos(block.getX(), block.getY(), block.getZ()), Blocks.DECORATED_POT, 1, 1);
                            ((CraftPlayer) player).getHandle().connection.send(packet);
                        }, 50, 50);

                        game.vases.add(block);
                    }

                    game.animateEmerald(game.vases.get(1));

                    new BukkitRunnable() {
                        public int countdown = 3;

                        @Override
                        public void run() {
                            if (player.isOnline()&&game.players.contains(player)) {
                                if (countdown > 0) {
                                    player.sendTitle(Title.builder().title(ChatColor.RED + String.valueOf(countdown)).fadeIn(10).stay(20).fadeOut(10).build());
                                    countdown--;
                                } else {
                                    player.sendTitle(Title.builder().title(ChatColor.GREEN + "START").fadeIn(10).stay(20).fadeOut(10).build());
                                    game.round(game.vases, player, 1, 1);
                                    cancel();
                                }
                            } else {
                                cancel();
                            }
                        }
                    }.runTaskTimer(VaseShuffle.instance, (game.cooldown * 20L) - 60L, 20L);

                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED+"You don't have enough permissions!");
                }
            } else if (args[0].equalsIgnoreCase("stop")){
                if (player.hasPermission("vshuffle.stop")) {
                    Minigame minigame = VaseShuffle.minigame;
                    if (minigame==null) {
                        player.sendMessage(ChatColor.RED+"The game is not configured!");
                        return false;
                    }
                    if (minigame.players.contains(player)) {
                        minigame.cancelGame(player);
                        player.sendMessage(ChatColor.GREEN+"You have successfully logged out!");
                    } else {
                        player.sendMessage(ChatColor.RED+"You are not in the game.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED+"You don't have enough permissions!");
                }
            } else if (args[0].equalsIgnoreCase("setspawn")){
                if (player.hasPermission("vshuffle.setspawn")) {
                    Utils.writeLocation("spawn", player.getLocation());
                    player.sendMessage(ChatColor.GREEN+"The new spawn position of the vases has been successfully installed. Restart the plugin to apply the results!");
                } else {
                    sender.sendMessage(ChatColor.RED+"You don't have enough permissions!");
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED+"Incorrect number of arguments!");
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Arrays.asList("start", "stop", "setspawn");
    }
}
