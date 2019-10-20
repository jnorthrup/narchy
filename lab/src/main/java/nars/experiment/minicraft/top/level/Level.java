package nars.experiment.minicraft.top.level;


import nars.experiment.minicraft.top.entity.*;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.level.levelgen.LevelGen;
import nars.experiment.minicraft.top.level.tile.Tile;

import java.util.*;

public class Level {
    private final Random random = new Random();

    public int w;
    public int h;

    public byte[] tiles;
    public byte[] data;
    public List<Entity>[] entitiesInTiles;

    public int grassColor = 141;
    public int dirtColor = 322;
    public int sandColor = 550;
    private final int depth;
    public int monsterDensity = 8;

    public List<Entity> entities = new ArrayList<>();
    private final Comparator<Entity> spriteSorter = (e0, e1) -> Integer.compare(e0.y, e1.y);

    @SuppressWarnings("unchecked")
    public Level(int w, int h, int level, Level parentLevel) {
        if (level < 0) {
            dirtColor = 222;
        }
        this.depth = level;
        this.w = w;
        this.h = h;

        if (level == 1) {
            dirtColor = 444;
        }
        byte[][] maps;
        if (level == 0)
            maps = LevelGen.createAndValidateTopMap(w, h);
        else if (level < 0) {
            maps = LevelGen.createAndValidateUndergroundMap(w, h, -level);
            monsterDensity = 4;
        } else {
            maps = LevelGen.createAndValidateSkyMap(w, h);
            monsterDensity = 4;
        }

        tiles = maps[0];
        data = maps[1];

        if (parentLevel != null) {
            for (var y = 0; y < h; y++)
                for (var x = 0; x < w; x++) {
                    if (parentLevel.getTile(x, y) == Tile.stairsDown) {

                        setTile(x, y, Tile.stairsUp, 0);
                        if (level == 0) {
                            setTile(x - 1, y, Tile.hardRock, 0);
                            setTile(x + 1, y, Tile.hardRock, 0);
                            setTile(x, y - 1, Tile.hardRock, 0);
                            setTile(x, y + 1, Tile.hardRock, 0);
                            setTile(x - 1, y - 1, Tile.hardRock, 0);
                            setTile(x - 1, y + 1, Tile.hardRock, 0);
                            setTile(x + 1, y - 1, Tile.hardRock, 0);
                            setTile(x + 1, y + 1, Tile.hardRock, 0);
                        } else {
                            setTile(x - 1, y, Tile.dirt, 0);
                            setTile(x + 1, y, Tile.dirt, 0);
                            setTile(x, y - 1, Tile.dirt, 0);
                            setTile(x, y + 1, Tile.dirt, 0);
                            setTile(x - 1, y - 1, Tile.dirt, 0);
                            setTile(x - 1, y + 1, Tile.dirt, 0);
                            setTile(x + 1, y - 1, Tile.dirt, 0);
                            setTile(x + 1, y + 1, Tile.dirt, 0);
                        }
                    }

                }
        }

        entitiesInTiles = new ArrayList[w * h];
        for (var i = 0; i < w * h; i++) {
            entitiesInTiles[i] = new ArrayList<>();
        }

        if (level == 1) {
            var aw = new AirWizard();
            aw.x = w * 8;
            aw.y = h * 8;
            add(aw);
        }
    }

    public void renderBackground(Screen screen, int xScroll, int yScroll) {
        var w = (screen.w + 15) >> 4;
        var h = (screen.h + 15) >> 4;
        screen.setOffset(xScroll, yScroll);
        var yo = yScroll >> 4;
        var xo = xScroll >> 4;
        for (var y = yo; y <= h + yo; y++) {
            for (var x = xo; x <= w + xo; x++) {
                getTile(x, y).render(screen, this, x, y);
            }
        }
        screen.setOffset(0, 0);
    }

    private final List<Entity> rowSprites = new ArrayList<>();

    public Player player;

    public void renderSprites(Screen screen, int xScroll, int yScroll) {
        var w = (screen.w + 15) >> 4;
        var h = (screen.h + 15) >> 4;

        screen.setOffset(xScroll, yScroll);
        var yo = yScroll >> 4;
        var xo = xScroll >> 4;
        for (var y = yo; y <= h + yo; y++) {
            for (var x = xo; x <= w + xo; x++) {
                if (x < 0 || y < 0 || x >= this.w || y >= this.h) continue;
                rowSprites.addAll(entitiesInTiles[x + y * this.w]);
            }
            if (!rowSprites.isEmpty()) {
                sortAndRender(screen, rowSprites);
            }
            rowSprites.clear();
        }
        screen.setOffset(0, 0);
    }

    public void renderLight(Screen screen, int xScroll, int yScroll) {
        var w = (screen.w + 15) >> 4;
        var h = (screen.h + 15) >> 4;

        screen.setOffset(xScroll, yScroll);
        var r = 4;
        var yo = yScroll >> 4;
        var xo = xScroll >> 4;
        for (var y = yo - r; y <= h + yo + r; y++) {
            for (var x = xo - r; x <= w + xo + r; x++) {
                if (x < 0 || y < 0 || x >= this.w || y >= this.h) continue;
                var entities = entitiesInTiles[x + y * this.w];
                for (var i = 0; i < entities.size(); i++) {
                    var e = entities.get(i);

                    var lr = e.getLightRadius();
                    if (lr > 0) screen.renderLight(e.x - 1, e.y - 4, lr * 8);
                }
                var lr = getTile(x, y).getLightRadius(this, x, y);
                if (lr > 0) screen.renderLight(x * 16 + 8, y * 16 + 8, lr * 8);
            }
        }
        screen.setOffset(0, 0);
    }


    private void sortAndRender(Screen screen, List<Entity> list) {
        Collections.sort(list, spriteSorter);
        for (var i = 0; i < list.size(); i++) {
            list.get(i).render(screen);
        }
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return Tile.rock;
        return Tile.tiles[tiles[x + y * w]];
    }

    public void setTile(int x, int y, Tile t, int dataVal) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        tiles[x + y * w] = t.id;
        data[x + y * w] = (byte) dataVal;
    }

    public int getData(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return 0;
        return data[x + y * w] & 0xff;
    }

    public void setData(int x, int y, int val) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        data[x + y * w] = (byte) val;
    }

    public void add(Entity entity) {
        if (entity instanceof Player) {
            player = (Player) entity;
        }
        entity.removed = false;
        entities.add(entity);
        entity.init(this);

        insertEntity(entity.x >> 4, entity.y >> 4, entity);
    }

    public void remove(Entity e) {
        entities.remove(e);
        var xto = e.x >> 4;
        var yto = e.y >> 4;
        removeEntity(xto, yto, e);
    }

    private void insertEntity(int x, int y, Entity e) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        entitiesInTiles[x + y * w].add(e);
    }

    private void removeEntity(int x, int y, Entity e) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        entitiesInTiles[x + y * w].remove(e);
    }

    public void trySpawn(int count) {
        for (var i = 0; i < count; i++) {

            var maxLevel = 1;
            if (depth < 0) {
                maxLevel = (-depth) + 1;
            }
            var minLevel = 1;
            if (depth > 0) {
                minLevel = maxLevel = 4;
            }

            var lvl = random.nextInt(maxLevel - minLevel + 1) + minLevel;
            Mob mob;
            if (random.nextInt(2) == 0)
                mob = new Slime(lvl);
            else
                mob = new Zombie(lvl);

            if (mob.findStartPos(this)) {
                this.add(mob);
            }
        }
    }

    public void tick() {
        trySpawn(1);

        for (var i = 0; i < w * h / 50; i++) {
            var xt = random.nextInt(w);
            var yt = random.nextInt(w);
            getTile(xt, yt).tick(this, xt, yt);
        }
        for (var i = 0; i < entities.size(); i++) {
            var e = entities.get(i);
            var xto = e.x >> 4;
            var yto = e.y >> 4;

            e.tick();

            if (e.removed) {
                entities.remove(i--);
                removeEntity(xto, yto, e);
            } else {
                var xt = e.x >> 4;
                var yt = e.y >> 4;

                if (xto != xt || yto != yt) {
                    removeEntity(xto, yto, e);
                    insertEntity(xt, yt, e);
                }
            }
        }
    }

    public List<Entity> getEntities(int x0, int y0, int x1, int y1) {
        List<Entity> result = new ArrayList<>();
        var xt0 = (x0 >> 4) - 1;
        var yt0 = (y0 >> 4) - 1;
        var xt1 = (x1 >> 4) + 1;
        var yt1 = (y1 >> 4) + 1;
        for (var y = yt0; y <= yt1; y++) {
            for (var x = xt0; x <= xt1; x++) {
                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                var entities = entitiesInTiles[x + y * this.w];
                for (var i = 0; i < entities.size(); i++) {
                    var e = entities.get(i);
                    if (e.intersects(x0, y0, x1, y1)) result.add(e);
                }
            }
        }
        return result;
    }
}