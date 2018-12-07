package jcog.signal.wave2d;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import static java.awt.color.ColorSpace.TYPE_RGB;

/**
 * Pan/Zoom filter for a BuferredImage source
 */
public class ScaledBitmap2D extends MonoBufImgBitmap2D /* TODO extends ArrayBitmap2D directly, bypassing Swing */ /*implements ImageObserver*/ {

    float sx1 = 0;
    float sy1 = 0;
    float sx2 = 1;
    float sy2 = 1;

    /**
     * output pixel width / height
     */
    public int pw, ph;
    private Graphics2D outgfx;


    public ScaledBitmap2D(Supplier<BufferedImage> source, int pw, int ph) {
        super();
        this.pw = pw;
        this.ph = ph;
        this.source = source;
        update();
    }

    public ScaledBitmap2D crop(float sx1, float sy1, float sx2, float sy2) {
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
        if (source instanceof Bitmap2D)
            ((Bitmap2D) source).update();

        if (source == null)
            return;

        Image in = source.get();
        if (in == null)
            return;

        if (raster == null || outgfx == null || raster.getWidth() != pw || raster.getHeight() != ph) {

            if (outgfx!=null)
                outgfx.dispose();

            img = new BufferedImage(pw, ph, in instanceof BufferedImage ? ((BufferedImage)in).getType() : TYPE_RGB);
            raster = img.getRaster();
            outgfx = img.createGraphics();












        }




        outgfx.setColor(Color.BLACK);
        outgfx.fillRect(0, 0, pw, ph); 



        /*
            public abstract boolean drawImage(Image img,
                                      int dx1, int dy1, int dx2, int dy2,
                                      int sx1, int sy1, int sx2, int sy2,
                                      Color bgcolor,
                                      ImageObserver observer);
         */
        int sw = in.getWidth(null), sh = in.getHeight(null);

        //boolean stillChanging =
        outgfx.drawImage(in,
                0, 0, pw, ph,
                Math.round(sx1 * sw),
                Math.round(sy1 * sh),
                Math.round(sx2 * sw),
                Math.round(sy2 * sh),
                Color.BLACK, null);
//        while (stillChanging) {
//
//            Thread.yield();
//        }

    }

//    @Override
//    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
//        return true;
//    }

}
