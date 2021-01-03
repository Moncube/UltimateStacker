package com.songoda.ultimatestacker.listeners;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.nms.NmsManager;
import com.songoda.core.nms.nbt.NBTEntity;
import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.settings.Settings;
import com.songoda.ultimatestacker.stackable.entity.EntityStack;
import com.songoda.ultimatestacker.stackable.entity.Split;
import com.songoda.ultimatestacker.stackable.entity.StackedEntity;

import net.minecraft.server.v1_16_R3.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

public class InteractListeners implements Listener {

    private final UltimateStacker plugin;
    
    public static HashMap<Integer,HashMap<UUID,Villager>> stacks = new HashMap<Integer,HashMap<UUID,Villager>>();

    public InteractListeners(UltimateStacker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onVillagerGuiClick(InventoryClickEvent event) {
    	InventoryView iv = event.getView();
    	
    	if(iv.getTitle().equals("§aInterface PNJs")) {
    		
    		ItemStack item = event.getCurrentItem();
    		
    		//if(item.getType() == Material.VILLAGER_SPAWN_EGG) {
    			
                net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(item);
                if(NMSitem.getTag().hasKey("uuid")) {
                
				//System.out.println("uuid : "+uuid);
				
					for (Entity e : event.getWhoClicked().getNearbyEntities(5, 5, 5)){
						
						if(plugin.getEntityStackManager().isStackedAndLoaded((LivingEntity)e)) {
							
							//System.out.println("loaded");
							
							EntityStack stack = plugin.getEntityStackManager().getStack((LivingEntity)e);
							
							for(int id : stacks.keySet()) {
								
								//System.out.println("id : "+id);
								
								//System.out.println("id stack : "+stack.getId());
	
								if(id==stack.getId()) {
									
									//System.out.println("same stack id");
									
									HashMap<UUID,Villager> villagerHashMap = stacks.get(id);
									
									for(UUID villagerStackedUuid : villagerHashMap.keySet()) {
										
										Villager villager = villagerHashMap.get(villagerStackedUuid);
										
										//System.out.println(pnjTest(villager.getUniqueId().toString()));
										
										//System.out.println("tag : "+NMSitem.getTag().getString("uuid"));
										
										if(villager.getUniqueId().toString().equals(NMSitem.getTag().getString("uuid"))) {
											
											//System.out.println("changement host");
											
				                	        for(StackedEntity se : stack.stackedEntities) {
				                	        	
				                	        	if(se.getUniqueId()==villagerStackedUuid) {
				                	        		
				                	        		StackedEntity seHost = stack.getHostAsStackedEntity();
	    				        					LivingEntity entity = stack.getHostEntity();
	    				            				entity.remove();
					                	        	NBTEntity nbtEntity = NmsManager.getNbt().newEntity();
					                	            nbtEntity.deSerialize(se.getSerializedEntity());
					                	            LivingEntity newEntity = (LivingEntity) nbtEntity.spawn(e.getLocation());
					                	            stack.stackedEntities.remove(se);
					                	            plugin.getDataManager().deleteStackedEntitySync(newEntity.getUniqueId());
					                	            
					                	            plugin.getEntityStackManager().updateStackSync(entity, newEntity);
					                	            stack.updateStackSync();

	    				                	        stack.addEntityToStackLast(entity);
	    				                	        plugin.getDataManager().createStackedEntitySync(stack,seHost);
				                	        	}
				                	        }   	       
										}
									}
								}
							}
						}
					}
				}
			//}
    		event.setCancelled(true);
		}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerInteract(PlayerInteractEntityEvent event)
    {
        Player player = event.getPlayer();
        LivingEntity entity = (LivingEntity)event.getRightClicked();
        
        if(entity.getType()==EntityType.VILLAGER) {
        	
        	if (!plugin.getEntityStackManager().isStackedAndLoaded(entity)) return;
        	
        	EntityStack stack = plugin.getEntityStackManager().getStack(entity);
        	
        	if(stack.getAmount()>1) {
        		
        		if(!player.isSneaking()) {
        			/* A determiner */
        		}	
        		else {
        			
        			HashMap<UUID,Villager> villagerHashMap = new HashMap<UUID,Villager>();
        			List<Villager> liste = new ArrayList<>();
        			
        			int nb = stack.getAmount();
        			
        			for(int i=0;i<nb;i++) {
        				
        				StackedEntity se = stack.getHostAsStackedEntity();
        				LivingEntity le = stack.getHostEntity();
        				
        				Villager villager = (Villager)le;
        				liste.add(villager);
        				
        				villagerHashMap.put(se.getUniqueId(), villager);
        			
    					entity = stack.getHostEntity();
        				entity.remove();
            	        LivingEntity newEntity2 = stack.takeOneAndSpawnEntitySync(entity.getLocation());
            	        stack = plugin.getEntityStackManager().updateStackSync(entity, newEntity2);//
            	        stack.updateStackSync();
            	        stack.addEntityToStackLast(entity);
    					plugin.getDataManager().createStackedEntitySync(stack,se);
        			}
        			
        			stacks.put(stack.getId(),villagerHashMap);
        			
        			
        			/* ------------------- PARTIE INVENTAIRE ------------------ */
        			
        			Inventory inv = Bukkit.createInventory(null,54,"§aInterface PNJs");			
                    ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,1);
                    ItemMeta imGlass = glass.getItemMeta();
                    imGlass.setDisplayName(" ");
                    glass.setItemMeta(imGlass);
        			
                    int indexList = 0;
                    
                    for(int i=0;i<54;i+=1)
                    {
                        if(i<=8 || i>=45)
                        {
                            inv.setItem(i,glass);
                        }

                        if(i<=44 && i>=9)
                        {   
                            if(indexList<stack.getAmount())
                            {
                            	Villager villagerEntity = liste.get(indexList);
                            	
                            	ItemStack villagerLink = new ItemStack(getProfessionBlock(villagerEntity.getProfession().toString()),1);
                            	ItemMeta imVillagerLink = villagerLink.getItemMeta();
                            	imVillagerLink.setDisplayName("§5§o"+villagerEntity.getProfession().toString());
                        		villagerLink.setItemMeta(imVillagerLink);
                        		
                        		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(villagerLink);
                                NBTTagCompound comp = NMSitem.getTag();
                                comp.setString("uuid",villagerEntity.getUniqueId().toString());
                                NMSitem.setTag(comp);
                                villagerLink = CraftItemStack.asBukkitCopy(NMSitem);

                                inv.setItem(i,villagerLink);
                                indexList++;
                            }
                        }
                    }		
                    event.setCancelled(true);
    				player.openInventory(inv);
        		}
        	}
        }
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
    	case "SHEPERD" : 
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
