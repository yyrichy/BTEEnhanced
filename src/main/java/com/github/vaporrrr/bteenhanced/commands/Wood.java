package com.github.vaporrrr.bteenhanced.commands;

import com.github.vaporrrr.bteenhanced.wood.WoodManager;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class Wood implements CommandExecutor {
    private Map<UUID, Wood> woodMap;
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission("bteplus.wood") && !commandSender.isOp()) {
            return false;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return true;
        }
        Player player = (Player) commandSender;
        com.sk89q.worldedit.entity.Player p = new BukkitPlayer((WorldEditPlugin) we, null, player);
        if (args.length < 1) {
            p.printError("Specify a schematic name.");
            return false;
        }
        if (args.length < 2) {
            p.printError("Specify the block you want trees to be placed above.");
            return false;
        }
        // If flags
        if (args.length > 2) {
            ArrayList<String> flags = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
            WoodManager.create(p, args[0], args[1], flags);
        } else {
            WoodManager.create(p, args[0], args[1]);
        }
        return true;
    }
}
