package nars.experiment.minicraft.side;

/**
 * Created by me on 9/19/16.
 */
public enum TileID {
    DIRT(100), GRASS(0), LEAVES(0), PLANK(112), WOOD(119), STONE(115), AIR(0), WATER(0), SAND(
            110), IRON_ORE(105), COAL_ORE(99), DIAMOND_ORE(109), COBBLE(98), CRAFTING_BENCH(102), ADMINITE(
            0), SAPLING(83), LADDER(76), TORCH(106), NONE(0);


    public final int breaksInto;

    TileID(int id) {
        this.breaksInto = id;
    }
}
