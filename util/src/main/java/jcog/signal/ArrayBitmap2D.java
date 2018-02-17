package jcog.signal;

public class ArrayBitmap2D implements Bitmap2D {

    private final float[][] x;

    public ArrayBitmap2D(float[][] x) {
        this.x = x;
    }

    @Override
    public int width() {
        return x[0].length;
    }

    @Override
    public int height() {
        return x.length;
    }

    @Override
    public float brightness(int xx, int yy) {
        return x[xx][yy];
    }
}
