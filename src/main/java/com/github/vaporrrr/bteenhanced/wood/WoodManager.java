package com.github.vaporrrr.bteenhanced.wood;

import com.github.vaporrrr.bteenhanced.BTEEnhanced;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class WoodManager {
    private static Map<UUID, ArrayList<Wood>> woodMap = Maps.newHashMap();
    private static final Plugin plugin = BTEEnhanced.getPlugin(BTEEnhanced.class);

    public static void create(Player p, String schematicLoc, String targetBlock, ArrayList<String> flags) {
        float radius = Float.NaN;
        boolean ignoreAir = true;
        boolean randomRotation = true;
        for (String flag : flags) {
            if (flag.startsWith("-")) {
                if (flag.equals("-includeAir")) {
                    ignoreAir = false;
                } else if (flag.equals("-dontRotate")) {
                    randomRotation = false;
                } else if (flag.startsWith("-r:")) {
                    try {
                        radius = Float.parseFloat(flag.substring(flag.indexOf(':') + 1));
                    } catch (Exception e) {
                        p.printError("Radius is not a number.");
                        return;
                    }
                }
            }
        }
        Wood wood = new Wood(p, schematicLoc, targetBlock, radius, ignoreAir, randomRotation);
        add(wood, p.getUniqueId());
        wood.execute();
    }

    public static void create(Player p, String schematicLoc, String targetBlock) {
        Wood wood = new Wood(p, schematicLoc, targetBlock, Float.NaN, true, true);
        add(wood, p.getUniqueId());
        wood.execute();
    }

    public static void undo(Player p) {
        if (woodMap == null || !woodMap.containsKey(p.getUniqueId())) {
            p.printError("Nothing to undo, could not find player in map.");
            return;
        }
        EditSession newEditSession = new EditSession((LocalWorld) p.getWorld(), -1);
        EditSession editSession = null;
        int index = -1;
        Wood wood = null;
        ArrayList<Wood> woodArrayList = woodMap.get(p.getUniqueId());
        for (int i = woodArrayList.size() - 1; i >= 0; i--) {
            wood = woodArrayList.get(i);
            if (!wood.isUndone()) {
                editSession = wood.getEditSession();
                index = i;
                break;
            }
        }
        if (editSession == null) {
            p.print("Nothing to undo, redo something first.");
        } else {
            editSession.undo(newEditSession);
            updateUndone(index, wood, true, p.getUniqueId());
            p.print("Undone.");
        }
    }

    public static void redo(Player p) {
        if (woodMap == null || !woodMap.containsKey(p.getUniqueId())) {
            p.printError("Nothing to redo, could not find player in map.");
            return;
        }
        EditSession newEditSession = new EditSession((LocalWorld) p.getWorld(), -1);
        EditSession editSession = null;
        int index = -1;
        Wood wood = null;
        ArrayList<Wood> woodArrayList = woodMap.get(p.getUniqueId());
        for (int i = 0; i < woodArrayList.size(); i++) {
            wood = woodArrayList.get(i);
            if (wood.isUndone()) {
                editSession = wood.getEditSession();
                index = i;
                break;
            }
        }
        if (editSession == null) {
            p.print("Nothing to redo, undo something first.");
        } else {
            editSession.redo(newEditSession);
            updateUndone(index, wood, false, p.getUniqueId());
            p.print("Redone.");
        }
    }

    public static void add(Wood wood, UUID UUID) {
        if (woodMap.containsKey(UUID)) {
            ArrayList<Wood> woodArrayList = woodMap.get(UUID);
            if (woodArrayList.size() >= plugin.getConfig().getInt("ActionsToKeep")) {
                woodArrayList.remove(0);
            }
            woodArrayList.add(wood);
        } else {
            ArrayList<Wood> woodArrayList = new ArrayList<>();
            woodArrayList.add(wood);
            woodMap.put(UUID, woodArrayList);
        }
    }

    public static void updateUndone(int i, Wood wood, boolean undone, UUID UUID) {
        wood.setUndone(undone);
        ArrayList<Wood> newWoodList = woodMap.get(UUID);
        newWoodList.remove(i);
        newWoodList.add(wood);
        woodMap.put(UUID, newWoodList);
    }
}

