package com.github.vaporrrr.bteenhanced.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;

public class TreeBrush implements CommandExecutor {
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission("schematicbrush.brush.use") && !commandSender.isOp()) {
            return false;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return true;
        }
        if (args.length == 0) {
            commandSender.sendMessage(ChatColor.RED + "Specify a tree type: " + String.join(", ", treeTypes()));
            return true;
        }
        Bukkit.dispatchCommand(commandSender, "//schbr trees/" + String.join("/", args) + "/*@** -place:bottom -yoff:2");
        return true;
    }

    public ArrayList<String> treeTypes() {
        File treesFolder = new File(we.getDataFolder() + File.separator + "schematics" + File.separator + "trees");
        ArrayList<String> treeTypes = new ArrayList<>();
        File[] files = treesFolder.listFiles();
        if (!treesFolder.exists() || files == null) {
            treeTypes.add("Trees folder not found in WorldEdit schematics folder.");
        } else {
            for (File file : files) {
                if (file.isDirectory()) {
                    treeTypes.add(file.getName());
                }
            }
        }
        return treeTypes;
    }
}
