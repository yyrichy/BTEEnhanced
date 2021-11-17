package com.github.vaporrrr.bteenhanced.wood;

import com.google.common.collect.Maps;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.entity.Player;

import java.util.*;

public class WoodManager {
    private static Map<UUID, Wood> woodMap = Maps.newHashMap();
    public static void create(Player p, String schematicLoc, String targetBlock, ArrayList<String> flags) {
        float radius = Float.NaN;
        for (String flag : flags) {
            if (flag.contains("-r:")) {
                if (flag.length() < 4) {
                    p.printError("If you want to set the radius, you must specify a number.");
                    return;
                }
                try {
                    radius = Float.parseFloat(flag.substring(flag.indexOf(':') + 1));
                } catch (Exception e) {
                    p.printError("Radius is not a number.");
                    return;
                }
            }
        }
        Wood wood = new Wood(p, schematicLoc, targetBlock, radius, !flags.contains("-useAir"), !flags.contains("-dontRotate"));
        woodMap.put(p.getUniqueId(), wood);
        wood.execute();
    }
    public static void create(Player p, String schematicLoc, String targetBlock) {
        Wood wood = new Wood(p, schematicLoc, targetBlock, Float.NaN, true, true);
        woodMap.put(p.getUniqueId(), wood);
        wood.execute();
    }
    public static void undo(Player p) {
        if (woodMap == null || !woodMap.containsKey(p.getUniqueId())) {
            p.printError("Nothing to undo.");
            return;
        }
        EditSession newEditSession = new EditSession((LocalWorld) p.getWorld(), -1);
        EditSession editSession = woodMap.get(p.getUniqueId()).getEditSession();
        editSession.undo(newEditSession);
        p.print("Undone.");
    }
    public static void redo(Player p) {
        if (woodMap == null || !woodMap.containsKey(p.getUniqueId())) {
            p.printError("Nothing to redo.");
            return;
        }
        EditSession newEditSession = new EditSession((LocalWorld) p.getWorld(), -1);
        EditSession editSession = woodMap.get(p.getUniqueId()).getEditSession();
        editSession.redo(newEditSession);
        p.print("Redone.");
    }
}
