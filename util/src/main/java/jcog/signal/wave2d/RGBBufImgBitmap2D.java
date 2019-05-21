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
        int x = cell[0];
        int y = cell[1];

        int p = img.getRGB(x, y);

        int plane = cell[2];
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
