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

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

public class Tree {
    private final BlockVector blockVector;
    private Clipboard clipboard;

    public Tree(BlockVector blockVector) {
        this.blockVector = blockVector;
    }

    public Tree(BlockVector blockVector, Clipboard clipboard) {
        this.blockVector = blockVector;
        this.clipboard = clipboard;
    }

    public int getX() {
        return (int) blockVector.getX();
    }

    public int getY() {
        return (int) blockVector.getY();
    }

    public int getZ() {
        return (int) blockVector.getZ();
    }

    public BlockVector getBlockVector() {
        return blockVector;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }
}
