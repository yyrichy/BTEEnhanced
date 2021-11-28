package com.github.vaporrrr.bteenhanced;

import com.github.vaporrrr.bteenhanced.bstats.Metrics;
import com.github.vaporrrr.bteenhanced.commands.DelLast;
import com.github.vaporrrr.bteenhanced.commands.ReloadConfig;
import com.github.vaporrrr.bteenhanced.commands.WoodCommand;
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
        Metrics metrics = new Metrics(this, 13388);
    }
}
