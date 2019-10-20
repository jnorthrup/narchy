package nars.experiment.minicraft.side;

import org.eclipse.collections.impl.map.mutable.primitive.CharObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import java.util.EnumMap;
import java.util.Map;

public class Constants {

    public static final Map<TileID, Tile> tileTypes = new EnumMap(TileID.class);
    public static final IntObjectHashMap<TileID> tileIDs = new IntObjectHashMap<>();

    static {
        tileTypes.put(TileID.DIRT, new Tile(new TileType("sprites/tiles/dirt.png", TileID.DIRT)));
        tileTypes.put(TileID.GRASS, new Tile(new TileType("sprites/tiles/dirtwithgrass.png",
                TileID.GRASS)));
        tileTypes.put(TileID.LEAVES, new Tile(new TileType("sprites/tiles/leaves.png",
                TileID.LEAVES, false, false, 1)));
        tileTypes
                .put(TileID.PLANK, new Tile(new TileType("sprites/tiles/plank.png", TileID.PLANK)));
        tileTypes.put(TileID.WOOD, new Tile(new TileType("sprites/tiles/wood.png", TileID.WOOD,
                true, false, 0)));
        tileTypes
                .put(TileID.STONE, new Tile(new TileType("sprites/tiles/stone.png", TileID.STONE)));
        tileTypes.put(TileID.AIR, new Tile(new TileType("sprites/tiles/air.png", TileID.AIR, true,
                false, 0)));
        tileTypes.put(TileID.WATER, new Tile(new TileType("sprites/tiles/water.png", TileID.WATER,
                true, true, 1)));
        tileTypes.put(TileID.SAND, new Tile(new TileType("sprites/tiles/sand.png", TileID.SAND)));
        tileTypes.put(TileID.IRON_ORE, new Tile(new TileType("sprites/tiles/ironore.png",
                TileID.IRON_ORE)));
        tileTypes.put(TileID.COAL_ORE, new Tile(new TileType("sprites/tiles/coalore.png",
                TileID.COAL_ORE)));
        tileTypes.put(TileID.DIAMOND_ORE, new Tile(new TileType("sprites/tiles/diamondore.png",
                TileID.DIAMOND_ORE)));
        tileTypes.put(TileID.COBBLE, new Tile(new TileType("sprites/tiles/cobble.png",
                TileID.COBBLE)));
        tileTypes.put(TileID.CRAFTING_BENCH, new Tile(new TileType("sprites/tiles/craft.png",
                TileID.CRAFTING_BENCH)));
        tileTypes.put(TileID.ADMINITE, new Tile(new TileType("sprites/tiles/adminite.png",
                TileID.ADMINITE)));
        tileTypes.put(TileID.SAPLING, new Tile(new TileType("sprites/tiles/sapling.png",
                TileID.SAPLING, true, false, 0)));
        tileTypes.put(TileID.LADDER, new Tile(new TileType("sprites/tiles/ladder.png",
                TileID.LADDER, true, false, 0)));
        tileTypes.put(TileID.TORCH, new Tile(new TileType("sprites/tiles/torch.png", TileID.TORCH,
                true, false, 0, Constants.LIGHT_VALUE_TORCH)));

        for (TileID tileID : TileID.values()) {
            tileIDs.put(tileID.breaksInto, tileID);
        }
        tileIDs.compact();
    }

    public static final CharObjectHashMap<Item> itemTypes = ItemLoader.loadItems(16);

    public static final int LIGHT_VALUE_TORCH = 13;
    public static final int LIGHT_VALUE_SUN = 15;

    public static boolean DEBUG = true;
    public static final boolean DEBUG_VISIBILITY_ON = true;
    public static final int LIGHT_VALUE_OPAQUE = 10000;
}
