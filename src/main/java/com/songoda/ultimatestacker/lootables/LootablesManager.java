package com.songoda.ultimatestacker.lootables;

import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.lootables.Lootables;
import com.songoda.core.lootables.Modify;
import com.songoda.core.lootables.loot.*;
import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.settings.Settings;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class LootablesManager {

    private final Lootables lootables;

    private final LootManager lootManager;

    private final String lootablesDir = UltimateStacker.getInstance().getDataFolder() + File.separator + "lootables";
    private final static int MAX_INT = Integer.MAX_VALUE/2;

    public LootablesManager() {
        this.lootables = new Lootables(lootablesDir);
        this.lootManager = new LootManager(lootables);
    }

    public List<Drop> getDrops(LivingEntity entity) {
        List<Drop> toDrop = new ArrayList<>();

        if (entity instanceof Ageable && !((Ageable) entity).isAdult() && !(entity instanceof Zombie)
                || !lootManager.getRegisteredLootables().containsKey(entity.getType().name())) return toDrop;

        Lootable lootable = lootManager.getRegisteredLootables().get(entity.getType().name());
        int looting = entity.getKiller() != null
                && entity.getKiller().getItemInHand().containsEnchantment(Enchantment.LOOTING)
                ? entity.getKiller().getItemInHand().getEnchantmentLevel(Enchantment.LOOTING)
                : 0;

        int rerollChance = Settings.REROLL.getBoolean() ? looting / (looting + 1) : 0;

        for (Loot loot : lootable.getRegisteredLoot())
            toDrop.addAll(runLoot(entity, loot, rerollChance, looting));

        //Apply SuperiorSkyblock2 mob-drops multiplier if present
        /*if (superiorSkyblock2Hook.isEnabled()) {
            for (Drop drop : toDrop) {
                if (drop.getItemStack() == null) continue; //Maybe it is just exp
                drop.getItemStack().setAmount(superiorSkyblock2Hook.getDropMultiplier(entity.getLocation()) * drop.getItemStack().getAmount());
            }
        }*/

        return toDrop;
    }

    private List<Drop> runLoot(LivingEntity entity, Loot loot, int rerollChance, int looting) {
        Modify modify = null;
        if (entity instanceof Sheep) {
            modify = (Loot loot2) -> {
                Material material = loot2.getMaterial();
                if (material != null && material.name().contains("WOOL") && ((Sheep) entity).getColor() != null) {
                    if (!((Sheep) entity).readyToBeSheared()) return null;
                    if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13))
                        loot2.setMaterial(Material.valueOf(((Sheep) entity).getColor() + "_WOOL"));

                }
                return loot2;
            };
        }
        EntityType killer = null;
        Entity killerEntity = null;
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            killerEntity = ((EntityDamageByEntityEvent) entity.getLastDamageCause()).getDamager();
            killer = killerEntity.getType();
            if (killerEntity instanceof Projectile) {
                Projectile projectile = (Projectile) killerEntity;
                if (projectile.getShooter() instanceof Entity) {
                    killerEntity = ((Entity) projectile.getShooter());
                    killer = killerEntity.getType();
                }
            }
        }
        return lootManager.runLoot(modify,
                entity.getFireTicks() > 0,
                killerEntity instanceof Creeper && ((Creeper) killerEntity).isPowered(),
                entity.getKiller() != null ? entity.getKiller().getItemInHand() : null,
                killer,
                loot,
                rerollChance,
                looting);
    }

    public List<ItemStack> getItemStackDrops(LivingEntity entity, int times) {
        return getDrops(entity, times).stream().map(Drop::getItemStack).collect(Collectors.toList());
    }

    public List<Drop> getDrops(LivingEntity entity, int times) {
        return getDrops(entity, times, 3);
    }

    public List<Drop> getDrops(LivingEntity entity, int times, int attempts) {
        attempts--;
        List<Drop> toDrop = new ArrayList<>();
        if (entity instanceof Ageable && !((Ageable) entity).isAdult() && !(entity instanceof Zombie)
                || !lootManager.getRegisteredLootables().containsKey(entity.getType().name())) return toDrop;

        Lootable lootable = lootManager.getRegisteredLootables().get(entity.getType().name());
        int looting = entity.getKiller() != null
                && entity.getKiller().getItemInHand().containsEnchantment(Enchantment.LOOTING)
                ? entity.getKiller().getItemInHand().getEnchantmentLevel(Enchantment.LOOTING)
                : 0;

        double extraChance = looting / (looting + 1.0);

        boolean isCharged = entity instanceof Creeper && ((Creeper) entity).isPowered();

        //Run main loot
        for (Loot loot : lootable.getRegisteredLoot()) {
            if (loot.isRequireCharged() && !isCharged) continue;
            if (!loot.getOnlyDropFor().isEmpty() && loot.getOnlyDropFor().stream().noneMatch(type -> entity.getKiller() != null && type == entity.getKiller().getType())) continue;
            int finalLooting = loot.isAllowLootingEnchant() ? looting : 0;

            long max = (long) (((long) (loot.getMax() + finalLooting) * times) * (loot.getChance()/100 + (loot.isAllowLootingEnchant() ? extraChance : 0)));
            long min = (long) ((loot.getMin()) * times * (loot.getChance()/100));

            long amount = ThreadLocalRandom.current().nextLong((max - min) + 1) + min;

            if (loot.getMaterial() != null && amount > 0) {
                ItemStack item = new ItemStack(entity.getFireTicks() > 0
                        ? loot.getBurnedMaterial() != null ? loot.getBurnedMaterial() : loot.getMaterial()
                        : loot.getMaterial());
                if (amount > MAX_INT) {
                    while (amount > MAX_INT) {
                        ItemStack loop = item.clone();
                        loop.setAmount(MAX_INT);
                        amount -= MAX_INT;
                        toDrop.add(new Drop(loop));
                    }
                }
                //Leftover
                item.setAmount((int) amount);
                toDrop.add(new Drop(item));
            }


            //Run child loot //TODO: remove duplicated code
            for (Loot child : loot.getChildLoot()) {
                if (child.isRequireCharged() && !isCharged) continue;
                if (child.getOnlyDropFor().size() != 0 && child.getOnlyDropFor().stream().noneMatch(type -> entity.getKiller() != null && type == entity.getKiller().getType())) continue;

                int choildFinalLooting = child.isAllowLootingEnchant() ? looting : 0;

                long childMax = (long) (((long) (child.getMax() + finalLooting) * times) * (child.getChance()/100 + (child.isAllowLootingEnchant() ? extraChance : 0)));
                long childmin = (long) ((child.getMin()) * times * (child.getChance()/100));
                childmin = (long) (childmin - childmin*0.90);


                long childamount = ThreadLocalRandom.current().nextLong((childMax - childmin) + 1) + childmin;

                if (childamount > 0) {
                    ItemStack item = new ItemStack(entity.getFireTicks() > 0
                            ? child.getBurnedMaterial() != null ? child.getBurnedMaterial() : child.getMaterial()
                            : child.getMaterial());
                    if (childamount > MAX_INT) {
                        while (childamount > MAX_INT) {
                            ItemStack loop = item.clone();
                            loop.setAmount(MAX_INT);
                            childamount -= MAX_INT;
                            toDrop.add(new Drop(loop));
                        }
                    }
                    //Leftover
                    item.setAmount((int) childamount);
                    toDrop.add(new Drop(item));
                }
            }
        }

        if (toDrop.isEmpty() && attempts > 0) {
            return getDrops(entity, times, attempts);
        }
        return toDrop;
    }


    public void createDefaultLootables() {
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_17)) {
            // Add Glow Squid.
            lootManager.addLootable(new Lootable("GLOW_SQUID",
                    new LootBuilder()
                            .setMaterial(Material.GLOW_INK_SAC)
                            .setMin(1)
                            .setMax(3).build()));

            // Add Glow Squid.
            lootManager.addLootable(new Lootable("SQUID",
                    new LootBuilder()
                            .setMaterial(Material.GLOW_INK_SAC)
                            .setMin(1)
                            .setMax(3).build()));
        }
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_14)) {
            // Add Trader Llama.
            lootManager.addLootable(new Lootable("TRADER_LLAMA",
                    new LootBuilder()
                            .setMaterial(Material.LEATHER)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Pillager.
            lootManager.addLootable(new Lootable("PILLAGER",
                    new LootBuilder()
                            .setMaterial(Material.ARROW)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Ravager.
            lootManager.addLootable(new Lootable("RAVAGER",
                    new LootBuilder()
                            .setMaterial(Material.SADDLE).build()));

            // Add Cat.
            lootManager.addLootable(new Lootable("CAT",
                    new LootBuilder()
                            .setMaterial(Material.STRING).build()));

            // Add Panda.
            lootManager.addLootable(new Lootable("PANDA",
                    new LootBuilder()
                            .setMaterial(Material.BAMBOO)
                            .setMin(0)
                            .setMax(2).build()));
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {

            // Add Phantom.
            lootManager.addLootable(new Lootable("PHANTOM",
                    new LootBuilder()
                            .setMaterial(Material.PHANTOM_MEMBRANE)
                            .setMin(0)
                            .setMax(1)
                            .addOnlyDropFors(EntityType.PLAYER).build()));

            // Add Pufferfish.
            lootManager.addLootable(new Lootable("PUFFERFISH",
                    new LootBuilder()
                            .setMaterial(Material.PUFFERFISH).build(),
                    new LootBuilder()
                            .setMaterial(Material.BONE_MEAL)
                            .setChance(5).build()));

            // Add Salmon.
            lootManager.addLootable(new Lootable("SALMON",
                    new LootBuilder()
                            .setMaterial(Material.SALMON)
                            .setBurnedMaterial(Material.COOKED_SALMON).build(),
                    new LootBuilder()
                            .setMaterial(Material.BONE_MEAL)
                            .setChance(5).build()));

            // Add Tropical Fish.
            lootManager.addLootable(new Lootable("TROPICAL_FISH",
                    new LootBuilder()
                            .setMaterial(Material.TROPICAL_FISH).build(),
                    new LootBuilder()
                            .setMaterial(Material.BONE_MEAL)
                            .setChance(5).build(),
                    new LootBuilder()
                            .setMaterial(Material.BONE)
                            .setMin(1)
                            .setMax(2)
                            .setChance(25)
                            .addOnlyDropFors(EntityType.PLAYER).build()));

            // Add Dolphin.
            lootManager.addLootable(new Lootable("DOLPHIN",
                    new LootBuilder()
                            .setMaterial(Material.COD)
                            .setBurnedMaterial(Material.COOKED_COD)
                            .setMin(0)
                            .setMax(1).build()));

            // Add Cod.
            lootManager.addLootable(new Lootable("COD",
                    new LootBuilder()
                            .setMaterial(Material.COD)
                            .setBurnedMaterial(Material.COOKED_COD).build(),
                    new LootBuilder()
                            .setMaterial(Material.BONE_MEAL)
                            .setChance(5).build()));

            // Add Turtle.
            lootManager.addLootable(new Lootable("TURTLE",
                    new LootBuilder()
                            .setMaterial(Material.SEAGRASS)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Drowned.
            lootManager.addLootable(new Lootable("DROWNED",
                    new LootBuilder()
                            .setMaterial(Material.ROTTEN_FLESH)
                            .setMin(0)
                            .setMax(2).build(),
                    new LootBuilder()
                            .setMaterial(Material.GOLD_INGOT)
                            .setChance(5)
                            .addOnlyDropFors(EntityType.PLAYER).build()));
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) {
            // Add Parrot.
            lootManager.addLootable(new Lootable("PARROT",
                    new LootBuilder()
                            .setMaterial(Material.FEATHER)
                            .setMin(1)
                            .setMax(2).build()));
        }


        Loot fish1 = new LootBuilder()
                .addChildLoot(new LootBuilder()
                                .setMaterial(Material.COD)
                                .setBurnedMaterial(Material.COOKED_COD)
                                .setChance(50).build(),
                        new LootBuilder()
                                .setMaterial(Material.PRISMARINE_CRYSTALS)
                                .setChance(33).build())
                .build();

        Loot fish2 = new LootBuilder()
                .setChance(2.5)
                .addChildLoot(new LootBuilder()
                                .setMaterial(Material.COD)
                                .setChance(60)
                                .setAllowLootingEnchant(false).build(),
                        new LootBuilder()
                                .setMaterial(Material.SALMON)
                                .setChance(25)
                                .setAllowLootingEnchant(false).build(),
                        new LootBuilder()
                                .setMaterial(Material.PUFFERFISH)
                                .setChance(13)
                                .setAllowLootingEnchant(false).build(),
                        new LootBuilder()
                                .setMaterial(Material.TROPICAL_FISH)
                                .setChance(2)
                                .setAllowLootingEnchant(false).build())
                .addOnlyDropFors(EntityType.PLAYER).build();

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
            // Add Zombie Villager.
            lootManager.addLootable(new Lootable("ZOMBIE_VILLAGER",
                    new LootBuilder()
                            .setMaterial(Material.ROTTEN_FLESH)
                            .setMin(0)
                            .setMax(2).build(),
                    new LootBuilder()
                            .setChance(2.5)
                            .setChildDropCount(1)
                            .addOnlyDropFors(EntityType.PLAYER)
                            .addChildLoot(new LootBuilder().setMaterial(Material.IRON_INGOT)
                                            .setAllowLootingEnchant(false).build(),
                                    new LootBuilder().setMaterial(Material.CARROT)
                                            .setAllowLootingEnchant(false).build(),
                                    new LootBuilder().setMaterial(Material.POTATO)
                                            .setAllowLootingEnchant(false).build())
                            .build()));

            // Add Llama.
            lootManager.addLootable(new Lootable("LLAMA",
                    new LootBuilder()
                            .setMaterial(Material.LEATHER)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Zombie Horse.
            lootManager.addLootable(new Lootable("ZOMBIE_HORSE",
                    new LootBuilder()
                            .setMaterial(Material.ROTTEN_FLESH)
                            .setMin(0)
                            .setMax(2).build()));
            // Add Elder Guardian.
            lootManager.addLootable(new Lootable("ELDER_GUARDIAN",
                    new LootBuilder()
                            .setMaterial(Material.PRISMARINE_SHARD)
                            .setMin(0)
                            .setMax(2).build(),
                    fish1,
                    new LootBuilder()
                            .setMaterial(Material.SPONGE)
                            .addOnlyDropFors(EntityType.PLAYER)
                            .setAllowLootingEnchant(false).build(),
                    fish2));

            // Add Mule.
            lootManager.addLootable(new Lootable("MULE",
                    new LootBuilder()
                            .setMaterial(Material.LEATHER)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Stray.
            lootManager.addLootable(new Lootable("STRAY",
                    new LootBuilder()
                            .setMaterial(Material.ARROW)
                            .setMin(0)
                            .setMax(2).build(),
                    new LootBuilder()
                            .setMaterial(Material.BONE)
                            .setMin(0)
                            .setMax(2).build()));

            Loot witherSkull = new LootBuilder()
                    .setMaterial(Material.WITHER_SKELETON_SKULL)
                    .setChance(2.5)
                    .setAllowLootingEnchant(false)
                    .addOnlyDropFors(EntityType.PLAYER).build();

            // Add Wither Skeleton.
            lootManager.addLootable(new Lootable("WITHER_SKELETON",
                    new LootBuilder()
                            .setMaterial(Material.COAL)
                            .setChance(33).build(),
                    new LootBuilder()
                            .setMaterial(Material.BONE)
                            .setMin(0)
                            .setMax(2).build(),
                    witherSkull));        // Add Skeleton Horse.
            lootManager.addLootable(new Lootable("SKELETON_HORSE",
                    new LootBuilder()
                            .setMaterial(Material.BONE)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Donkey.
            lootManager.addLootable(new Lootable("DONKEY",
                    new LootBuilder()
                            .setMaterial(Material.LEATHER)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Vindicator.
            lootManager.addLootable(new Lootable("VINDICATOR",
                    new LootBuilder()
                            .setMaterial(Material.EMERALD)
                            .setMin(0)
                            .setMax(1)
                            .addOnlyDropFors(EntityType.PLAYER).build()));

            // Add Evoker.
            lootManager.addLootable(new Lootable("EVOKER",
                    new LootBuilder()
                            .setMaterial(Material.TOTEM_OF_UNDYING)
                            .setAllowLootingEnchant(false).build(),
                    new LootBuilder()
                            .setMaterial(Material.EMERALD)
                            .setChance(50)
                            .addOnlyDropFors(EntityType.PLAYER).build()));
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {


            // Shulker.
            lootManager.addLootable(new Lootable("SHULKER",
                    new LootBuilder()
                            .setMaterial(Material.SHULKER_SHELL)
                            .setChance(50)
                            .setLootingIncrease(6.25).build()));
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
            // Add Polar Bear.
            lootManager.addLootable(new Lootable("POLAR_BEAR",
                    new LootBuilder()
                            .setMaterial(Material.COD)
                            .setChance(75)
                            .setMin(0)
                            .setMax(2).build(),
                    new LootBuilder()
                            .setMaterial(Material.SALMON)
                            .setChance(25)
                            .setMin(0)
                            .setMax(2).build()));
        } else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_10)) {
            // Add Polar Bear.
            lootManager.addLootable(new Lootable("POLAR_BEAR",
                    new LootBuilder()
                            .setMaterial(Material.COD)
                            .setChance(75)
                            .setMin(0)
                            .setMax(2).build(),
                    new LootBuilder()
                            .setMaterial(Material.SALMON)
                            .setChance(25)
                            .setMin(0)
                            .setMax(2).build()));
        }

        // Add Pig.
        lootManager.addLootable(new Lootable("PIG",
                new LootBuilder()
                        .setMaterial(Material.PORKCHOP)
                        .setBurnedMaterial(Material.COOKED_PORKCHOP)
                        .setMin(1)
                        .setMax(3).build()));


        // Add Cow.
        lootManager.addLootable(new Lootable("COW",
                new LootBuilder()
                        .setMaterial(Material.LEATHER)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.BEEF)
                        .setBurnedMaterial(Material.COOKED_BEEF)
                        .setMin(1)
                        .setMax(3).build()));

        // Add Mushroom Cow.
        lootManager.addLootable(new Lootable("MUSHROOM_COW",
                new LootBuilder()
                        .setMaterial(Material.LEATHER)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.BEEF)
                        .setBurnedMaterial(Material.COOKED_BEEF)
                        .setMin(1)
                        .setMax(3).build()));

        // Add Chicken.
        lootManager.addLootable(new Lootable("CHICKEN",
                new LootBuilder()
                        .setMaterial(Material.FEATHER)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.CHICKEN)
                        .setBurnedMaterial(Material.COOKED_CHICKEN).build()));
        // Add Zombie.
        lootManager.addLootable(new Lootable("ZOMBIE",
                new LootBuilder()
                        .setMaterial(Material.ROTTEN_FLESH)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.ZOMBIE_HEAD)
                        .setRequireCharged(true).build(),
                new LootBuilder()
                        .setChance(2.5)
                        .setChildDropCount(1)
                        .setAllowLootingEnchant(false)
                        .addOnlyDropFors(EntityType.PLAYER)
                        .addChildLoot(new LootBuilder().setMaterial(Material.IRON_INGOT)
                                        .setAllowLootingEnchant(false).build(),
                                new LootBuilder().setMaterial(Material.CARROT)
                                        .setAllowLootingEnchant(false).build(),
                                new LootBuilder().setMaterial(Material.POTATO)
                                        .setAllowLootingEnchant(false).build())
                        .build()));

        // Add Husk.
        lootManager.addLootable(new Lootable("ZOMBIE",
                new LootBuilder()
                        .setMaterial(Material.ROTTEN_FLESH)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setChance(2.5)
                        .setChildDropCount(1)
                        .setAllowLootingEnchant(false)
                        .addOnlyDropFors(EntityType.PLAYER)
                        .addChildLoot(new LootBuilder().setMaterial(Material.IRON_INGOT)
                                        .setAllowLootingEnchant(false).build(),
                                new LootBuilder().setMaterial(Material.CARROT)
                                        .setAllowLootingEnchant(false).build(),
                                new LootBuilder().setMaterial(Material.POTATO)
                                        .setAllowLootingEnchant(false).build())
                        .build()));

        // Add Creeper.
        lootManager.addLootable(new Lootable("CREEPER",
                new LootBuilder()
                        .setMaterial(Material.GUNPOWDER)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.CREEPER_HEAD)
                        .setRequireCharged(true).build(),
                new LootBuilder()
                        .setChildDropCount(1)
                        .addOnlyDropFors(EntityType.SKELETON,
                                ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11) ? EntityType.STRAY : null)
                        .addChildLoot(new LootBuilder().setMaterial(Material.MUSIC_DISC_11).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_13).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_BLOCKS).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_CAT).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_CHIRP).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_FAR).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_MALL).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_MELLOHI).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_STAL).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_STRAD).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_WAIT).build(),
                                new LootBuilder().setMaterial(Material.MUSIC_DISC_WARD).build())
                        .build()));

        // Add Guardian.
        lootManager.addLootable(new Lootable("GUARDIAN",
                new LootBuilder()
                        .setMaterial(Material.PRISMARINE_SHARD)
                        .setMin(0)
                        .setMax(2).build(),
                fish1,
                fish2));

        // Add Witch.
        lootManager.addLootable(new Lootable("WITCH",
                new LootBuilder()
                        .setChildDropCounMin(1)
                        .setChildDropCountMax(3)
                        .addChildLoot(new LootBuilder()
                                        .setMaterial(Material.GLOWSTONE_DUST)
                                        .setChance(12.5)
                                        .setMin(0)
                                        .setMax(2).build(),
                                new LootBuilder()
                                        .setMaterial(Material.SUGAR)
                                        .setChance(12.5)
                                        .setMin(0)
                                        .setMax(2).build(),
                                new LootBuilder()
                                        .setMaterial(Material.REDSTONE)
                                        .setChance(12.5)
                                        .setMin(0)
                                        .setMax(2).build(),
                                new LootBuilder()
                                        .setMaterial(Material.SPIDER_EYE)
                                        .setChance(12.5)
                                        .setMin(0)
                                        .setMax(2).build(),
                                new LootBuilder()
                                        .setMaterial(Material.GLASS_BOTTLE)
                                        .setChance(12.5)
                                        .setMin(0)
                                        .setMax(2).build(),
                                new LootBuilder()
                                        .setMaterial(Material.GUNPOWDER)
                                        .setChance(12.5)
                                        .setMin(0)
                                        .setMax(2).build(),
                                new LootBuilder()
                                        .setMaterial(Material.STICK)
                                        .setChance(25)
                                        .setMin(0)
                                        .setMax(2).build()
                        ).build()));

        // Add Sheep.
        lootManager.addLootable(new Lootable("SHEEP",
                new LootBuilder()
                        .setMaterial(Material.MUTTON)
                        .setBurnedMaterial(Material.COOKED_MUTTON)
                        .setMin(1)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.WHITE_WOOL)
                        .setMin(2)
                        .setMax(2).build()));

        // Add Squid.
        lootManager.addLootable(new Lootable("SQUID",
                new LootBuilder()
                        .setMaterial(Material.INK_SAC)
                        .setMin(1)
                        .setMax(3).build()));

        // Add Spider.
        lootManager.addLootable(new Lootable("SPIDER",
                new LootBuilder()
                        .setMaterial(Material.STRING)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.SPIDER_EYE)
                        .setChance(33)
                        .addOnlyDropFors(EntityType.PLAYER).build()));

        // Add Cave Spider.
        lootManager.addLootable(new Lootable("CAVE_SPIDER",
                new LootBuilder()
                        .setMaterial(Material.STRING)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.SPIDER_EYE)
                        .setChance(33)
                        .addOnlyDropFors(EntityType.PLAYER).build()));

        // Add Enderman.
        lootManager.addLootable(new Lootable("ENDERMAN",
                new LootBuilder()
                        .setMaterial(Material.ENDER_PEARL)
                        .setMin(0)
                        .setMax(1).build()));

        // Add Blaze.
        lootManager.addLootable(new Lootable("BLAZE",
                new LootBuilder()
                        .setMaterial(Material.BLAZE_ROD)
                        .setMin(0)
                        .setMax(1)
                        .addOnlyDropFors(EntityType.PLAYER).build()));

        // Add Horse.
        lootManager.addLootable(new Lootable("HORSE",
                new LootBuilder()
                        .setMaterial(Material.LEATHER)
                        .setMin(0)
                        .setMax(2).build()));

        // Magma Cube.
        lootManager.addLootable(new Lootable("MAGMA_CUBE",
                new LootBuilder()
                        .setMaterial(Material.MAGMA_CREAM)
                        .setChance(25).build()));
        // Add Skeleton.
        lootManager.addLootable(new Lootable("SKELETON",
                new LootBuilder()
                        .setMaterial(Material.ARROW)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.BONE)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.SKELETON_SKULL)
                        .setRequireCharged(true).build()));

        // Add Snowman.
        lootManager.addLootable(new Lootable("SNOWMAN",
                new LootBuilder()
                        .setMaterial(Material.SNOWBALL)
                        .setMin(0)
                        .setMax(15).build()));

        // Add Rabbit.
        lootManager.addLootable(new Lootable("RABBIT",
                new LootBuilder()
                        .setMaterial(Material.RABBIT_HIDE)
                        .setMin(0)
                        .setMax(1).build(),
                new LootBuilder()
                        .setMaterial(Material.RABBIT_FOOT)
                        .setMin(0)
                        .setMax(1)
                        .setChance(10).build(),
                new LootBuilder()
                        .setMaterial(Material.RABBIT)
                        .setBurnedMaterial(Material.COOKED_RABBIT)
                        .setMin(0)
                        .setMax(1).build()));

        // Add Iron Golem.
        lootManager.addLootable(new Lootable("IRON_GOLEM",
                new LootBuilder()
                        .setMaterial(Material.POPPY)
                        .setMin(0)
                        .setMax(2).build(),
                new LootBuilder()
                        .setMaterial(Material.IRON_INGOT)
                        .setMin(3)
                        .setMax(5).build()));

        // Add Slime.
        lootManager.addLootable(new Lootable("SLIME",
                new LootBuilder()
                        .setMaterial(Material.SLIME_BALL)
                        .setMin(0)
                        .setMax(2).build()));

        // Add Ghast.
        lootManager.addLootable(new Lootable("GHAST",
                new LootBuilder()
                        .setMaterial(Material.GHAST_TEAR)
                        .setMin(0)
                        .setMax(1).build(),
                new LootBuilder()
                        .setMaterial(Material.GUNPOWDER)
                        .setMin(0)
                        .setMax(2).build()));

        // Add Zombie Pigman
        if (ServerVersion.isServerVersionBelow(ServerVersion.V1_16))
            lootManager.addLootable(new Lootable("PIG_ZOMBIE",
                    new LootBuilder()
                            .setMaterial(Material.ROTTEN_FLESH)
                            .setMin(0)
                            .setMax(1).build(),
                    new LootBuilder()
                            .setMaterial(Material.GOLD_NUGGET)
                            .setMin(0)
                            .setMax(1).build(),
                    new LootBuilder()
                            .setMaterial(Material.GOLD_INGOT)
                            .setChance(2.5)
                            .addOnlyDropFors(EntityType.PLAYER).build()));
        else {
            // Add Strider
            lootManager.addLootable(new Lootable("STRIDER",
                    new LootBuilder()
                            .setMaterial(Material.STRING)
                            .setMin(0)
                            .setMax(5).build()));

            // Add Hoglin
            lootManager.addLootable(new Lootable("HOGLIN",
                    new LootBuilder()
                            .setMaterial(Material.PORKCHOP)
                            .setBurnedMaterial(Material.COOKED_PORKCHOP)
                            .setMin(2)
                            .setMax(4).build(),
                    new LootBuilder()
                            .setMaterial(Material.LEATHER)
                            .setMin(0)
                            .setMax(2).build()));

            // Add Zombified Piglin
            lootManager.addLootable(new Lootable("ZOMBIFIED_PIGLIN",
                    new LootBuilder()
                            .setMaterial(Material.ROTTEN_FLESH)
                            .setMin(0)
                            .setMax(1).build(),
                    new LootBuilder()
                            .setMaterial(Material.GOLD_NUGGET)
                            .setMin(0)
                            .setMax(1).build()));

            // Add Piglin
            lootManager.addLootable(new Lootable("PIGLIN"));
        }

        // Add Wither.
        lootManager.addLootable(new Lootable("WITHER",
                new LootBuilder()
                        .setMaterial(Material.NETHER_STAR)
                        .setAllowLootingEnchant(false).build()));

        // Add Villager.
        lootManager.addLootable(new Lootable("VILLAGER",
                new LootBuilder().build()));

        // Add Silverfish.
        lootManager.addLootable(new Lootable("SILVERFISH",
                new LootBuilder().build()));

        lootManager.addLootable(new Lootable("WOLF",
                new LootBuilder().build()));

        lootManager.saveLootables(true);
    }

    public LootManager getLootManager() {
        return lootManager;
    }
}