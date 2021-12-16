package com.github.vaporrrr.bteenhanced;

import com.github.vaporrrr.bteenhanced.bstats.Metrics;
import com.github.vaporrrr.bteenhanced.commands.*;
import org.bukkit.plugin.java.JavaPlugin;

public class BTEEnhanced extends JavaPlugin {
    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("BTEEnhanced enabled!");
        getCommand("wood").setExecutor(new WoodCommand());
        getCommand("bteenhanced-reload").setExecutor(new ReloadConfig());
        getCommand("dellast").setExecutor(new DelLast());
        getCommand("delpoint").setExecutor(new DelPoint());
        getCommand("treebrush").setExecutor(new TreeBrush());
        getConfig().options().copyDefaults(true);
        saveConfig();
        new Metrics(this, 13388);
        if (getConfig().getBoolean("UpdateCheckEnabled")) {
            Thread updateChecker = new Thread(new UpdateChecker(this));
            updateChecker.start();
        } else {
            getLogger().info("Update checking is disabled. Check for releases at https://github.com/vaporrrr/BTEEnhanced/releases.");
        }
    }
}
