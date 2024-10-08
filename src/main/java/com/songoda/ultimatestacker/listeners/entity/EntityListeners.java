package com.songoda.ultimatestacker.listeners.entity;

import com.songoda.core.configuration.Config;
import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.api.UltimateStackerApi;
import com.songoda.ultimatestacker.api.stack.entity.EntityStack;
import com.songoda.ultimatestacker.api.stack.entity.EntityStackManager;
import com.songoda.ultimatestacker.api.stack.item.StackedItem;
import com.songoda.ultimatestacker.settings.Settings;
import com.songoda.ultimatestacker.tasks.SpawnTask;
import com.songoda.ultimatestacker.utils.Paire;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.objects.Island;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntityListeners implements Listener {

    private final UltimateStacker plugin;
    private int searchRadius = Settings.SEARCH_RADIUS.getInt() * 16; //SEARCH_RADIUS is in chunks, so multiply by 16 to get blocks
    private Config mobsConfig;

    public EntityListeners(UltimateStacker plugin) {
        this.plugin = plugin;
        mobsConfig = plugin.getMobFile();
    }

    @EventHandler
    public void onEgg(ItemSpawnEvent event) {
        Material material = event.getEntity().getItemStack().getType();
        if (material != Material.EGG
                && !material.name().equalsIgnoreCase("SCUTE")) return;

        Location location = event.getLocation();

        List<Entity> entities = new ArrayList<>(location.getWorld().getNearbyEntities(location, .1, 1, .1));

        if (entities.isEmpty()) return;

        Entity nonLivingEntity = entities.get(0);

        if (!(nonLivingEntity instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) nonLivingEntity;

        EntityStackManager stackManager = plugin.getEntityStackManager();

        if (!stackManager.isStackedEntity(entity)) return;

        EntityStack stack = stackManager.getStackedEntity(entity);

        ItemStack item = event.getEntity().getItemStack();
        int amount = (stack.getAmount() - 1) + item.getAmount();
        if (amount < 1) return;
        item.setAmount(Math.min(amount, item.getMaxStackSize()));
        if (amount > item.getMaxStackSize()) {
            StackedItem stackedItem = UltimateStackerApi.getStackedItemManager().getStackedItem(event.getEntity());
            stackedItem.setAmount(amount - item.getMaxStackSize());
        }
        event.getEntity().setItemStack(item);
    }

    @EventHandler
    public void onHurt(EntityDamageByEntityEvent event) {
        if (!Settings.STACK_ENTITIES.getBoolean() || !(event.getDamager() instanceof Player)) return;

        Entity entity = event.getEntity();

        if (!(entity instanceof LivingEntity)) return;
        if (event.getDamager() instanceof Player) {
            plugin.getEntityStackManager().setLastPlayerDamage(entity, (Player) event.getDamager());
        }

        if (plugin.getEntityStackManager().isStackedEntity(entity)
                && Settings.DISABLE_KNOCKBACK.getBoolean()
                && ((Player) event.getDamager()).getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK) == 0) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                event.getEntity().setVelocity(new Vector());
            }, 0L);
        }
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        String spawnReason = event.getSpawnReason().name();
        if (plugin.isInstantStacking()) {
            LivingEntity spawningEntity = event.getEntity();
            EntityStackManager stackManager = plugin.getEntityStackManager();
            if (stackManager.isStackedEntity(spawningEntity)) return; //We don't want to stack split entities or respawned stacks

            List<LivingEntity> stackableFriends = plugin.getStackingTask().getSimilarEntitiesAroundEntity(spawningEntity, spawningEntity.getLocation());
            if (stackableFriends.isEmpty()) {
                event.getEntity().setMetadata("US_REASON", new FixedMetadataValue(plugin, spawnReason));
                return;
            }

            LivingEntity friendStack = stackableFriends.get(0);
            if (stackManager.isStackedEntity(friendStack)) {
                EntityStack stack = stackManager.getStackedEntity(friendStack);
                //getSimilarEntitiesAroundEntity check for max stack size, we don't need to check again
                stack.add(1);
                event.setCancelled(true);
            } else {
                stackManager.createStackedEntity(friendStack, 2);
            }
            return;
        }

        event.getEntity().setMetadata("US_REASON", new FixedMetadataValue(plugin, spawnReason));
    }

    /*@EventHandler
    public void onBlow(EntityExplodeEvent event) {
        if (!plugin.spawnersEnabled()) return;

        List<Block> destroyed = event.blockList();
        Iterator<Block> it = destroyed.iterator();
        List<Block> toCancel = new ArrayList<>();
        while (it.hasNext()) {
            Block block = it.next();
            if (block.getType() != Material.SPAWNER)
                continue;

            Location spawnLocation = block.getLocation();

            SpawnerStack spawner = UltimateStackerApi.getSpawnerStackManager().getSpawner(block);

            if (Settings.SPAWNERS_DONT_EXPLODE.getBoolean())
                toCancel.add(block);
            else {
                String chance = "";
                if (event.getEntity() instanceof Creeper)
                    chance = Settings.EXPLOSION_DROP_CHANCE_TNT.getString();
                else if (event.getEntity() instanceof TNTPrimed)
                    chance = Settings.EXPLOSION_DROP_CHANCE_CREEPER.getString();
                int ch = Integer.parseInt(chance.replace("%", ""));
                double rand = Math.random() * 100;
                if (rand - ch < 0 || ch == 100) {
                    CreatureSpawner cs = (CreatureSpawner) block.getState();
                    EntityType blockType = cs.getSpawnedType();
                    ItemStack item = Methods.getSpawnerItem(blockType, spawner.getAmount());
                    spawnLocation.getWorld().dropItemNaturally(spawnLocation.clone().add(.5, 0, .5), item);

                    SpawnerStack spawnerStack = UltimateStackerApi.getSpawnerStackManager().removeSpawner(spawnLocation);
                    plugin.getPluginDataManager().delete(spawnerStack);
                    plugin.removeHologram(spawnerStack);
                }
            }

            Location nloc = spawnLocation.clone();
            nloc.add(.5, -.4, .5);
            List<Entity> near = (List<Entity>) nloc.getWorld().getNearbyEntities(nloc, 8, 8, 8);
            for (Entity ee : near) {
                if (ee.getLocation().getX() == nloc.getX() && ee.getLocation().getY() == nloc.getY() && ee.getLocation().getZ() == nloc.getZ()) {
                    ee.remove();
                }
            }
        }

        for (Block block : toCancel) {
            event.blockList().remove(block);
        }
    }*/

    
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
    	if ( (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NETHER_PORTAL) &&
    			Settings.STACK_ENTITIES.getBoolean() &&
        		event.getLocation().getWorld().getName().equals("askyblock") &&
        		UltimateStacker.getInstance().getMobFile().getBoolean("Mobs." + event.getEntityType().name() + ".Enabled")) {
    		
    		
    		Island spawnIsland = BentoBox.getInstance().getIslands().getIslandAt(event.getLocation()).orElse(null);
    		Flag flag = BentoBox.getInstance().getFlagsManager().getFlag("MONSTER_NATURAL_SPAWN").orElse(null);
    		if ( spawnIsland != null && flag != null && spawnIsland.getFlag(flag) > 0 ) {
    			
    			event.setCancelled(true);
            	if ( event.getEntity() instanceof LivingEntity ) {
            		
            		int amountAlreadySeen = 0;
            		Paire<EntityType, Location> paire;
            		
                    if ( getPaireOfLocation(event.getLocation(), event.getEntityType()) != null ) {
                    	paire = getPaireOfLocation(event.getLocation(), event.getEntityType());
                    	amountAlreadySeen = SpawnTask.waitingToSpawnFromFarms.get(paire);
                    	
                    	
                    	//add to the ignored Locations
                    	List<Location> ignoredLocations;
                    	if ( SpawnTask.ignoredLocations.get(paire.getSecondElement()) != null ) { //map already contains an initialized list of ignored locations
                    		ignoredLocations = SpawnTask.ignoredLocations.get(paire.getSecondElement());
                    	} else {
                    		ignoredLocations = new ArrayList<>();
                    	}
                    	ignoredLocations.add(event.getLocation());

                        SpawnTask.ignoredLocations.put(paire.getSecondElement(), ignoredLocations);
                    }
                    else
                    	paire = new Paire<>(event.getEntityType(), event.getLocation());
                    
                    
                    amountAlreadySeen++;
                    SpawnTask.waitingToSpawnFromFarms.put(paire, amountAlreadySeen);
            	}
    			
    		}
        		
    		
    	}
    }
    
    /*
     * returns the first Paire where the distance is less than 7
     */
    private Paire<EntityType, Location> getPaireOfLocation(Location loc, EntityType type) {
    	return SpawnTask.waitingToSpawnFromFarms.keySet().stream().filter(paire -> paire.getSecondElement().distance(loc) < 50 && paire.getFirstElement() == type).findFirst().orElse(null);

    }

}
