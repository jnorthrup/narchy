package nars.video;

import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.factory.transform.wavelet.GFactoryWavelet;
import boofcv.struct.image.GrayF32;
import boofcv.struct.wavelet.WlCoef_F32;
import jcog.signal.wave2d.Bitmap2D;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.function.Supplier;

/**
 * 2D wavelet transform of input image
 * https:
 */
public class WaveletBag implements Bitmap2D {

    public static final int NUM_LEVELS = 2;
    final Supplier<BufferedImage> source;
    private final int px;
    private final int py;


    GrayF32 image;

    public final float[][] pixels;

    WaveletTransform<GrayF32,GrayF32,WlCoef_F32> waveletTran =
            FactoryWaveletTransform.create(GrayF32.class, GFactoryWavelet.haar(GrayF32.class), NUM_LEVELS,0,255);
    
    private GrayF32 imageWavelet;


    public WaveletBag(BufferedImage b, int px, int py) {
        this(()->b, px, py);
    }

    public WaveletBag(Supplier<BufferedImage> b, int px, int py) {
        this.source = b;

        this.pixels = new float[px][py];
        this.px = px;
        this.py = py;
    }

    @Override
    public void update() {
        
        BufferedImage src = source.get();
        if (image == null || image.getWidth()!=src.getWidth() || image.getHeight()!=src.getHeight())
            image = new GrayF32(src.getWidth(), src.getHeight());
        bufferedToGray(src, image.data, image.startIndex, image.stride);

        imageWavelet = waveletTran.transform(image, imageWavelet);
        UtilWavelet.adjustForDisplay(imageWavelet, waveletTran.getLevels(), 1f);

        


    }

    public static void bufferedToGray(BufferedImage src, float[] data, int dstStartIndex, int dstStride) {
        int width = src.getWidth();
        int height = src.getHeight();
        int x;
        int argb;
        int r;
        if(src.getType() == 10) {
            WritableRaster y = src.getRaster();
            float[] index = new float[1];

            for(x = 0; x < height; ++x) {
                argb = dstStartIndex + x * dstStride;

                for(r = 0; r < width; ++r) {
                    y.getPixel(r, x, index);
                    data[argb++] = index[0];
                }
            }
        } else {
            for(int var14 = 0; var14 < height; ++var14) {
                int var15 = dstStartIndex + var14 * dstStride;

                for(x = 0; x < width; ++x) {
                    argb = src.getRGB(x, var14);
                    r = argb >>> 16 & 255;
                    int g = argb >>> 8 & 255;
                    int b = argb & 255;
                    float ave = (float)(r + g + b) / 3.0F;
                    data[var15++] = ave;
                }
            }
        }

    }

    @Override
    public int width() {
        return px;
    }

    @Override
    public int height() {
        return py;
    }

    @Override
    public float brightness(int xx, int yy) {
        return imageWavelet!=null ? imageWavelet.get(xx,yy) : 0.5f;
    }



}
