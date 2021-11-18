package com.github.vaporrrr.bteenhanced.wood;

import com.github.vaporrrr.bteenhanced.BTEEnhanced;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.AffineTransform;
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
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

public class Wood {
    private final Player p;
    private final String schematicLoc;
    private final String targetBlock;
    private float radius;
    private float radiusSum;
    private int schematicsOverMaxSize = 0;
    private final boolean ignoreAirBlocks;
    private final boolean randomRotation;
    private boolean undone = false;
    private EditSession editSession;
    private Tree[][] surfaceGrid;
    private Tree[][] grid;
    private ArrayList<Clipboard> schematics = new ArrayList<>();
    private BlockVector minPoint;
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
    private static final Plugin plugin = BTEEnhanced.getPlugin(BTEEnhanced.class);

    public Wood(Player p, String schematicLoc, String targetBlock, float radius, boolean ignoreAirBlocks, boolean randomRotation) {
        this.p = p;
        this.schematicLoc = schematicLoc;
        this.targetBlock = targetBlock;
        this.radius = radius;
        this.ignoreAirBlocks = ignoreAirBlocks;
        this.randomRotation = randomRotation;
        this.editSession = new EditSession((LocalWorld) p.getWorld(), -1);
    }

    public EditSession getEditSession() {
        return editSession;
    }

    public boolean isUndone() {
        return undone;
    }

    public void setUndone(boolean undone) {
        this.undone = undone;
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

        if (schematicLoc.charAt(schematicLoc.length() - 1) == '*') {
            File directory;
            if (schematicLoc.length() == 1) {
                directory = schematicsFolder;
            } else {
                String fileSeperator = schematicLoc.substring(schematicLoc.length() - 1 - File.separator.length(), schematicLoc.length() - 1);
                if (fileSeperator.equals(File.separator) || fileSeperator.equals("/")) {
                    directory = new File(schematicsFolder + File.separator + schematicLoc.substring(0, schematicLoc.length() - 2));
                } else {
                    p.printError("Could not understand the path.");
                    return;
                }
            }
            if (directory.exists()) {
                if (inBaseDirectory(schematicsFolder, directory)) {
                    setSchematics(directory);
                } else {
                    p.printError("Only files inside the schematics folder are allowed.");
                    return;
                }
            } else {
                p.printError("Folder does not exist.");
                return;
            }
        } else {
            File file = new File(schematicsFolder + File.separator + schematicLoc + ".schematic");
            Clipboard clipboard;
            ClipboardFormat format = ClipboardFormat.SCHEMATIC;
            ClipboardReader reader;
            try {
                if (inBaseDirectory(schematicsFolder, file)) {
                    reader = format.getReader(new FileInputStream(file));
                    clipboard = reader.read(p.getWorld().getWorldData());
                } else {
                    p.printError("Only files inside the schematics folder are allowed.");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                p.printError("Schematic " + file.getName() + " does not exist.");
                return;
            }
            schematics.add(clipboard);
        }

        surfaceGrid = new Tree[region.getWidth()][region.getLength()];
        Vector minimumPoint = region.getMinimumPoint();
        minPoint = new BlockVector(minimumPoint.getX(), minimumPoint.getY(), minimumPoint.getZ());
        editSession = new EditSession((LocalWorld) p.getWorld(), -1);
        boolean startingPointSet = false;
        BlockVector blockVector = null;
        for (BlockVector p : region) {
            if (editSession.getBlock(new Vector(p.getX(), p.getY() + 1, p.getZ())).isAir()) {
                BaseBlock block = editSession.getBlock(p);
                if ((block.getId() + ":" + block.getData()).equals(targetBlock) || (block.getData() == 0 && String.valueOf(block.getId()).equals(targetBlock))) {
                    int pX = Math.abs((int) (p.getX() - minPoint.getX()));
                    int pZ = Math.abs((int) (p.getZ() - minPoint.getZ()));
                    Tree tree = surfaceGrid[pX][pZ];
                    if (tree == null || tree.getY() < (int) p.getY()) {
                        surfaceGrid[pX][pZ] = new Tree(p);
                        if (!startingPointSet) {
                            blockVector = surfaceGrid[pX][pZ].getBlockVector();
                            startingPointSet = true;
                        }
                    }
                }
            }
        }
        if (surfaceGrid == null) {
            p.printError("No suitable surface points found. Use block ID to specify what block you want to place trees on.");
            return;
        }
        ArrayList<Tree> points = poissonDiskSampling(plugin.getConfig().getInt("MaxTries"), new Tree(blockVector, getRandomSchematic()), region.getWidth(), region.getLength());

        Random rand = new Random();
        for (Tree tree : points) {
            Vector pos = new Vector(tree.getX(), tree.getY() + 1, tree.getZ());
            ClipboardHolder clipboardHolder = new ClipboardHolder(tree.getClipboard(), p.getWorld().getWorldData());
            if (randomRotation) {
                AffineTransform transform = new AffineTransform();
                transform.rotateY(rand.nextInt(4) * 90);
                clipboardHolder.setTransform(transform);
            }
            PasteBuilder pb = clipboardHolder.createPaste(editSession, editSession.getWorld().getWorldData()).to(pos)
                    .ignoreAirBlocks(ignoreAirBlocks);
            try {
                Operations.completeLegacy(pb.build());
            } catch (MaxChangedBlocksException e) {
                p.printError("Max changed blocks.");
                return;
            }
        }
        p.print("Done! " + points.size() + " trees pasted. " + schematics.size() + " schematics used. " + (schematicsOverMaxSize == 0 ? "" : " schematics too large."));
    }

    private ArrayList<Tree> poissonDiskSampling(int k, Tree p0, int width, int height) {
        int N = 2;
        ArrayList<Tree> points = new ArrayList<>();
        ArrayList<Tree> active = new ArrayList<>();
        if (Float.isNaN(radius)) radius = radiusSum / schematics.size();
        float cellSize = (float) Math.floor(radius / Math.sqrt(N));
        Random generator = new Random();

        int ncells_width = (int) (Math.ceil(width / cellSize) + 1);
        int ncells_height = (int) Math.ceil(height / cellSize) + 1;

        points.add(p0);
        active.add(p0);

        grid = new Tree[ncells_width][ncells_height];
        for (int i = 0; i < ncells_width; i++)
            for (int j = 0; j < ncells_height; j++)
                grid[i][j] = null;
        insertPoint(cellSize, p0);

        while (active.size() > 0) {
            int random_index = generator.nextInt(active.size());
            BlockVector p = active.get(random_index).getBlockVector();
            Clipboard randomClipboard = getRandomSchematic();

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

                Tree pnew = surfaceGrid[(int) pnewx][(int) pnewz];
                pnew = new Tree(pnew.getBlockVector(), randomClipboard);
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

    private void insertPoint(float cellSize, Tree p) {
        int pX = Math.abs((int) (p.getBlockVector().getX() - minPoint.getX()));
        int pZ = Math.abs((int) (p.getBlockVector().getZ() - minPoint.getZ()));
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

    private static double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private Clipboard getRandomSchematic() {
        return schematics.get(new Random().nextInt(schematics.size()));
    }

    private void setSchematics(File directory) {
        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormat.SCHEMATIC;
        ClipboardReader reader;
        int maxSchemSize = plugin.getConfig().getInt("MaxSchemSize");
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile() && fileIsSchematic(file)) {
                    try {
                        if (file.length() <= maxSchemSize) {
                            reader = format.getReader(new FileInputStream(file));
                            clipboard = reader.read(p.getWorld().getWorldData());
                            schematics.add(clipboard);
                            radiusSum += calculateRadius(clipboard);
                        } else {
                            schematicsOverMaxSize++;
                        }
                    } catch (IOException e) {
                        p.printError("Schematic " + file.getName() + " not found.");
                    }
                } else if (file.isDirectory()) {
                    setSchematics(file);
                }
            }
        }
    }

    public static boolean fileIsSchematic(File file) {
        int period = file.getName().lastIndexOf('.');
        return file.getName().substring(period + 1).equals("schematic");
    }

    public float calculateRadius(Clipboard clipboard) {
        return Math.max(clipboard.getRegion().getWidth() / 2f, clipboard.getRegion().getLength() / 2f) + 1;
    }

    public boolean inBaseDirectory(File base, File user) {
        URI parentURI = base.toURI();
        URI childURI = user.toURI();
        return !parentURI.relativize(childURI).isAbsolute();
    }
}
