package jcog.signal.wave2d;

public class ImageFlip extends ProxyBitmap2D {
    final boolean flipX;
    final boolean flipY;

    public ImageFlip(boolean flipX, boolean flipY, Bitmap2D src) {
        super(src);
        this.flipX = flipX;
        this.flipY = flipY;
    }

    @Override
    public float brightness(int x, int y) {
        if (flipX) x = (width()-1)-x;
        if (flipY) y = (height()-1)-y;
        return super.brightness(x, y);
    }

}
