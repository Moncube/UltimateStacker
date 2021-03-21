/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.songoda.ultimatestacker.tasks;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftAbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.material.Bed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.listeners.InteractListeners;
import com.songoda.ultimatestacker.stackable.entity.EntityStack;
import com.songoda.ultimatestacker.stackable.entity.StackedEntity;

import io.lumine.xikage.mythicmobs.adapters.bukkit.entities.BukkitIronGolem;
import net.minecraft.server.v1_16_R3.EntityIronGolem;
import net.minecraft.server.v1_16_R3.EntityVillager;
import net.minecraft.server.v1_16_R3.VillagerType;

/**
 *
 * @author Florian
 */
public class GolemSpawningTask extends BukkitRunnable {
    
	private final UltimateStacker plugin;
        
    public GolemSpawningTask(UltimateStacker plugin)
    {
        this.plugin = plugin;
        runTaskTimer(plugin, 0, 1000);
    }
    
    @Override
    public void run()
    {
    	//System.out.println("GolemTask activée");
        //World w = Bukkit.getWorld("askyblock");
    	for(World w : Bukkit.getWorlds()) {
    		
    		for(Entity e : w.getEntities()) {
    			
    			if(e.getType()==EntityType.VILLAGER) {

    				if(!((Villager)e).isSleeping()) {

    	        		if (plugin.getEntityStackManager().isStackedAndLoaded((LivingEntity)e)) {

    	        			Villager v = (Villager)e;
    	        			
    	        			/* ---- Détection d'un lit valide autour du villageois ---- */
    	        			
    	        			Boolean bedPass = false;
    	        			ArrayList<Block> beds = getBeds(v.getLocation().getBlock(), 8);
    	        			
    	        			for(int i=0;i<beds.size() && bedPass==false;i++) {
    	        				
    	        				/*
    	        				double distance = v.getLocation().distanceSquared(beds.get(i).getLocation());	
    	        				EntityVillager nmsVillager;
        	        			nmsVillager.getNavigation().a(beds.get(i).getX(),beds.get(i).getY(),beds.get(i).getZ(),distance*0.01);
        	        			*/
    	        				
    	        				
    	        				// BROKEN -> PATHFINDING PAR NMS OU A LA MAIN ?
    	        				
    	        				//if(v.sleep(beds.get(i).getLocation())) {
    	        					//v.wakeup();
    	        					bedPass=true;
    	        				//}
    	        			}
    	        			
    	        			/* ---- Détection d'un zombie qui aggro le villageois et détection de la non présence de golems ---- */
    	        			
    	        			Boolean sightPass = false;
    	        			Boolean golemPass = true;
    	        			List<Entity> entities = v.getNearbyEntities(10, 10, 10);
    	        			for(Entity entity : entities) {
    	        				if(entity.getType()==EntityType.ZOMBIE || entity.getType()==EntityType.ZOMBIE_VILLAGER) {
    	        					if(((LivingEntity)entity).hasLineOfSight(e)) {
    	        						sightPass = true;
    	        					}
    	        				}
    	        				
    	        				if(entity.getType()==EntityType.IRON_GOLEM) {
    	        					golemPass=false;
    	        				}
    	        			}
    	        			
    	        			/*
    	        			
    	        			if(sightPass==true) {
    	        				System.out.println("sightPass = OK");
    	        			}
    	        			if(bedPass==true) {
    	        				System.out.println("bedPass = OK");
    	        			}
    	        			if(golemPass==true) {
    	        				System.out.println("golemPass = OK");
    	        			}
    	        			
    	        			*/
    	        			
    	        			if(sightPass==true && bedPass==true && golemPass==true) {
    	        				EntityStack stack = plugin.getEntityStackManager().getStack((LivingEntity)e);
    	        				int nb = stack.getAmount();
    	        				
    	        				List<Villager> stackedVillagers = plugin.getInteractListeners().getVillagers(stack);
    	        				if(stackedVillagers==null) {
    	        					plugin.getInteractListeners().updateVillagers(stack);
    	        					stackedVillagers = plugin.getInteractListeners().getVillagers(stack);
    	        				}
    	        				
    	        				List<Location> golems = new ArrayList<Location>();
    	        				
    	        				List<Location> temp = new ArrayList<Location>();
    	        				

	        					for(int i=0;i<1000;i++) {
        	        				for(Villager villager : stackedVillagers) {
        	        					if(villager.getRecipes()!=null) {
			        						Location l = trySpawningGolem(w,e);
			        						if(l!=null && !temp.contains(l)) {
			        							Entity e2 = w.spawnEntity(l, EntityType.IRON_GOLEM);
			        							e2.remove();
			        							golems.add(l);
			        							temp.add(l);
		    	        					}
            	        				}
        	        					/*
        	        					else
        	        					{
        	        						System.out.println("Villager invalide");
        	        					}
        	        					*/
	        						}
        	        				temp.clear();
	        					}


    	        				//System.out.println("golems à spawn:"+golems.size());
    	        				
    	        				/*
    	        				for(int i=0;i<1000;i++) {
    	        					Random rnd = new Random();
    	        					int nombreAttendu = rnd.nextInt(7001);
    	        					int nombreTire = rnd.nextInt(7001);
    	        					if(nombreAttendu == nombreTire) {
    	        						
    	        					}
    	        				}
    	        				*/
    	        				
    	        			}
    	        		}
    				}
    			}
    		}
		}  
    }
    
    private ArrayList<Block> getBeds(Block debut, int rayon){
        ArrayList<Block> beds = new ArrayList<Block>();
        for(double x = debut.getLocation().getX() - rayon; x <= debut.getLocation().getX() + rayon; x++){
          for(double y = debut.getLocation().getY() - rayon; y <= debut.getLocation().getY() + rayon; y++){
            for(double z = debut.getLocation().getZ() - rayon; z <= debut.getLocation().getZ() + rayon; z++){
              Location loc = new Location(debut.getWorld(), x, y, z);
              if(loc.getBlock().getType().toString().contains("_BED")) {
            	  beds.add(loc.getBlock());
              }
            }
          }
        }
        return beds;
    }
    
    private Location getValidGolemSpawnPosition(Location loc, int posX, int posZ) {
    	Location l = loc.clone();
    	l.add(posX, 6, posZ);
    	
    	for(int j=6;j>=-6;j--) {
    		Location l1 = l.clone();
    		l = l.add(0, -1, 0);
    		if((l1.getBlock().isEmpty() || l1.getBlock().isLiquid()) && l.getBlock().getType().isOccluding()) {
    			return l1;
    		}
    	}
    	return null;
    }
    
    private Location trySpawningGolem(World w,Entity e) {
		Random rnd = new Random();
		for(int i=0;i<10;i++) {
			
			int posX = rnd.nextInt(16)-8;
			int posZ = rnd.nextInt(16)-8;
			Location l = getValidGolemSpawnPosition(e.getLocation(),posX,posZ);
			if(l!=null) {
				if(isNotColliding(l)) {
					return l;
				}
			}
		}
		return null;
    }
    
    private boolean isNotColliding(Location l) {
    	
    	boolean valid = true;
    	Location l1 = l.clone();
    	if(!l1.add(0, -1, 0).getBlock().isSolid()) {
    		valid = false;
    	}

		for(int i=0;i<2 && valid==true;i++) {
    		Location l2 = l.clone();
    		l2.add(0, i, 0);
    		if(!l2.getBlock().isEmpty()) { //&&!l2.getBlock().isLiquid()
    			valid = false;
    		}
    	}
			
    	return valid;
    }
    
    
    
}
