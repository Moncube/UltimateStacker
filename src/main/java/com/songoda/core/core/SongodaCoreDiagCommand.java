package com.songoda.core.core;


import com.songoda.core.SongodaCore;
import com.songoda.core.commands.AbstractCommand;
import com.songoda.ultimatestacker.UltimateStacker;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.List;

public class SongodaCoreDiagCommand extends AbstractCommand {
    private final DecimalFormat format = new DecimalFormat("##.##");

    public SongodaCoreDiagCommand() {
        super(false, "diag");
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        sender.sendMessage("");
        sender.sendMessage("Songoda Diagnostics Information");
        sender.sendMessage("");
        sender.sendMessage("Plugins:");

        for (PluginInfo plugin : SongodaCore.getPlugins()) {
            sender.sendMessage(plugin.getJavaPlugin().getName()
                    + " (" + plugin.getJavaPlugin().getDescription().getVersion() + " Core " + plugin.getCoreLibraryVersion() + ")");
        }

        sender.sendMessage("");
        sender.sendMessage("Server Version: " + Bukkit.getVersion());
        sender.sendMessage("Operating System: " + System.getProperty("os.name"));
        sender.sendMessage("Allocated Memory: " + format.format(Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "Mb");
        sender.sendMessage("Online Players: " + Bukkit.getOnlinePlayers().size());


        double[] tps = UltimateStacker.getInstance().getServer().getTPS();

        sender.sendMessage("TPS from last 1m, 5m, 15m: " + format.format(tps[0]) + ", " + format.format(tps[1]) + ", " + format.format(tps[2]));

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "songoda.admin";
    }

    @Override
    public String getSyntax() {
        return "/songoda diag";
    }

    @Override
    public String getDescription() {
        return "Display diagnostics information.";
    }
}