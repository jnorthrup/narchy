package jcog.signal.wave2d;

import jcog.Util;
import jcog.signal.Tensor;

public class RGBToGrayBitmap2DTensor extends Bitmap2DTensor {
    private final Tensor rgbMix;
    private volatile RGBBitmap2DTensor src;

    public RGBToGrayBitmap2DTensor(Tensor rgbMix) {
        this.rgbMix = rgbMix;
    }

    public RGBToGrayBitmap2DTensor(RGBBitmap2DTensor src, Tensor rgbMix) {
        this(rgbMix);
        update(src);
    }

    public void update(RGBBitmap2DTensor src) {
        this.src = src;
        resize(src.width(), src.height(), 1);
    }

    public float get(int x, int y) {

        float R = rgbMix.getAt(0);
        float G = rgbMix.getAt(1);
        float B = rgbMix.getAt(2);
        float RGB = Math.abs(R) + Math.abs(G) + Math.abs(B);
        if (RGB < Float.MIN_NORMAL)
            return 0f;

        float r = src.get(x, y, 0);
        float g = src.get(x, y, 1);
        float b = src.get(x, y, 2);
        return Util.unitize((r * R + g * G + b * B) / RGB);
    }

    @Override
    public float get(int... cell) {
        int x = cell[0];
        int y = cell[1];
        return get(x, y);
    }

}
