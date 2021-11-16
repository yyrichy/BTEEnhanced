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
import jdk.nashorn.internal.ir.Block;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Wood {
    private final Player p;
    private final String schematicLoc;
    private final String targetBlock;
    private float radius;
    private final boolean ignoreAirBlocks;
    private EditSession editSession;
    private BlockVector[][] surfaceGrid;
    private BlockVector[][] grid;
    private BlockVector minPoint;
    private static final Plugin plugin = Main.getPlugin(Main.class);
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");

    public Wood(Player p, String schematicLoc, String targetBlock, String rad, boolean ignoreAirBlocks) {
        this.p = p;
        this.schematicLoc = schematicLoc;
        this.targetBlock = targetBlock;
        this.ignoreAirBlocks = ignoreAirBlocks;
        try {
            this.radius = Float.parseFloat(rad);
        } catch (NullArgumentException e) {
            this.radius = 5;
            p.printError("Radius must be a number, set radius to 5.");
        }


        this.editSession = new EditSession((LocalWorld) p.getWorld(), -1);
    }

    public EditSession getEditSession() {
        return editSession;
    }

    public void execute() {
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
        editSession = new EditSession((LocalWorld) p.getWorld(), -1);

        try {
            reader = format.getReader(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            p.printError("Schematic not found.");
            return;
        }
        try {
            clipboard = reader.read(p.getWorld().getWorldData());
        } catch (IOException e) {
            e.printStackTrace();
            p.printError("Could not read clipboard.");
            return;
        }

        ClipboardHolder cliph = new ClipboardHolder(clipboard, p.getWorld().getWorldData());

        surfaceGrid = new BlockVector[region.getWidth()][region.getLength()];
        Vector minimumPoint = region.getMinimumPoint();
        minPoint = new BlockVector(minimumPoint.getX(), minimumPoint.getY(), minimumPoint.getZ());
        for (BlockVector p : region) {
            if (editSession.getBlock(new Vector(p.getX(), p.getY() + 1, p.getZ())).isAir()) {
                BaseBlock block = editSession.getBlock(p);
                if ((block.getId() + ":" + block.getData()).equals(targetBlock) || (block.getData() == 0 && String.valueOf(block.getId()).equals(targetBlock))){
                    int pX = Math.abs((int) (p.getX() - minPoint.getX()));
                    int pZ = Math.abs((int) (p.getZ() - minPoint.getZ()));
                    BlockVector pos = surfaceGrid[pX][pZ];
                    if (pos == null || pos.getY() < p.getY()) {
                        surfaceGrid[pX][pZ] = p;
                    }
                }
            }
        }
        ArrayList<BlockVector> points = poissonDiskSampling(radius, 20, region.getCenter().toBlockVector(), region.getWidth(), region.getLength());

        for (BlockVector point : points) {
            Vector pos = new Vector(point.getX(), point.getY() + 1, point.getZ());
            PasteBuilder pb = cliph.createPaste(editSession, editSession.getWorld().getWorldData()).to(pos)
                    .ignoreAirBlocks(ignoreAirBlocks);
            try {
                Operations.completeLegacy(pb.build());
            } catch (MaxChangedBlocksException e) {
                p.printError("Max changed blocks.");
                return;
            }
        }
        p.print("Done! " + points.size() + " trees pasted.");
    }

    private ArrayList<BlockVector> poissonDiskSampling(float radius, int k, BlockVector p0, int width, int height) {
        int N = 2;
        ArrayList<BlockVector> points = new ArrayList<>();
        ArrayList<BlockVector> active = new ArrayList<>();
        float cellSize = (float) Math.floor(radius / Math.sqrt(N));
        Random generator = new Random();

        int ncells_width = (int) (Math.ceil(width/cellSize) + 1);
        int ncells_height = (int) Math.ceil(height/cellSize) + 1;

        points.add(p0);
        active.add(p0);

        grid = new BlockVector[ncells_width][ncells_height];
        for (int i = 0; i < ncells_width; i++)
            for (int j = 0; j < ncells_height; j++)
                grid[i][j] = null;
        insertPoint(cellSize, p0);

        while (active.size() > 0) {
            int random_index = generator.nextInt(active.size());
            BlockVector p = active.get(random_index);

            boolean found = false;
            for (int tries = 0; tries < k; tries++) {
                float theta = generator.nextFloat() * 360;
                float new_radius = radius + generator.nextFloat() * (2 * radius - radius);
                int pX = Math.abs((int) (p.getX() - minPoint.getX()));
                int pZ = Math.abs((int) (p.getZ() - minPoint.getZ()));
                float pnewx = (float) (pX + new_radius * Math.cos(Math.toRadians(theta)));
                float pnewz = (float) (pZ + new_radius * Math.sin(Math.toRadians(theta)));

                if (!isValidPoint(cellSize, (int) pnewx, (int) pnewz, radius, width, height, ncells_width, ncells_height)) {
                    continue;
                }

                BlockVector pnew = surfaceGrid[(int) pnewx][(int) pnewz];
                points.add(pnew);
                insertPoint(cellSize, pnew);
                active.add(pnew);
                found = true;
                break;
            }

            /* If no point was found after k tries, remove p */
            if (!found) {
                active.remove(random_index);
            }
        }

        return points;
    }

    private void insertPoint(float cellSize, BlockVector p) {
        int pX = Math.abs((int) (p.getX() - minPoint.getX()));
        int pZ = Math.abs((int) (p.getZ() - minPoint.getZ()));
        int xindex = (int) Math.floor(pX / cellSize);
        int zindex = (int) Math.floor(pZ / cellSize);
        grid[xindex][zindex] = p;
    }

    private boolean isValidPoint(float cellSize, int pX, int pZ, float radius, int width, int height, int gwidth, int gheight) {
        /* Make sure the point is in the region */
        if (pX < 0 || pZ < 0 || pX >= width || pZ >= height) {
            return false;
        }
        if (surfaceGrid[pX][pZ] == null) {
            return false;
        }
        /* Check neighboring eight cells */
        int xindex = (int) Math.floor(pX / cellSize);
        int zindex = (int) Math.floor(pZ / cellSize);
        int i0 = Math.max(xindex - 1, 0);
        int i1 = Math.min(xindex + 1, gwidth - 1);
        int j0 = Math.max(zindex - 1, 0);
        int j1 = Math.min(zindex + 1, gheight - 1);

        for (int i = i0; i <= i1; i++)
            for (int j = j0; j <= j1; j++)
                if (grid[i][j] != null)
                    if (dist(Math.abs(grid[i][j].getX() - minPoint.getX()), Math.abs(grid[i][j].getZ() - minPoint.getZ()), pX, pZ) < radius)
                        return false;

        /* If we get here, return true */
        return true;
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }
}
