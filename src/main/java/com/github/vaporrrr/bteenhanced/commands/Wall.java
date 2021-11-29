package com.github.vaporrrr.bteenhanced.commands;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.shape.ArbitraryShape;
import com.sk89q.worldedit.regions.shape.RegionShape;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Wall implements CommandExecutor {
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
    private static final WorldEdit worldEdit = WorldEdit.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission("bteenhanced.region.wall") && !commandSender.isOp()) {
            return false;
        }
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
            return true;
        }
        Player player = (Player) commandSender;
        com.sk89q.worldedit.entity.Player p = new BukkitPlayer((WorldEditPlugin) we, null, player);
        if (args.length == 0) {
            p.printError("Include a pattern.");
            return false;
        }
        SessionManager manager = worldEdit.getSessionManager();
        LocalSession session = manager.get(p);
        Region region;
        World selectionWorld = session.getSelectionWorld();
        try {
            if (selectionWorld == null) throw new IncompleteRegionException();
            region = session.getSelection(selectionWorld);
        } catch (IncompleteRegionException ex) {
            p.printError("Please make a region selection first.");
            return true;
        }

        PatternFactory patternFactory = worldEdit.getPatternFactory();
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(p);
        Extent extent = p.getExtent();
        if (extent instanceof World) {
            parserContext.setWorld((World) extent);
        }
        parserContext.setExtent(extent);
        parserContext.setSession(session);
        parserContext.setRestricted(true);

        Pattern pattern;
        try {
            pattern = (Pattern) patternFactory.parseFromInput(args[0], parserContext);
        } catch (InputParseException e) {
            e.printStackTrace();
            p.printError(e.getClass().getSimpleName());
            return true;
        }

        final int minY = region.getMinimumPoint().getBlockY();
        final int maxY = region.getMaximumPoint().getBlockY();
        EditSession editSession = new EditSession((LocalWorld) p.getWorld(), -1);
        final ArbitraryShape shape = new RegionShape(region) {
            @Override
            protected BaseBlock getMaterial(int x, int y, int z, BaseBlock defaultMaterial) {
                if (y > maxY || y < minY) {
                    // Put holes into the floor and ceiling by telling ArbitraryShape that the shape goes on outside the region
                    return defaultMaterial;
                }
                return super.getMaterial(x, y, z, defaultMaterial);
            }
        };
        try {
            shape.generate(editSession, pattern, true);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
            p.printError(e.getClass().getSimpleName());
            return true;
        }
        return true;
    }
}
