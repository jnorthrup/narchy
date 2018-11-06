package nars.video;

import jcog.Util;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.BufferedBitmap2D;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public class BufferedImageBitmap2D implements BufferedBitmap2D, Supplier<BufferedImage> {
    public enum ColorMode {
        R, G, B, RGB
    }

    ColorMode mode = ColorMode.RGB;

    Supplier<BufferedImage> source;
    public BufferedImage sourceImage;

    public BufferedImageBitmap2D() {
        this.source = null;
    }

    public BufferedImageBitmap2D(Supplier<BufferedImage> source) {
        this.source = source;
        update();
    }

    public BufferedImageBitmap2D mode(ColorMode c) {
        this.mode = c;
        return this;
    }

    @Override
    public float brightness(int xx, int yy, float rFactor, float gFactor, float bFactor) {
        if (sourceImage !=null) {
            rFactor = Util.unitize(rFactor);
            gFactor = Util.unitize(gFactor);
            bFactor = Util.unitize(bFactor);
            float sum = rFactor + gFactor + bFactor;
            if (sum == 0)
                return 0;

            int rgb = sourceImage.getRGB(xx, yy);
            float r = rFactor > 0 ? rFactor * Bitmap2D.decode8bRed(rgb) : 0;
            float g = gFactor > 0 ? gFactor * Bitmap2D.decode8bGreen(rgb) : 0;
            float b = bFactor > 0 ? bFactor * Bitmap2D.decode8bBlue(rgb) : 0;
            return (r + g + b) / (sum);
        }
        return Float.NaN;
    }











    public float red(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : Bitmap2D.decode8bRed(sourceImage.getRGB(x, y));
    }
    public float green(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : Bitmap2D.decode8bGreen(sourceImage.getRGB(x, y));
    }
    public float blue(int x, int y) { return outsideBuffer(x, y) ? Float.NaN : Bitmap2D.decode8bBlue(sourceImage.getRGB(x,y)); }

    public boolean outsideBuffer(int x, int y) {
        return sourceImage == null || (x < 0) || (y < 0) || (x >= sourceImage.getWidth()) || (y >= sourceImage.getHeight());
    }

    /** for chaining these together */
    @Override public BufferedImage get() {
        update();
        return sourceImage;
    }


    @Override
    public int width() {
        return sourceImage.getWidth();
    }

    @Override
    public int height() {
        return sourceImage.getHeight();
    }


    @Override
    public void update() {
        if (this.source!=null)
            sourceImage = source.get();
    }

    public BufferedImageBitmap2D filter(ColorMode c) {
        return new BufferedImageBitmap2D(this){
            @Override
            public int width() {
                return BufferedImageBitmap2D.this.width();
            }
            @Override
            public int height() {
                return BufferedImageBitmap2D.this.height();
            }
        }.mode(c);
    }

    @Override public float brightness(int xx, int yy) {
        if (sourceImage !=null) {
            int rgb = sourceImage.getRGB(xx, yy);
            switch (mode) {
                case R: return Bitmap2D.decode8bRed(rgb);
                case G: return Bitmap2D.decode8bGreen(rgb);
                case B: return Bitmap2D.decode8bBlue(rgb);
                case RGB:
                    return (Bitmap2D.decode8bRed(rgb) + Bitmap2D.decode8bGreen(rgb) + Bitmap2D.decode8bBlue(rgb)) / 3f;
            }
        }
        return Float.NaN;
    }
}
