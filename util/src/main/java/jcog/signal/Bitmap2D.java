package jcog.signal;


import jcog.TODO;
import jcog.math.tensor.Tensor;

import java.awt.image.BufferedImage;

public interface Bitmap2D {

    static int encodeRGB(float r, float g, float b) {
        return  (Math.round(r*255) << 16) |
                (Math.round(g*255) << 8) |
                (Math.round(b*255));
    }

    /** explicit refresh update the image */
    default void update() {

    }

    int width();
    int height();


    /** returns a value 0..1.0 indicating the monochrome brightness (white level) at the specified pixel */
    float brightness(int xx, int yy);

    /** RGB filtered brightness, if supported; otherwise the factors are ignored */
    default float brightness(int xx, int yy, float rFactor, float gFactor, float bFactor) {
        return brightness(xx, yy); 
    }

    static float rgbToMono(int r, int g, int b) {
        return (r+g+b)/256f/3f;
    }


    @FunctionalInterface interface EachPixelRGB {
        void pixel(int x, int y, int aRGB);
    }
    @FunctionalInterface interface EachPixelRGBf {
        void pixel(int x, int y, float r, float g, float b, float a);
    }
    @FunctionalInterface interface PerPixelMono {
        void pixel(int x, int y, float whiteLevel);
    }
    @FunctionalInterface interface PerIndexMono {
        void pixel(int index, float whiteLevel);
    }







    default void intToFloat(EachPixelRGBf m, int x, int y, int p) {
        
        int a = 255;
        float r = decodeRed(p);
        float g = decodeGreen(p);
        float b = decodeBlue(p);
        m.pixel(x, y, r, g, b, a/256f);
    }




















    static float decodeRed(int p) {
        return ((p & 0x00ff0000) >> 16)/256f;
    }
    static float decodeGreen(int p) {
        return ((p & 0x0000ff00) >> 8)/256f;
    }
    static float decodeBlue(int p) {
        return ((p & 0x000000ff))/256f;
    }
    static float decodeAlpha(int p) {
        return ((p & 0xff000000) >> 24)/256f;
    }


    class RGBTensor implements Tensor {
        private final BufferedImage img;
        private final int[] shape;

        public RGBTensor(BufferedImage i) {
            this.img = i;
            int bitplanes = i.getRaster().getNumBands();
            this.shape = new int[] { i.getWidth(), i.getHeight(), bitplanes };
        }

        @Override
        public float get(int linearCell) {
//            int w = shape[0];
//            int planes = shape[2];
//
//            int y = linearCell / (w * planes);
//            int x = ((linearCell/planes) - (y * w * planes));
//
//            System.out.println(linearCell + " : " + x + " " + y);
//
//
//           return get(x, y, p)
            throw new TODO();
        }

        @Override
        public float get(int... cell) {
            int x = cell[0];
            int y = cell[1];

            int p = img.getRGB(x, y);

            int plane = cell[2];
            switch (plane) {
                case 0:
                    return decodeRed(p);
                case 1:
                    return decodeGreen(p);
                case 2:
                    return decodeBlue(p);
                case 3:
                    return decodeAlpha(p);
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public float[] snapshot() {
            throw new TODO();
        }

        @Override
        public int[] shape() {
            return shape;
        }


    }
}
