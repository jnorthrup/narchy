package jcog.signal;


import jcog.TODO;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.Tensor;

import java.awt.image.BufferedImage;

public interface Bitmap2D {

    static int encodeRGB(float r, float g, float b) {
        return (Math.round(r * 255) << 16) |
                (Math.round(g * 255) << 8) |
                (Math.round(b * 255));
    }

    /**
     * explicit refresh update the image
     */
    default void update() {

    }

    int width();

    int height();


    /**
     * returns a value 0..1.0 indicating the monochrome brightness (white level) at the specified pixel
     */
    float brightness(int xx, int yy);

    /**
     * RGB filtered brightness, if supported; otherwise the factors are ignored
     */
    default float brightness(int xx, int yy, float rFactor, float gFactor, float bFactor) {
        return brightness(xx, yy);
    }

    static float rgbToMono(int r, int g, int b) {
        return (r + g + b) / 256f / 3f;
    }


    @FunctionalInterface
    interface EachPixelRGB {
        void pixel(int x, int y, int aRGB);
    }

    @FunctionalInterface
    interface EachPixelRGBf {
        void pixel(int x, int y, float r, float g, float b, float a);
    }

    @FunctionalInterface
    interface PerPixelMono {
        void pixel(int x, int y, float whiteLevel);
    }

    @FunctionalInterface
    interface PerIndexMono {
        void pixel(int index, float whiteLevel);
    }


//    default void intToFloat(EachPixelRGBf m, int x, int y, int p) {
//
//        int a = 255;
//        float r = decodeRed(p);
//        float g = decodeGreen(p);
//        float b = decodeBlue(p);
//        m.pixel(x, y, r, g, b, a/256f);
//    }


    static float decodeRed(int p) {
        return ((p & 0x00ff0000) >> 16) / 256f;
    }

    static float decodeGreen(int p) {
        return ((p & 0x0000ff00) >> 8) / 256f;
    }

    static float decodeBlue(int p) {
        return ((p & 0x000000ff)) / 256f;
    }

    static float decodeAlpha(int p) {
        return ((p & 0xff000000) >> 24) / 256f;
    }


    /**
     * single color plane, float value intensity 0..1
     */
    abstract class BitmapTensor implements Tensor, Bitmap2D {
        protected int[] shape;

        public BitmapTensor() {
            this(0, 0, 0);
        }

        public BitmapTensor(int w, int h, int bitplanes) {
            resize(w, h, bitplanes);
        }

        protected void resize(int w, int h, int bitplanes) {
            if (shape != null) {
                if (shape.length == 3 && shape[0] == w && shape[1] == h && shape[2] == bitplanes)
                    return; //no change
            }
            this.shape = new int[]{w, h, bitplanes};
        }

        @Override
        public int[] shape() {
            return shape;
        }

        public int width() {
            return shape[0];
        }

        public int height() {
            return shape[1];
        }

        @Override
        public float[] snapshot() {
            throw new TODO();
        }

        @Override
        public float brightness(int xx, int yy) {
            //TODO handle alpha channel correctly
            int planes = shape[2];
            float sum = 0;
            for (int p = 0; p < planes; p++) {
                sum += get(xx, yy, p);
            }
            return sum / planes;
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
    }

    class BitmapRGBTensor extends BitmapTensor {
        private final BufferedImage img;

        public BitmapRGBTensor(BufferedImage i) {
            super(i.getWidth(), i.getHeight(), i.getRaster().getNumBands());
            this.img = i;
        }


        @Override
        public float get(int... cell) {
            int x = cell[0];
            int y = cell[1];
            int plane = cell[2];

            int p = img.getRGB(x, y);

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


    }

    /**
     * labeled FloatRange controls underlying ArrayTensor.  useful for UI purposes that rely on reflection for constructing widgets
     */
    class RGB extends ArrayTensor {
        public final FloatRange red, green, blue;

        public RGB() {
            this(0, 1);
        }

        public RGB(float min, float max) {
            super(new float[3]);
            red = new FloatRange(1f, min, max) {
                @Override
                public void set(float newValue) {
                    super.set(data[0] = newValue);
                }
            };
            green = new FloatRange(1f, min, max) {
                @Override
                public void set(float newValue) {
                    super.set(data[1] = newValue);
                }
            };
            blue = new FloatRange(1f, min, max) {
                @Override
                public void set(float newValue) {
                    super.set(data[2] = newValue);
                }
            };
        }
    }

    class RGBToGrayBitmapTensor extends BitmapTensor {
        private final Tensor rgbMix;
        private volatile BitmapRGBTensor src;

        public RGBToGrayBitmapTensor(Tensor rgbMix) {
            this.rgbMix = rgbMix;
        }

        public RGBToGrayBitmapTensor(BitmapRGBTensor src, Tensor rgbMix) {
            this(rgbMix);
            update(src);
        }

        public void update(BitmapRGBTensor src) {
            this.src = src;
            resize(src.width(), src.height(), 1);
        }

        public float get(int x, int y) {

            float R = rgbMix.get(0);
            float G = rgbMix.get(1);
            float B = rgbMix.get(2);
            float RGB = Math.abs(R) + Math.abs(G) + Math.abs(B);
            if (RGB < Float.MIN_NORMAL)
                return 0f;

            float r = src.get(x, y, 0);
            float g = src.get(x, y, 1);
            float b = src.get(x, y, 2);
            return Util.unitize((r * R + g * G + b * B) / RGB);
        }

        @Override
        public float get(int... cell) {
            int x = cell[0];
            int y = cell[1];
            return get(x, y);
        }

    }

}
