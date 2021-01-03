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
    		
    		if(item.getType() == Material.VILLAGER_SPAWN_EGG) {
    			
    			ItemMeta im = item.getItemMeta();
    			List<String> lore = im.getLore();
    			
                net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(item);
                if(NMSitem.getTag().hasKey("uuid")) {
                
				//String uuid = lore.get(i).replaceAll("§", "");
				
				//String uuid = lore.get(i).replaceAll("§", "");
				
				//System.out.println("uuid : "+uuid);
				
					for (Entity e : event.getWhoClicked().getNearbyEntities(5, 5, 5)){
						
						if(plugin.getEntityStackManager().isStackedAndLoaded((LivingEntity)e)) {
							
							System.out.println("loaded");
							
							EntityStack stack = plugin.getEntityStackManager().getStack((LivingEntity)e);
							
							for(int id : stacks.keySet()) {
								
								System.out.println("id : "+id);
								
								System.out.println("id stack : "+stack.getId());
	
								if(id==stack.getId()) {
									
									System.out.println("same stack id");
									
									HashMap<UUID,Villager> villagerHashMap = stacks.get(id);
									
									for(UUID villagerStackedUuid : villagerHashMap.keySet()) {
										
										Villager villager = villagerHashMap.get(villagerStackedUuid);
										
										//System.out.println(pnjTest(villager.getUniqueId().toString()));
										
										System.out.println("tag : "+NMSitem.getTag().getString("uuid"));
										
										if(villager.getUniqueId().toString().equals(NMSitem.getTag().getString("uuid"))) {
											
											System.out.println("changement host");
											
				                	        for(StackedEntity se : stack.stackedEntities) {
				                	        	
				                	        	if(se.getUniqueId()==villagerStackedUuid) {
				                	        		
	    				        					LivingEntity entity = stack.getHostEntity();
	    				            				entity.remove();
					                	        	NBTEntity nbtEntity = NmsManager.getNbt().newEntity();
					                	            nbtEntity.deSerialize(se.getSerializedEntity());
					                	            LivingEntity newEntity = (LivingEntity) nbtEntity.spawn(e.getLocation());
					                	            stack.stackedEntities.remove(se);
					                	            plugin.getDataManager().deleteStackedEntity(newEntity.getUniqueId());
					                	            plugin.getEntityStackManager().updateStack(entity, newEntity);
	    				                	        stack.addEntityToStackLast(entity);
	    				                	        stack.updateStack();
				                	        	}
				                	        }   	       
										}
									}
								}
							}
						}
					}
				}
			}
    		event.setCancelled(true);
		}
    }
    
    
    public static String convertToInvisibleString(String s) {
    	
    	String invString="";
		for(char c : s.toCharArray()){
		  invString+="§"+String.valueOf(c);
		}
		return invString;
    }
    
    public static String pnjTest(String s) {
    	
    	String result="";
    	char[] tab = s.toCharArray();
    	for(int i=0;i<tab.length;i++) {
    		if(i!=tab.length && i+1!=tab.length && tab[i+1]=='-') {
    			result+=String.valueOf(tab[i]);
    			result+=String.valueOf(tab[i+1]);
    		}
    		if(i+1==tab.length) {
    			result+=String.valueOf(tab[i]);
    		}
    	}
    	return result;
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
        		
        		int nbr = stack.getAmount();
        		
        		if(!player.isSneaking()) {
        		
        			/*
        			List<Entity> entites =  player.getNearbyEntities(30, 30, 30);
        			*/
        		}	
        		else {
        			
        			//player.sendMessage("taille se : "+stack.getAmount());
        			
        			HashMap<UUID,Villager> villagerHashMap = new HashMap<UUID,Villager>();
        			List<Villager> liste = new ArrayList<>();
        			
        			List<StackedEntity> seList = new ArrayList<>();
        			
        			EntityStack lastStack = null;
        			
        			int nb = stack.getAmount();
        			
        			for(int i=0;i<nb;i++) {
        				
        				player.sendMessage("i = "+i);
        				StackedEntity se = stack.getHostAsStackedEntity();
        				LivingEntity le = stack.getHostEntity();
        				
        				player.sendMessage(le.getUniqueId().toString());
        				
            	      
        				Villager villager = (Villager)le;
        				liste.add(villager);
        				
        				villagerHashMap.put(se.getUniqueId(), villager);
        			
    					entity = stack.getHostEntity();
        				entity.remove();
        			
        				//plugin.getDataManager().deleteStackedEntity(entity.getUniqueId());
        				
            	        LivingEntity newEntity2 = stack.takeOneAndSpawnEntity(entity.getLocation());
            	        if(newEntity2 == null) {
            	        	player.sendMessage("newEntity2 null");
            	        }
            	        //plugin.getDataManager().deleteStackedEntity(stack.getHostUniqueId());//?
            	        stack = plugin.getEntityStackManager().updateStack(entity, newEntity2);
            	        if(stack == null) {
            	        	player.sendMessage("stack null");
            	        }
            	        
            	        
            	        stack.updateStack();
            	        
            	        player.sendMessage("amount : "+stack.getAmount());
            	        
            	        
            	        stack.addEntityToStackLast(entity);
            	        if(i!=10) {
            	        	player.sendMessage("création entity bd :"+se.getUniqueId());
        					plugin.getDataManager().createStackedEntity(stack,se);
        					//seList.add(se);
        				}
        				
        			}
        			
        			/*
        			for(StackedEntity se : seList) {
        				player.sendMessage("creation entity bd : "+se.getUniqueId().toString());
        			}
        			
        			plugin.getDataManager().createStackedEntities(stack, seList);
        			*/
        			
        			/*
        	        List<StackedEntity> seList = new ArrayList<StackedEntity>();
        	        for(StackedEntity seEntity : stack.stackedEntities) {
        	        	seList.add(seEntity);
        	        }
        	        
        	        //plugin.getDataManager().deleteStackedEntities(seList);
        	        plugin.getDataManager().createStackedEntities(stack, seList);
        	        
        	        */
        			stacks.put(stack.getId(),villagerHashMap);
        			
        			
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
                				
                            	ItemStack egg = new ItemStack(Material.VILLAGER_SPAWN_EGG,1);
                            	ItemMeta imEgg = egg.getItemMeta();
                            	
                            	Villager villagerEntity = liste.get(indexList);
                            	
                            	//Location loc = villagerEntity.getMemory(MemoryKey.JOB_SITE);
                            	
                            	/*
                            	List<MerchantRecipe> recipes = villagerEntity.getRecipes();
                            		
                            	if(recipes!=null) {
                            		
                            		for(MerchantRecipe mr : recipes) {
                            			
                            			
                            		}
                            	}
                            	*/
                            	
                            	List<String> lore = new ArrayList<String>();
                            	
                            	lore.add(villagerEntity.getProfession().toString());
                        		
                        		//lore.add(convertToInvisibleString(villagerEntity.getUniqueId().toString()));
                        		
                        		imEgg.setLore(lore);
                        		egg.setItemMeta(imEgg);
                        		
                        		net.minecraft.server.v1_16_R3.ItemStack NMSitem = CraftItemStack.asNMSCopy(egg);
                                NBTTagCompound comp = NMSitem.getTag();
                                comp.setString("uuid",villagerEntity.getUniqueId().toString());
                                NMSitem.setTag(comp);
                                egg = CraftItemStack.asBukkitCopy(NMSitem);

                                inv.setItem(i,egg);
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
