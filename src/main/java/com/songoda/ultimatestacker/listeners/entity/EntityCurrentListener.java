package com.songoda.ultimatestacker.listeners.entity;

import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.stackable.entity.EntityStack;
import com.songoda.ultimatestacker.stackable.entity.EntityStackManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

public class EntityCurrentListener implements Listener {

    private final UltimateStacker plugin;

    public EntityCurrentListener(UltimateStacker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(EntityTransformEvent event) {
        EntityStackManager stackManager = plugin.getEntityStackManager();
        if (stackManager.isStackedAndLoaded(event.getEntity().getUniqueId())
                && event.getEntity() instanceof LivingEntity
                && event.getTransformedEntity() instanceof LivingEntity) {
        	
        	EntityStack stack = stackManager.updateStack((LivingEntity) event.getEntity(), (LivingEntity) event.getTransformedEntity());
        	if ( !UltimateStacker.getInstance().getMobFile().getBoolean("Mobs." + event.getEntityType().name() + ".TransformWholeStack") ) {
        		stack.releaseHost();
                stack.updateStack();
        	} else {
        		LivingEntity entity = (LivingEntity) event.getTransformedEntity();
	            EntityStack newStack = plugin.getEntityStackManager().addStack(entity);
	            newStack.createDuplicates(stack.getAmount()-1);
	            newStack.updateStack();
	            plugin.getStackingTask().attemptSplit(newStack, entity);
	            
        	}
        }
    }
}
