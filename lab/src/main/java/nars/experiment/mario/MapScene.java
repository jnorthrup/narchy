package nars.experiment.mario;

import nars.experiment.mario.level.ImprovedNoise;
import nars.experiment.mario.level.LevelGenerator;
import nars.experiment.mario.sprites.Mario;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Random;


public class MapScene extends Scene {
    private static final int TILE_GRASS = 0;
    private static final int TILE_WATER = 1;
    private static final int TILE_LEVEL = 2;
    private static final int TILE_ROAD = 3;
    private static final int TILE_DECORATION = 4;

    private int[][] level;
    private int[][] data;

    private int xMario;
    private int yMario;
    private int xMarioA;
    private int yMarioA;

    private int tick;
    private final Image staticBg;
    private final Graphics staticGr;
    private Random random = new Random();
    private int moveTime;
    private final MarioComponent marioComponent;
    private long seed;
    private int worldNumber;

    private int levelId;
    private int farthest;
    private int xFarthestCap;
    private int yFarthestCap;

    public MapScene(GraphicsConfiguration graphicsConfiguration, MarioComponent marioComponent, long seed) {
        this.marioComponent = marioComponent;
        this.seed = seed;

        random = new Random(seed);
        staticBg = graphicsConfiguration.createCompatibleImage(320, 240, Transparency.BITMASK);
        staticGr = staticBg.getGraphics();
    }

    @Override
    public void init() {
        worldNumber = -1;
        nextWorld();
    }

    private void nextWorld() {
        worldNumber++;

        if (worldNumber == 8) {
            marioComponent.win();
            return;
        }

        moveTime = 0;
        levelId = 0;
        farthest = 0;
        xFarthestCap = 0;
        yFarthestCap = 0;

        seed = random.nextLong();
        random = new Random(seed);

        while (!generateLevel())
            ;
        renderStatic(staticGr);
    }

    public static void startMusic() {
        Art.startMusic(0);
    }

    private boolean generateLevel() {
        random = new Random(seed);
        var n0 = new ImprovedNoise(random.nextLong());
        var n1 = new ImprovedNoise(random.nextLong());
        var dec = new ImprovedNoise(random.nextLong());

        var width = 320 / 16 + 1;
        var height = 240 / 16 + 1;
        level = new int[width][height];
        data = new int[width][height];
        var xo0 = random.nextDouble() * 512;
        var yo0 = random.nextDouble() * 512;
        var xo1 = random.nextDouble() * 512;
        var yo1 = random.nextDouble() * 512;
        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                var xd = ((x + 1) / (double) width - 0.5) * 2;
                var yd = ((y + 1) / (double) height - 0.5) * 2;
                @SuppressWarnings("unused")
                var d = Math.sqrt(xd * xd + yd * yd) * 2;
                if (x == 0 || y == 0 || x >= width - 3 || y >= height - 3) d = 100;
                var t0 = n0.perlinNoise(x * 10.0 + xo0, y * 10.0 + yo0);
                var t1 = n1.perlinNoise(x * 10.0 + xo1, y * 10.0 + yo1);
                var td = (t0 - t1);
                var t = (td * 2);
                level[x][y] = t > 0 ? TILE_WATER : TILE_GRASS;
            }
        }

        var lowestX = 9999;
        var lowestY = 9999;
        var t = 0;
        for (var i = 0; i < 100 && t < 12; i++) {
            var x = random.nextInt((width - 1) / 3) * 3 + 2;
            var y = random.nextInt((height - 1) / 3) * 3 + 1;
            if (level[x][y] == TILE_GRASS) {
                if (x < lowestX) {
                    lowestX = x;
                    lowestY = y;
                }
                level[x][y] = TILE_LEVEL;
                data[x][y] = -1;
                t++;
            }
        }

        data[lowestX][lowestY] = -2;

        while (findConnection(width, height))
            ;

        findCaps(width, height);

        if (xFarthestCap == 0) return false;

        data[xFarthestCap][yFarthestCap] = -2;
        data[xMario / 16][yMario / 16] = -11;


        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                if (level[x][y] == TILE_GRASS && (x != xFarthestCap || y != yFarthestCap - 1)) {
                    var t0 = dec.perlinNoise(x * 10.0 + xo0, y * 10.0 + yo0);
                    if (t0 > 0) level[x][y] = TILE_DECORATION;
                }
            }
        }

        return true;
    }

    private void travel(int x, int y, int dir, int depth) {
        if (level[x][y] != TILE_ROAD && level[x][y] != TILE_LEVEL) {
            return;
        }
        if (level[x][y] == TILE_ROAD) {
            if (data[x][y] == 1) return;
            else data[x][y] = 1;
        }

        if (level[x][y] == TILE_LEVEL) {
            if (data[x][y] > 0) {
                if (levelId != 0 && random.nextInt(4) == 0) {
                    data[x][y] = -3;
                } else {
                    data[x][y] = ++levelId;
                }
            } else if (depth > 0) {
                data[x][y] = -1;
                if (depth > farthest) {
                    farthest = depth;
                    xFarthestCap = x;
                    yFarthestCap = y;
                }
            }
        }

        if (dir != 2) travel(x - 1, y, 0, depth++);
        if (dir != 3) travel(x, y - 1, 1, depth++);
        if (dir != 0) travel(x + 1, y, 2, depth++);
        if (dir != 1) travel(x, y + 1, 3, depth++);
    }

    private void findCaps(int width, int height) {
        var xCap = -1;
        var yCap = -1;

        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                if (level[x][y] == TILE_LEVEL) {
                    var roads = 0;
                    for (var xx = x - 1; xx <= x + 1; xx++)
                        for (var yy = y - 1; yy <= y + 1; yy++) {
                            if (level[xx][yy] == TILE_ROAD) roads++;
                        }

                    if (roads == 1) {
                        if (xCap == -1) {
                            xCap = x;
                            yCap = y;
                        }
                        data[x][y] = 0;
                    } else {
                        data[x][y] = 1;
                    }
                }
            }
        }

        xMario = xCap * 16;
        yMario = yCap * 16;

        travel(xCap, yCap, -1, 0);
    }

    private boolean findConnection(int width, int height) {
        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                if (level[x][y] == TILE_LEVEL && data[x][y] == -1) {
                    connect(x, y, width, height);
                    return true;
                }
            }
        }
        return false;
    }

    private void connect(int xSource, int ySource, int width, int height) {
        var maxDist = 10000;
        var xTarget = 0;
        var yTarget = 0;
        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                if (level[x][y] == TILE_LEVEL && data[x][y] == -2) {
                    var xd = Math.abs(xSource - x);
                    var yd = Math.abs(ySource - y);
                    var d = xd * xd + yd * yd;
                    if (d < maxDist) {
                        xTarget = x;
                        yTarget = y;
                        maxDist = d;
                    }
                }
            }
        }

        drawRoad(xSource, ySource, xTarget, yTarget);
        level[xSource][ySource] = TILE_LEVEL;
        data[xSource][ySource] = -2;
        return;
    }

    private void drawRoad(int x0, int y0, int x1, int y1) {
        var xFirst = random.nextBoolean();

        if (xFirst) {
            while (x0 > x1) {
                data[x0][y0] = 0;
                level[x0--][y0] = TILE_ROAD;
            }
            while (x0 < x1) {
                data[x0][y0] = 0;
                level[x0++][y0] = TILE_ROAD;
            }
        }
        while (y0 > y1) {
            data[x0][y0] = 0;
            level[x0][y0--] = TILE_ROAD;
        }
        while (y0 < y1) {
            data[x0][y0] = 0;
            level[x0][y0++] = TILE_ROAD;
        }
        if (!xFirst) {
            while (x0 > x1) {
                data[x0][y0] = 0;
                level[x0--][y0] = TILE_ROAD;
            }
            while (x0 < x1) {
                data[x0][y0] = 0;
                level[x0++][y0] = TILE_ROAD;
            }
        }
    }

    public void renderStatic(Graphics g) {
        var map = Art.map;

        for (var x = 0; x < 320 / 16; x++) {
            for (var y = 0; y < 240 / 16; y++) {
                g.drawImage(map[worldNumber / 4][0], x * 16, y * 16, null);
                switch (level[x][y]) {
                    case TILE_LEVEL:
                        var type = data[x][y];
                        switch (type) {
                            case 0:
                                g.drawImage(map[0][7], x * 16, y * 16, null);
                                break;
                            case -1:
                                g.drawImage(map[3][8], x * 16, y * 16, null);
                                break;
                            case -3:
                                g.drawImage(map[0][8], x * 16, y * 16, null);
                                break;
                            case -10:
                                g.drawImage(map[1][8], x * 16, y * 16, null);
                                break;
                            case -11:
                                g.drawImage(map[1][7], x * 16, y * 16, null);
                                break;
                            case -2:
                                g.drawImage(map[2][7], x * 16, y * 16 - 16, null);
                                g.drawImage(map[2][8], x * 16, y * 16, null);
                                break;
                            default:
                                g.drawImage(map[type - 1][6], x * 16, y * 16, null);
                                break;
                        }
                        break;
                    case TILE_ROAD: {
                       var  p0 = isRoad(x - 1, y) ? 1 : 0;
                       var  p1 = isRoad(x, y - 1) ? 1 : 0;
                       var  p2 = isRoad(x + 1, y) ? 1 : 0;
                       var  p3 = isRoad(x, y + 1) ? 1 : 0;
                       var  s = p0 + p1 * 2 + p2 * 4 + p3 * 8;
                        g.drawImage(map[s][2], x * 16, y * 16, null);
                    }
                    break;
                    case TILE_WATER:
                        for (var xx = 0; xx < 2; xx++) {
                            var x2 = x * 2;
                            for (var yy = 0; yy < 2; yy++) {
                                var p0 = isWater(x2 + (xx - 1), y * 2 + (yy - 1)) ? 0 : 1;
                              var p1 = isWater(x2 + (xx + 0), y * 2 + (yy - 1)) ? 0 : 1;
                              var p2 = isWater(x2 + (xx - 1), y * 2 + (yy + 0)) ? 0 : 1;
                              var p3 = isWater(x2 + (xx + 0), y * 2 + (yy + 0)) ? 0 : 1;
                              var s = p0 + p1 * 2 + p2 * 4 + p3 * 8 - 1;
                                if (s >= 0 && s < 14) {
                                    g.drawImage(map[s][4 + ((xx + yy) & 1)], x * 16 + xx * 8, y * 16 + yy * 8, null);
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

    private final DecimalFormat df = new DecimalFormat("00");

    @Override
    public void render(Graphics g, float alpha) {
        g.drawImage(staticBg, 0, 0, null);
        var map = Art.map;

        for (var y = 0; y <= 240 / 16; y++) {
            for (var x = 320 / 16; x >= 0; x--) {
                if (level[x][y] == TILE_WATER) {
                    if (isWater(x * 2 - 1, y * 2 - 1)) {
                        g.drawImage(map[15][4 + (tick / 6 + y) % 4], x * 16 - 8, y * 16 - 8, null);
                    }
                } else if (level[x][y] == TILE_DECORATION) {
                    g.drawImage(map[(tick + y * 12) / 6 % 4][10 + worldNumber % 4], x * 16, y * 16, null);
                } else if (level[x][y] == TILE_LEVEL && data[x][y] == -2 && tick / 12 % 2 == 0) {
                    g.drawImage(map[3][7], x * 16 + 16, y * 16 - 16, null);
                }
            }
        }
        if (!Mario.large) {
            g.drawImage(map[(tick) / 6 % 2][1], xMario + (int) (xMarioA * alpha), yMario + (int) (yMarioA * alpha) - 6, null);
        } else {
            if (!Mario.fire) {
                g.drawImage(map[(tick) / 6 % 2 + 2][0], xMario + (int) (xMarioA * alpha), yMario + (int) (yMarioA * alpha) - 6 - 16, null);
                g.drawImage(map[(tick) / 6 % 2 + 2][1], xMario + (int) (xMarioA * alpha), yMario + (int) (yMarioA * alpha) - 6, null);
            } else {
                g.drawImage(map[(tick) / 6 % 2 + 4][0], xMario + (int) (xMarioA * alpha), yMario + (int) (yMarioA * alpha) - 6 - 16, null);
                g.drawImage(map[(tick) / 6 % 2 + 4][1], xMario + (int) (xMarioA * alpha), yMario + (int) (yMarioA * alpha) - 6, null);
            }
        }

        drawStringDropShadow(g, "MARIO " + df.format(Mario.lives), 0, 0, 7);

        drawStringDropShadow(g, "WORLD " + (worldNumber + 1), 32, 0, 7);
    }


    private static void drawStringDropShadow(Graphics g, String text, int x, int y, int c) {
        drawString(g, text, x * 8 + 5, y * 8 + 5, 0);
        drawString(g, text, x * 8 + 4, y * 8 + 4, c);
    }

    private static void drawString(Graphics g, String text, int x, int y, int c) {
        var ch = text.toCharArray();
        for (var i = 0; i < ch.length; i++) {
            g.drawImage(Art.font[ch[i] - 32][c], x + i * 8, y, null);
        }
    }

    private boolean isRoad(int x, int y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (level[x][y] == TILE_ROAD) return true;
        return level[x][y] == TILE_LEVEL;
    }

    private boolean isWater(int x, int y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        for (var xx = 0; xx < 2; xx++) {
            for (var yy = 0; yy < 2; yy++) {
                if (level[(x + xx) / 2][(y + yy) / 2] != TILE_WATER) return false;
            }
        }

        return true;
    }

    private boolean canEnterLevel;

    @Override
    public void tick() {
        xMario += xMarioA;
        yMario += yMarioA;
        tick++;
        var x = xMario / 16;
        var y = yMario / 16;
        if (level[x][y] == TILE_ROAD) {
            data[x][y] = 0;
        }

        if (moveTime > 0) {
            moveTime--;
        } else {
            xMarioA = 0;
            yMarioA = 0;
            if (canEnterLevel && keys[Mario.KEY_JUMP]) {
                if (level[x][y] == TILE_LEVEL && data[x][y] == -11) {
                } else {
                    if (level[x][y] == TILE_LEVEL && data[x][y] != 0 && data[x][y] > -10) {
                        Mario.levelString = (worldNumber + 1) + "-";
                        var difficulty = worldNumber + 1;
                        var type = LevelGenerator.TYPE_OVERGROUND;
                        if (data[x][y] > 1 && new Random(seed + x * 313211 + y * 534321).nextInt(3) == 0) {
                            type = LevelGenerator.TYPE_UNDERGROUND;
                        }
                        if (data[x][y] < 0) {
                            switch (data[x][y]) {
                                case -2:
                                    Mario.levelString += "X";
                                    difficulty += 2;
                                    break;
                                case -1:
                                    Mario.levelString += "?";
                                    break;
                                default:
                                    Mario.levelString += "#";
                                    difficulty += 1;
                                    break;
                            }

                            type = LevelGenerator.TYPE_CASTLE;
                        } else {
                            Mario.levelString += data[x][y];
                        }

                        Art.stopMusic();
                        marioComponent.startLevel(seed * x * y + x * 31871 + y * 21871, difficulty, type);
                    }
                }
            }
            canEnterLevel = !keys[Mario.KEY_JUMP];

            if (keys[Mario.KEY_LEFT]) {
                keys[Mario.KEY_LEFT] = false;
                tryWalking(-1, 0);
            }
            if (keys[Mario.KEY_RIGHT]) {
                keys[Mario.KEY_RIGHT] = false;
                tryWalking(1, 0);
            }
            if (keys[Mario.KEY_UP]) {
                keys[Mario.KEY_UP] = false;
                tryWalking(0, -1);
            }
            if (keys[Mario.KEY_DOWN]) {
                keys[Mario.KEY_DOWN] = false;
                tryWalking(0, 1);
            }
        }
    }

    public void tryWalking(int xd, int yd) {
        var x = xMario / 16;
        var y = yMario / 16;
        var xt = xMario / 16 + xd;
        var yt = yMario / 16 + yd;

        if (level[xt][yt] == TILE_ROAD || level[xt][yt] == TILE_LEVEL) {
            if (level[xt][yt] == TILE_ROAD) {
                if ((data[xt][yt] != 0) && (data[x][y] != 0 && data[x][y] > -10)) return;
            }
            xMarioA = xd * 8;
            yMarioA = yd * 8;
            moveTime = calcDistance(x, y, xd, yd) * 2 + 1;
        }
    }

    private int calcDistance(int x, int y, int xa, int ya) {
        var distance = 0;
        while (true) {
            x += xa;
            y += ya;
            if (level[x][y] != TILE_ROAD) return distance;
            if (level[x - ya][y + xa] == TILE_ROAD) return distance;
            if (level[x + ya][y - xa] == TILE_ROAD) return distance;
            distance++;
        }
    }

    @Override
    public float getX(float alpha) {
        return 160;
    }

    @Override
    public float getY(float alpha) {
        return 120;
    }

    public void levelWon() {
        var x = xMario / 16;
        var y = yMario / 16;
        if (data[x][y] == -2) {
            nextWorld();
            return;
        }
        if (data[x][y] != -3) data[x][y] = 0;
        else data[x][y] = -10;
        renderStatic(staticGr);
    }
}