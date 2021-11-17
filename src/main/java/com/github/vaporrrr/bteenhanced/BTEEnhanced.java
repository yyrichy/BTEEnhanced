package com.github.vaporrrr.bteenhanced;

import com.github.vaporrrr.bteenhanced.commands.Wood;
import com.github.vaporrrr.bteenhanced.commands.WoodRedo;
import com.github.vaporrrr.bteenhanced.commands.WoodUndo;
import org.bukkit.plugin.java.JavaPlugin;

public class BTEEnhanced extends JavaPlugin {
    @Override public void onDisable(){}
    @Override public void onEnable(){
        getLogger().info("BTEEnhanced enabled!");
        getCommand("wood").setExecutor(new Wood());
        getCommand("wood-undo").setExecutor(new WoodUndo());
        getCommand("wood-redo").setExecutor(new WoodRedo());
    }
}
