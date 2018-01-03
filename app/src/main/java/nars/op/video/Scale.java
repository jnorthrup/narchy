package nars.op.video;


import jcog.signal.Bitmap2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

/**
 * Pan/Zoom filter for a BuferredImage source
 */
public class Scale extends BufferedImageBitmap2D {

    private final Supplier<BufferedImage> src;


    /**
     * output pixel width / height
     */
    public int pw, ph;

    public Scale(Supplier<BufferedImage> source, int pw, int ph) {
        super();

        this.src = source;
        this.pw = pw;
        this.ph = ph;
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
    public void update(float frameRate) {
        if (src instanceof Bitmap2D)
            ((Bitmap2D) src).update(frameRate);

        Image in = src.get();
        if (in == null)
            return;

        if (out == null || out.getWidth() != pw || out.getHeight() != ph) {

            out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
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



        Graphics2D outgfx = out.createGraphics(); //create a graphics object to paint to
        outgfx.setColor(Color.BLACK);
        outgfx.fillRect(0, 0, pw, ph); //TODO add fade option
        outgfx.drawImage(in, 0, 0, pw, ph, null); //draw the image scaled
        outgfx.dispose();
    }

}
