package jcog.signal.wave2d;

import jcog.TODO;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * 2D monochrome adapter to BufferedImage
 * TODO cache results in 8-bit copy
 */
public class MonoBufImgBitmap2D implements Bitmap2D {

    protected Supplier<BufferedImage> source;
    protected BufferedImage img;
    private boolean alpha;
    private final int[] tmp4 = new int[4];

    public enum ColorMode {
        R, G, B, RGB
    }

    ColorMode mode = ColorMode.RGB;

    protected transient Raster raster;

    public MonoBufImgBitmap2D() {
        this.source = null;
    }

    public MonoBufImgBitmap2D(Supplier<BufferedImage> source) {
        this.source = source;

    }

    public MonoBufImgBitmap2D mode(ColorMode c) {
        this.mode = c;
        return this;
    }

    @Override
    public float brightness(int xx, int yy, float rFactor, float gFactor, float bFactor) {
        if (raster != null) {


            if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
                throw new TODO();
            } else {
                var sum = rFactor + gFactor + bFactor;
                if (sum < Float.MIN_NORMAL)
                    return 0;

                var rgb = raster.getPixel(xx, yy, tmp4);
                float r, g, b;
                if (alpha) {
                    //HACK handle either ARGB and RGBA intelligently
                    var colormodel = img.getColorModel();
                    r = colormodel.getRed(rgb);
                    g = colormodel.getGreen(rgb);
                    b = colormodel.getBlue(rgb);

                    //r = rgb[1]; g = rgb[2]; b = rgb[3];
                } else {
                    r = rgb[0];
                    g = rgb[1];
                    b = rgb[2];
                }
                return (r * rFactor + g * gFactor + b * bFactor) / (256f * sum);
            }
        }
        return Float.NaN;
    }


    @Override
    public int width() {
        return raster.getWidth();
    }

    @Override
    public int height() {
        return raster.getHeight();
    }


    @Override
    public void updateBitmap() {

        var src = this.source;

        var nextImage = src != null ? src.get() : null;

        this.img = (nextImage != null ? nextImage : Empty);
        raster = this.img.getRaster();
        var cm = img.getColorModel();
        alpha = cm.hasAlpha();
        if (alpha) {
            if (cm.getTransparency() == 3) {
                //RGBA
                //HACK pretend like its RGB since these bytes are first
                alpha = false;
            } //else: cm.getTransparency() == 0 means ARGB
        }
    }

    static final BufferedImage Empty = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

    public MonoBufImgBitmap2D filter(ColorMode c) {
        return new MonoBufImgBitmap2D(() -> img) {
            @Override
            public int width() {
                return MonoBufImgBitmap2D.this.width();
            }

            @Override
            public int height() {
                return MonoBufImgBitmap2D.this.height();
            }

        }.mode(c);
    }

    @Override
    public float brightness(int xx, int yy) {
        if (raster != null) {

            if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
                return raster.getSample(xx, yy, 0)/256f;
            } else {

                /* HACK */
                var rgb = raster.getPixel(xx, yy, new int[4]);
                float v;
                if (alpha) {
                    //ARGB
                    switch (mode) {
                        case R:
                            v = rgb[1];
                            break;
                        case G:
                            v = rgb[2];
                            break;
                        case B:
                            v = rgb[3];
                            break;
                        case RGB:
                            var sum = IntStream.of(1, 2, 3).map(i -> rgb[i]).sum();
                            v = (sum) / 3f;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                } else {
                    switch (mode) {
                        case R:
                            v = rgb[0];
                            break;
                        case G:
                            v = rgb[1];
                            break;
                        case B:
                            v = rgb[2];
                            break;
                        case RGB:
                            var sum = IntStream.of(0, 1, 2).map(i -> rgb[i]).sum();
                            v = (sum) / 3f;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
                return v / 256f;
            }
        }
        return Float.NaN;
    }
}
