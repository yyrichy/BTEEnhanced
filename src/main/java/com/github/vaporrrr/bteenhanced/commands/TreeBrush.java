/*
 * BTEEnhanced, a building tool
 * Copyright 2022 (C) vaporrrr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


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
        if (Bukkit.getPluginManager().getPlugin("SchematicBrush") == null) {
            commandSender.sendMessage(ChatColor.RED + "Plugin SchematicBrush is not installed.");
            return true;
        }
        if (args.length == 0) {
            commandSender.sendMessage(ChatColor.RED + "Specify a tree type: " + String.join(", ", treeTypes()));
            return true;
        }
        Bukkit.dispatchCommand(commandSender, "/schbr trees/" + String.join("/", args) + "/*@** -place:bottom -yoff:2");
        return true;
    }

    //TODO: Add ability to see options/folders in every folder, not just "trees/"
    public static ArrayList<String> treeTypes() {
        File folder = new File(we.getDataFolder() + File.separator + "schematics" + File.separator + "trees");
        ArrayList<String> treeTypes = new ArrayList<>();
        File[] files = folder.listFiles();
        if (!folder.exists() || files == null) {
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
