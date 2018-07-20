package nars.experiment.minicraft.side.items;

import nars.experiment.minicraft.side.ItemDefinition;

/**
 * Created by me on 9/19/16.
 */
public class Items {

    public static final ItemDefinition[] items = {
            new ItemDefinition(100, "dirt", "sprites/tiles/dirt.png", 0),
            new ItemDefinition(115, "stone", "sprites/tiles/stone.png", 0),
            new ItemDefinition(110, "sand", "sprites/tiles/sand.png", 0),
            new ItemDefinition(105, "iron", "sprites/entities/iron.png", 0),
            new ItemDefinition(99, "coal", "sprites/entities/coal.png", 0),
            new ItemDefinition(109, "diamond", "sprites/entities/diamond.png", 0),
            new ItemDefinition(98, "cobble", "sprites/tiles/cobble.png", 0),
            new ItemDefinition(119, "wood", "sprites/tiles/wood.png", 0),
            new ItemDefinition(83, "sapling", "sprites/tiles/sapling.png", 0),
            new ItemDefinition(102, "craft", "sprites/tiles/craft.png", 1, new int[][]{{112, 112}, {112, 112}}),
            new ItemDefinition(107, "stick", "sprites/entities/stick.png", 4, new int[][]{{112}, {112}}),
            new ItemDefinition(76, "ladder", "sprites/tiles/ladder.png", 8, new int[][]{{107, 0, 107}, {107, 107, 107}, {107, 0, 107}}),
            new ItemDefinition(112, "plank", "sprites/tiles/plank.png", 4, new int[][]{{119}}),
            new ItemDefinition(106, "torch", "sprites/tiles/torch.png", 4, new int[][]{{99}, {107}})
    };


}
