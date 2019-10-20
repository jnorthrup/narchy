package jcog.signal.wave2d;

import jcog.Util;
import jcog.signal.Tensor;

public class RGBToMonoBitmap2D extends PlanarBitmap2D {
    private final Tensor rgbMix;
    private volatile RGBBufImgBitmap2D src;

    public RGBToMonoBitmap2D(Tensor rgbMix) {
        this.rgbMix = rgbMix;
    }

    public RGBToMonoBitmap2D(RGBBufImgBitmap2D src, Tensor rgbMix) {
        this(rgbMix);
        update(src);
    }

    public void update(RGBBufImgBitmap2D src) {
        this.src = src;
        updateBitmap();
        //resize(src.width(), src.height(), 1); //HACK FIX
    }

    public float get(int x, int y) {

        var R = rgbMix.getAt(0);
        var G = rgbMix.getAt(1);
        var B = rgbMix.getAt(2);
        var RGB = Math.abs(R) + Math.abs(G) + Math.abs(B);
        if (RGB < Float.MIN_NORMAL)
            return 0f;

        var r = src.get(x, y, 0);
        var g = src.get(x, y, 1);
        var b = src.get(x, y, 2);
        return Util.unitize((r * R + g * G + b * B) / RGB);
    }

    @Override
    public float get(int... cell) {
        var x = cell[0];
        var y = cell[1];
        return get(x, y);
    }

}
