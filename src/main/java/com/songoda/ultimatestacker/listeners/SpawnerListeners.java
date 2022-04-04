package com.songoda.ultimatestacker.listeners;

import com.songoda.core.compatibility.CompatibleHand;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.nms.NmsManager;
import com.songoda.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.songoda.core.utils.EntityUtils;
import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.settings.Settings;
import com.songoda.ultimatestacker.stackable.spawner.SpawnerStack;
import com.songoda.ultimatestacker.stackable.spawner.SpawnerStackManager;
import com.songoda.ultimatestacker.utils.Paire;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.SpawnEgg;
import org.bukkit.metadata.FixedMetadataValue;

public class SpawnerListeners implements Listener {

    private final UltimateStacker plugin;

    private static final boolean mcmmo = Bukkit.getPluginManager().isPluginEnabled("mcMMO");

    public SpawnerListeners(UltimateStacker plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawn(SpawnerSpawnEvent event) {
        /*if (!Settings.STACK_ENTITIES.getBoolean()
                || !plugin.spawnersEnabled()
                || plugin.getStackingTask().isWorldDisabled(event.getLocation().getWorld())) return;

        SpawnerStackManager spawnerStackManager = plugin.getSpawnerStackManager();
        if (!spawnerStackManager.isSpawner(event.getSpawner().getLocation())) return;

        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.FIREWORK) return;
        if (entity.getVehicle() != null) {
            entity.getVehicle().remove();
            entity.remove();
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
            if (entity.getPassengers().size() != 0) {
                for (Entity e : entity.getPassengers()) {
                    e.remove();
                }
                entity.remove();
            }
        }
        entity.remove();

        Location location = event.getSpawner().getLocation();

<<<<<<< HEAD
        plugin.getStackingTask().attemptSplit(stack, (LivingEntity) event.getEntity());*/

    	if ( Settings.STACK_ENTITIES.getBoolean() &&
    			event.getLocation().getWorld().getName().equals("askyblock") &&
    			UltimateStacker.getInstance().getMobFile().getBoolean("Mobs." + event.getEntityType().name() + ".Enabled")) {

    		event.setCancelled(true);
        	if ( event.getEntity() instanceof LivingEntity ) {
        		Paire<Integer, Location> paire = new Paire<>(0, event.getLocation());
                if ( UltimateStacker.waitingToSpawnFromSpawner.containsKey(event.getSpawner()) )
                	paire = UltimateStacker.waitingToSpawnFromSpawner.get(event.getSpawner());

                paire.setFirstElement(paire.getFirstElement()+1);
                UltimateStacker.waitingToSpawnFromSpawner.put(event.getSpawner(), paire);
        	}
    	}
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void PlayerInteractEventEgg(PlayerInteractEvent event) {
        if (!plugin.spawnersEnabled()
                || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;

        if (event.getItem() == null)
            return;

        Material itemType = event.getItem().getType();
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block == null || itemType == Material.AIR) return;

        if (block.getType() != (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? Material.SPAWNER : Material.valueOf("MOB_SPAWNER"))
                || !itemType.toString().contains(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? "SPAWN_EGG" : "MONSTER_EGG"))
            return;

        event.setCancelled(true);

        if (!Settings.EGGS_CONVERT_SPAWNERS.getBoolean()
                || (event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasDisplayName()
                && !new NBTItem(event.getItem()).hasKey("UC"))) {
            return;
        }

        SpawnerStackManager manager = plugin.getSpawnerStackManager();

        SpawnerStack spawner = manager.isSpawner(block.getLocation())
                ? manager.getSpawner(block) : manager.addSpawner(new SpawnerStack(block.getLocation(), 1));

        int stackSize = spawner.getAmount();
        int amt = player.getInventory().getItemInHand().getAmount();

        EntityType entityType;
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13))
            entityType = EntityType.valueOf(itemType.name().replace("_SPAWN_EGG", "")
                    .replace("MOOSHROOM", "MUSHROOM_COW")
                    .replace("ZOMBIE_PIGMAN", "PIG_ZOMBIE"));
        else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) {
            String str = NmsManager.getNbt().of(event.getItem()).toString();
            if (str.contains("minecraft:"))
                entityType = EntityType.fromName(str.substring(str.indexOf("minecraft:") + 10, str.indexOf("\"}")));
            else
                entityType = EntityType.fromName(str.substring(str.indexOf("EntityTag:{id:") + 15, str.indexOf("\"}")));
        } else
            entityType = ((SpawnEgg) event.getItem().getData()).getSpawnedType();

        if (!player.hasPermission("ultimatestacker.egg." + entityType.name())) {
            event.setCancelled(true);
            return;
        }

        if (amt < stackSize) {
            plugin.getLocale().getMessage("event.egg.needmore")
                    .processPlaceholder("amount", stackSize).sendPrefixedMessage(player);
            event.setCancelled(true);
            return;
        }


        CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();

        if (entityType == creatureSpawner.getSpawnedType()) {
            plugin.getLocale().getMessage("event.egg.sametype")
                    .processPlaceholder("type", entityType.name()).sendPrefixedMessage(player);
            return;
        }

        creatureSpawner.setSpawnedType(entityType);
        creatureSpawner.update();

        plugin.updateHologram(spawner);
        if (player.getGameMode() != GameMode.CREATIVE)
            CompatibleHand.getHand(event).takeItem(player, stackSize);
    }
}
