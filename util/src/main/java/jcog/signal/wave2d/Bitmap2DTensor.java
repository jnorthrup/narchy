package jcog.signal.wave2d;

import jcog.TODO;
import jcog.signal.Tensor;

/**
 * single color plane, float value intensity 0..1
 */
public abstract class Bitmap2DTensor implements Tensor, Bitmap2D {
    protected int[] shape;

    public Bitmap2DTensor() {
        this(0, 0, 0);
    }

    public Bitmap2DTensor(int w, int h, int bitplanes) {
        resize(w, h, bitplanes);
    }

    protected void resize(int w, int h, int bitplanes) {
        if (shape != null) {
            if (shape.length == 3 && shape[0] == w && shape[1] == h && shape[2] == bitplanes)
                return; //no change
        }
        this.shape = new int[]{w, h, bitplanes};
    }

    @Override
    public int[] shape() {
        return shape;
    }

    public int width() {
        return shape[0];
    }

    public int height() {
        return shape[1];
    }

    @Override
    public float brightness(int xx, int yy) {
        //TODO handle alpha channel correctly
        int planes = shape[2];
        float sum = 0;
        for (int p = 0; p < planes; p++) {
            sum += get(xx, yy, p);
        }
        return sum / planes;
    }

    @Override
    public float getAt(int i) {
//            int w = shape[0];
//            int planes = shape[2];
//
//            int y = linearCell / (w * planes);
//            int x = ((linearCell/planes) - (y * w * planes));
//
//            System.out.println(linearCell + " : " + x + " " + y);
//
//
//           return get(x, y, p)
        throw new TODO();
    }
}
