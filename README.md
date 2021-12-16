# BTEEnhanced (1.12.2) 🍝

Bukkit plugin created for the BuildtheEarth project to make creating custom forests easier. Uses [Bridson's algorithm](https://sighack.com/post/poisson-disk-sampling-bridsons-algorithm) for poisson disk sampling (randomly picking packed points to place trees at).


[![](https://bstats.org/signatures/bukkit/BTEEnhanced.svg)](https://bstats.org/plugin/bukkit/BTEEnhanced "BTEEnhanced on bStats")

**Commands:**
<details>
    <summary>/wood {schematic(s)} [!]{block ID(s)} [flags: -includeAir,-dontRotate,-r:x]</summary>
    *(Aliases: //wood, //w)* More info in "How to use /wood"
</details>
<details>
    <summary>/treebrush {type} [size] [thickness]</summary>
    *(Aliases: /tbr, //tbr, /treebr)* Easy to use brush specifically for trees on top of //schbr ([Schematic Brush Plugin](https://github.com/mikeprimm/SchematicBrush)). Ex: /tbr oak M thin
</details>
<details>
    <summary>/bteenhanced-reload</summary>
    Reload config
</details>
<details>
    <summary>//dell [num]</summary>
    *(Aliases: /dellast, /dell, //dellast)* Deletes the last `[num]` amount of points in the selection. (Currently only supports poly2d selections) If `[num]` is not specified it will delete the last point.
</details>
<details>
    <summary>//delp {num}</summary>
    *(Aliases: /delpoint, /delp, //delpoint)* Deletes the `{num}`'th point in the selection. (Currently only supports poly2d selections)
</details>

**Permissions:** Look [here](src/main/resources/plugin.yml)

**Config:** Look [here](src/main/resources/config.yml)

**Dependencies:**
- `WorldEdit`

## How to use /wood
First make a region selection, all selections such as cuboid, poly, and convex work.
*This plugin saves edit sessions from /wood to the player's local session, so players can use WorldEdit's //undo and //redo. This means players will need to have the WorldEdit permissions for //undo and //redo*

`{schematic(s)}` is the path of a schematic file or a folder containing schematics. (From the WorldEdit schematics folder)
Adding a * after the file separator will randomize the schematics from that folder (and sub folders).

`[!]{block ID(s)}` are the blocks you want trees to be placed above. If you add a "!" at the start it uses all blocks except the ones you mention.

### Flags
All are **optional**
<details>
    <summary>-includeAir</summary>
    Equivalent of not adding -a when pasting with WorldEdit. (By default command ignores air blocks)
</details>
<details>
    <summary>-dontRotate</summary>
    Disables the random rotation (90 degree increments) of schematics.
</details>
<details>
    <summary>-r:x</summary>
    Overrides the automatically created default radius. Radius being the minimum spacing between trees. The radius by default is calculated by averaging the width or height (whichever is larger), and dividing by 2. An example of the flag being used is -r:10
</details>

## Examples
These schematic paths are for trees from the BTE tree pack.
<details>
    <summary>/wood trees/oak/M/* 2,251:0</summary>
    Uses all schematics in `plugins/WorldEdit/trees/oak/M/`, including subdirectories. 2 is the block ID for grass blocks, and 251:0 is the block ID for white concrete, meaning trees will only be placed above grass and white concrete.
</details>
<details>
    <summary>/wood trees/snow/* 4</summary>
    Uses all schematics in `plugins/WorldEdit/trees/snow/`, including subdirectories. In this case since the BTE tree pack has S,M,L snow trees, it will use all three sizes. 4 is the block data for planks, but since there are blocks that have the same block data of 4 (4 in 4:2 for ex.), and different IDs (2 of 4:2 for ex.), not including a ":" when typing "4" will include all blocks with data 4. Oak planks, jungle planks, etc.
</details>
<details>
    <summary>/wood trees/snow/S/Pine_Snowy_Small !35:5 -dontRotate -r:6</summary>
    Uses only the Pine_Snowy_Small.schematic. Trees are pasted above all blocks except 35:5, which is green wool. `-dontRotate` prevents a random rotation from being applied to each tree. `-r:6` overrides the radius to 6.
</details>
