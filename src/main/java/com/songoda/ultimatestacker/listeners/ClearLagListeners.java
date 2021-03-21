package com.songoda.ultimatestacker.listeners;

import com.songoda.ultimatestacker.UltimateStacker;
import me.minebuilders.clearlag.events.EntityRemoveEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Villager;

public class ClearLagListeners implements Listener {

    private final UltimateStacker plugin;

    public ClearLagListeners(UltimateStacker plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClearLaggTask(EntityRemoveEvent event) {
        for (Entity entity : event.getWorld().getEntities()) {
            if (entity instanceof LivingEntity && plugin.getEntityStackManager().isStackedAndLoaded((LivingEntity)entity)) {
            	if(entity.getType()!=EntityType.VILLAGER) {
            		plugin.getEntityStackManager().removeStack(entity);
                    event.addEntity(entity);
            	}
            }
        }
    }
}
