package com.github.vaporrrr.bteenhanced;

import com.github.vaporrrr.bteenhanced.commands.ReloadConfig;
import com.github.vaporrrr.bteenhanced.commands.Wood;
import com.github.vaporrrr.bteenhanced.commands.WoodRedo;
import com.github.vaporrrr.bteenhanced.commands.WoodUndo;
import org.bukkit.plugin.java.JavaPlugin;

public class BTEEnhanced extends JavaPlugin {
    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("BTEEnhanced enabled!");
        getCommand("wood").setExecutor(new Wood());
        getCommand("wood-undo").setExecutor(new WoodUndo());
        getCommand("wood-redo").setExecutor(new WoodRedo());
        getCommand("bteenhanced-reload").setExecutor(new ReloadConfig());
    }
}
