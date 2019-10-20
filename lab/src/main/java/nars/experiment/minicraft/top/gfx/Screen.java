package nars.experiment.minicraft.top.gfx;

import java.util.Arrays;

public class Screen {
    /*
     * public static final int MAP_WIDTH = 64;
     *
     * public int[] tiles = new int[MAP_WIDTH * MAP_WIDTH]; public int[] colors = new int[MAP_WIDTH * MAP_WIDTH]; public int[] databits = new int[MAP_WIDTH * MAP_WIDTH];
     */
    public int xOffset;
    public int yOffset;

    public static final int BIT_MIRROR_X = 0x01;
    public static final int BIT_MIRROR_Y = 0x02;

    public final int w;
    public final int h;
    public int[] pixels;

    private final SpriteSheet sheet;

    public Screen(int w, int h, SpriteSheet sheet) {
        this.sheet = sheet;
        this.w = w;
        this.h = h;

        pixels = new int[w * h];



        /*
         * for (int i = 0; i < MAP_WIDTH * MAP_WIDTH; i++) { colors[i] = Color.get(00, 40, 50, 40); tiles[i] = 0;
         *
         * if (random.nextInt(40) == 0) { tiles[i] = 32; colors[i] = Color.get(111, 40, 222, 333); databits[i] = random.nextInt(2); } else if (random.nextInt(40) == 0) { tiles[i] = 33; colors[i] = Color.get(20, 40, 30, 550); } else { tiles[i] = random.nextInt(4); databits[i] = random.nextInt(4);
         *
         * } }
         *
         * Font.setMap("Testing the 0341879123", this, 0, 0, Color.get(0, 555, 555, 555));
         */
    }

    public void clear(int color) {
        Arrays.fill(pixels, color);
    }

    /*
     * public void renderBackground() { for (int yt = yScroll >> 3; yt <= (yScroll + h) >> 3; yt++) { int yp = yt * 8 - yScroll; for (int xt = xScroll >> 3; xt <= (xScroll + w) >> 3; xt++) { int xp = xt * 8 - xScroll; int ti = (xt & (MAP_WIDTH_MASK)) + (yt & (MAP_WIDTH_MASK)) * MAP_WIDTH; render(xp, yp, tiles[ti], colors[ti], databits[ti]); } }
     *
     * for (int i = 0; i < sprites.size(); i++) { Sprite s = sprites.get(i); render(s.x, s.y, s.img, s.col, s.bits); } sprites.clear(); }
     */

    public void render(int _xp, int _yp, int tile, int colors, int bits) {
        var xp = _xp - xOffset;
        var yp = _yp - yOffset;
        var mirrorX = (bits & BIT_MIRROR_X) > 0;
        var mirrorY = (bits & BIT_MIRROR_Y) > 0;

        var xTile = tile % 32;
        var yTile = tile / 32;
        var sw = sheet.width;
        var toffs = xTile * 8 + yTile * 8 * sw;
        var sp = sheet.pixels;
        var pp = this.pixels;

        for (var y = 0; y < 8; y++) {
            var ys = y;
            if (mirrorY) ys = 7 - y;
            var yyp = y + yp;
            if (yyp < 0 || yyp >= h) continue;
            for (var x = 0; x < 8; x++) {
                var xxp = x + xp;
                if (xxp < 0 || xxp >= w) continue;

                var xs = x;
                if (mirrorX) xs = 7 - x;
                var col = (colors >> (sp[xs + ys * sw + toffs] * 8)) & 255;
                if (col < 255) {
                    pp[xxp + (yyp) * w] = col;
                }
            }
        }
    }

    public void setOffset(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    private static final int[] dither = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    public void overlay(Screen screen2, int xa, int ya) {
        var oPixels = screen2.pixels;
        var i = 0;
        for (var y = 0; y < h; y++) {
            var yy = ((y + ya) & 3) * 4;
            for (var x = 0; x < w; x++) {
                if (oPixels[i] / 10 <= dither[((x + xa) & 3) + yy]) pixels[i] = 0;
                i++;
            }

        }
    }

    public void renderLight(int x, int y, int r) {
        x -= xOffset;
        y -= yOffset;
        var x0 = x - r;
        var x1 = x + r;
        var y0 = y - r;
        var y1 = y + r;
        if (x0 < 0) x0 = 0;
        if (y0 < 0) y0 = 0;
        if (x1 > w) x1 = w;
        if (y1 > h) y1 = h;

        var rr = r * r;
        for (var yy = y0; yy < y1; yy++) {
            var yd = yy - y;
            yd *= yd;
            for (var xx = x0; xx < x1; xx++) {
                var xd = xx - x;
                var dist = xd * xd + yd;


                if (dist <= rr) {
                    var br = 255 - dist * 255f / (rr);
                    var pi = xx + yy * w;
                    if (pixels[pi] < br)
                        pixels[pi] = Math.round(br);
                }
            }
        }
    }
}