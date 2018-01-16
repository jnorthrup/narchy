package nars.video;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.InterleavedU8;

/**
 * https://github.com/lessthanoptimal/BoofCV/blob/master/main/geo/src/boofcv/alg/distort/LensDistortionOps.java
 * https://github.com/lessthanoptimal/BoofCV/blob/master/main/ip/test/boofcv/alg/distort/impl/GeneralImageDistortTests.java#L140
 * */
public class ImageCrop implements ImageProcess {
    private final int w, h;

//    /** proportion of image relative to center (0,0) -> (+/= 1, +/- 1) */
//    public float x, y;
//
//    /** proportion of image width/height */
//    public float pw, ph;

    //InterleavedU8 output;

    private int x1;
    private int y1;
    private InterleavedU8 output;

    public ImageCrop() {
        super();
        x1 = 0;
        y1 = 0;
        w = 100;
        h = 64;
    }



    @Override public ImageBase apply(ImageBase input) {
        x1 += (int) (-5 + Math.random() * 10);
        y1 += (int) (-5 + Math.random() * 10);

        int iw = input.getWidth();
        int ih = input.getHeight();

        x1 = Math.max(0, x1);
        y1 = Math.max(0, y1);
        x1 = Math.min(iw - w, x1);
        y1 = Math.min(ih - h, y1);

        return output = ((InterleavedU8)input).subimage(
                x1, y1,
                x1 + w, y1 + h, output);
//        FDistort tr = new FDistort().setRefs(input, output).interpNN().scaleExt();
//        //tr.apply(input,output/*,x0,y0,x1,y1*/);
//        tr.apply();
//        return output;
    }


}
