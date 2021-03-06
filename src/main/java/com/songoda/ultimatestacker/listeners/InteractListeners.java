package com.songoda.ultimatestacker.listeners;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.hooks.WorldGuardHook;
import com.songoda.core.nms.NmsManager;
import com.songoda.core.nms.nbt.NBTEntity;
import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.settings.Settings;
import com.songoda.ultimatestacker.stackable.entity.EntityStack;
import com.songoda.ultimatestacker.stackable.entity.Split;
import com.songoda.ultimatestacker.stackable.entity.StackedEntity;

import net.minecraft.server.v1_16_R3.*;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class InteractListeners implements Listener {

    private final UltimateStacker plugin;
            
    private HashMap<Player,HashMap<Integer,List<Inventory>>> achats = new HashMap<Player,HashMap<Integer,List<Inventory>>>();
    private HashMap<Player,HashMap<Integer,List<Inventory>>> ventes = new HashMap<Player,HashMap<Integer,List<Inventory>>>();
    private HashMap<Player,HashMap<Integer,List<Inventory>>> parcours = new HashMap<Player,HashMap<Integer,List<Inventory>>>();
        
    private HashMap<UUID,Villager> stackedVillagers = new HashMap<UUID,Villager>();
    
    private HashMap<Island,Integer> breedLimit = new HashMap<Island,Integer>();
    
    File f;
    FileConfiguration cfg;
    
    public HashMap<UUID,Villager> getStackedVillagers(){
    	return stackedVillagers;
    }
    
    private List<Villager> toRename = new ArrayList<>();
    
    public InteractListeners(UltimateStacker plugin) {
        this.plugin = plugin;
        updateBreedLimit();
        createVillagersLimitsYAML(plugin);
    }
    
    public void createVillagersLimitsYAML(UltimateStacker plugin) {
    	f = new File(plugin.getDataFolder(),"VillagersLimits.yml");
    	cfg = YamlConfiguration.loadConfiguration(f);
    	try {
			cfg.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    @EventHandler
    public void onVillagerGuiClick(InventoryClickEvent event) {
    	InventoryView iv = event.getView();
		ItemStack item = event.getCurrentItem();

		if(iv.getTitle().contains("§aMatériaux recherchés") || iv.getTitle().contains("§aMatériaux à vendre")) {
			
			EntityStack stackTest = getStack(event);
			if(stackTest==null) {
				event.setCancelled(true);
				event.getWhoClicked().closeInventory();
				event.getWhoClicked().sendMessage("§4Ce stack de villageois n'existe plus !");
				return;
			}
			
			net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(item);
			
    		if(!(NMSitem.hasTag() && (NMSitem.getTag().hasKey("id") || NMSitem.getTag().hasKey("uuid")))) {
				event.setCancelled(true);
				return;
			}	
			
			if(item.hasItemMeta()) {
				if(item.getItemMeta().getDisplayName().equals("§4Fermer")) {
					event.setCancelled(true);
					List<Inventory> invs = null;
					boolean pass=false;
					
					if(parcours.containsKey((Player)event.getWhoClicked())) {
						HashMap<Integer,List<Inventory>> dic = parcours.get((Player)event.getWhoClicked());
						if(dic.containsKey(NMSitem.getTag().getInt("id"))) {
							invs = dic.get(NMSitem.getTag().getInt("id"));
							pass = true;
						}
					}
					
					if(pass==false) {
						invs = getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event),"Parcours",null,null);
					}
					else {
						HashMap<Integer,List<Inventory>> inventories;
						if(parcours.containsKey((Player)event.getWhoClicked())) {
							inventories = parcours.get((Player)event.getWhoClicked());
						}
						else {
							inventories = new HashMap<Integer,List<Inventory>>();
						}
						inventories.put(getStack(event).getId(), invs);
						parcours.put((Player)event.getWhoClicked(),inventories);
					}
					
					event.getWhoClicked().openInventory(invs.get(0));
					
					return;
				}
			}
			
			if(item.hasItemMeta()) {
				if(item.getItemMeta().getDisplayName().equals("§5Page suivante")) {
					
					String s="";
					char[] c = iv.getTitle().toCharArray();
					for(int i=0;i<c.length;i++) {
						if(Character.isDigit(c[i]) && c[i-1]!='§') {
							s+=c[i];
						}
					}
					int page = Integer.valueOf(s);
										
					boolean valid = true;
					
					HashMap<Player, HashMap<Integer, List<Inventory>>> inventaires;
					
					if(iv.getTitle().contains("§aMatériaux recherchés")) {
						inventaires = achats;
					}
					else
					{
						inventaires = ventes;
					}
					
					if(inventaires.containsKey((Player)event.getWhoClicked())) {
						
						HashMap<Integer,List<Inventory>> playerInventories = inventaires.get((Player)event.getWhoClicked());
						
						if(playerInventories.containsKey(NMSitem.getTag().getInt("id"))) {
							event.getWhoClicked().openInventory(playerInventories.get(NMSitem.getTag().getInt("id")).get(page)); //Decalage de 1 
						}
						else {
							valid = false;
						}
					}
					else {
	    				valid = false;
					}
					
					if(valid==false) {
	    				ItemMeta im = item.getItemMeta();
	    				im.setDisplayName("§5§o§m"+"Page suivante");
	    				im.setLore(Arrays.asList("§4Erreur lors de l'obtention de l'inventaire ! "));
	    				item.setItemMeta(im);
	    				((Player)event.getWhoClicked()).updateInventory();
					}
				}
				
				if(item.getItemMeta().getDisplayName().equals("§5Page précédente")) {
					
					String s="";
					char[] c = iv.getTitle().toCharArray();
					for(int i=0;i<c.length;i++) {
						if(Character.isDigit(c[i]) && c[i-1]!='§') {
							s+=c[i];
						}
					}
					int page = Integer.valueOf(s);
					
					boolean valid = true;
					
					HashMap<Player, HashMap<Integer, List<Inventory>>> inventaires;
					
					if(iv.getTitle().contains("§aMatériaux recherchés")) {
						inventaires = achats;
					}
					else
					{
						inventaires = ventes;
					}
					
					if(inventaires.containsKey((Player)event.getWhoClicked())) {
						
						HashMap<Integer,List<Inventory>> playerInventories = inventaires.get((Player)event.getWhoClicked());
						
						if(playerInventories.containsKey(NMSitem.getTag().getInt("id"))) {
							event.getWhoClicked().openInventory(playerInventories.get(NMSitem.getTag().getInt("id")).get(page-2)); //Decalage de 1
						}
						else {
							valid = false;
						}
					}
					else {
	    				valid = false;
					}
					
					if(valid==false) {
	    				ItemMeta im = item.getItemMeta();
	    				im.setDisplayName("§5§o§m"+"Page précédente");
	    				im.setLore(Arrays.asList("§4Erreur lors de l'obtention de l'inventaire ! "));
	    				item.setItemMeta(im);
	    				((Player)event.getWhoClicked()).updateInventory();
					}
				}
			}
			iv.close();	
			List<Inventory> invs;
			Inventory inventory;
			
			if(iv.getTitle().contains("§aMatériaux recherchés")) {
				
         		net.minecraft.server.v1_16_R3.ItemStack itemWithoutNMS = CraftItemStack.asNMSCopy(item);
                NBTTagCompound compound = itemWithoutNMS.getOrCreateTag();
                compound.remove("id");
                itemWithoutNMS.setTag(compound);
                ItemStack searchedItem = CraftItemStack.asBukkitCopy(itemWithoutNMS);
                
                HashMap<Integer,ItemStack> itemHashMap = new HashMap<Integer,ItemStack>();
                itemHashMap.put(getStack(event).getId(), searchedItem);
				
				invs = getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), "Parcours", null,item);
				
				HashMap<Integer,List<Inventory>> inventories;
				if(parcours.containsKey((Player)event.getWhoClicked())) {
					inventories = parcours.get((Player)event.getWhoClicked());
				}
				else {
					inventories = new HashMap<Integer,List<Inventory>>();
				}
				inventories.put(getStack(event).getId(), invs);
				parcours.put((Player)event.getWhoClicked(),inventories);
				
				inventory = invs.get(0);
				
			}
			else {
				
         		net.minecraft.server.v1_16_R3.ItemStack itemWithoutNMS = CraftItemStack.asNMSCopy(item);
                NBTTagCompound compound = itemWithoutNMS.getOrCreateTag();
                compound.remove("id");
                itemWithoutNMS.setTag(compound);
                ItemStack searchedItem = CraftItemStack.asBukkitCopy(itemWithoutNMS);
                
                HashMap<Integer,ItemStack> itemHashMap = new HashMap<Integer,ItemStack>();
                itemHashMap.put(getStack(event).getId(), searchedItem);
				
				invs = getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), "Parcours", item,null);
				
				HashMap<Integer,List<Inventory>> inventories;
				if(parcours.containsKey((Player)event.getWhoClicked())) {
					inventories = parcours.get((Player)event.getWhoClicked());
				}
				else {
					inventories = new HashMap<Integer,List<Inventory>>();
				}
				inventories.put(getStack(event).getId(), invs);
				parcours.put((Player)event.getWhoClicked(),inventories);
				
				inventory = invs.get(0);
				
			}
			
			event.getWhoClicked().openInventory(inventory);
			event.setCancelled(true);
		}
		
		if(iv.getTitle().equals("§aEffacer le nom du villageois ?")){
			net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(item);
			if(item.getItemMeta().getDisplayName().equals("§aOui")) {
				Villager toDelete = null;
				for(Villager v : toRename) {
					if(v.getUniqueId().toString().equals(NMSitem.getTag().getString("uuid"))) {
						v.setCustomName(null);
						toDelete = v;
					}
				}
				toRename.remove(toDelete);
				
			}
			event.setCancelled(true);
			event.getWhoClicked().closeInventory();
		}
		
    	if(iv.getTitle().contains("§aInterface PNJs")) {
    		
    		EntityStack stackTest = getStack(event);
    		if(stackTest==null) {
    			event.setCancelled(true);
    			event.getWhoClicked().closeInventory();
    			event.getWhoClicked().sendMessage("§4Ce stack de villageois n'existe plus !");
    			return;
    		}
    		
    		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(item);
    		if(!(NMSitem.hasTag() && (NMSitem.getTag().hasKey("id") || NMSitem.getTag().hasKey("uuid")))) {
				event.setCancelled(true);
				return;
			}
    		
    		String displayName = "";
    		
    		if(item.hasItemMeta()) {
    			 displayName = item.getItemMeta().getDisplayName();
    		}	
    		
    		if(displayName.equals("§5Vendre") || displayName.equals("§5Acheter")) { 
    			
    			String mode;
    		
    			switch (displayName) {
    			
					case "§5Vendre" : 		
	    				mode = "Parcours IN";
						break;
						
					case "§5Acheter" :
						mode= "Parcours OUT";
						break;
						
					default :
						mode= "Parcours";
						break;
    			}
				List<Inventory> invs = getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), mode, null, null);
				event.getWhoClicked().openInventory(invs.get(0));

    		}
    		else {
    			if(NMSitem.getTag().hasKey("uuid")) {
    				
    	    		EntityStack stack = getStack(event);
    	    		Villager villager = getVillager(NMSitem.getTag().getString("uuid"),stack);
    	    		
    	    		if(villager == null) {
    	    			event.setCancelled(true);
    	    			event.getWhoClicked().closeInventory();
    	    			event.getWhoClicked().sendMessage("§4Ce villageois n'existe plus !");
    	    			return;
    	    		}
    	    		
    	    		boolean exists = spawnVillager(villager.getUniqueId().toString(),stack);
    	    		
    	    		if(exists==true || NMSitem.getTag().getString("uuid").equals(stack.getHostAsStackedEntity().getUniqueId().toString())) {
            			if(event.getClick()==ClickType.RIGHT) {
            				event.getWhoClicked().openInventory(getEntityInventory(villager.getUniqueId().toString()));
            			}
            			else {
            				event.getWhoClicked().openMerchant(villager,true);
            			}
    	    		}
    	    		else {
    	    			if(item.hasItemMeta()) {
    	    				ItemMeta im = item.getItemMeta();
    	    				im.setDisplayName("§5§o§m"+villager.getProfession().toString());
    	    				im.setLore(Arrays.asList("§4Ce villageois n'existe plus ! "));
    	    				item.setItemMeta(im);
    	    				((Player)event.getWhoClicked()).updateInventory();
    	    			}
    	    			event.setCancelled(true);
    	    		}

    			}
    			
    			if(displayName.contains("§5Vente : ") || displayName.contains("§5Achat : ")) {
    				List<Inventory> invs = getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), "Parcours", null, null);
    				Inventory inventaire = invs.get(0);
    				
    				HashMap<Integer,List<Inventory>> removeInventory = parcours.get((Player)event.getWhoClicked());
    				if(removeInventory.containsKey(getStack(event).getId())) {
    					removeInventory.remove(getStack(event).getId());
    					parcours.put((Player)event.getWhoClicked(),removeInventory);
    				}

                    event.getWhoClicked().openInventory(inventaire);
    			}
    			
    			if(item.hasItemMeta()) {
    				if(item.getItemMeta().getDisplayName().equals("§5Page suivante")) {
    					
    					String s="";
    					char[] c = iv.getTitle().toCharArray();
    					for(int i=0;i<c.length;i++) {
    						if(Character.isDigit(c[i]) && c[i-1]!='§') {
    							s+=c[i];
    						}
    					}
    					int page = Integer.valueOf(s);
    					    					
    					boolean valid = true;

    					if(parcours.containsKey((Player)event.getWhoClicked())) {
    						
    						HashMap<Integer,List<Inventory>> playerInventories = parcours.get((Player)event.getWhoClicked());
    						
    						if(playerInventories.containsKey(NMSitem.getTag().getInt("id"))) {
    							event.getWhoClicked().openInventory(playerInventories.get(NMSitem.getTag().getInt("id")).get(page)); //Decalage de 1 
    						}
    						else {
        	    				event.getWhoClicked().openInventory(getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), 
        	    						"Parcours", null, null).get(page));
    						}
    					}
    					else {
    	    				event.getWhoClicked().openInventory(getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), 
    	    						"Parcours", null, null).get(page));
    					}
    					
    					if(valid==false) {
    	    				ItemMeta im = item.getItemMeta();
    	    				im.setDisplayName("§5§o§m"+"Page suivante");
    	    				im.setLore(Arrays.asList("§4Erreur lors de l'obtention de l'inventaire ! "));
    	    				item.setItemMeta(im);
    	    				((Player)event.getWhoClicked()).updateInventory();
    					}
    				}
    				
    				if(item.getItemMeta().getDisplayName().equals("§5Page précédente")) {
    					
    					String s="";
    					char[] c = iv.getTitle().toCharArray();
    					for(int i=0;i<c.length;i++) {
    						if(Character.isDigit(c[i]) && c[i-1]!='§') {
    							s+=c[i];
    						}
    					}
    					int page = Integer.valueOf(s);
    					
    					boolean valid = true;
    					
    					if(parcours.containsKey((Player)event.getWhoClicked())) {
    						
    						HashMap<Integer,List<Inventory>> playerInventories = parcours.get((Player)event.getWhoClicked());
    						
    						if(playerInventories.containsKey(NMSitem.getTag().getInt("id"))) {
    							event.getWhoClicked().openInventory(playerInventories.get(NMSitem.getTag().getInt("id")).get(page-2)); //Decalage de 1
    						}
    						else {
        	    				event.getWhoClicked().openInventory(getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), 
        	    						"Parcours", null, null).get(page-2));
    						}
    					}
    					else {
    	    				event.getWhoClicked().openInventory(getSortedInventories(NMSitem.getTag().getInt("id"), getStack(event), 
    	    						"Parcours", null, null).get(page-2));
    					}
    					
    					if(valid==false) {
    	    				ItemMeta im = item.getItemMeta();
    	    				im.setDisplayName("§5§o§m"+"Page précédente");
    	    				im.setLore(Arrays.asList("§4Erreur lors de l'obtention de l'inventaire ! "));
    	    				item.setItemMeta(im);
    	    				((Player)event.getWhoClicked()).updateInventory();
    					}
    				}
    			}
    		}
    		
    		event.setCancelled(true);
		}
    	
    	if(iv.getTitle().equals("§aOptions PNJ")) {
    		
    		EntityStack stackTest = getStack(event);
    		if(stackTest==null) {
    			event.setCancelled(true);
    			event.getWhoClicked().closeInventory();
    			event.getWhoClicked().sendMessage("§4Ce stack de villageois n'existe plus !");
    			return;
    		}
    		
    		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(item);
    		
    		if(item == null) {
				event.setCancelled(true);
				return;
			}
			
    		if(NMSitem.getTag().hasKey("uuid")){
    			
    			EntityStack stack = getStack(event);
    			Villager villager = getVillager(NMSitem.getTag().getString("uuid"),stack);
    			
    			if(villager==null) {
    				event.setCancelled(true);
    				event.getWhoClicked().closeInventory();
    				event.getWhoClicked().sendMessage("§4Ce villageois n'existe plus !");
    				return;
    			}

    			if(item.getItemMeta().getDisplayName().equals("§5Accèder à l'inventaire")) {
    				spawnVillager(villager.getUniqueId().toString(),stack);
    				event.getWhoClicked().openMerchant(villager,true);
    			}
    			    			
    			if(item.getItemMeta().getDisplayName().equals("§5Spawner le villageois")) {
    				spawnVillager(villager.getUniqueId().toString(),stack);
    				event.getWhoClicked().closeInventory();
    			}
    			
    			if(item.getItemMeta().getDisplayName().equals("§5Destacker le villageois")) {
    				spawnVillagerOutOfStack(villager.getUniqueId().toString(), stack);
    				event.getWhoClicked().closeInventory();
    			}
    		}
    		event.setCancelled(true);
    	}
	}
    
    public Inventory getEntityInventory(String uuid) {
    	
		Inventory inv = Bukkit.createInventory(null,27,"§aOptions PNJ");			
        ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,1);
        ItemMeta imGlass = glass.getItemMeta();
        imGlass.setDisplayName(" ");
        glass.setItemMeta(imGlass);
        
        for(int i=0;i<27;i++) {
        	
        	if(i!=11 && i!=13 && i!=15) {
        		inv.setItem(i, glass);
        	}
        	else
        	{
        		ItemStack is = null;
        		
        		if(i==11) {
        			is = new ItemStack(Material.VILLAGER_SPAWN_EGG);
        			ItemMeta imRename = is.getItemMeta();
        			imRename.setDisplayName("§5Spawner le villageois");
        			is.setItemMeta(imRename);
        		}
        		if(i==13) {
        			is = new ItemStack(Material.EMERALD);
        			ItemMeta imRename = is.getItemMeta();
        			imRename.setDisplayName("§5Accèder à l'inventaire");
        			is.setItemMeta(imRename);
        		}
        		if(i==15) {
        			is = new ItemStack(Material.BARRIER);
        			ItemMeta imRename = is.getItemMeta();
        			imRename.setDisplayName("§5Destacker le villageois");
        			is.setItemMeta(imRename);
        		}
        		
        		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(is);
                NBTTagCompound comp = NMSitem.getTag();
                comp.setString("uuid",uuid);
                NMSitem.setTag(comp);
                is = CraftItemStack.asBukkitCopy(NMSitem);
                
    			inv.setItem(i, is);
        	}
        }
        
        return inv;
    }
    
    public EntityStack getStack(InventoryClickEvent event) {
    	
    	EntityStack stack = null;
    	
    	for (Entity e : event.getWhoClicked().getNearbyEntities(5, 5, 5)){
    		if(e instanceof Villager) {
    			if(plugin.getEntityStackManager().isStackedAndLoaded((LivingEntity)e)) {
    				stack = plugin.getEntityStackManager().getStack((LivingEntity)e);
    			}
    		}
    	}	
    	
    	//plugin.getEntityStackManager().getStack();
    	
    	return stack;
    }
    
    public Villager getVillager(String searchedUuid, EntityStack stack) {
    	
    	Villager villager = null;

		for(UUID villagerStackedUuid : stackedVillagers.keySet()) {
			Villager v = stackedVillagers.get(villagerStackedUuid);
			if(v.getUniqueId().toString().equals(searchedUuid)) {
				villager = v;
			}
		}
		
    	return villager;
    }
    
    public boolean spawnVillager(String uuid, EntityStack stack) {
    	
    	boolean valid = false;
    	
        for(StackedEntity se : stack.stackedEntities) {
        	if(se.getUniqueId().toString().equals(uuid)) {
        		StackedEntity seHost = stack.getHostAsStackedEntity();
        		LivingEntity entity = stack.getHostEntity();
        		
        		/* --- SAUVEGARDE --- */
        		
        		if(stackedVillagers.containsKey(seHost.getUniqueId())) {
        			stackedVillagers.put(seHost.getUniqueId(), (Villager)entity);
        		}
        		else {
        			System.out.println("§4[UltimateStacker] Erreur de synchronisation des villageois");
        		}
        		
        		/* ------------------ */
        		
				entity.remove();
	        	NBTEntity nbtEntity = NmsManager.getNbt().newEntity();
	            nbtEntity.deSerialize(se.getSerializedEntity());
	            
	            LivingEntity newEntity = (LivingEntity) nbtEntity.spawn(stack.getHostEntity().getLocation());
	            stack.stackedEntities.remove(se);
	            plugin.getDataManager().deleteStackedEntitySync(newEntity.getUniqueId());
	            
	            plugin.getEntityStackManager().updateStackSync(entity, newEntity);
	            stack.updateStackSync();

    	        stack.addEntityToStackLast(entity);
    	        plugin.getDataManager().createStackedEntitySync(stack,seHost);
    	        
    	        valid = true;
        	}
        }   	
        
        return valid;
    }
    
    public Villager spawnVillagerOutOfStack(String uuid, EntityStack stack) {
    	
    	Villager villager = null;
    	
    	if(stack.getHostAsStackedEntity().getUniqueId().toString().equals(uuid)) {
    		
    		StackedEntity seHost = stack.getHostAsStackedEntity();
    		LivingEntity entity = stack.getHostEntity();
    		
    		/* --- SAUVEGARDE --- */
    		
    		if(stackedVillagers.containsKey(seHost.getUniqueId())) {
    			stackedVillagers.put(seHost.getUniqueId(), (Villager)entity);
    		}
    		else {
    			System.out.println("§4[UltimateStacker] Erreur de synchronisation des villageois");
    		}
    		
    		/* ------------------ */
    		
    		entity.remove();
    		
    		NBTEntity nbtEntity = NmsManager.getNbt().newEntity();
            nbtEntity.deSerialize(seHost.getSerializedEntity());
    		LivingEntity newEntity = (LivingEntity) nbtEntity.spawn(stack.getHostEntity().getLocation());
    		newEntity.setCustomName("Villageois");	
    		
    		LivingEntity newEntity2 = stack.takeOneAndSpawnEntitySync(stack.getHostEntity().getLocation());
    		stack = plugin.getEntityStackManager().updateStackSync(entity, newEntity2);//
	        stack.updateStackSync();  	
    	}
    	
    	for(StackedEntity se : stack.stackedEntities) {
         	if(se.getUniqueId().toString().equals(uuid)) {
         		
 	        	NBTEntity nbtEntity = NmsManager.getNbt().newEntity();
 	            nbtEntity.deSerialize(se.getSerializedEntity());     
 	            LivingEntity newEntity = (LivingEntity) nbtEntity.spawn(stack.getHostEntity().getLocation());
 	            newEntity.setCustomName("Villageois");	
 	            stack.stackedEntities.remove(se);
 	            plugin.getDataManager().deleteStackedEntitySync(newEntity.getUniqueId());
 	            stack.updateStackSync();
         	}
    	}
    	
         if(stack.getAmount()==1) {
          	stack.destroy();
          	plugin.getDataManager().deleteHost(stack);
          }
    	 
    	 return villager;
    }

    public List<Inventory> getSortedInventories(Integer stackId, EntityStack stack, String mode,ItemStack in,ItemStack out) {
    	
    	List<Inventory> invs = new ArrayList<>();;
    	
    	if(stack.getId()==stackId) { //Pas essentiel?
    			
			List<Villager> villageois = getVillagers(stack);
			
			if(villageois==null) {
				updateVillagers(stack);
				villageois = getVillagers(stack);
			}
			
			System.out.println(villageois.size());

 			/* -------------- PARTIE INVENTAIRES --------------- */
			
	    	List<ItemStack> itemstacks = new ArrayList<>();
	    	
    		ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,1);
            ItemMeta imGlass = glass.getItemMeta();
            imGlass.setDisplayName(" ");
            glass.setItemMeta(imGlass);
            
	    	int p = 1;
	    	Inventory inv = Bukkit.createInventory(null,54,"§aInterface PNJs "+p);
	    	
	    	for(int i=0;i<villageois.size();i++) {
	    		
	    		Villager villagerEntity = villageois.get(i);
	    			    		
	    		if(mode.equals("Parcours OUT") || mode.equals("Parcours IN")) {
	    			if(mode.equals("Parcours OUT")) {
	    				List<org.bukkit.inventory.MerchantRecipe> mrs = villagerEntity.getRecipes();
	    				
	    				for(org.bukkit.inventory.MerchantRecipe mr : mrs) {
	    					if(!itemstacks.contains(mr.getResult().asQuantity(1))) {
	    	                    itemstacks.add(mr.getResult().asQuantity(1));
	    					}
	    				}
	    			}
	    			else {
	    				List<org.bukkit.inventory.MerchantRecipe> mrs = villagerEntity.getRecipes();
	    				
	    				for(org.bukkit.inventory.MerchantRecipe mr : mrs) {
	    					for(ItemStack is : mr.getIngredients()) {
	    						if(!itemstacks.contains(is.asQuantity(1))) {
		    						itemstacks.add(is.asQuantity(1));
		    					}
	    					}
	    				}
	    			}
	    		}
	    		else {
	    			double temp = (int)i/36;
		    		if(p!=temp+1 || i==0) {	
		    			if(i!=0) {
	                    	ItemStack next = new ItemStack(Material.ARROW);
	                    	ItemMeta imNext = next.getItemMeta();
	                    	imNext.setDisplayName("§5Page suivante");
	                    	next.setItemMeta(imNext);
	                    	
                    		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(next);
                    		NBTTagCompound comp = NMSitem.getTag();
                           	comp.setInt("id",stack.getId());
                           	NMSitem.setTag(comp);
                           	next = CraftItemStack.asBukkitCopy(NMSitem);
	                    	
	                    	inv.setItem(50, next);
	                    	
	                    	invs.add(inv);
			    			p+=1;
		    			}
				    	inv = Bukkit.createInventory(null,54,"§aInterface PNJs "+p);
				    	
				    	for(int i2=0;i2<54;i2+=1) {
				    		
		                    if(i2<=8 || i2>=45) {
		                		inv.setItem(i2,glass);                        	                    
		                    }
		                    
		                    if(i!=0) {
			                    if(i2==48) {
			                    	ItemStack last = new ItemStack(Material.ARROW);
			                    	ItemMeta imLast = last.getItemMeta();
			                    	imLast.setDisplayName("§5Page précédente");
			                    	last.setItemMeta(imLast);
			                    	
		                    		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(last);
		                    		NBTTagCompound comp = NMSitem.getTag();
		                           	comp.setInt("id",stack.getId());
		                           	NMSitem.setTag(comp);
		                           	last = CraftItemStack.asBukkitCopy(NMSitem);
			                    	
			                    	inv.setItem(i2, last);
			                    } 
		                    }
		                    
	                         if(i2==7) {
	                        	 if(in==null) {
	 	                        	ItemStack chest = new ItemStack(Material.CHEST);
		                	        ItemMeta imChest = chest.getItemMeta();
		                	        imChest.setDisplayName("§5Vendre");
		                	        chest.setItemMeta(imChest);
		                	        
	                        		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(chest);
	                        		NBTTagCompound comp = NMSitem.getTag();
	                               	comp.setInt("id",stack.getId());
	                               	NMSitem.setTag(comp);
	                               	chest = CraftItemStack.asBukkitCopy(NMSitem);
	                               
	                               	inv.setItem(i2, chest);
	                        	 }
	                        	 else {
	                        		ItemStack in2 = in.clone();
				    				ItemMeta imItem = in2.getItemMeta();
				    				imItem.setDisplayName("§5Vente : "+in2.getType());
				    				imItem.setLore(Arrays.asList("§fCliquer pour supprimer","§fde la recherche"));
				    				in2.setItemMeta(imItem);
				    				
				             		net.minecraft.server.v1_16_R3.ItemStack NMSitem2 = CraftItemStack.asNMSCopy(in2);
				                    NBTTagCompound comp = NMSitem2.getOrCreateTag();
				                    comp.setInt("id",stack.getId());
				                    NMSitem2.setTag(comp);
				                    ItemStack itemIN = CraftItemStack.asBukkitCopy(NMSitem2);
				    	
				    				inv.setItem(i2, itemIN);
	                        	 }
	                         }

	                     	 if(i2==8) {
	                     		 if(out==null) {
	 	                	        ItemStack emerald = new ItemStack(Material.EMERALD_ORE);
		                	        ItemMeta imEmerald = glass.getItemMeta();
		                	        imEmerald.setDisplayName("§5Acheter");
		                	        emerald.setItemMeta(imEmerald);
		                	        
	                        		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(emerald);
	                        		NBTTagCompound comp = NMSitem.getOrCreateTag();
	                        		comp.setInt("id",stack.getId());
	                        		NMSitem.setTag(comp);
	                        		emerald = CraftItemStack.asBukkitCopy(NMSitem);
		                	        
	                        		inv.setItem(i2, emerald);
	                     		 }
	                     		 else {
	                     			ItemStack out2 = out.clone();
				    				ItemMeta imItem = out2.getItemMeta();
				    				imItem.setDisplayName("§5Achat : "+out2.getType());
				    				imItem.setLore(Arrays.asList("§fCliquer pour supprimer","§fde la recherche"));
				    				out2.setItemMeta(imItem);
				    				
				             		net.minecraft.server.v1_16_R3.ItemStack NMSitem2 = CraftItemStack.asNMSCopy(out2);
				                    NBTTagCompound comp = NMSitem2.getTag();
				                    comp.setInt("id",stack.getId());
				                    NMSitem2.setTag(comp);
				                    ItemStack itemOUT = CraftItemStack.asBukkitCopy(NMSitem2);
				    	
				    				inv.setItem(i2, itemOUT);
	                     		 }
	                     	 }
		                }
		    		}
		    		
	    			List<org.bukkit.inventory.MerchantRecipe> mrs = villagerEntity.getRecipes();
		    		
	    			boolean pass;
	    			if(in == null && out == null) {
	    				pass = true;
	    			}
	    			else {
	    				pass = false;
	    			}
	    			
	    			if(in != null) {
	    				for(org.bukkit.inventory.MerchantRecipe mr : mrs) {
	    					for(ItemStack is : mr.getIngredients()) {
	    						
			             		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(in);
			                    NBTTagCompound comp = NMSitem.getOrCreateTag();
			                    comp.remove("id");
			                    NMSitem.setTag(comp);
			                    ItemStack in2 = CraftItemStack.asBukkitCopy(NMSitem);
			                    
	    						if(in2.isSimilar(is)) {
		    						pass = true;
		    					}
	    					}
	    				}
		    		}
		    		
		    		if(out != null) {
		    			for(org.bukkit.inventory.MerchantRecipe mr : mrs) {
		    				
		             		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(out);
		                    NBTTagCompound comp = NMSitem.getOrCreateTag();
		                    comp.remove("id");
		                    NMSitem.setTag(comp);
		                    ItemStack out2 = CraftItemStack.asBukkitCopy(NMSitem);
		                    
		    				if(out2.isSimilar(mr.getResult())) {
	    						pass = true;
	    					}
	    				}
		    		}
		    		
		    		if(pass==true) {
	                 	ItemStack villagerLink = new ItemStack(getProfessionBlock(villagerEntity.getProfession().toString()),1);
	                 	ItemMeta imVillagerLink = villagerLink.getItemMeta();
	                 	imVillagerLink.setDisplayName("§5§o"+villagerEntity.getProfession().toString());
	             		villagerLink.setItemMeta(imVillagerLink);
	             		
	             		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(villagerLink);
	                    NBTTagCompound comp = NMSitem.getTag();
	                    comp.setString("uuid",villagerEntity.getUniqueId().toString());
	                    NMSitem.setTag(comp);
	                    villagerLink = CraftItemStack.asBukkitCopy(NMSitem);
	                    
	                    inv.addItem(villagerLink);
		    		}
	    		}	
	    	}
	    	
	    	if(mode.equals("Parcours OUT") || mode.equals("Parcours IN")) {
	    		p=1;
	    		
	    		if(mode.equals("Parcours OUT")) {
	    			inv = Bukkit.createInventory(null,54,"§aMatériaux recherchés "+p);	
	    		}
	    		else {
	    			inv = Bukkit.createInventory(null,54,"§aMatériaux à vendre "+p);	
	    		}	
	    		
	    		int size = itemstacks.size();
	    		if(size==0) {
	    			size=1;
	    		}
	    			
	    		for(int i=0;i<size;i++) {
	    			double temp = (int)i/36;
		    		if(p!=temp+1 || i==0) {	
		    			if(i!=0) {
	                    	ItemStack next = new ItemStack(Material.ARROW);
	                    	ItemMeta imNext = next.getItemMeta();
	                    	imNext.setDisplayName("§5Page suivante");
	                    	next.setItemMeta(imNext);
	                    	
	                    	inv.setItem(50, next);
	                    	
			    			invs.add(inv);
			    			p+=1;
		    			}

		    			if(mode.equals("Parcours OUT")) {
			    			inv = Bukkit.createInventory(null,54,"§aMatériaux recherchés "+p);	
			    		}
			    		else {
			    			inv = Bukkit.createInventory(null,54,"§aMatériaux à vendre "+p);	
			    		}
		    			
		                for(int i2=0;i2<54;i2+=1)
		                {
		                    if(i2<=8 || i2>=45) {
		                		inv.setItem(i2,glass);                        	                    
		                    }
		                    
		                    if(i!=0) {
			                    if(i2==48) {
			                    	ItemStack last = new ItemStack(Material.ARROW);
			                    	ItemMeta imLast = last.getItemMeta();
			                    	imLast.setDisplayName("§5Page précédente");
			                    	last.setItemMeta(imLast);
			                    	
			                    	inv.setItem(i2, last);
			                    }
			                    
		                    }

		                    if(i2==8) {
		                    	ItemStack close = new ItemStack(Material.BARRIER);
		                    	ItemMeta imClose = close.getItemMeta();
		                    	imClose.setDisplayName("§4Fermer");
		                    	close.setItemMeta(imClose);
		                    	
			    				net.minecraft.server.v1_16_R3.ItemStack NMSitem2 = CraftItemStack.asNMSCopy(close);
		                        NBTTagCompound comp = NMSitem2.getOrCreateTag();
		                        comp.setInt("id",stack.getId());
		                        NMSitem2.setTag(comp);
		                        close = CraftItemStack.asBukkitCopy(NMSitem2);
		                    	
		                    	inv.setItem(i2, close);
		                    }
		                } 
		    		}
		    		
		    		if(itemstacks.size()!=0) {
			    		if(itemstacks.get(i).getMaxStackSize()!=1) {
			    			ItemStack is = new ItemStack(itemstacks.get(i).getType(),1);
	            	        
		    				net.minecraft.server.v1_16_R3.ItemStack NMSitem2 = CraftItemStack.asNMSCopy(is);
	                        NBTTagCompound comp = NMSitem2.getOrCreateTag();
	                        comp.setInt("id",stack.getId());
	                        NMSitem2.setTag(comp);
	                        is = CraftItemStack.asBukkitCopy(NMSitem2);
	                 		
			    			inv.addItem(is);
			    		}
			    		else {
			    			ItemStack is = itemstacks.get(i);
			    			
	                 		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(is);
	                        NBTTagCompound comp = NMSitem.getOrCreateTag();
	                        comp.setInt("id",stack.getId());
	                        NMSitem.setTag(comp);
	                        is = CraftItemStack.asBukkitCopy(NMSitem);
	                        
			    			inv.addItem(is);
			    		}
		    		}
	    		}
	    	}
	    	
	    	invs.add(inv);
    	}

    	return invs;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerInteract(PlayerInteractEntityEvent event)
    {
        if(event.getRightClicked() instanceof LivingEntity) {
        	
        	LivingEntity entity = (LivingEntity)event.getRightClicked();
        	
        	if(entity.getType()==EntityType.VILLAGER) {
        		
        		if(event.getPlayer().isSneaking()) {

	        		if (!plugin.getEntityStackManager().isStackedAndLoaded(entity)) {
	        			
	    				if(entity.getCustomName()!=null && entity.getCustomName().equals("Villageois")) {
	    					if(entity.hasAI()) { //Empêche que l'event se produise sur des shopkeepers
	        					event.getPlayer().openInventory(deleteNameTagInv(event.getRightClicked()));
	        					toRename.add((Villager)entity);
	    					}
	    				}
	    				else {
	    					return;
	    				}
	        		}
	        		else
	        		{
	            		Villager v = (Villager)entity;
	            		//event.getPlayer().sendMessage(""+v.getRestocksToday());

	            		World w = event.getPlayer().getWorld();
	            		//event.getPlayer().sendMessage("getTime : "+w.getTime());
	                 	
	                 	EntityStack stack = plugin.getEntityStackManager().getStack(entity);
	                 	
	         			List<Inventory> invs = null;
	         			boolean found = false;
	         			
	         			
	         			for(Player aPlayer : parcours.keySet()) {
	         				if(aPlayer.equals(event.getPlayer())) {
	         					if(parcours.get(aPlayer).keySet().contains(stack.getId())) {
	     	     					invs = parcours.get(aPlayer).get(stack.getId());
	     	     					found = true;
	     						}
	     					}
	     				}
	         			
	         			/*
	         			//Check changement de métier villageois host
	         			if(found==true) {
		         			List<Villager> villagers = getVillagers(stack);
		         			Boolean trouve = false;
	    					for(int i=0;i<villagers.size() && trouve==false;i++) {
	    						if(villagers.get(i).getUniqueId()==v.getUniqueId()) {
	    							System.out.println("trouve");
	    							if(villagers.get(i).getRecipes()!=v.getRecipes()) {
	    								found=false;
	    								trouve=true;
	    								System.out.println("found false");
	    							}
	    						}
	    					}
	         			}	    				
						*/
	         			
	         			if(found==false) {
	         				invs = getSortedInventories(stack.getId(), stack, "Parcours", null, null);
	         			}
	         			
	         			event.getPlayer().openInventory(invs.get(0));
	         			
	         			event.setCancelled(true);
	        		}
        		}
            }
        }  
    }
    
    public List<Villager>getVillagers(EntityStack stack){
    	List<Villager> villagers = new ArrayList<Villager>();	
		
     	if(stack.getHostEntity()!=null) {
     		updateHost(stack);
     	}
     	
     	if(stackedVillagers.containsKey(stack.getHostAsStackedEntity().getUniqueId())){
     		villagers.add(stackedVillagers.get(stack.getHostAsStackedEntity().getUniqueId()));
     	}
     	
     	for(StackedEntity se : stack.stackedEntities) {
     		if(stackedVillagers.containsKey(se.getUniqueId())) {
     			villagers.add(stackedVillagers.get(se.getUniqueId()));
     		}
     	}

     	if(villagers.size()==stack.getAmount()) {
     		return villagers;
     	}
     	else {
     		return null;
     	}
    }
    
    public Inventory deleteNameTagInv(Entity e) {
    	
    	Inventory inv = Bukkit.createInventory(null,9,"§aEffacer le nom du villageois ?");
    	
        ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,1);
        ItemMeta imGlass = glass.getItemMeta();
        imGlass.setDisplayName(" ");
        glass.setItemMeta(imGlass);
        
        ItemStack greenGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE,1);
        ItemMeta imGreenGlass = greenGlass.getItemMeta();
        imGreenGlass.setDisplayName("§aOui");
        greenGlass.setItemMeta(imGreenGlass);
        
 		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(greenGlass);
        NBTTagCompound comp = NMSitem.getTag();
        comp.setString("uuid",e.getUniqueId().toString());
        NMSitem.setTag(comp);
        greenGlass = CraftItemStack.asBukkitCopy(NMSitem);
        
        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE,1);
        ItemMeta imRedGlass = redGlass.getItemMeta();
        imRedGlass.setDisplayName("§4Non");
        redGlass.setItemMeta(imRedGlass);
        
        for(int i=0;i<9;i++) {
        	inv.setItem(i,glass);
        }
        
        inv.setItem(3, greenGlass);
        inv.setItem(5, redGlass);
        
    	return inv;
    }
    
    public Material getProfessionBlock(String profession) {
    	
    	Material jobBlock;
    	
    	switch(profession) {
    	
    	case "FISHERMAN" : 
    		jobBlock = Material.BARREL;
    		break;
    	case "CARTOGRAPHER" : 
    		jobBlock = Material.CARTOGRAPHY_TABLE;
    		break;
    	case "BUTCHER" : 
    		jobBlock = Material.SMOKER;
    		break;
    	case "TOOLSMITH" : 
    		jobBlock = Material.SMITHING_TABLE;
    		break;
    	case "WEAPONSMITH" : 
    		jobBlock = Material.GRINDSTONE;
    		break;
    	case "ARMORER" : 
    		jobBlock = Material.BLAST_FURNACE;
    		break;
    	case "LEATHERWORKER" : 
    		jobBlock = Material.CAULDRON;
    		break;
    	case "CLERIC" : 
    		jobBlock = Material.BREWING_STAND;
    		break;
    	case "FARMER" : 
    		jobBlock = Material.COMPOSTER;
    		break;
    	case "FLETCHER" : 
    		jobBlock = Material.FLETCHING_TABLE;
    		break;
    	case "SHEPHERD" : 
    		jobBlock = Material.LOOM;
    		break;
    	case "LIBRARIAN" : 
    		jobBlock = Material.LECTERN;
    		break;
    	case "MASON" : 
    		jobBlock = Material.STONECUTTER;
    		break;
    	default : 
    		jobBlock = Material.VILLAGER_SPAWN_EGG;
    		break;
    	}
    	
    	return jobBlock;
    }
    
    /*     PARTIE FARM DES VILLAGEOIS -- ANNULE
    
    @EventHandler
    public void onFarm(EntityChangeBlockEvent event) {
    	if(event.getEntityType()==EntityType.VILLAGER){
    		LivingEntity entity = (LivingEntity)event.getEntity();
    		if (plugin.getEntityStackManager().isStackedAndLoaded(entity)) {
    			EntityStack stack = plugin.getEntityStackManager().getStack(entity);
    			for(StackedEntity se : stack.stackedEntities) {
    				
    			}
    		}
    	}
    }
    
    */
    
    public void updateVillagers(EntityStack stack) {

		int nb = stack.getAmount();
		for(int i=0;i<nb;i++) {
			
			StackedEntity se = stack.getHostAsStackedEntity();
			LivingEntity le = stack.getHostEntity();
			
			Villager villager = (Villager)le;

			if(!(stackedVillagers.containsKey(se.getUniqueId())) || i==0) { //i==0 => updateHost
				stackedVillagers.put(se.getUniqueId(),villager);
			}
		
			LivingEntity entity = stack.getHostEntity();
			entity.remove();
 	        LivingEntity newEntity2 = stack.takeOneAndSpawnEntitySync(entity.getLocation());
 	        stack = plugin.getEntityStackManager().updateStackSync(entity, newEntity2);
 	        stack.updateStackSync();
 	        stack.addEntityToStackLast(entity);
			plugin.getDataManager().createStackedEntitySync(stack,se);
		}
    }
    
    
    public void updateHost(EntityStack stack) {
    	StackedEntity se = stack.getHostAsStackedEntity();
		LivingEntity le = stack.getHostEntity();
		Villager villager = (Villager)le;
		stackedVillagers.put(se.getUniqueId(),villager);
    }
    
    @EventHandler
    public void onBreed(EntityBreedEvent event) {
    	if(event.getMother() instanceof Villager || event.getFather() instanceof Villager) {
    		EntityStack stack = null;
    		LivingEntity e = null;
    		
    		if(plugin.getEntityStackManager().isStackedAndLoaded(event.getMother())) {
    			stack = plugin.getEntityStackManager().getStack(event.getMother());
    			e = stack.getHostEntity();
    		}
    		else
    		{
    			if(plugin.getEntityStackManager().isStackedAndLoaded(event.getFather())) {
    				stack = plugin.getEntityStackManager().getStack(event.getFather());
    				e = stack.getHostEntity();
    			}
    		}
    		
    		if(stack!=null) {
    			Boolean pass = false;
    			int limit = 0;
    			
    			if(Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
    				
    	       		Island island = BentoBox.getInstance().getIslands().getIslandAt(e.getLocation()).orElse(null);
            		if(island!=null) {
            			if(breedLimit.containsKey(island)){
            				limit = breedLimit.get(island);
            				if(limit<5) {			
            					pass = true;
            				}
            			}
            			else {
            				pass = true;
            			}
            		}
            		
            		if(pass==true) {
            			
            			int toAdd = 0;
            			while(limit<5 && stack.stackedEntities.size() - toAdd > 0) {
            				
            				toAdd+=1;
            				Entity entity = Bukkit.getWorld("bskyblock_world").spawnEntity(e.getLocation(), EntityType.VILLAGER);
            				((Villager)entity).setBaby();
            			}
            			
            			breedLimit.put(island, limit+toAdd);
            			
            		}
    			}
    			else
    			{
    				System.out.println("[UltimateStacker] §4Impossible d'établir la dépendance avec le plugin BentoBox");
    			}
    		}
    	}
    }
    
    public void updateBreedLimit() {
    	new BukkitRunnable() {
			
			@Override
			public void run() {
				breedLimit.clear();
			}
			
		}.runTaskLater(plugin, 22000); //Marge de 2000 ticks
    }
    
    
    @EventHandler
    public void onVillagerSpawn(EntitySpawnEvent event) {
    	if(event.getLocation().getWorld().getName().equals("bskyblock_world")) {
    		Island spawnIsland = BentoBox.getInstance().getIslands().getIslandAt(event.getLocation()).orElse(null);
			if ( spawnIsland != null ) {
				if(event.getEntityType()==EntityType.VILLAGER) {
					
					Boolean villagerFlag = WorldGuardHook.isEnabled() ? WorldGuardHook.getBooleanFlag(event.getLocation(), "villager-stacking") : null;
					
					int villagers = event.getLocation().getWorld().getEntitiesByClasses(event.getEntityType().getEntityClass()).stream()
							.filter(e -> e instanceof Villager)
							.filter(e -> isOnIsland(spawnIsland, e) && ((Villager)event.getEntity()).hasAI())
							.mapToInt(e-> 1).sum();
				
					String msg = "";
					
					if(villagerFlag==true) {
						
						int stackedVillagers = event.getLocation().getWorld().getEntitiesByClasses(event.getEntityType().getEntityClass()).stream()
								.filter(e -> e instanceof Villager)
								.filter(e -> isOnIsland(spawnIsland, e) && ((Villager)event.getEntity()).hasAI())
								.filter(e -> plugin.getEntityStackManager().isStackedAndLoaded((LivingEntity)e))
								.mapToInt(e -> plugin.getEntityStackManager().getStack((LivingEntity)e).getAmount()).sum();
						
						if(villagers >= cfg.getInt("villagers")) {
							msg = (ChatColor.RED+"Vous ne pouvez pas dépasser la limite de "+ cfg.getInt("villagers")+" villageois déstackés !");
						}
						
						if(stackedVillagers >= cfg.getInt("stackedVillagers")) {
							msg = (ChatColor.RED+"Vous ne pouvez pas dépasser la limite de "+cfg.getInt("stackedVillagers")+" villageois !");
						}
					}
					else {
						if(villagers >= cfg.getInt("oldVillagers")) {
							msg = (ChatColor.RED+"Vous ne pouvez pas dépasser la limite de "+ cfg.getInt("oldVillagers")+" villageois !");
						}
					}

					if(msg!="") {
						event.setCancelled(true);
						for ( Entity entity : event.getLocation().getNearbyEntities(4, 4, 4) ) {
							if ( entity.getType() == EntityType.PLAYER ) {			
								((Player) entity).sendMessage(msg);
							}	
						}
					}
				}
			}
    	}
    }
    
    
	private boolean isOnIsland(Island island, Entity entity) {
		return entity.getLocation().getX() >= island.getMinProtectedX() && entity.getLocation().getX() <= island.getMaxProtectedX() && 
				entity.getLocation().getZ() >= island.getMinProtectedZ() && entity.getLocation().getZ() <= island.getMaxProtectedZ(); 
	}
    

    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity)) return;
        Player player = event.getPlayer();
        LivingEntity entity = (LivingEntity)event.getRightClicked();

        ItemStack item = player.getInventory().getItemInHand();

        if (!plugin.getEntityStackManager().isStackedAndLoaded(entity)) return;

        if (item.getType() != Material.NAME_TAG && !correctFood(item, entity)) return;

        EntityStack stack = plugin.getEntityStackManager().getStack(entity);

        if (stack.getAmount() <= 1
                || item.getType() == Material.NAME_TAG
                && Settings.SPLIT_CHECKS.getStringList().stream().noneMatch(line -> Split.valueOf(line) == Split.NAME_TAG)
                || item.getType() != Material.NAME_TAG
                && Settings.SPLIT_CHECKS.getStringList().stream().noneMatch(line -> Split.valueOf(line) == Split.ENTITY_BREED))
            return;

        if (item.getType() == Material.NAME_TAG)
            event.setCancelled(true);
        else if (entity instanceof Ageable && !((Ageable) entity).isAdult())
            return;

        stack.releaseHost();

        if (item.getType() == Material.NAME_TAG) {
            entity.setCustomName(item.getItemMeta().getDisplayName());
        } else {
            if (entity instanceof Ageable
                    && !((Ageable) entity).isAdult()) {
                return;
            }
            entity.setMetadata("inLove", new FixedMetadataValue(plugin, true));

            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (entity.isDead()) return;
                entity.removeMetadata("inLove", plugin);
            }, 20 * 20);
        }
    }

    private boolean correctFood(ItemStack is, Entity entity) {
        Material type = is.getType();
        switch (entity.getType().name()) {
            case "COW":
            case "MUSHROOM_COW":
            case "SHEEP":
                return type == Material.WHEAT;
            case "PIG":
                return type == Material.CARROT || (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9) && type == Material.BEETROOT) || type == Material.POTATO;
            case "CHICKEN":
                return type == CompatibleMaterial.WHEAT_SEEDS.getMaterial()
                        || type == Material.MELON_SEEDS
                        || type == Material.PUMPKIN_SEEDS
                        || type == CompatibleMaterial.BEETROOT_SEEDS.getMaterial();
            case "HORSE":
                return (type == Material.GOLDEN_APPLE || type == Material.GOLDEN_CARROT) && ((Horse)entity).isTamed();
            case "WOLF":
                return type == CompatibleMaterial.BEEF.getMaterial()
                        || type == CompatibleMaterial.CHICKEN.getMaterial()
                        || type == CompatibleMaterial.COD.getMaterial()
                        || type == CompatibleMaterial.MUTTON.getMaterial()
                        || type == CompatibleMaterial.PORKCHOP.getMaterial()
                        || type == CompatibleMaterial.RABBIT.getMaterial()
                        || CompatibleMaterial.SALMON.matches(is)
                        || type == CompatibleMaterial.COOKED_BEEF.getMaterial()
                        || type == CompatibleMaterial.COOKED_CHICKEN.getMaterial()
                        || type == CompatibleMaterial.COOKED_COD.getMaterial()
                        || type == CompatibleMaterial.COOKED_MUTTON.getMaterial()
                        || type == CompatibleMaterial.COOKED_PORKCHOP.getMaterial()
                        || type == CompatibleMaterial.COOKED_RABBIT.getMaterial()
                        || CompatibleMaterial.COOKED_SALMON.matches(is)
                        && ((Wolf) entity).isTamed();
            case "OCELOT":
                return (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)
                        ? type == Material.SALMON
                        || type == Material.COD
                        || type == Material.PUFFERFISH
                        || type == Material.TROPICAL_FISH
                        : type == CompatibleMaterial.COD.getMaterial()); // Now broken in 1.13 ((Ocelot) entity).isTamed()
            case "PANDA":
                return (type == Material.BAMBOO);
            case "FOX":
                return type == Material.SWEET_BERRIES;
            case "CAT":
                return (type == Material.COD || type == Material.SALMON) && ((Cat) entity).isTamed();
            case "RABBIT":
                return type == Material.CARROT || type == Material.GOLDEN_CARROT || type == Material.DANDELION;
            case "LLAMA":
                return type == Material.HAY_BLOCK;
            case "TURTLE":
                return type == Material.SEAGRASS;
            default:
                return false;
        }
    }

}
