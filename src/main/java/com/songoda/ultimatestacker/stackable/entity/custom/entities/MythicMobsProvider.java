package com.songoda.ultimatestacker.stackable.entity.custom.entities;

import com.songoda.ultimatestacker.stackable.entity.custom.CustomEntity;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public abstract class MythicMobsProvider extends CustomEntity {

    protected MythicMobsProvider(Plugin plugin) {
        super(plugin);
    }

    public abstract boolean isCustomEntity(Entity entity);
}