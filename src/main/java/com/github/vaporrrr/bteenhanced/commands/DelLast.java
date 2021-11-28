package com.github.vaporrrr.bteenhanced.commands;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class DelLast implements CommandExecutor {
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission("bteenhanced.selection.dellast") && !commandSender.isOp()) {
            return false;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return true;
        }
        Player player = (Player) commandSender;
        com.sk89q.worldedit.entity.Player p = new BukkitPlayer((WorldEditPlugin) we, null, player);

        int numToDelete = 1;
        if (args.length > 0) {
            try {
                numToDelete = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                p.printError("Number of points to delete must be an integer.");
                return true;
            }
        }
        if (numToDelete == 0) {
            p.print("Ok... why would you try to delete 0 points.");
            return true;
        } else if (numToDelete < 0) {
            p.printError("You can't delete a negative amount of points.");
            return true;
        }

        WorldEdit worldEdit = WorldEdit.getInstance();
        SessionManager manager = worldEdit.getSessionManager();
        LocalSession localSession = manager.get(p);
        Region region;
        World selectionWorld = localSession.getSelectionWorld();
        try {
            if (selectionWorld == null) throw new IncompleteRegionException();
            region = localSession.getSelection(selectionWorld);
        } catch (IncompleteRegionException e) {
            p.printError("Please make a region selection first.");
            return true;
        }

        if(!(region instanceof Polygonal2DRegion)) {
            p.printError("Currently only poly regions are supported.");
            return true;
        }

        Polygonal2DRegion reg = (Polygonal2DRegion) region;
        List<BlockVector2D> points = reg.getPoints();

        if (numToDelete > points.size() - 1) {
            p.printError("You can't delete that many points, there must be at least one point left over. You can delete up to " + (points.size() - 1));
            return true;
        }

        List<BlockVector2D> newPoints = points.subList(0, points.size() - numToDelete);
        Polygonal2DRegionSelector regionSelector  = new Polygonal2DRegionSelector(selectionWorld, newPoints, reg.getMinimumY(), reg.getMaximumY());
        localSession.setRegionSelector(selectionWorld, regionSelector);
        regionSelector.explainRegionAdjust(p, localSession);
        p.print("Selection edited!");
        return true;
    }
}
