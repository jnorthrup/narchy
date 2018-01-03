package nars.op.video;

import jcog.Util;
import jcog.signal.Bitmap2D;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import static jcog.signal.Bitmap2D.*;

/**
 * exposes a buffered image as a camera video source
 */
public class BufferedImageBitmap2D implements Bitmap2D, Supplier<BufferedImage> {

    public BufferedImageBitmap2D() {

    }

    public BufferedImageBitmap2D(Supplier<BufferedImage> delegate) {
        this.source = delegate;
        this.out = source.get();
    }

    public enum ColorMode {
        R, G, B, RGB
    }

    ColorMode mode = ColorMode.RGB;
    Supplier<BufferedImage> source;
    public BufferedImage out;

    public BufferedImageBitmap2D filter(ColorMode c) {
        return new BufferedImageBitmap2D(this){
            @Override
            public int width() {
                return BufferedImageBitmap2D.this.width(); //HACK
            }
            @Override
            public int height() {
                return BufferedImageBitmap2D.this.height(); //HACK
            }
        }.mode(c);
    }

    //HACK TODO use better filter
    public BufferedImageBitmap2D blur() {
        return new BufferedImageBitmap2D(this){
            @Override
            public int width() {
                return BufferedImageBitmap2D.this.width(); //HACK
            }
            @Override
            public int height() {
                return BufferedImageBitmap2D.this.height(); //HACK
            }

            @Override
            public float brightness(int xx, int yy) {
                float c = super.brightness(xx, yy);
                float up = yy > 0 ? super.brightness(xx, yy-1) : c;
                float left = xx > 0 ? super.brightness(xx-1, yy) : c;
                float down = yy < height()-1 ? super.brightness(xx, yy+1) : c;
                float right = xx < width()-1 ? super.brightness(xx+1, yy) : c;
                return (c*8 + up + left + down + right)/12;
            }
        };
    }

    public BufferedImageBitmap2D mode(ColorMode c) {
        this.mode = c;
        return this;
    }

    @Override
    public int width() {
        return out.getWidth();
    }

    @Override
    public int height() {
        return out.getHeight();
    }


    @Override
    public void update(float frameRate) {
        if (this.source!=null) //get next frame
            out = source.get();
    }

//    public void see(EachPixelRGB p) {
//        final BufferedImage b = this.out;
//        if (b == null)
//            return;
//
//        int height = height();
//        int width = width();
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                p.pixel(x, y, b.getRGB(x, y));
//            }
//        }
//    }


    @Override
    public float brightness(int xx, int yy, float rFactor, float gFactor, float bFactor) {
        if (out!=null) {
            rFactor = Util.unitize(rFactor);
            gFactor = Util.unitize(gFactor);
            bFactor = Util.unitize(bFactor);
            float sum = rFactor + gFactor + bFactor;
            if (sum == 0)
                return 0;

            int rgb = out.getRGB(xx, yy);
            float r = rFactor > 0 ? rFactor * decodeRed(rgb) : 0;
            float g = gFactor > 0 ? gFactor * decodeGreen(rgb) : 0;
            float b = bFactor > 0 ? bFactor * decodeBlue(rgb) : 0;
            return (r + g + b) / (sum);
        }
        return Float.NaN;
    }

    @Override public float brightness(int xx, int yy) {
        if (out!=null) {
            int rgb = out.getRGB(xx, yy);
            switch (mode) {
                case R: return decodeRed(rgb);
                case G: return decodeGreen(rgb);
                case B: return decodeBlue(rgb);
                case RGB:
                    return (decodeRed(rgb) + decodeGreen(rgb) + decodeBlue(rgb)) / 3f;
            }
        }
        return Float.NaN;
    }

//    public void updateBuffered(EachPixelRGBf m) {
//        see(
//                (x, y, p) -> {
//                    intToFloat(m, x, y, p);
//                }
//        );
//    }

    public float red(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeRed(out.getRGB(x, y));
    }
    public float green(int x, int y) {
        return outsideBuffer(x, y) ? Float.NaN : decodeGreen(out.getRGB(x, y));
    }
    public float blue(int x, int y) { return outsideBuffer(x, y) ? Float.NaN : decodeBlue(out.getRGB(x,y)); }

    public boolean outsideBuffer(int x, int y) {
        return out == null || (x < 0) || (y < 0) || (x >= out.getWidth()) || (y >= out.getHeight());
    }

    /** for chaining these together */
    @Override public BufferedImage get() {
        update(1);
        return out;
    }
}
