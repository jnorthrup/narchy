package jcog.signal;


public interface Bitmap2D {

    static int encodeRGB(float r, float g, float b) {
        return  (Math.round(r*255) << 16) |
                (Math.round(g*255) << 8) |
                (Math.round(b*255));
    }

    public static class ArrayBitmap2D implements Bitmap2D {

        private final float[][] x;

        public ArrayBitmap2D(float[][] x) {
            this.x = x;
        }

        @Override
        public int width() {
            return x[0].length;
        }

        @Override
        public int height() {
            return x.length;
        }

        @Override
        public float brightness(int xx, int yy) {
            return x[xx][yy];
        }
    }

    /** explicit refresh update the image */
    default void update(float frameRate) {

    }

    int width();
    int height();


    /** returns a value 0..1.0 indicating the monochrome brightness (white level) at the specified pixel */
    float brightness(int xx, int yy);

    /** RGB filtered brightness, if supported; otherwise the factors are ignored */
    default float brightness(int xx, int yy, float rFactor, float gFactor, float bFactor) {
        return brightness(xx, yy); //DEFAULT: unsupported
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

//    default void see(EachPixelRGBf m) {
//        see((x, y, p)-> {
//            intToFloat(m, x, y, p);
//        });
//    }

    default void intToFloat(EachPixelRGBf m, int x, int y, int p) {
        //int a = (p & 0xff000000) >> 24;
        int a = 255;
        float r = decodeRed(p);
        float g = decodeGreen(p);
        float b = decodeBlue(p);
        m.pixel(x, y, r, g, b, a/256f);
    }


//    default void seeMono(PerPixelMono m) {
//        see((x, y, p)-> {
//            int r = (p & 0x00ff0000) >> 16;
//            int g = (p & 0x0000ff00) >> 8;
//            int b = (p & 0x000000ff);
//
//            m.pixel(x, y, ((r+g+b) / 256f)/3f);
//        });
//    }
//
//
//    default void seeMono(PerIndexMono m) {
//        seeMono((x, y, p)-> {
//            m.pixel(width() * y + x, p);
//        });
//    }


    static float decodeRed(int p) {
        return ((p & 0x00ff0000) >> 16)/256f;
    }
    static float decodeGreen(int p) {
        return ((p & 0x0000ff00) >> 8)/256f;
    }
    static float decodeBlue(int p) {
        return ((p & 0x000000ff))/256f;
    }




//    public static float noise(float v, float noiseLevel, Random rng) {
//        if (noiseLevel > 0) {
//            return Util.clamp(v + (rng.nextFloat() * noiseLevel));
//        }
//        return v;
//    }

}
