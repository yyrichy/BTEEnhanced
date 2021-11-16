package com.github.vaporrrr.bteenhanced.wood;

import com.google.common.collect.Maps;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.Player;
import org.bukkit.Bukkit;

import java.util.*;

public class WoodManager {
    private static Map<UUID, Wood> woodMap = Maps.newHashMap();
    public static void create(Player p, String schematicLoc, String targetBlock, String radius, ArrayList<String> flags) {
        Wood wood = new Wood(p, schematicLoc, targetBlock, radius, !flags.contains("-ua"));
        woodMap.put(p.getUniqueId(), wood);
        wood.execute();
    }
    public static void create(Player p, String schematicLoc, String targetBlock, String radius) {
        Wood wood = new Wood(p, schematicLoc, targetBlock, radius, true);
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
