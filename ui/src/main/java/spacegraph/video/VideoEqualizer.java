package spacegraph.video;

import jcog.Util;
import jcog.math.FloatRange;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class VideoEqualizer extends VideoTransform {

    public VideoEqualizer(VideoSource in) {
        super(in);
    }

    private BufferedImage out = null;

    private final int[] histogram = new int[256];
    /** cumulative histogram */
    private final int[] chistogram = new int[256];
    private final float[] arr = new float[256];

    public final FloatRange momentum = new FloatRange(0.99f, (float) 0, 1f);

    /**
     * https://www.codeproject.com/Tips/1172662/Histogram-Equalisation-in-Java
     */
    private BufferedImage equalize(BufferedImage src) {
        WritableRaster in = src.getRaster();
        int W = in.getWidth();
        int H = in.getHeight();
        int bands = src.getColorModel().getNumColorComponents();

        Arrays.fill(histogram, 0);

        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                histogram[sample(in, x, y, bands)]++;
            }
        }

        float momentum = this.momentum.floatValue();
        int totpix = W * H;
        for (int i = 0; i < 256; i++) {
            int c = (i > 0 ? chistogram[i - 1] : 0) + histogram[i];
            chistogram[i] = c;
            arr[i] = Util.lerp(momentum, (((float) chistogram[i] * 255f) / (float) totpix), arr[i]);
        }

        if (out == null || out.getWidth() != W || out.getHeight()!=H) {
            out = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
        }

        WritableRaster o = out.getRaster();
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int nVal = (int) arr[sample(in, x, y, bands)];
                o.setSample(x, y, 0, nVal);
            }
        }

        //nImg.setData(out);
        return out;
    }

    @Override
    public BufferedImage apply(BufferedImage f) {
        return equalize(f);
    }

    static int sample(WritableRaster in, int x, int y, int bands) {
        if (bands == 0)
            return in.getSample(x, y, 0);
        else {
            int total = 0;
            for (int i = 0; i < bands; i++) {
                int sample = in.getSample(x, y, i);
                total += sample;
            }
            return (total) / bands;
        }
    }

}
