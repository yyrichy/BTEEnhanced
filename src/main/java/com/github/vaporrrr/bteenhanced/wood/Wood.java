package com.github.vaporrrr.bteenhanced.wood;

import com.github.vaporrrr.bteenhanced.Main;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.PasteBuilder;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Wood {
    private final Player p;
    private final String schematicLoc;
    private final String targetBlock;
    private final boolean ignoreAirBlocks;
    private EditSession editSession;
    private static final Plugin plugin = Main.getPlugin(Main.class);
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
    public Wood (Player p, String schematicLoc, String targetBlock, boolean ignoreAirBlocks) {
        this.p = p;
        this.schematicLoc = schematicLoc;
        this.targetBlock = targetBlock;
        this.ignoreAirBlocks = ignoreAirBlocks;
        editSession = new EditSession((LocalWorld) p.getWorld(), -1);
    }
    public EditSession getEditSession() {
        return editSession;
    }
    public void execute () {
        File schematicsFolder = new File(we.getDataFolder() + File.separator + "schematics");
        WorldEdit worldEdit = WorldEdit.getInstance();
        SessionManager manager = worldEdit.getSessionManager();
        LocalSession localSession = manager.get(p);
        Region region;
        World selectionWorld = localSession.getSelectionWorld();
        try {
            if (selectionWorld == null) throw new IncompleteRegionException();
            region = localSession.getSelection(selectionWorld);
        } catch (IncompleteRegionException ex) {
            p.printError("Please make a region selection first.");
            return;
        }

        Clipboard clipboard;
        File file = new File(schematicsFolder + File.separator + schematicLoc + ".schematic");
        ClipboardFormat format = ClipboardFormat.SCHEMATIC;
        ClipboardReader reader;

        try {
            reader = format.getReader(new FileInputStream(file));
        } catch (IOException e){
            p.printError("Schematic not found.");
            return;
        }
        try {
            clipboard = reader.read(p.getWorld().getWorldData());
            localSession.setClipboard(new ClipboardHolder(clipboard, p.getWorld().getWorldData()));
        } catch (IOException e){
            e.printStackTrace();
            p.printError("Clipboard ex.");
            return;
        }

        editSession = new EditSession((LocalWorld) p.getWorld(), -1);
        ClipboardHolder cliph;

        try {
            cliph = localSession.getClipboard();
        } catch (EmptyClipboardException e) {
            e.printStackTrace();
            p.printError("Empty Clipboard.");
            editSession.flushQueue();
            return;
        }

        for (BlockVector point : region){
            //Only surface blocks
            if(editSession.getBlock(new Vector(point.getX(), point.getY() + 1, point.getZ())).isAir()){
                BaseBlock block = editSession.getBlock(point);
                boolean blockIsTarget = (block.getId() + ":" + block.getData()).equals(targetBlock) || (block.getData() == 0 && String.valueOf(block.getId()).equals(targetBlock));
                if (blockIsTarget) {
                    Vector pos = new Vector(point.getX(), point.getY() + 1, point.getZ());
                    PasteBuilder pb = cliph.createPaste(editSession, editSession.getWorld().getWorldData()).to(pos)
                            .ignoreAirBlocks(ignoreAirBlocks);
                    try {
                        Operations.completeLegacy(pb.build());
                    } catch (MaxChangedBlocksException e){
                        p.printError("Max changed blocks.");
                        return;
                    }
                }
            }
        }
        p.print("Done!");
    }
}
