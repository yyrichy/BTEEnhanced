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
