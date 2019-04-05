package jcog.signal.wave2d;

import org.jetbrains.annotations.Nullable;

/**
 * exposes a buffered image as a camera video source
 * TODO integrate with Tensor API
 */
public class ProxyBitmap2D implements Bitmap2D {

    private Bitmap2D src;
    private int w,h;

    public ProxyBitmap2D() {
        set(null);
    }

    public ProxyBitmap2D(Bitmap2D src) {
        set(src);
    }

    public synchronized <P extends ProxyBitmap2D> P set(@Nullable Bitmap2D src) {
        this.src = src;
        updateBitmap();
        return (P) this;
    }


    @Override
    public void updateBitmap() {
        if (src!=null) {
            src.updateBitmap();
            w = src.width();
            h = src.height();
        } else {
            w = h = 0;
        }
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
    public float brightness(int x, int y) {
        return src.brightness(x, y);
    }

}
