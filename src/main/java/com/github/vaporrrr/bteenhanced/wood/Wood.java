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
import java.sql.Array;
import java.util.ArrayList;
import java.util.Random;

public class Wood {
    private final Player p;
    private final String schematicLoc;
    private final String targetBlock;
    private final boolean ignoreAirBlocks;
    private EditSession editSession;
    private ArrayList<BlockVector> surfacePoints;
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
            if (editSession.getBlock(new Vector(point.getX(), point.getY() + 1, point.getZ())).isAir()) {
                BaseBlock block = editSession.getBlock(point);
                boolean blockIsTarget = (block.getId() + ":" + block.getData()).equals(targetBlock) || (block.getData() == 0 && String.valueOf(block.getId()).equals(targetBlock));
                if (blockIsTarget) {
                    surfacePoints.add(point);
                }
            }
        }
        ArrayList<BlockVector> points = poissonDiskSampling(10f, 30, region.getCenter().toBlockVector());



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

    private ArrayList<BlockVector> poissonDiskSampling(float radius, int k, BlockVector startingPoint) {
        ArrayList<BlockVector> points = new ArrayList<>();
        ArrayList<BlockVector> active = new ArrayList<>();

        BlockVector p0 = startingPoint;
        Random generator = new Random();

        points.add(p0);
        active.add(p0);

        while (active.size() > 0) {
            int random_index = generator.nextInt(active.size());
            BlockVector p = active.get(random_index);

            boolean found = false;
            for (int tries = 0; tries < k; tries++) {
                float theta = generator.nextFloat() * 360;
                float new_radius = generator.nextFloat() * (2*radius - radius);
                float pnewx = (float) (p.getX() + new_radius * Math.cos(Math.toRadians(theta)));
                float pnewy = (float) (p.getZ() + new_radius * Math.sin(Math.toRadians(theta)));

                BlockVector pnew = new PVector(pnewx, pnewy);

                if (!isValidPoint(cellsize, pnew, radius))
                    continue;

                points.add(pnew);
                active.add(pnew);
                found = true;
                break;
            }

            /* If no point was found after k tries, remove p */
            if (!found)
                active.remove(random_index);
        }

        return points;
    }

    boolean isValidPoint(float cellsize, BlockVector p, float radius) {
        /* Make sure the point is in the region */
        if(!surfacePoints.stream().anyMatch(point -> point.equals(p))){
            return false;
        }
        /* Check neighboring eight cells */
        int xindex = floor(p.x / cellsize);
        int yindex = floor(p.y / cellsize);
        int i0 = max(xindex - 1, 0);
        int i1 = min(xindex + 1, gwidth - 1);
        int j0 = max(yindex - 1, 0);
        int j1 = min(yindex + 1, gheight - 1);

        for (int i = i0; i <= i1; i++)
            for (int j = j0; j <= j1; j++)
                if (grid[i][j] != null)
                    if (dist(grid[i][j].x, grid[i][j].y, p.x, p.y) < radius)
                        return false;

        /* If we get here, return true */
        return true;
    }
}
