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
import jdk.nashorn.internal.ir.Block;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Wood {
    private final Player p;
    private final String schematicLoc;
    private String[] targetBlocks;
    private float radius;
    private float radiusSum;
    private int schematicsOverMaxSize = 0;
    private int selectedBlocks = 0;
    private boolean ignoreAirBlocks = true;
    private boolean randomRotation = true;
    private boolean inverseMask = false;
    private EditSession editSession;
    private Tree[][] possibleVectorsGrid;
    private Tree[][] grid;
    private float cellSize;
    private int ncells_width;
    private int ncells_height;
    private final ArrayList<Tree> points = new ArrayList<>();
    private final ArrayList<Clipboard> schematics = new ArrayList<>();
    private BlockVector minPoint;
    private static final Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
    private static final Plugin plugin = BTEEnhanced.getPlugin(BTEEnhanced.class);

    public Wood(Player p, String schematicLoc, String target, ArrayList<String> flags) {
        this.p = p;
        this.schematicLoc = schematicLoc;
        this.radius = Float.NaN;
        this.editSession = new EditSession((LocalWorld) p.getWorld(), -1);
        setTargetBlocks(target);
        for (String flag : flags) {
            if (flag.startsWith("-")) {
                if (flag.equals("-includeAir")) {
                    this.ignoreAirBlocks = false;
                } else if (flag.equals("-dontRotate")) {
                    this.randomRotation = false;
                } else if (flag.startsWith("-r:")) {
                    try {
                        this.radius = Float.parseFloat(flag.substring(flag.indexOf(':') + 1));
                    } catch (Exception e) {
                        p.printError("Radius is not a number.");
                        return;
                    }
                }
            }
        }
    }

    public Wood(Player p, String schematicLoc, String target) {
        this.p = p;
        this.schematicLoc = schematicLoc;
        this.radius = Float.NaN;
        this.editSession = new EditSession((LocalWorld) p.getWorld(), -1);
        setTargetBlocks(target);
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
                if (!p.hasPermission("bteenhanced.wood.all")) {
                    p.printError("You do not have permission for using the entire schematics folder.");
                    return;
                } else {
                    directory = schematicsFolder;
                }
            } else {
                String fileSeparator = schematicLoc.substring(schematicLoc.length() - 1 - File.separator.length(), schematicLoc.length() - 1);
                if (fileSeparator.equals(File.separator) || (fileSeparator.equals("/") && "\\".equals(File.separator))) {
                    directory = new File(schematicsFolder + File.separator + schematicLoc.substring(0, schematicLoc.length() - 2));
                } else {
                    p.printError("Could not understand the path.");
                    return;
                }
            }
            if (directory.exists() && inBaseDirectory(schematicsFolder, directory)) {
                loadSchematics(directory);
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
                    if (file.length() > plugin.getConfig().getInt("MaxSchemSize")) {
                        p.printError("Schematic is over max size.");
                        return;
                    }
                    reader = format.getReader(new FileInputStream(file));
                    clipboard = reader.read(p.getWorld().getWorldData());
                } else {
                    p.printError("Schematic " + file.getName() + " does not exist.");
                    return;
                }
            } catch (Exception e) {
                p.printError("Schematic " + file.getName() + " does not exist.");
                return;
            }
            radiusSum = radius(clipboard);
            schematics.add(clipboard);
        }

        if (Float.isNaN(radius)) radius = radiusSum / schematics.size();

        possibleVectorsGrid = new Tree[region.getWidth()][region.getLength()];
        for (int i = 0; i < region.getWidth(); i++) {
            for (int j = 0; j < region.getLength(); j++) {
                possibleVectorsGrid[i][j] = null;
            }
        }
        Vector minimumPoint = region.getMinimumPoint();
        minPoint = new BlockVector(minimumPoint.getX(), minimumPoint.getY(), minimumPoint.getZ());
        editSession = new EditSession((LocalWorld) p.getWorld(), -1);
        ArrayList<BlockVector> startBlockVectors = new ArrayList<>();
        int prevX = 0;
        int prevZ = 0;
        for (BlockVector p : region) {
            BaseBlock block = editSession.getBlock(p);
            if (editSession.getBlock(new Vector(p.getX(), p.getY() + 1, p.getZ())).isAir() && !block.isAir()) {
                if (matchesTarget(block) != inverseMask) {
                    int pX = Math.abs((int) (p.getX() - minPoint.getX()));
                    int pZ = Math.abs((int) (p.getZ() - minPoint.getZ()));
                    Tree tree = possibleVectorsGrid[pX][pZ];
                    if (tree == null || tree.getY() < (int) p.getY()) {
                        selectedBlocks++;
                        plugin.getLogger().info(pX + " " + pZ);
                        if ((Math.abs(pX - prevX) >= radius) || (Math.abs(pZ - prevZ) >= radius)) {
                            if (!isNeighboring(pX, pZ, region.getWidth(), region.getLength())) {
                                plugin.getLogger().info("true");
                                startBlockVectors.add(p);
                            }
                        }
                        possibleVectorsGrid[pX][pZ] = new Tree(p);
                    }
                    prevX = pX;
                    prevZ = pZ;
                }
            }
        }
        if (selectedBlocks == 0) {
            p.printError("No suitable surface points found. No blocks had air above and " + (inverseMask ? "weren't " : "were ") + Arrays.toString(targetBlocks));
            return;
        }

        for (BlockVector p : startBlockVectors) {
            plugin.getLogger().info("vectors: " + p.getX() + " " + p.getZ());
        }

        int width = region.getWidth();
        int height = region.getLength();
        int MAX_TRIES = plugin.getConfig().getInt("MaxTries");
        cellSize = (float) Math.floor(radius / Math.sqrt(2));
        ncells_width = (int) (Math.ceil(width / cellSize) + 1);
        ncells_height = (int) Math.ceil(height / cellSize) + 1;
        grid = new Tree[ncells_width][ncells_height];
        for (int i = 0; i < ncells_width; i++) {
            for (int j = 0; j < ncells_height; j++) {
                grid[i][j] = null;
            }
        }

        for (BlockVector p : startBlockVectors) {
            points.addAll(poissonDiskSampling(MAX_TRIES, new Tree(p, getRandomSchematic()), width, height));
        }

        Random random = new Random();
        for (Tree tree : points) {
            Vector pos = new Vector(tree.getX(), tree.getY() + 1, tree.getZ());
            ClipboardHolder clipboardHolder = new ClipboardHolder(tree.getClipboard(), p.getWorld().getWorldData());
            if (randomRotation) {
                AffineTransform transform = new AffineTransform();
                transform.rotateY(random.nextInt(4) * 90);
                clipboardHolder.setTransform(transform);
            }
            PasteBuilder pb = clipboardHolder.createPaste(editSession, editSession.getWorld().getWorldData()).to(pos)
                    .ignoreAirBlocks(ignoreAirBlocks);
            try {
                Operations.completeLegacy(pb.build());
            } catch (MaxChangedBlocksException e) {
                p.printError("Number of blocks changed exceeds limit set.");
                return;
            }
        }
        localSession.remember(editSession);
        p.print("Done! " + points.size() + " trees pasted. " + schematics.size() + " schematics in pool. " + selectedBlocks + " blocks matched mask. " + (schematicsOverMaxSize == 0 ? "" : schematicsOverMaxSize + " schematics too large."));
    }

    private ArrayList<Tree> poissonDiskSampling(int k, Tree p0, int width, int height) {
        ArrayList<Tree> points = new ArrayList<>();
        ArrayList<Tree> active = new ArrayList<>();
        /*
            This seems to provide okay spacing. With Bridson's algorithm, changing the spacing per tree is not possible.
            Using trees with a wide range of widths/lengths may result in trees too close or far from each other.
         */
        Random generator = new Random();
        points.add(p0);
        active.add(p0);
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
                Tree pnew = possibleVectorsGrid[(int) pnewx][(int) pnewz];
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
        if (possibleVectorsGrid[pX][pZ] == null) {
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
                    if (distance(Math.abs(grid[i][j].getX() - minPoint.getX()), Math.abs(grid[i][j].getZ() - minPoint.getZ()), pX, pZ) < radius)
                        return false;

        /* If we get here, return true */
        return true;
    }

    private boolean isNeighboring(int pX, int pZ, int width, int height) {
        int i0 = (int) Math.max(pX - radius, 0);
        int i1 = (int) Math.min(pX + radius, width - 1);
        int j0 = (int) Math.max(pZ - radius, 0);
        int j1 = (int) Math.min(pZ + radius, height - 1);
        plugin.getLogger().info(i0 + " " + i1 + " " + j0 + " " + j1);

        for (int i = i0; i <= i1; i++)
            for (int j = j0; j <= j1; j++)
                if (possibleVectorsGrid[i][j] != null) {
                    plugin.getLogger().info(i + " " + j + " not null");
                    return true;
                }
        return false;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private Clipboard getRandomSchematic() {
        return schematics.get(new Random().nextInt(schematics.size()));
    }

    private void loadSchematics(File directory) {
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
                            radiusSum += radius(clipboard);
                        } else {
                            schematicsOverMaxSize++;
                        }
                    } catch (IOException e) {
                        p.printError("Schematic " + file.getName() + " not found.");
                    }
                } else if (file.isDirectory()) {
                    loadSchematics(file);
                }
            }
        }
    }

    public static boolean fileIsSchematic(File file) {
        int period = file.getName().lastIndexOf('.');
        return file.getName().substring(period + 1).equals("schematic");
    }

    public void setTargetBlocks(String target) {
        if (target.startsWith("!") && target.length() > 1) {
            this.targetBlocks = target.substring(1).split(",");
            this.inverseMask = true;
        } else {
            this.targetBlocks = target.split(",");
        }
    }

    public boolean matchesTarget(BaseBlock block) {
        for (String targetBlock : targetBlocks) {
            if ((block.getId() + ":" + block.getData()).equals(targetBlock) || (!targetBlock.contains(":") && Integer.toString(block.getId()).equals(targetBlock))) {
                return true;
            }
        }
        return false;
    }

    public float radius(Clipboard clipboard) {
        return Math.max(clipboard.getRegion().getWidth() / 2f, clipboard.getRegion().getLength() / 2f) + 1;
    }

    public boolean inBaseDirectory(File base, File user) {
        URI parentURI = base.toURI();
        URI childURI = user.toURI();
        return !parentURI.relativize(childURI).isAbsolute();
    }
}
