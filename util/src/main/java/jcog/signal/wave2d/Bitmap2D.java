package jcog.signal.wave2d;


import jcog.signal.Tensor;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

public interface Bitmap2D extends Tensor {

    static int encodeRGB8b(float r, float g, float b) {
        return (Math.round(r * 255) << 16) |
                (Math.round(g * 255) << 8) |
                (Math.round(b * 255));
    }

    @Override
    default int[] shape() {
        return new int[] { width(), height() };
    }

    @Override
    default float getAt(int i) {
        int w = width();
        int y = i / w;
        int x = i % w;
        return brightness(x, y);
    }

    @Override
    default float get(int... cell) {
        return brightness(cell[0], cell[1]);
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

    /**
     * returns a new proxy bitmap that applies per-pixel brightness function
     * TODO variation of this that takes a per-frame brightness histogram as parameter */
    default ProxyBitmap2D brightnessCurve(FloatToFloatFunction b) {
        return new ProxyBitmap2D(this) {
            @Override
            public int width() {
                return Bitmap2D.this.width();
            }

            @Override
            public int height() {
                return Bitmap2D.this.height();
            }

            @Override
            public float brightness(int x, int y) {
                return b.valueOf(Bitmap2D.this.get(x, y));
            }
        };
    }


    /** TODO make separate class with controls */
    @Deprecated default ProxyBitmap2D blurred() {
        return new ProxyBitmap2D(this){
            @Override
            public int width() {
                return Bitmap2D.this.width();
            }
            @Override
            public int height() {
                return Bitmap2D.this.height();
            }

            @Override
            public float brightness(int x, int y) {
                float c = Bitmap2D.this.brightness(x, y);
                float up = y > 0 ? Bitmap2D.this.brightness(x, y-1) : c;
                float left = x > 0 ? Bitmap2D.this.brightness(x-1, y) : c;
                float down = y < height()-1 ? Bitmap2D.this.brightness(x, y+1) : c;
                float right = x < width()-1 ? Bitmap2D.this.brightness(x+1, y) : c;
                return (c*8 + up + left + down + right)/12;
            }
        };
    }


//    static float rgbToMono(int r, int g, int b) {
//        return (r + g + b) / 256f / 3f;
//    }

//
//    @FunctionalInterface
//    interface EachPixelRGB {
//        void pixel(int x, int y, int aRGB);
//    }
//
//    @FunctionalInterface
//    interface EachPixelRGBf {
//        void pixel(int x, int y, float r, float g, float b, float a);
//    }

//    @FunctionalInterface
//    interface PerPixelMono {
//        void pixel(int x, int y, float whiteLevel);
//    }
//
//    @FunctionalInterface
//    interface PerIndexMono {
//        void pixel(int index, float whiteLevel);
//    }


//    default void intToFloat(EachPixelRGBf m, int x, int y, int p) {
//
//        int a = 255;
//        float r = decodeRed(p);
//        float g = decodeGreen(p);
//        float b = decodeBlue(p);
//        m.pixel(x, y, r, g, b, a/256f);
//    }


    static float decode8bRed(int p) {
        return ((p & 0x00ff0000) >> 16) / 256f;
    }

    static float decode8bGreen(int p) {
        return ((p & 0x0000ff00) >> 8) / 256f;
    }

    static float decode8bBlue(int p) {
        return ((p & 0x000000ff)) / 256f;
    }

    static float decode8bAlpha(int p) {
        return ((p & 0xff000000) >> 24) / 256f;
    }


}
