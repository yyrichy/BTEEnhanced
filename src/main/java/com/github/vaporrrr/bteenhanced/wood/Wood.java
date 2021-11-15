package com.github.vaporrrr.bteenhanced.wood;

import com.github.vaporrrr.bteenhanced.Main;
import com.sk89q.worldedit.*;
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
import java.util.ArrayList;

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

        int trees = 0;
        ArrayList<BlockVector> points = randomPoints(20, 15, region, editSession);
        for (BlockVector point : points){
            Vector pos = new Vector(point.getX(), point.getY() + 1, point.getZ());
            PasteBuilder pb = cliph.createPaste(editSession, editSession.getWorld().getWorldData()).to(pos)
                    .ignoreAirBlocks(ignoreAirBlocks);
            try {
                Operations.completeLegacy(pb.build());
                trees++;
            } catch (MaxChangedBlocksException e){
                p.printError("Max changed blocks.");
                return;
            }
        }
        p.print("Done! " + trees + " trees pasted.");
    }

    private static ArrayList<BlockVector> randomPoints(int density, int distanceCheck, Region region, EditSession editSession) {
        ArrayList<BlockVector> result = new ArrayList<>();
        ArrayList<BlockVector> points = new ArrayList<>();
        for (BlockVector point : region) {
            if(!editSession.getBlock(point).isAir() && editSession.getBlock(new Vector(point.getX(), point.getY() + 1, point.getZ())).isAir()) {
                points.add(point);
            }
        }
        int num = points.size() / density;
        for(int i = 0; i < num; i++){
            BlockVector bestCandidate = null; double bestDistance = 0;
            for(int j = 0; j < distanceCheck; j++) {
                BlockVector randomPoint = points.get((int)(Math.random()* points.size()));
                double x = randomPoint.getX(); double z = randomPoint.getZ();
                double distanceToClosestPoint = (1/0.0);
                for (BlockVector point : result) {
                    double dx = x - point.getX(); double dz = z - point.getZ();
                    double distance = dx * dx + dz * dz;
                    if(distanceToClosestPoint > distance){
                        distanceToClosestPoint = distance;
                    }
                }
                if(bestDistance < distanceToClosestPoint){
                    bestDistance = distanceToClosestPoint;
                    bestCandidate = randomPoint;
                }
            }
            if(bestCandidate != null) {
                result.add(bestCandidate);
            }
        }
        return result;
    }
}
