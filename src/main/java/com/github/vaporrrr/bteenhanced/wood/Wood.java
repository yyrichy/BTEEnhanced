/*
 * BTEEnhanced, a building tool
 * Copyright 2022 (C) vaporrrr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Wood {
    private static final int N = 2;
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
    private int cellsWidth;
    private int cellsHeight;
    private final ArrayList<Tree> points = new ArrayList<>();
    private final ArrayList<Clipboard> schematics = new ArrayList<>();
    private BlockVector minPoint;
    private final Random random = new Random();
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
        final long startTime = System.nanoTime();
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
                if (!p.hasPermission("bteenhanced.admin.allschematics")) {
                    p.printError("You do not have permission for using the entire schematics folder.");
                    plugin.getLogger().warning(p.getName() + "(" + p.getUniqueId() + ") tried using the entire schematics folder.");
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
        int width = region.getWidth();
        int height = region.getLength();
        ArrayList<BlockVector> startBlockVectors = new ArrayList<>();
        int prevX = 0;
        int prevZ = 0;
        for (BlockVector p : region) {
            BaseBlock block = editSession.getBlock(p);
            if ((editSession.getBlock(new Vector(p.getX(), p.getY() + 1, p.getZ())).isAir() && !block.isAir()) && matchesTarget(block) != inverseMask) {
                int x = Math.abs((int) (p.getX() - minPoint.getX()));
                int z = Math.abs((int) (p.getZ() - minPoint.getZ()));
                if (startBlockVectors.size() == 0) {
                    startBlockVectors.add(p);
                    possibleVectorsGrid[x][z] = new Tree(p);
                    selectedBlocks++;
                } else {
                    Tree tree = possibleVectorsGrid[x][z];
                    if (tree == null || tree.getY() < (int) p.getY()) {
                        int xDist = x - prevX;
                        int zDist = z - prevZ;
                        // Allowing trees to be 1 closer produces better results in testing
                        int radius2 = (int) (radius - 1);
                        if (zDist > radius2) {
                            startBlockVectors.add(p);
                        } else if ((xDist * xDist + zDist * zDist) + 1 > radius2 * radius2) {
                            if (!isAdjacentToExistingPoint(x, z, width, height)) {
                                startBlockVectors.add(p);
                            }
                        }
                        possibleVectorsGrid[x][z] = new Tree(p);
                        selectedBlocks++;
                        prevX = x;
                        prevZ = z;
                    }
                }
            }
        }
        if (selectedBlocks == 0) {
            p.printError("No suitable surface points found. No blocks had air above and " + (inverseMask ? "weren't " : "were ") + Arrays.toString(targetBlocks));
            return;
        }

        final int MAX_TRIES = plugin.getConfig().getInt("MaxTries");
        cellSize = (float) Math.floor(radius / Math.sqrt(N));
        cellsWidth = (int) (Math.ceil(width / cellSize) + 1);
        cellsHeight = (int) Math.ceil(height / cellSize) + 1;
        grid = new Tree[cellsWidth][cellsHeight];
        for (int i = 0; i < cellsWidth; i++) {
            for (int j = 0; j < cellsHeight; j++) {
                grid[i][j] = null;
            }
        }

        for (BlockVector p : startBlockVectors) {
            points.addAll(poissonDiskSampling(MAX_TRIES, new Tree(p, randomSchematic()), width, height));
        }

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
        p.print("Took " + (System.nanoTime() - startTime) / 1e6 + " milliseconds.");
    }

    private ArrayList<Tree> poissonDiskSampling(int k, Tree startingPoint, int width, int height) {
        ArrayList<Tree> points = new ArrayList<>();
        ArrayList<Tree> active = new ArrayList<>();
        /*
            This seems to provide okay spacing. With Bridson's algorithm, changing the spacing per tree is not possible.
            Using trees with a wide range of widths/lengths may result in trees too close or far from each other.
         */
        points.add(startingPoint);
        active.add(startingPoint);
        insertPoint(cellSize, startingPoint);

        while (active.size() > 0) {
            int randomIndex = random.nextInt(active.size());
            BlockVector p = active.get(randomIndex).getBlockVector();
            Clipboard randomClipboard = randomSchematic();

            boolean found = false;
            for (int tries = 0; tries < k; tries++) {
                float theta = random.nextFloat() * 360;
                float newRadius = radius + random.nextFloat() * (2 * radius - radius);
                int x = Math.abs((int) (p.getX() - minPoint.getX()));
                int z = Math.abs((int) (p.getZ() - minPoint.getZ()));
                float newX = (float) (x + newRadius * Math.cos(Math.toRadians(theta)));
                float newZ = (float) (z + newRadius * Math.sin(Math.toRadians(theta)));
                if (!isValidPoint(cellSize, (int) newX, (int) newZ, width, height)) {
                    continue;
                }
                Tree tree = possibleVectorsGrid[(int) newX][(int) newZ];
                tree = new Tree(tree.getBlockVector(), randomClipboard);
                points.add(tree);
                insertPoint(cellSize, tree);
                active.add(tree);
                found = true;
                break;
            }
            /* If no point was found after k tries, remove p */
            if (!found) {
                active.remove(randomIndex);
            }
        }
        return points;
    }

    private void insertPoint(float cellSize, Tree tree) {
        int x = Math.abs((int) (tree.getBlockVector().getX() - minPoint.getX()));
        int z = Math.abs((int) (tree.getBlockVector().getZ() - minPoint.getZ()));
        int xIndex = (int) Math.floor(x / cellSize);
        int zIndex = (int) Math.floor(z / cellSize);
        grid[xIndex][zIndex] = tree;
    }

    private boolean isValidPoint(float cellSize, int x, int z, int width, int height) {
        /* Make sure the point is in the region */
        if (x < 0 || z < 0 || x >= width || z >= height) {
            return false;
        }
        if (possibleVectorsGrid[x][z] == null) {
            return false;
        }
        /* Check neighboring eight cells */
        int xIndex = (int) Math.floor(x / cellSize);
        int zIndex = (int) Math.floor(z / cellSize);
        int i0 = Math.max(xIndex - 1, 0);
        int i1 = Math.min(xIndex + 1, cellsWidth - 1);
        int j0 = Math.max(zIndex - 1, 0);
        int j1 = Math.min(zIndex + 1, cellsHeight - 1);

        for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
                if (grid[i][j] != null) {
                    if (distance(Math.abs(grid[i][j].getX() - minPoint.getX()), Math.abs(grid[i][j].getZ() - minPoint.getZ()), x, z) < radius) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isAdjacentToExistingPoint(int x, int z, int width, int height) {
        // Allowing trees to be 1 closer produces better results in testing
        int i0 = (int) Math.max(x - radius + 1, 0);
        int i1 = (int) Math.min(x + radius - 1, width - 1);
        int j0 = (int) Math.max(z - radius + 1, 0);
        int j1 = (int) Math.min(z + radius - 1, height - 1);
        for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
                if (possibleVectorsGrid[i][j] != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private Clipboard randomSchematic() {
        return schematics.get(random.nextInt(schematics.size()));
    }

    private void loadSchematics(File directory) {
        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormat.SCHEMATIC;
        ClipboardReader reader;
        final int MAX_SCHEM_SIZE = plugin.getConfig().getInt("MaxSchemSize");
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile() && fileIsSchematic(file)) {
                    try {
                        if (file.length() <= MAX_SCHEM_SIZE) {
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

    private void setTargetBlocks(String target) {
        if (target.startsWith("!") && target.length() > 1) {
            this.targetBlocks = target.substring(1).split(",");
            this.inverseMask = true;
        } else {
            this.targetBlocks = target.split(",");
        }
    }

    private boolean matchesTarget(BaseBlock block) {
        for (String targetBlock : targetBlocks) {
            if ((block.getId() + ":" + block.getData()).equals(targetBlock) || (!targetBlock.contains(":") && Integer.toString(block.getId()).equals(targetBlock))) {
                return true;
            }
        }
        return false;
    }

    private static boolean fileIsSchematic(File file) {
        int period = file.getName().lastIndexOf('.');
        return file.getName().substring(period + 1).equals("schematic");
    }

    private static boolean inBaseDirectory(File base, File user) {
        URI parentURI = base.toURI();
        URI childURI = user.toURI();
        return !parentURI.relativize(childURI).isAbsolute();
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private static float radius(Clipboard clipboard) {
        return Math.max(clipboard.getRegion().getWidth() / 2f, clipboard.getRegion().getLength() / 2f) + 1;
    }
}
