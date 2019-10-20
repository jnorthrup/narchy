package nars.op;

import jcog.learn.Autoencoder;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave2d.Bitmap2D;

/**
 * Autoencoder dimensional reduction of bitmap input
 */
public class AutoencodedBitmap implements Bitmap2D {

    public static final float LEARNING_RATE = 0.05f;
    public static final float NOISE_LEVEL = 0.005f;
    final Bitmap2D source;
    private final float[] output;

    public final float[] input;

    final Autoencoder ae;
    private final int sx;
    private final int sy;
    private final int w;
    private final int h;

    public AutoencodedBitmap(Bitmap2D b, int sx, int sy, int ox, int oy) {
        this.source = b;

        var w = b.width();
        var h = b.height();
        this.sx = sx;
        this.sy = sy;


        var i = sx * sy;
        this.input = new float[i];
        this.ae = new Autoencoder(i, ox * oy, new XoRoShiRo128PlusRandom(1));

        this.w = w / sx * ox;
        this.h = h / sy * oy;
        this.output = new float[this.w * this.h];
    }

    @Override
    public void updateBitmap() {
        source.updateBitmap();


        var w = source.width();
        var h = source.height();

        var k = 0;
        for (var Y = 0; Y < h; Y+=sy) {
            for (var X = 0; X < w; X+=sx) {

                var j = 0;
                for (var y = 0; y < sy; y++) {
                    for (var x = 0; x < sx; x++) {
                        var b = source.brightness(X + x, Y + y);
                        if (b!=b)
                            b = 0.5f;
                        input[j++] = b;
                    }
                }
                assert(j==input.length);

                ae.put(input, LEARNING_RATE, NOISE_LEVEL, 0, true);

                var o = ae.output();
                for (var anO : o) {
                    output[k++] = anO;
                }
            }
        }
        assert(k == output.length);

    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public float brightness(int xx, int yy) {
        return output[yy * w + xx];
    }



}
