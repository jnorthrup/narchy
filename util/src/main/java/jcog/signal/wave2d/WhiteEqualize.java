package jcog.signal.wave2d;

import jcog.Util;
import jcog.pri.ScalarValue;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.function.Supplier;

public class WhiteEqualize implements Bitmap2D, Supplier<BufferedImage> {

    private final Supplier<BufferedImage> src;
    BufferedImage store = null;

    private transient byte[] s = null;
    int W, H;

    public WhiteEqualize(Supplier<BufferedImage> src) {
        this.src = src;
        update(); //initialize
    }

    @Override
    public void update() {
        if (src instanceof Bitmap2D)
            ((Bitmap2D)src).update();

        BufferedImage from = src.get();
        if (from == null) {
            W = H = 0;
            store = null;
            return;
        }


        W = from.getWidth();
        H = from.getHeight();
        if (store == null || store.getWidth()!= W || store.getHeight()!= H)
            store = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);

        s = ((DataBufferByte) store.getRaster().getDataBuffer()).getData();
        int i = 0;

        double brightSum = 0;
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = from.getRGB(x,y);
                float r = Bitmap2D.decode8bRed(rgb);
                float g = Bitmap2D.decode8bGreen(rgb);
                float b = Bitmap2D.decode8bBlue(rgb);
                float bri = (r + g + b)/3f;
                if (bri < min) min = bri;
                if (bri > max) max = bri;
                brightSum += bri;
                s[i++] = (byte) (0xff & Math.round(256 * bri));
            }
        }
        float mean = (float) (brightSum / (W * H));
        float range = max-min;
        if (range < ScalarValue.EPSILON)
            return; //no change, just pass-thru

        //center around the mean value
        if (max - mean < mean - min)
            max = Util.min(1, (mean - min) + mean);
        else
            min = Util.max(0, mean - (max - mean));

        range = max - min;

        for (i = 0; i < s.length; i++) {
            s[i] = (byte) (0xff & (int)(256f * Util.unitize((Byte.toUnsignedInt(s[i])/256f)/range + min)));
        }


    }

    @Override
    public BufferedImage get() {
        return store;
    }

    @Override
    public int width() {
        return W;
    }

    @Override
    public int height() {
        return H;
    }

    @Override
    public float brightness(int xx, int yy) {
        if (s == null)
            return 0.5f;

        return Byte.toUnsignedInt(s[yy * W + xx])/256f;
    }
}
