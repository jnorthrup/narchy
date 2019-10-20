package nars.experiment.mario.level;

import java.util.Random;


public class BgLevelGenerator {
    private static final Random levelSeedRandom = new Random();

    public static Level createLevel(int width, int height, boolean distant, int type) {
        var levelGenerator = new BgLevelGenerator(width, height, distant, type);
        return levelGenerator.createLevel(levelSeedRandom.nextLong());
    }

    private final int width;
    private final int height;
    private final boolean distant;
    private final int type;

    private BgLevelGenerator(int width, int height, boolean distant, int type) {
        this.width = width;
        this.height = height;
        this.distant = distant;
        this.type = type;
    }

    private Level createLevel(long seed) {
        var level = new Level(width, height);
        var random = new Random(seed);

        switch (type) {
            case LevelGenerator.TYPE_OVERGROUND:

                var range = distant ? 4 : 6;
                var offs = distant ? 2 : 1;
                var oh = random.nextInt(range) + offs;
                var h = random.nextInt(range) + offs;
                for (var x = 0; x < width; x++) {
                    oh = h;
                    while (oh == h) {
                        h = random.nextInt(range) + offs;
                    }
                    for (var y = 0; y < height; y++) {
                        var h0 = (oh < h) ? oh : h;
                        var h1 = (oh < h) ? h : oh;
                        if (y < h0) {
                            if (distant) {
                                var s = 2;
                                if (y < 2) s = y;
                                level.setBlock(x, y, (byte) (4 + s * 8));
                            } else {
                                level.setBlock(x, y, (byte) 5);
                            }
                        } else if (y == h0) {
                            var s = h0 == h ? 0 : 1;
                            s += distant ? 2 : 0;
                            level.setBlock(x, y, (byte) s);
                        } else if (y == h1) {
                            var s = h0 == h ? 0 : 1;
                            s += distant ? 2 : 0;
                            level.setBlock(x, y, (byte) (s + 16));
                        } else {
                            var s = y > h1 ? 1 : 0;
                            if (h0 == oh) s = 1 - s;
                            s += distant ? 2 : 0;
                            level.setBlock(x, y, (byte) (s + 8));
                        }
                    }
                }
                break;
            case LevelGenerator.TYPE_UNDERGROUND:
                if (distant) {
                    var tt = 0;
                    for (var x = 0; x < width; x++) {
                        if (random.nextDouble() < 0.75) tt = 1 - tt;
                        for (var y = 0; y < height; y++) {
                            var t = tt;
                            var yy = y - 2;
                            if (yy < 0 || yy > 4) {
                                yy = 2;
                                t = 0;
                            }
                            level.setBlock(x, y, (byte) (4 + t + (3 + yy) * 8));
                        }
                    }
                } else {
                    for (var x = 0; x < width; x++) {
                        for (var y = 0; y < height; y++) {
                            var t = x % 2;
                            var yy = y - 1;
                            if (yy < 0 || yy > 7) {
                                yy = 7;
                                t = 0;
                            }
                            if (t == 0 && yy > 1 && yy < 5) {
                                t = -1;
                                yy = 0;
                            }
                            level.setBlock(x, y, (byte) (6 + t + (yy) * 8));
                        }
                    }
                }
                break;
            case LevelGenerator.TYPE_CASTLE:
                if (distant) {
                    for (var x = 0; x < width; x++) {
                        for (var y = 0; y < height; y++) {
                            var t = x % 2;
                            var yy = y - 1;
                            if (yy > 2 && yy < 5) {
                                yy = 2;
                            } else if (yy >= 5) {
                                yy -= 2;
                            }
                            if (yy < 0) {
                                t = 0;
                                yy = 5;
                            } else if (yy > 4) {
                                t = 1;
                                yy = 5;
                            } else if (t < 1 && yy == 3) {
                                t = 0;
                                yy = 3;
                            } else if (t < 1 && yy > 0 && yy < 3) {
                                t = 0;
                                yy = 2;
                            }
                            level.setBlock(x, y, (byte) (1 + t + (yy + 4) * 8));
                        }
                    }
                } else {
                    for (var x = 0; x < width; x++) {
                        for (var y = 0; y < height; y++) {
                            var t = x % 3;
                            var yy = y - 1;
                            if (yy > 2 && yy < 5) {
                                yy = 2;
                            } else if (yy >= 5) {
                                yy -= 2;
                            }
                            if (yy < 0) {
                                t = 1;
                                yy = 5;
                            } else if (yy > 4) {
                                t = 2;
                                yy = 5;
                            } else if (t < 2 && yy == 4) {
                                t = 2;
                                yy = 4;
                            } else if (t < 2 && yy > 0 && yy < 4) {
                                t = 4;
                                yy = -3;
                            }
                            level.setBlock(x, y, (byte) (1 + t + (yy + 3) * 8));
                        }
                    }
                }
                break;
        }
        return level;
    }
}