package com.songoda.ultimatestacker.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.songoda.ultimatestacker.UltimateStacker;
import com.songoda.ultimatestacker.tasks.StackingTask;

public class CommandMoncube implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if ( !(sender instanceof Player) || sender.isOp() ) {
			UltimateStacker.getInstance().getStackingTask().cancel();
			for( BukkitTask task : Bukkit.getScheduler().getPendingTasks() ) {
	    		if ( task.getOwner().equals(UltimateStacker.getInstance()) )
	    			task.cancel();
	    			
	    	}
			UltimateStacker.getInstance().setStackingTask(new StackingTask(UltimateStacker.getInstance()));
			UltimateStacker.getInstance().startSpawnerAndFarmsActivity();
		}
		return true;
	}

}
