package nars.experiment.minicraft.top.level.levelgen;

import nars.experiment.minicraft.top.level.tile.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class LevelGen {
    private static final Random random = new Random();
    public double[] values;
    private final int w;
    private final int h;

    public LevelGen(int w, int h, int featureSize) {
        this.w = w;
        this.h = h;

        values = new double[w * h];

        for (var y = 0; y < w; y += featureSize) {
            for (var x = 0; x < w; x += featureSize) {
                setSample(x, y, random.nextFloat() * 2 - 1);
            }
        }

        var stepSize = featureSize;
        var scale = 1.0 / w;
        double scaleMod = 1;
        do {
            var halfStep = stepSize / 2;
            for (var y = 0; y < w; y += stepSize) {
                for (var x = 0; x < w; x += stepSize) {
                    var a = sample(x, y);
                    var b = sample(x + stepSize, y);
                    var c = sample(x, y + stepSize);
                    var d = sample(x + stepSize, y + stepSize);

                    var e = (a + b + c + d) / 4.0 + (random.nextFloat() * 2 - 1) * stepSize * scale;
                    setSample(x + halfStep, y + halfStep, e);
                }
            }
            for (var y = 0; y < w; y += stepSize) {
                for (var x = 0; x < w; x += stepSize) {
                    var a = sample(x, y);
                    var b = sample(x + stepSize, y);
                    var c = sample(x, y + stepSize);
                    var d = sample(x + halfStep, y + halfStep);
                    var e = sample(x + halfStep, y - halfStep);
                    var f = sample(x - halfStep, y + halfStep);

                    var H = (a + b + d + e) / 4.0 + (random.nextFloat() * 2 - 1) * stepSize * scale * 0.5;
                    var g = (a + c + d + f) / 4.0 + (random.nextFloat() * 2 - 1) * stepSize * scale * 0.5;
                    setSample(x + halfStep, y, H);
                    setSample(x, y + halfStep, g);
                }
            }
            stepSize /= 2;
            scale *= (scaleMod + 0.8);
            scaleMod *= 0.3;
        } while (stepSize > 1);
    }

    private double sample(int x, int y) {
        return values[(x & (w - 1)) + (y & (h - 1)) * w];
    }

    private void setSample(int x, int y, double value) {
        values[(x & (w - 1)) + (y & (h - 1)) * w] = value;
    }

    public static byte[][] createAndValidateTopMap(int w, int h) {
        do {
            var result = createTopMap(w, h);

            var count = new int[256];

            for (var i = 0; i < w * h; i++) {
                count[result[0][i] & 0xff]++;
            }
            if (count[Tile.rock.id & 0xff] < 100) continue;
            if (count[Tile.sand.id & 0xff] < 100) continue;
            if (count[Tile.grass.id & 0xff] < 100) continue;
            if (count[Tile.tree.id & 0xff] < 100) continue;
            if (count[Tile.stairsDown.id & 0xff] < 2) continue;

            return result;

        } while (true);
    }

    public static byte[][] createAndValidateUndergroundMap(int w, int h, int depth) {
        do {
            var result = createUndergroundMap(w, h, depth);

            var count = new int[256];

            for (var i = 0; i < w * h; i++) {
                count[result[0][i] & 0xff]++;
            }
            if (count[Tile.rock.id & 0xff] < 100) continue;
            if (count[Tile.dirt.id & 0xff] < 100) continue;
            if (count[(Tile.ironOre.id & 0xff) + depth - 1] < 20) continue;
            if (depth < 3) if (count[Tile.stairsDown.id & 0xff] < 2) continue;

            return result;

        } while (true);
    }

    public static byte[][] createAndValidateSkyMap(int w, int h) {
        do {
            var result = createSkyMap(w, h);

            var count = new int[256];

            for (var i = 0; i < w * h; i++) {
                count[result[0][i] & 0xff]++;
            }
            if (count[Tile.cloud.id & 0xff] < 2000) continue;
            if (count[Tile.stairsDown.id & 0xff] < 2) continue;

            return result;

        } while (true);
    }

    private static byte[][] createTopMap(int w, int h) {
        var mnoise1 = new LevelGen(w, h, 16);
        var mnoise2 = new LevelGen(w, h, 16);
        var mnoise3 = new LevelGen(w, h, 16);

        var noise1 = new LevelGen(w, h, 32);
        var noise2 = new LevelGen(w, h, 32);

        var map = new byte[w * h];
        for (var y = 0; y < h; y++) {
            for (var x = 0; x < w; x++) {
                var i = x + y * w;

                var val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;
                var mval = Math.abs(mnoise1.values[i] - mnoise2.values[i]);
                mval = Math.abs(mval - mnoise3.values[i]) * 3 - 2;

                var xd = x / (w - 1.0) * 2 - 1;
                var yd = y / (h - 1.0) * 2 - 1;
                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;
                var dist = xd >= yd ? xd : yd;
                dist = dist * dist * dist * dist;
                dist = dist * dist * dist * dist;
                val = val + 1 - dist * 20;

                if (val < -0.5) {
                    map[i] = Tile.water.id;
                } else if (val > 0.5 && mval < -1.5) {
                    map[i] = Tile.rock.id;
                } else {
                    map[i] = Tile.grass.id;
                }
            }
        }

        for (var i = 0; i < w * h / 2800; i++) {
            var xs = random.nextInt(w);
            var ys = random.nextInt(h);
            for (var k = 0; k < 10; k++) {
                var x = xs + random.nextInt(21) - 10;
                var y = ys + random.nextInt(21) - 10;
                for (var j = 0; j < 100; j++) {
                    var xo = x + random.nextInt(5) - random.nextInt(5);
                    var yo = y + random.nextInt(5) - random.nextInt(5);
                    for (var yy = yo - 1; yy <= yo + 1; yy++)
                        for (var xx = xo - 1; xx <= xo + 1; xx++)
                            if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                                if (map[xx + yy * w] == Tile.grass.id) {
                                    map[xx + yy * w] = Tile.sand.id;
                                }
                            }
                }
            }
        }

        /*
         * for (int i = 0; i < w * h / 2800; i++) { int xs = random.nextInt(w); int ys = random.nextInt(h); for (int k = 0; k < 10; k++) { int x = xs + random.nextInt(21) - 10; int y = ys + random.nextInt(21) - 10; for (int j = 0; j < 100; j++) { int xo = x + random.nextInt(5) - random.nextInt(5); int yo = y + random.nextInt(5) - random.nextInt(5); for (int yy = yo - 1; yy <= yo + 1; yy++) for (int xx = xo - 1; xx <= xo + 1; xx++) if (xx >= 0 && yy >= 0 && xx < w && yy < h) { if (map[xx + yy * w] == Tile.grass.id) { map[xx + yy * w] = Tile.dirt.id; } } } } }
         */

        for (var i = 0; i < w * h / 400; i++) {
            var x = random.nextInt(w);
            var y = random.nextInt(h);
            for (var j = 0; j < 200; j++) {
                var xx = x + random.nextInt(15) - random.nextInt(15);
                var yy = y + random.nextInt(15) - random.nextInt(15);
                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tile.grass.id) {
                        map[xx + yy * w] = Tile.tree.id;
                    }
                }
            }
        }

        var data = new byte[w * h];
        for (var i = 0; i < w * h / 400; i++) {
            var x = random.nextInt(w);
            var y = random.nextInt(h);
            var col = random.nextInt(4);
            for (var j = 0; j < 30; j++) {
                var xx = x + random.nextInt(5) - random.nextInt(5);
                var yy = y + random.nextInt(5) - random.nextInt(5);
                if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                    if (map[xx + yy * w] == Tile.grass.id) {
                        map[xx + yy * w] = Tile.flower.id;
                        data[xx + yy * w] = (byte) (col + random.nextInt(4) * 16);
                    }
                }
            }
        }

        for (var i = 0; i < w * h / 100; i++) {
            var xx = random.nextInt(w);
            var yy = random.nextInt(h);
            if (xx >= 0 && yy >= 0 && xx < w && yy < h) {
                if (map[xx + yy * w] == Tile.sand.id) {
                    map[xx + yy * w] = Tile.cactus.id;
                }
            }
        }

        var count = 0;
        stairsLoop:
        for (var i = 0; i < w * h / 100; i++) {
            var x = random.nextInt(w - 2) + 1;
            var y = random.nextInt(h - 2) + 1;

            for (var yy = y - 1; yy <= y + 1; yy++)
                for (var xx = x - 1; xx <= x + 1; xx++) {
                    if (map[xx + yy * w] != Tile.rock.id) continue stairsLoop;
                }

            map[x + y * w] = Tile.stairsDown.id;
            count++;
            if (count == 4) break;
        }

        return new byte[][]{map, data};
    }

    private static byte[][] createUndergroundMap(int w, int h, int depth) {
        var mnoise1 = new LevelGen(w, h, 16);
        var mnoise2 = new LevelGen(w, h, 16);
        var mnoise3 = new LevelGen(w, h, 16);

        var nnoise1 = new LevelGen(w, h, 16);
        var nnoise2 = new LevelGen(w, h, 16);
        var nnoise3 = new LevelGen(w, h, 16);

        var wnoise1 = new LevelGen(w, h, 16);
        var wnoise2 = new LevelGen(w, h, 16);
        var wnoise3 = new LevelGen(w, h, 16);

        var noise1 = new LevelGen(w, h, 32);
        var noise2 = new LevelGen(w, h, 32);

        var map = new byte[w * h];
        for (var y = 0; y < h; y++) {
            for (var x = 0; x < w; x++) {
                var i = x + y * w;

                var val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;

                var mval = Math.abs(mnoise1.values[i] - mnoise2.values[i]);
                mval = Math.abs(mval - mnoise3.values[i]) * 3 - 2;

                var nval = Math.abs(nnoise1.values[i] - nnoise2.values[i]);
                nval = Math.abs(nval - nnoise3.values[i]) * 3 - 2;

                var wval = Math.abs(nval - wnoise3.values[i]) * 3 - 2;

                var xd = x / (w - 1.0) * 2 - 1;
                var yd = y / (h - 1.0) * 2 - 1;
                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;
                var dist = xd >= yd ? xd : yd;
                dist = dist * dist * dist * dist;
                dist = dist * dist * dist * dist;
                val = val + 1 - dist * 20;

                if (val > -2 && wval < -2.0 + (depth) / 2f * 3) {
                    if (depth > 2)
                        map[i] = Tile.lava.id;
                    else
                        map[i] = Tile.water.id;
                } else if (val > -2 && (mval < -1.7 || nval < -1.4)) {
                    map[i] = Tile.dirt.id;
                } else {
                    map[i] = Tile.rock.id;
                }
            }
        }

        var r = 2;
        for (var i = 0; i < w * h / 400; i++) {
            var x = random.nextInt(w);
            var y = random.nextInt(h);
            for (var j = 0; j < 30; j++) {
                var xx = x + random.nextInt(5) - random.nextInt(5);
                var yy = y + random.nextInt(5) - random.nextInt(5);
                if (xx >= r && yy >= r && xx < w - r && yy < h - r) {
                    if (map[xx + yy * w] == Tile.rock.id) {
                        map[xx + yy * w] = (byte) ((Tile.ironOre.id & 0xff) + depth - 1);
                    }
                }
            }
        }

        if (depth < 3) {
            var count = 0;
            stairsLoop:
            for (var i = 0; i < w * h / 100; i++) {
                var x = random.nextInt(w - 20) + 10;
                var y = random.nextInt(h - 20) + 10;

                for (var yy = y - 1; yy <= y + 1; yy++)
                    for (var xx = x - 1; xx <= x + 1; xx++) {
                        if (map[xx + yy * w] != Tile.rock.id) continue stairsLoop;
                    }

                map[x + y * w] = Tile.stairsDown.id;
                count++;
                if (count == 4) break;
            }
        }

        var data = new byte[w * h];
        return new byte[][]{map, data};
    }

    private static byte[][] createSkyMap(int w, int h) {
        var noise1 = new LevelGen(w, h, 8);
        var noise2 = new LevelGen(w, h, 8);

        var map = new byte[w * h];
        for (var y = 0; y < h; y++) {
            for (var x = 0; x < w; x++) {
                var i = x + y * w;

                var val = Math.abs(noise1.values[i] - noise2.values[i]) * 3 - 2;

                var xd = x / (w - 1.0) * 2 - 1;
                var yd = y / (h - 1.0) * 2 - 1;
                if (xd < 0) xd = -xd;
                if (yd < 0) yd = -yd;
                var dist = xd >= yd ? xd : yd;
                dist = dist * dist * dist * dist;
                dist = dist * dist * dist * dist;
                val = -val * 1 - 2.2;
                val = val + 1 - dist * 20;

                if (val < -0.25) {
                    map[i] = Tile.infiniteFall.id;
                } else {
                    map[i] = Tile.cloud.id;
                }
            }
        }

        stairsLoop:
        for (var i = 0; i < w * h / 50; i++) {
            var x = random.nextInt(w - 2) + 1;
            var y = random.nextInt(h - 2) + 1;

            for (var yy = y - 1; yy <= y + 1; yy++)
                for (var xx = x - 1; xx <= x + 1; xx++) {
                    if (map[xx + yy * w] != Tile.cloud.id) continue stairsLoop;
                }

            map[x + y * w] = Tile.cloudCactus.id;
        }

        var count = 0;
        stairsLoop:
        for (var i = 0; i < w * h; i++) {
            var x = random.nextInt(w - 2) + 1;
            var y = random.nextInt(h - 2) + 1;

            for (var yy = y - 1; yy <= y + 1; yy++)
                for (var xx = x - 1; xx <= x + 1; xx++) {
                    if (map[xx + yy * w] != Tile.cloud.id) continue stairsLoop;
                }

            map[x + y * w] = Tile.stairsDown.id;
            count++;
            if (count == 2) break;
        }

        var data = new byte[w * h];
        return new byte[][]{map, data};
    }

    public static void main(String[] args) {
        var d = 0;
        while (true) {
            var w = 128;
            var h = 128;

            var map = LevelGen.createAndValidateTopMap(w, h)[0];


            var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            var pixels = new int[w * h];
            for (var y = 0; y < h; y++) {
                for (var x = 0; x < w; x++) {
                    var i = x + y * w;

                    if (map[i] == Tile.water.id) pixels[i] = 0x000080;
                    if (map[i] == Tile.grass.id) pixels[i] = 0x208020;
                    if (map[i] == Tile.rock.id) pixels[i] = 0xa0a0a0;
                    if (map[i] == Tile.dirt.id) pixels[i] = 0x604040;
                    if (map[i] == Tile.sand.id) pixels[i] = 0xa0a040;
                    if (map[i] == Tile.tree.id) pixels[i] = 0x003000;
                    if (map[i] == Tile.lava.id) pixels[i] = 0xff2020;
                    if (map[i] == Tile.cloud.id) pixels[i] = 0xa0a0a0;
                    if (map[i] == Tile.stairsDown.id) pixels[i] = 0xffffff;
                    if (map[i] == Tile.stairsUp.id) pixels[i] = 0xffffff;
                    if (map[i] == Tile.cloudCactus.id) pixels[i] = 0xff00ff;
                }
            }
            img.setRGB(0, 0, w, h, pixels, 0, w);
            JOptionPane.showMessageDialog(null, null, "Another", JOptionPane.YES_NO_OPTION, new ImageIcon(img.getScaledInstance(w * 4, h * 4, Image.SCALE_AREA_AVERAGING)));
        }
    }
}