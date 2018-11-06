package jcog.signal.wave2d;

import jcog.util.IntIntToFloatFunction;

import java.lang.reflect.Array;

public class ArrayBitmap2D implements Bitmap2D {

    private final float[][] b;
    transient private final int[] shapeCached;

    public ArrayBitmap2D(int w, int h) {
        this((float[][]) Array.newInstance(float.class, h, w));
    }

    public ArrayBitmap2D(float[][] x) {
        this.b = x;
        this.shapeCached = Bitmap2D.super.shape();
    }

    @Override
    public int[] shape() {
        return shapeCached;
    }

    @Override
    public int width() {
        return b[0].length;
    }

    @Override
    public int height() {
        return b.length;
    }

    @Override
    public float brightness(int xx, int yy) {
        return b[yy][xx];
    }

    public void set(int x, int y, float v) {
        this.b[y][x] = v;
    }

    public void set(IntIntToFloatFunction set) {
        int W = width();
        int H = height();
        for (int x = 0; x < W; x++)
            for (int y = 0; y < H; y++)
                this.b[y][x] = set.value(x,y);
    }
}
