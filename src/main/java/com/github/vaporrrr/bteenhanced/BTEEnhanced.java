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
