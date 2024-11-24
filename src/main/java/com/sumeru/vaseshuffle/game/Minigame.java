package com.sumeru.vaseshuffle.game;

import com.google.common.util.concurrent.AtomicDouble;
import com.sumeru.vaseshuffle.VaseShuffle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.DecoratedPot;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Minigame {
    public final ArrayList<Player> players = new ArrayList<>();
    public final AtomicInteger emeraldIndex = new AtomicInteger(1);
    public final ArrayList<Block> vases = new ArrayList<>();
    public int iterations = 3;
    public World world;
    public Location location;
    public int cooldown;
    public int timeToThink;
    public int round = 1;
    public int n = 1;
    public int velocity = 1;

    public Minigame(World world, Location loc, int cooldown, int timeToThink) {
        this.world = world;
        this.location = loc;
        this.cooldown = cooldown;
        this.timeToThink = timeToThink;
    }

    public void setTypeAndData(Block block) {
        block.setType(Material.DECORATED_POT);
        DecoratedPot state = (DecoratedPot) block.getState();
        state.setSherd(org.bukkit.block.DecoratedPot.Side.FRONT, Material.HEART_POTTERY_SHERD);
        state.setSherd(org.bukkit.block.DecoratedPot.Side.BACK, Material.HEART_POTTERY_SHERD);
        state.setSherd(org.bukkit.block.DecoratedPot.Side.LEFT, Material.HEART_POTTERY_SHERD);
        state.setSherd(org.bukkit.block.DecoratedPot.Side.RIGHT, Material.HEART_POTTERY_SHERD);
        state.update();
        block.setMetadata("custom_pot", new FixedMetadataValue(VaseShuffle.instance, true));
    }
    public void round(ArrayList<Block> vases, Player player, int current, int emeraldI) {
        emeraldIndex.set(emeraldI);
        shuffle(vases, emeraldIndex.get()).thenAccept(emeraldIndex::set).whenComplete((integer, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }
            if (current == iterations) {
                player.sendMessage(Component.text("In which vase is the emerald? You have " + timeToThink + " seconds to right-click it!").color(NamedTextColor.LIGHT_PURPLE));
                player.getPersistentDataContainer().set(new NamespacedKey(VaseShuffle.instance, "pressingTime"), PersistentDataType.BOOLEAN, true);
                Bukkit.getScheduler().scheduleSyncDelayedTask(com.sumeru.vaseshuffle.VaseShuffle.instance, () -> {
                    if (Boolean.TRUE.equals(player.getPersistentDataContainer().get(new NamespacedKey(VaseShuffle.instance, "pressingTime"), PersistentDataType.BOOLEAN))) {
                        player.sendMessage(Component.text("Time is up!").color(NamedTextColor.AQUA));
                        cancelGame(player);
                    }
                }, timeToThink * 20L);
            } else {
                Bukkit.getScheduler().scheduleSyncDelayedTask(VaseShuffle.instance, () -> {
                    if (player.isOnline()&&players.contains(player)) {
                        round(vases, player, current + 1, emeraldIndex.get());
                    }
                }, cooldown * 20L / n);
            }
        });
    }


    public void animateEmerald(Block block) {
        Location itemLocation = block.getLocation().clone().add(0.5D, 1.5D, 0.5D);
        ItemDisplay display2 = (ItemDisplay) block.getWorld().spawnEntity(itemLocation, EntityType.ITEM_DISPLAY);
        display2.setItemStack(new ItemStack(Material.EMERALD, 1));
        display2.getPersistentDataContainer().set(new NamespacedKey(VaseShuffle.instance, "isCustomEmerald"), PersistentDataType.BOOLEAN, true);
        Matrix4f scaleMatrix = new Matrix4f().scale(0.5F);
        Matrix4f translationMatrix = new Matrix4f();
        Matrix4f rotationMatrix = new Matrix4f();

        AtomicDouble y = new AtomicDouble(0.0D);
        AtomicBoolean rotate = new AtomicBoolean(false);
        AtomicInteger n = new AtomicInteger(1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!display2.isValid()) {
                    this.cancel();
                    return;
                }

                if (!rotate.get()) {
                    if (y.get() < 1.5D) {
                        y.addAndGet(0.1D);
                        translationMatrix.translation(0.0F, (float) y.get() * n.get(), 0.0F);
                    } else {
                        rotate.set(true);
                    }
                } else {
                    rotationMatrix.rotateY(0.1F);
                }

                Matrix4f combinedMatrix = new Matrix4f(scaleMatrix).mul(rotationMatrix).mul(translationMatrix);
                display2.setTransformationMatrix(combinedMatrix);
                display2.setInterpolationDelay(0);
                display2.setInterpolationDuration(1);
            }
        }.runTaskTimer(VaseShuffle.instance, 1L, 1L);

        Bukkit.getScheduler().scheduleSyncDelayedTask(VaseShuffle.instance, () -> {
            rotate.set(false);
            y.set(0.0D);
            n.set(-1);
        }, (long) this.cooldown * 10L);

        Bukkit.getScheduler().scheduleSyncDelayedTask(VaseShuffle.instance, display2::remove, (long) this.cooldown * 20L);
    }


    private ItemDisplay spawnItemDisplay(Location loc) {
        return world.spawn(loc.add(0.5D, 0.5D, 0.5D), ItemDisplay.class, display -> {
            ItemStack stack = new ItemStack(Material.DECORATED_POT);
            ItemMeta meta = stack.getItemMeta();
            if (meta instanceof BlockStateMeta) {
                DecoratedPot state = (DecoratedPot) ((BlockStateMeta) meta).getBlockState();
                state.setSherd(org.bukkit.block.DecoratedPot.Side.FRONT, Material.HEART_POTTERY_SHERD);
                state.setSherd(org.bukkit.block.DecoratedPot.Side.BACK, Material.HEART_POTTERY_SHERD);
                state.setSherd(org.bukkit.block.DecoratedPot.Side.LEFT, Material.HEART_POTTERY_SHERD);
                state.setSherd(org.bukkit.block.DecoratedPot.Side.RIGHT, Material.HEART_POTTERY_SHERD);
                state.update();
                ((BlockStateMeta) meta).setBlockState(state);
                stack.setItemMeta(meta);
            }
            display.getPersistentDataContainer().set(new NamespacedKey(VaseShuffle.instance, "isCustomPot"), PersistentDataType.BOOLEAN, true);
            display.setGlowing(true);
            display.setGlowColorOverride(Color.GREEN);
            display.setItemStack(stack);
        }, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    public void cancelGame(Player player) {
        player.getPersistentDataContainer().set(new NamespacedKey(VaseShuffle.instance, "pressingTime"), PersistentDataType.BOOLEAN, false);
        vases.forEach(block -> {
            block.setType(Material.AIR);
            block.setBlockData(Material.AIR.createBlockData());
            block.getState().update();
        });

        world.getEntitiesByClass(ItemDisplay.class).stream()
                .filter(entity -> entity.getItemStack() != null && entity.getItemStack().getType() == Material.DECORATED_POT)
                .filter(entity -> entity.getPersistentDataContainer().has(new NamespacedKey(VaseShuffle.instance, "isCustomPot")))
                .forEach(Entity::remove);

        world.getEntitiesByClass(ItemDisplay.class).stream()
                .filter(entity -> entity.getItemStack() != null && entity.getItemStack().getType() == Material.EMERALD)
                .filter(entity -> entity.getPersistentDataContainer().has(new NamespacedKey(VaseShuffle.instance, "isCustomEmerald")))
                .forEach(Entity::remove);

        players.remove(player);
        round = 1;
        vases.clear();
        velocity = 1;
        emeraldIndex.set(1);
        iterations = 3;
    }

    private CompletableFuture<Integer> shuffle(ArrayList<Block> vases, int emeraldIndex) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        if (!vases.isEmpty() && !players.isEmpty()) {
            int i = ThreadLocalRandom.current().nextInt(vases.size());
            final Block from = vases.get(i);
            final Block to = (i == 0 && vases.size() == 3) ? vases.get(1) :
                    (i == 0) ? vases.get(4) :
                            (i == 1) ? vases.get(ThreadLocalRandom.current().nextInt(2) * 2) :
                                    (i == 2 && vases.size() == 3) ? vases.get(1) :
                                            (i == 2 && vases.size() == 5) ? vases.get(3) :
                                                    (i == 3) ? vases.get(2) :
                                                            vases.get(0);
            final ItemDisplay display = spawnItemDisplay(from.getLocation());
            final ItemDisplay display2 = spawnItemDisplay(to.getLocation());

            final boolean shuffleFrom = from.getZ() < to.getZ();
            final boolean shuffleTo = to.getZ() < from.getZ();
            final float angle = 45.0F;
            final AtomicDouble g = new AtomicDouble(0.8D);
            final float initialVelocity = 3.0F;
            final AtomicDouble t = new AtomicDouble(0.0D);
            Bukkit.getScheduler().scheduleSyncDelayedTask(VaseShuffle.instance, () -> {
                from.setType(Material.AIR);
                to.setType(Material.AIR);
            }, 2);

            final Transformation transformation = display.getTransformation();
            final Transformation transformation2 = display2.getTransformation();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (display.isValid() && display2.isValid()) {
                        double x = initialVelocity * t.get() * Math.cos(Math.toRadians(angle));
                        double y = initialVelocity * t.get() * Math.sin(Math.toRadians(angle)) + 0.5D * g.get() * t.get() * t.get();

                        Location loc = from.getLocation().clone().add(y, 0.0D, shuffleFrom ? x : -x);
                        Location loc2 = to.getLocation().clone().add(-y, 0.0D, shuffleTo ? x : -x);

                        loc.setX(Math.clamp(loc.getX(), from.getLocation().getBlockX(), to.getLocation().getBlockX() + 2));
                        loc2.setX(Math.clamp(loc2.getX(), to.getLocation().getBlockX() - 2, from.getLocation().getBlockX()));

                        double z = loc.getZ();
                        double zClamped = shuffleFrom
                                ? Math.clamp(
                                z,
                                from.getLocation().getBlockZ(),
                                to.getLocation().getBlockZ()
                        )
                                : Math.clamp(
                                z,
                                to.getLocation().getBlockZ(),
                                from.getLocation().getBlockZ()
                        );

                        loc.setZ(zClamped);

                        double z2 = loc2.getZ();
                        double zClamped2 = shuffleFrom
                                ? Math.clamp(
                                z2,
                                from.getLocation().getBlockZ(),
                                to.getLocation().getBlockZ()
                        )
                                : Math.clamp(
                                z2,
                                to.getLocation().getBlockZ(),
                                from.getLocation().getBlockZ()
                        );

                        loc2.setZ(zClamped2);

                        Vector3f translation = new Vector3f((float) (loc.getX() - from.getLocation().getX()), 0.0F, (float) (loc.getZ() - from.getLocation().getZ()));
                        Vector3f translation2 = new Vector3f((float) (loc2.getX() - to.getLocation().getX()), 0.0F, (float) (loc2.getZ() - to.getLocation().getZ()));

                        if ((loc.getBlockX() >= from.getLocation().getBlockX() && loc2.getBlockX() <= from.getLocation().getBlockX()) &&
                                (shuffleFrom ? to.getLocation().getBlockZ() >= loc.getBlockZ() : from.getLocation().getBlockZ() >= loc2.getBlockZ())) {

                            transformation.getTranslation().set(translation);
                            transformation2.getTranslation().set(translation2);
                            display.setTransformation(transformation);
                            display.setInterpolationDelay(0);
                            display.setInterpolationDuration(1);
                            display2.setInterpolationDelay(0);
                            display2.setInterpolationDuration(1);
                            display2.setTransformation(transformation2);
                        }

                        t.set(t.get() + 0.025D);
                        g.set(g.get() - 0.125D);
                    } else {
                        cancel();
                    }
                }
            }.runTaskTimer(VaseShuffle.instance, 2L, 1L);

            Bukkit.getScheduler().scheduleSyncDelayedTask(com.sumeru.vaseshuffle.VaseShuffle.instance, () -> {

                if (!players.isEmpty()) {
                    setTypeAndData(to);
                    setTypeAndData(from);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(VaseShuffle.instance, () -> {
                        display.remove();
                        display2.remove();
                    }, 10);
                    if (emeraldIndex == vases.indexOf(from)) {
                        future.complete(vases.indexOf(to));
                    } else if (emeraldIndex == vases.indexOf(to)) {
                        future.complete(vases.indexOf(from));
                    } else {
                        future.complete(emeraldIndex);
                    }
                } else {
                    display.remove();
                    display2.remove();
                    if (!future.isCancelled()) future.complete(null);
                }
            }, 43);

        } else {
            future.complete(null);
        }
        return future;
    }

}