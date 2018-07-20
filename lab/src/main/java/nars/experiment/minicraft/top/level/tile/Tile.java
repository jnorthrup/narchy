package nars.experiment.minicraft.top.level.tile;


import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.entity.Mob;
import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.item.Item;
import nars.experiment.minicraft.top.item.resource.Resource;
import nars.experiment.minicraft.top.level.Level;

import java.util.Random;

public class Tile {
    public static Tile[] tiles = new Tile[256];
    public static int tickCount;
    public static Tile none = new Tile(-1);
    protected Random random = new Random();

    public static final Tile grass = new GrassTile(0);
    public static Tile rock = new RockTile(1);
    public static final Tile water = new WaterTile(2);
    public static final Tile flower = new FlowerTile(3);
    public static final Tile tree = new TreeTile(4);
    public static final Tile dirt = new DirtTile(5);
    public static final Tile sand = new SandTile(6);
    public static final Tile cactus = new CactusTile(7);
    public static final Tile hole = new HoleTile(8);
    public static final Tile treeSapling = new SaplingTile(9, grass, tree);
    public static final Tile cactusSapling = new SaplingTile(10, sand, cactus);
    public static final Tile farmland = new FarmTile(11);
    public static final Tile wheat = new WheatTile(12);
    public static final Tile lava = new LavaTile(13);
    public static Tile stairsDown = new StairsTile(14, false);
    public static Tile stairsUp = new StairsTile(15, true);
    public static final Tile infiniteFall = new InfiniteFallTile(16);
    public static final Tile cloud = new CloudTile(17);
    public static Tile hardRock = new HardRockTile(18);
    public static Tile ironOre = new OreTile(19, Resource.ironOre);
    public static Tile goldOre = new OreTile(20, Resource.goldOre);
    public static Tile gemOre = new OreTile(21, Resource.gem);
    public static Tile cloudCactus = new CloudCactusTile(22);

    public final byte id;

    public boolean connectsToGrass;
    public boolean connectsToSand;
    public boolean connectsToLava;
    public boolean connectsToWater;

    public Tile(int id) {
        this.id = (byte) id;
        if (id >= 0) {
            if (tiles[id] != null) throw new RuntimeException("Duplicate tile ids!");
            tiles[id] = this;
        }
    }

    public void render(Screen screen, Level level, int x, int y) {
    }

    public boolean mayPass(Level level, int x, int y, Entity e) {
        return true;
    }

    public int getLightRadius(Level level, int x, int y) {
        return 0;
    }

    public void hurt(Level level, int x, int y, Mob source, int dmg, int attackDir) {
    }

    public void bumpedInto(Level level, int xt, int yt, Entity entity) {
    }

    public void tick(Level level, int xt, int yt) {
    }

    public void steppedOn(Level level, int xt, int yt, Entity entity) {
    }

    public boolean interact(Level level, int xt, int yt, Player player, Item item, int attackDir) {
        return false;
    }

    public static boolean use(Level level, int xt, int yt, Player player, int attackDir) {
        return false;
    }

    public boolean connectsToLiquid() {
        return connectsToWater || connectsToLava;
    }
}