package jcog.signal.wave2d;

import java.awt.image.BufferedImage;

public class RGBBufImgBitmap2D extends PlanarBitmap2D {
    public final BufferedImage img;

    public RGBBufImgBitmap2D(BufferedImage i) {
        super(i.getWidth(), i.getHeight(), i.getRaster().getNumBands());
        this.img = i;
    }


    @Override
    public float get(int... cell) {
        var x = cell[0];
        var y = cell[1];

        var p = img.getRGB(x, y);

        var plane = cell[2];
        switch (plane) {
            case 0:
                return Bitmap2D.decode8bRed(p);
            case 1:
                return Bitmap2D.decode8bGreen(p);
            case 2:
                return Bitmap2D.decode8bBlue(p);
            case 3:
                return Bitmap2D.decode8bAlpha(p);
            default:
                throw new UnsupportedOperationException();
        }
    }


}
