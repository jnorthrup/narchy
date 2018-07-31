package jcog.signal.wave2d;


public interface Bitmap2D {

    static int encodeRGB8b(float r, float g, float b) {
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
