/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.songoda.ultimatestacker.tasks;


import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;

import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.stackable.entity.EntityStack;
import com.songoda.ultimatestacker.stackable.entity.StackedEntity;

/**
 *
 * @author Florian
 */
public class RestockTask extends BukkitRunnable {
    
	private final UltimateStacker plugin;
        
    public RestockTask(UltimateStacker plugin)
    {
        this.plugin = plugin;
        runTaskTimer(plugin, 0, 100); //3000
    }
    
    @Override
    public void run()
    {
        //World w = Bukkit.getWorld("askyblock");
    	for(World w : Bukkit.getWorlds()) {
    		if(w.getTime()<=10000) { //9000 + 1000 de marge (correspond à la nuit)
        		for(Entity e : w.getEntities()) {
        			if(e.getType()==EntityType.VILLAGER) {
    	        		if (plugin.getEntityStackManager().isStackedAndLoaded((LivingEntity)e)) {
    	        			
    	        			EntityStack stack = plugin.getEntityStackManager().getStack((LivingEntity)e);
    	        			
	        				List<Villager> stackedVillagers = plugin.getInteractListeners().getVillagers(stack);
	        				if(stackedVillagers==null) {
	        					plugin.getInteractListeners().updateVillagers(stack);
	        					stackedVillagers = plugin.getInteractListeners().getVillagers(stack);
	        				}
	        				
    	         			for(Villager v : stackedVillagers) {
    	         				resetTrades(v);
    	         			}
    	        		}
        			}
        		}
    		}  
    	}
	}
    
    public void resetTrades(Villager v) {
    	
		if(v.getRestocksToday()<2) {
			
			Boolean restocked = false;
			List<org.bukkit.inventory.MerchantRecipe> mrs = v.getRecipes();
			for(org.bukkit.inventory.MerchantRecipe mr : mrs) {
				if(mr.getUses()==mr.getMaxUses()) {
					mr.setUses(0);
					restocked = true;
				}
			}
			if(restocked==true) {
				v.setRestocksToday(v.getRestocksToday()+1);
			}
		}
    }
   
}
