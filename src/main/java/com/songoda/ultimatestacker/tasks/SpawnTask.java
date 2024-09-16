package com.songoda.ultimatestacker.tasks;

import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.utils.Paire;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnTask extends BukkitRunnable {

    public static final Map<CreatureSpawner, Paire<Integer, Location>> waitingToSpawnFromSpawner = new HashMap<>();
    public static final Map<Paire<EntityType, Location>, Integer> waitingToSpawnFromFarms = new HashMap<>();
    public static final Map<Location, List<Location>> ignoredLocations = new HashMap<>();

    public SpawnTask() {
        runTaskTimer(UltimateStacker.getInstance(), 20*30, 20*60);
    }

    @Override
    public void run() {

        World islandsWorld = Bukkit.getWorld("askyblock");

        for ( Map.Entry<CreatureSpawner, Paire<Integer, Location>> entry : waitingToSpawnFromSpawner.entrySet() ) {

            LivingEntity entity = (LivingEntity)islandsWorld.spawnEntity(entry.getValue().getSecondElement(), entry.getKey().getSpawnedType());
            /*EntityStack stack = getEntityStackManager().addStack(entity);
            stack.createDuplicates(entry.getValue().getFirstElement() - 1);
            stack.updateStack();
            getStackingTask().attemptSplit(stack, entity);*/
            if ( entry.getValue().getFirstElement() - 1 > 0 )
                UltimateStacker.getInstance().getEntityStackManager().createStackedEntity(entity, entry.getValue().getFirstElement() - 1);
        }
        waitingToSpawnFromSpawner.clear();

        for ( Map.Entry<Paire<EntityType, Location>, Integer> entry : waitingToSpawnFromFarms.entrySet() ) {

            LivingEntity entity = (LivingEntity)islandsWorld.spawnEntity(getLocationToSpawnFarmEntities(entry.getKey().getSecondElement()), entry.getKey().getFirstElement());
            /*EntityStack stack = getEntityStackManager().addStack(entity);
            stack.createDuplicates(entry.getValue() - 1);
            stack.updateStack();
            getStackingTask().attemptSplit(stack, entity);*/
            if ( entry.getValue() - 1 > 0 )
                UltimateStacker.getInstance().getEntityStackManager().createStackedEntity(entity, entry.getValue() - 1);
        }
        waitingToSpawnFromFarms.clear();

    }

    private Location getLocationToSpawnFarmEntities(Location loc) {
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
    }
}
