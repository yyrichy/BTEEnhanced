# BTEEnhanced (1.12.2) 🍝

Bukkit plugin created for the BuildtheEarth project to make creating custom forests easier. Uses [Bridson's algorithm](https://sighack.com/post/poisson-disk-sampling-bridsons-algorithm) for poisson disk sampling (randomly picking packed points to place trees at).

Commands:
- `/wood <schematic(s)> [!]<blockID(s)> [flags -includeAir,-dontRotate,-r:x]` (Alias: //wood)
- `/bteenhanced-reload`

Permissions:
- `bteenhanced.*`  Wildcard
- `bteenhanced.reload` Reload config
- `bteenhanced.wood`Allows usage of /wood
- `bteenhanced.wood.all` Allows setting the /wood directory to *, using all schematics in the folder. Not a good idea since there will likely be large builds there. Instead, trees should be placed in a tree folder.

*This plugin saves edit sessions from /wood to the player's local session, so players can use WorldEdit's //undo and //redo. This means players will need to have the WorldEdit permissions for //undo and //redo*

Config: Look at the config.yml

Dependencies:
- `WorldEdit`

## How to use /wood
First make a region selection, all selections such as cuboid, poly, and convex work.

<schematic(s)> is the path of a schematic file or a folder containing schematics. (From the WorldEdit schematics folder)
Adding a * after the file separator will randomize the schematics from that folder (and subfolders).

[!]<blockID(s)> are the blocks you want trees to be placed above. If you add a "!" at the start it uses all blocks except the ones you mention.

### Flags
All are **optional**
- `-includeAir` is the equivalent of not adding -a when pasting. (By default ignores air blocks)
- `-dontRotate` disables the random rotation (90 degree increments) of schematics.
- `-r:x` overrides the automatically created default radius. Radius being the minimum spacing between trees. The radius by default is calculated by averaging the width or height (whichever is larger), and dividing by 2. An example of the flag being used is -r:10

## Examples
These schematic paths are for trees from the BTE tree pack.
- `/wood trees/oak/M/* 2,251:0` Uses all schematics in `plugins/WorldEdit/trees/oak/M/`, including subdirectories. 2 is the block ID for grass blocks, and 251:0 is the block ID for white concrete, meaning trees will only be placed above grass and white concrete.
- `/wood trees/snow/* 4` Uses all schematics in `plugins/WorldEdit/trees/snow/`, including subdirectories. In this case since the BTE tree pack has S,M,L snow trees, it will use all three sizes. 4 is the block data for planks, but since there are blocks that have the same block data of 4 (4 in 4:2 for ex.), and different IDs (2 of 4:2 for ex.), not including a ":" when typing "4" will include all blocks with data 4. Oak planks, jungle planks, etc.
- `/wood trees/snow/S/Pine_Snowy_Small !35:5 -dontRotate -r:6` Uses only the Pine_Snowy_Small.schematic. Trees are pasted above all blocks except 35:5, which is green wool. `-dontRotate` prevents a random rotation from being applied to each tree. `-r:6` overrides the radius to 6.
