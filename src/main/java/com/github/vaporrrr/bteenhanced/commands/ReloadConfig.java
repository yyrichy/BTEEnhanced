package com.github.vaporrrr.bteenhanced.commands;

import com.github.vaporrrr.bteenhanced.BTEEnhanced;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ReloadConfig implements CommandExecutor {
    private static final Plugin plugin = BTEEnhanced.getPlugin(BTEEnhanced.class);
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission("bteenhanced.reload") && !commandSender.isOp()) {
            return false;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return true;
        }
        plugin.reloadConfig();
        commandSender.sendMessage(ChatColor.GOLD + "Config reloaded.");
        return true;
    }
}
