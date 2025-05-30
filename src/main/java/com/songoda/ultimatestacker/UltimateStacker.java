package com.songoda.ultimatestacker;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.commands.CommandManager;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.configuration.Config;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EntityStackerManager;
import com.songoda.core.hooks.HologramManager;
import com.songoda.core.hooks.WorldGuardHook;
import com.songoda.core.utils.TextUtils;
import com.songoda.ultimatestacker.api.stack.entity.EntityStack;
import com.songoda.ultimatestacker.api.stack.entity.EntityStackManager;
import com.songoda.ultimatestacker.commands.CommandLootables;
import com.songoda.ultimatestacker.commands.CommandMoncube;
import com.songoda.ultimatestacker.commands.CommandReload;
import com.songoda.ultimatestacker.commands.CommandRemoveAll;
import com.songoda.ultimatestacker.commands.CommandSettings;
import com.songoda.ultimatestacker.commands.CommandSpawn;
import com.songoda.ultimatestacker.hook.StackerHook;
import com.songoda.ultimatestacker.hook.hooks.JobsHook;
import com.songoda.ultimatestacker.listeners.*;
import com.songoda.ultimatestacker.listeners.entity.EntityCurrentListener;
import com.songoda.ultimatestacker.listeners.entity.EntityListeners;
import com.songoda.ultimatestacker.listeners.item.ItemCurrentListener;
import com.songoda.ultimatestacker.listeners.item.ItemListeners;
import com.songoda.ultimatestacker.lootables.LootablesManager;
import com.songoda.ultimatestacker.settings.Settings;
import com.songoda.ultimatestacker.stackable.Hologramable;
import com.songoda.ultimatestacker.stackable.entity.EntityStackManagerImpl;
import com.songoda.ultimatestacker.stackable.entity.custom.CustomEntityManager;
import com.songoda.ultimatestacker.tasks.SpawnTask;
import com.songoda.ultimatestacker.tasks.StackingTaskV2;
import com.songoda.ultimatestacker.utils.Async;
import com.songoda.ultimatestacker.utils.Methods;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UltimateStacker extends SongodaPlugin {

    private static UltimateStacker INSTANCE;
    private final static Set<String> whitelist = new HashSet<>();
    private final static Set<String> blacklist = new HashSet<>();

    private final Config mobFile = new Config(this, "mobs.yml");
    private final Config itemFile = new Config(this, "items.yml");
    private final Config spawnerFile = new Config(this, "spawners.yml");

    private final GuiManager guiManager = new GuiManager(this);
    private final List<StackerHook> stackerHooks = new ArrayList<>();
    private EntityStackManager entityStackManager;
    private LootablesManager lootablesManager;
    private CommandManager commandManager;
    private CustomEntityManager customEntityManager;
    private StackingTaskV2 stackingTask;
    private boolean instantStacking;
    /*private RestockTask tradesTask;
    private GolemSpawningTask golemTask;
    private InteractListeners interactListeners;*/
    
    
    //PapiCapi
    /*public static Map<CreatureSpawner, Paire<Integer, Location>> waitingToSpawnFromSpawner = new HashMap<>();
    public static Map<Paire<EntityType, Location>, Integer> waitingToSpawnFromFarms = new HashMap<>();
    public static Map<Location, List<Location>> ignoredLocations = new HashMap<>();*/

    public static UltimateStacker getInstance() {
        return INSTANCE;
    }
    
    /*public InteractListeners getInteractListeners() {
    	return interactListeners;
    }*/

    public static void printNearbyPaps(String message, Location location) {
        if ( Bukkit.getPlayer("PapiCapi") != null &&
                Bukkit.getPlayer("PapiCapi").getLocation().getWorld().equals(location.getWorld()) &&
                Bukkit.getPlayer("PapiCapi").getLocation().distanceSquared(location) < 36 )
            System.out.println(message);
    }

    @Override
    public void onPluginLoad() {
        INSTANCE = this;

        // Register WorldGuard
        WorldGuardHook.addHook("mob-stacking", true);
        
        // Ajout d'un hook villageois pour la béta de l'addon villager pour le sky
        WorldGuardHook.addHook("villager-stacking", false);
    }

    @Override
    public void onPluginDisable() {
    	for ( Player player : Bukkit.getOnlinePlayers() ) {
    		player.saveData();
    	}
    	getLogger().info("[UltimateStacker/MONCUBE] Saved player's data");
    	
    	for( World world : Bukkit.getWorlds() ) {
    		world.save();
    	}
        getLogger().info("[UltimateStacker/MONCUBE] Saved worlds data");
    	
    	
    	for( BukkitTask task : Bukkit.getScheduler().getPendingTasks() ) {
    		if ( task.getOwner().equals(this) ) {
    			task.cancel();
    		}
    	}
        getLogger().info("[UltimateStacker/MONCUBE] Cancelled all tasks");

        if (this.stackingTask != null)
            this.stackingTask.cancel();

        HologramManager.removeAllHolograms();
        Async.shutdown();
    }

    @Override
    public void onPluginEnable() {
        // Run Songoda Updater
        Async.start();
        SongodaCore.registerPlugin(this, 16, Material.IRON_INGOT);
        // Setup Config
        Settings.setupConfig();
        this.setLocale(Settings.LANGUGE_MODE.getString(), false);
        blacklist.clear();
        whitelist.clear();
        whitelist.addAll(Settings.ITEM_WHITELIST.getStringList());
        blacklist.addAll(Settings.ITEM_BLACKLIST.getStringList());
        
        // Setup plugin commands
        this.commandManager = new CommandManager(this);
        this.commandManager.addMainCommand("us")
                .addSubCommands(new CommandSettings(this, guiManager),
                        new CommandRemoveAll(this),
                        new CommandReload(this),
                        new CommandSpawn(this),
                        new CommandLootables(this)
                        //new CommandConvert( guiManager)
                );

        this.lootablesManager = new LootablesManager();
        this.lootablesManager.createDefaultLootables();
        this.getLootablesManager().getLootManager().loadLootables();

        for (EntityType value : EntityType.values()) {
            if (value.isSpawnable() && value.isAlive() && !value.toString().contains("ARMOR")) {
                mobFile.addDefault("Mobs." + value.name() + ".Enabled", true);
                mobFile.addDefault("Mobs." + value.name() + ".Display Name", TextUtils.formatText(value.name().toLowerCase().replace("_", " "), true));
                mobFile.addDefault("Mobs." + value.name() + ".Max Stack Size", -1);
                mobFile.addDefault("Mobs." + value.name() + ".Kill Whole Stack", false);
            }
        }
        mobFile.load();
        mobFile.saveChanges();

        for (Material value : Material.values()) {
            itemFile.addDefault("Items." + value.name() + ".Has Hologram", true);
            itemFile.addDefault("Items." + value.name() + ".Max Stack Size", -1);
            //itemFile.addDefault("Items." + value.name() + ".Display Name", WordUtils.capitalizeFully(value.name().toLowerCase().replace("_", " ")));
            itemFile.addDefault("Items." + value.name() + ".Display Name", StringUtils.capitalize(value.name().toLowerCase().replace("_", " ")));
        }
        itemFile.load();
        itemFile.saveChanges();

        for (EntityType value : EntityType.values()) {
            if (value.isSpawnable() && value.isAlive() && !value.toString().contains("ARMOR")) {
                spawnerFile.addDefault("Spawners." + value.name() + ".Max Stack Size", -1);
                spawnerFile.addDefault("Spawners." + value.name() + ".Display Name", TextUtils.formatText(value.name().toLowerCase().replace("_", " "), true));
            }
        }
        spawnerFile.load();
        spawnerFile.saveChanges();

        this.entityStackManager = new EntityStackManagerImpl(this);
        this.customEntityManager = new CustomEntityManager();

        guiManager.init();
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_10))
            pluginManager.registerEvents(new BreedListeners(this), this);
        pluginManager.registerEvents(new DeathListeners(this), this);
        pluginManager.registerEvents(new ShearListeners(this), this);
        pluginManager.registerEvents(/*interactListeners = */new InteractListeners(this), this);
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13))
            pluginManager.registerEvents(new EntityCurrentListener(this), this);

        pluginManager.registerEvents(new EntityListeners(this), this);
        pluginManager.registerEvents(new ItemListeners(this), this);

        pluginManager.registerEvents(new ItemCurrentListener(), this);

        pluginManager.registerEvents(new TameListeners(this), this);
        pluginManager.registerEvents(new SpawnerListeners(this), this);
        pluginManager.registerEvents(new SheepDyeListeners(this), this);

        if (Settings.CLEAR_LAG.getBoolean() && pluginManager.isPluginEnabled("ClearLag"))
        	 pluginManager.registerEvents(new ClearLagListeners(this), this);

           

        // Register Hooks
        if (pluginManager.isPluginEnabled("Jobs"))
            stackerHooks.add(new JobsHook());

        HologramManager.load(this);
        EntityStackerManager.load();

        // Database stuff, go!
        /*try {
            if (Settings.MYSQL_ENABLED.getBoolean()) {
                /*String hostname = Settings.MYSQL_HOSTNAME.getString();
                int port = Settings.MYSQL_PORT.getInt();
                String database = Settings.MYSQL_DATABASE.getString();
                String username = Settings.MYSQL_USERNAME.getString();
                String password = Settings.MYSQL_PASSWORD.getString();
                boolean useSSL = Settings.MYSQL_USE_SSL.getBoolean();
                int poolSize = Settings.MYSQL_POOL_SIZE.getInt();

                this.databaseConnector = new MySQLConnector(this, hostname, port, database, username, password, useSSL, poolSize);
                this.getLogger().info("MySQL is disabled in code !!!");
            } else {
                this.databaseConnector = new SQLiteConnector(this);
                this.getLogger().info("Data handler connected using SQLite.");
            }
        } catch (Exception ex) {
            this.getLogger().severe("Fatal error trying to connect to database. Please make sure all your connection settings are correct and try again. Plugin has been disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        this.dataManager = new DataManager(this.databaseConnector, this);
        this.dataMigrationManager = new DataMigrationManager(this.databaseConnector, this.dataManager,
                new _1_InitialMigration(),
                new _2_EntityStacks(),
                new _3_BlockStacks(),
                new _4_DataPurge());
        this.dataMigrationManager.runMigrations();
        */

        //PapiCapi
        //startSpawnerAndFarmsActivity();
        
        getCommand("usmoncube").setExecutor(new CommandMoncube());
        //PapiCapi end
    }
    
    /*public void startSpawnerAndFarmsActivity() {
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			
			@Override
			public void run() {
				
				World islandsWorld = Bukkit.getWorld("askyblock");
				
				for ( Entry<CreatureSpawner, Paire<Integer, Location>> entry : waitingToSpawnFromSpawner.entrySet() ) {
					
					LivingEntity entity = (LivingEntity)islandsWorld.spawnEntity(entry.getValue().getSecondElement(), entry.getKey().getSpawnedType());
		            EntityStack stack = getEntityStackManager().addStack(entity);
		            stack.createDuplicates(entry.getValue().getFirstElement() - 1);
		            stack.updateStack();
		            getStackingTask().attemptSplit(stack, entity);
				}
				waitingToSpawnFromSpawner.clear();
				
				for ( Entry<Paire<EntityType, Location>, Integer> entry : waitingToSpawnFromFarms.entrySet() ) {
					
					LivingEntity entity = (LivingEntity)islandsWorld.spawnEntity(getLocationToSpawnFarmEntities(entry.getKey().getSecondElement()), entry.getKey().getFirstElement());
		            EntityStack stack = getEntityStackManager().addStack(entity);
		            stack.createDuplicates(entry.getValue() - 1);
		            stack.updateStack();
		            getStackingTask().attemptSplit(stack, entity);
		            
				}
				waitingToSpawnFromFarms.clear();
				
			}
			
		}, 20*30, 20*60);
    }*/


    /*private Location getLocationToSpawnFarmEntities(Location loc) {
    	if ( ignoredLocations.get(loc) != null ) {
    		//first get average for all coordinates
    		List<Location> ignoredLocationList = ignoredLocations.get(loc);
    		if ( ignoredLocationList.isEmpty() )
    			return loc;
    		
    		double xAvg = ignoredLocationList.parallelStream().mapToDouble(Location::getX).average().orElse(0);
    		double yAvg = ignoredLocationList.parallelStream().mapToDouble(Location::getY).average().orElse(0);
    		double zAvg = ignoredLocationList.parallelStream().mapToDouble(Location::getZ).average().orElse(0);
    		
    		if ( xAvg == 0 && yAvg == 0 && zAvg == 0 )
    			return loc;
    		
    		Location avgLoc = new Location(loc.getWorld(), xAvg, yAvg, zAvg);
    		
    		Location nearestLocation = ignoredLocationList.get(0);
    		//find the nearest location from the average
    		
    		for( Location tempLoc : ignoredLocationList ) {
    			if ( tempLoc.distance(avgLoc) < nearestLocation.distance(avgLoc) )
    				nearestLocation = tempLoc;
    		}
    		
    		return nearestLocation;
    		
    	}
    	
    	return loc;
    }*/
    
    
    /*
     * PapiCapi
     * Function to get the spawn location for spawners
     * 
     * Entry: the spawner location
     * Exit: the spawn location or loc if spawn location not found
     */
    /*private Location getSpawnLoc(Location loc) {
    	
    	for( int x = (loc.getBlockX() - 4) ; x < (loc.getBlockX() + 4) ; x++ ) {
			for ( int z = (loc.getBlockZ() - 4) ; z < (loc.getBlockZ() + 4) ; z++ ) {
				if ( (loc.getWorld().getBlockAt(x, loc.getBlockY(), z).getType() == Material.AIR || loc.getWorld().getBlockAt(x, loc.getBlockY(), z).getType() == Material.WATER) && 
						(loc.getWorld().getBlockAt(x, loc.getBlockY()+1, z).getType() == Material.AIR || loc.getWorld().getBlockAt(x, loc.getBlockY()+1, z).getType() == Material.WATER) ) {
					return loc.getWorld().getBlockAt(x, loc.getBlockY(), z).getLocation();
				}
			}
		}
    	return loc;
    }*/

    @Override
    public void onDataLoad() {
        if (HologramManager.isEnabled())
            // Set the offset so that the holograms don't end up inside the blocks.
            HologramManager.getHolograms().setPositionOffset(.5,.65,.5);

        // Load current data.
        final boolean useSpawnerHolo = Settings.SPAWNER_HOLOGRAMS.getBoolean();
        /*this.dataManager.getEntities((entities) -> {
            entityStackManager.addStacks(entities.values());
            entityStackManager.tryAndLoadColdEntities();
            this.stackingTask = new StackingTask(this);
            getServer().getPluginManager().registerEvents(new ChunkListeners(entityStackManager), this);
        });*/

        //Start stacking task
        if (Settings.STACK_ENTITIES.getBoolean()) {
            this.stackingTask = new StackingTaskV2(this);
        }
        new SpawnTask();

        //getServer().getPluginManager().registerEvents(new ChunkListeners(entityStackManager), this);
        this.instantStacking = Settings.STACK_ENTITIES.getBoolean() && Settings.INSTANT_STACKING.getBoolean();
    
        final boolean useBlockHolo = Settings.BLOCK_HOLOGRAMS.getBoolean();
        
        /*this.tradesTask = new RestockTask(this); // Ajout task qui gère les reset de trades
        this.golemTask = new GolemSpawningTask(this); // Ajout task qui gère le spawn des golems*/
    }

    public void addExp(Player player, EntityStack stack) {
        for (StackerHook stackerHook : stackerHooks) {
            stackerHook.applyExperience(player, stack);
        }
    }

    @Override
    public List<Config> getExtraConfig() {
        return Arrays.asList(mobFile, itemFile, spawnerFile);
    }

    @Override
    public void onConfigReload() {
        blacklist.clear();
        whitelist.clear();
        whitelist.addAll(Settings.ITEM_WHITELIST.getStringList());
        blacklist.addAll(Settings.ITEM_BLACKLIST.getStringList());

        this.setLocale(getConfig().getString("System.Language Mode"), true);
        this.locale.reloadMessages();

        this.stackingTask.cancel();
        this.stackingTask = new StackingTaskV2(this);

        this.mobFile.load();
        this.itemFile.load();
        this.spawnerFile.load();
        this.getLootablesManager().getLootManager().loadLootables();
    }

    public boolean spawnersEnabled() {
        return !this.getServer().getPluginManager().isPluginEnabled("EpicSpawners")
                && Settings.SPAWNERS_ENABLED.getBoolean();
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public LootablesManager getLootablesManager() {
        return lootablesManager;
    }

    public EntityStackManager getEntityStackManager() {
        return entityStackManager;
    }

    public StackingTaskV2 getStackingTask() {
        return stackingTask;
    }
    
    public void setStackingTask(StackingTaskV2 stackingTask) {
		this.stackingTask = stackingTask;
	}
    
    /*public RestockTask getTradesTask() {
    	return tradesTask;
    }
    
    public GolemSpawningTask getGolemTask() {
    	return golemTask;
    }*/

    public Config getMobFile() {
        return mobFile;
    }

    public Config getItemFile() {
        return itemFile;
    }

    public Config getSpawnerFile() {
        return spawnerFile;
    }

    /*public DatabaseConnector getDatabaseConnector() {
        return databaseConnector;
    }

    public DataManager getDataManager() {
        return dataManager;
    }*/

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public CustomEntityManager getCustomEntityManager() {
        return customEntityManager;
    }

    public void updateHologram(Hologramable stack) {
        // are holograms enabled?
        if (!stack.areHologramsEnabled() && !HologramManager.getManager().isEnabled()) return;
        // update the hologram
        if (!HologramManager.isHologramLoaded(stack.getHologramId())) {
            HologramManager.createHologram(stack.getHologramId(), stack.getLocation(), stack.getHologramName());
            return;
        }

        HologramManager.updateHologram(stack.getHologramId(), stack.getHologramName());
    }

    public void removeHologram(Hologramable stack) {
        HologramManager.removeHologram(stack.getHologramId());
    }

    public boolean isInstantStacking() {
        return instantStacking;
    }

    //////// Convenient API //////////

    /**
     * Spawn a stacked item at a location
     *
     * @param item     The item to spawn
     * @param amount   The amount of items to spawn
     * @param location The location to spawn the item
     */
    public static void spawnStackedItem(ItemStack item, int amount, Location location) {
        location.getWorld().dropItem(location, item, dropped -> {
            updateItemAmount(dropped, amount);
        });
    }

    /**
     * Change the stacked amount for this item
     *
     * @param item      item entity to update
     * @param newAmount number of items this item represents
     */
    public static void updateItemAmount(Item item, int newAmount) {
        updateItemAmount(item, item.getItemStack(), newAmount);
    }

    /**
     * Change the stacked amount for this item
     *
     * @param item      item entity to update
     * @param itemStack ItemStack that will represent this item
     * @param newAmount number of items this item represents
     */
    public static void updateItemAmount(Item item, ItemStack itemStack, int newAmount) {
        boolean blacklisted = isMaterialBlacklisted(itemStack);

        if (newAmount > (itemStack.getMaxStackSize() / 2) && !blacklisted)
            itemStack.setAmount(Math.max(1, itemStack.getMaxStackSize() / 2));
        else
            itemStack.setAmount(newAmount);

        // If amount is 0, Minecraft change the type to AIR
        if (itemStack.getType() == Material.AIR)
            return;

        updateItemMeta(item, itemStack, newAmount);
    }

    public static void updateItemMeta(Item item, ItemStack itemStack, int newAmount) {
        Material material = itemStack.getType();
        if (material == Material.AIR)
            return;

        String name = TextUtils.convertToInvisibleString("IS") + Methods.compileItemName(itemStack, newAmount);

        boolean blacklisted = isMaterialBlacklisted(itemStack);

        if (newAmount > (itemStack.getMaxStackSize() / 2) && !blacklisted) {
            item.setMetadata("US_AMT", new FixedMetadataValue(INSTANCE, newAmount));
        } else {
            item.removeMetadata("US_AMT", INSTANCE);
        }
        item.setItemStack(itemStack);

        if ((blacklisted && !Settings.ITEM_HOLOGRAM_BLACKLIST.getBoolean())
                || !INSTANCE.getItemFile().getBoolean("Items." + material + ".Has Hologram")
                || !Settings.ITEM_HOLOGRAMS.getBoolean()
                || newAmount < Settings.ITEM_MIN_HOLOGRAM_SIZE.getInt())
            return;

        item.setCustomName(name);
        item.setCustomNameVisible(true);
    }

    /**
     * Lookup the stacked size of this item
     *
     * @param item item to check
     * @return stacker-corrected value for the stack size
     */
    public static int getActualItemAmount(Item item) {
        ItemStack itemStack = item.getItemStack();
        int amount = itemStack.getAmount();
        if (/*amount >= (itemStack.getMaxStackSize() / 2) && */item.hasMetadata("US_AMT")) {
            return item.getMetadata("US_AMT").get(0).asInt();
        } else {
            return amount;
        }
    }

    /**
     * Check to see if the amount stored in this itemstack is not the stacked
     * amount
     *
     * @param item item to check
     * @return true if Item.getItemStack().getAmount() is different from the
     * stacked amount
     */
    public static boolean hasCustomAmount(Item item) {
        if (item.hasMetadata("US_AMT")) {
            return item.getItemStack().getAmount() != item.getMetadata("US_AMT").get(0).asInt();
        }
        return false;
    }

    /**
     * Check to see if this material is not permitted to stack
     *
     * @param item Item material to check
     * @return true if this material will not stack
     */
    public static boolean isMaterialBlacklisted(ItemStack item) {
        Material mat = item.getType();
        return isMaterialBlacklisted(mat.name()) || isMaterialBlacklisted(mat);
    }

    /**
     * Check to see if this material is not permitted to stack
     *
     * @param type Material to check
     * @return true if this material will not stack
     */
    public static boolean isMaterialBlacklisted(String type) {
        return !whitelist.isEmpty() && !whitelist.contains(type)
                || !blacklist.isEmpty() && blacklist.contains(type);
    }

    /**
     * Check to see if this material is not permitted to stack
     *
     * @param type Material to check
     * @return true if this material will not stack
     */
    public static boolean isMaterialBlacklisted(Material type) {
        return !whitelist.isEmpty() && !whitelist.contains(type.name())
                || !blacklist.isEmpty() && blacklist.contains(type.name());
    }

    /**
     * Check to see if this material is not permitted to stack
     *
     * @param type Material to check
     * @param data data value for this item (for 1.12 and older servers)
     * @return true if this material will not stack
     */
    public static boolean isMaterialBlacklisted(Material type, byte data) {
        String combined = type.toString() + ":" + data;

        return !whitelist.isEmpty() && !whitelist.contains(combined)
                || !blacklist.isEmpty() && blacklist.contains(combined);
    }
   
}
