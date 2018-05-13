package nars.video;


import jcog.signal.Bitmap2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.function.Supplier;

/**
 * Pan/Zoom filter for a BuferredImage source
 */
public class Scale extends BufferedImageBitmap2D implements ImageObserver {

    private final Supplier<BufferedImage> src;

    float sx1 = 0;
    float sy1 = 0;
    float sx2 = 1;
    float sy2 = 1;

    /**
     * output pixel width / height
     */
    public int pw, ph;
    private Graphics2D outgfx;

    public Scale(Supplier<BufferedImage> source, int pw, int ph) {
        super();

        this.src = source;
        this.pw = pw;
        this.ph = ph;
    }

    public Scale window(float sx1, float sy1, float sx2, float sy2) {
        this.sx1 = sx1; this.sy1 = sy1; this.sx2 = sx2; this.sy2 = sy2;
        return this;
    }

    @Override
    public int width() {
        return pw;
    }

    @Override
    public int height() {
        return ph;
    }

    @Override
    public void update() {
        if (src instanceof Bitmap2D)
            ((Bitmap2D) src).update();

        Image in = src.get();
        if (in == null)
            return;

        if (out == null || outgfx == null || out.getWidth() != pw || out.getHeight() != ph) {

            if (outgfx!=null)
                outgfx.dispose();

            out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
            outgfx = out.createGraphics(); //create a graphics object to paint to
//            outgfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
//                    //RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
//            );
//            outgfx.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
//                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
//                    //RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
//            );
//            outgfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                    RenderingHints.VALUE_ANTIALIAS_ON
//                    //RenderingHints.VALUE_ANTIALIAS_OFF
//            );
        }




        outgfx.setColor(Color.BLACK);
        outgfx.fillRect(0, 0, pw, ph); //TODO add fade option
//        boolean stillChanging =
//                outgfx.drawImage(in, 0, 0, pw, ph, this); //draw the image scaled

        /*
            public abstract boolean drawImage(Image img,
                                      int dx1, int dy1, int dx2, int dy2,
                                      int sx1, int sy1, int sx2, int sy2,
                                      Color bgcolor,
                                      ImageObserver observer);
         */
        int sw = in.getWidth(null);
        int sh = in.getHeight(null);
        boolean stillChanging = outgfx.drawImage(in,
                0, 0, pw, ph,
                Math.round(sx1 * sw),
                Math.round(sy1 * sh),
                Math.round(sx2 * sw),
                Math.round(sy2 * sh),
                Color.BLACK, this);
        if (stillChanging) {
            //TODO better handling here
            Thread.yield();
        }

    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        return true;
    }

}
