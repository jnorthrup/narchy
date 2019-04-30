package spacegraph.input.finger.impl;

import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.IWorkArrays;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedU8;
import jcog.TODO;
import jcog.math.v2;
import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.LazySurface;
import spacegraph.video.VideoSource;
import spacegraph.video.VideoSurface;
import spacegraph.video.VideoTransform;
import spacegraph.video.WebCam;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Random;
import java.util.function.Function;

import static spacegraph.SpaceGraph.window;

/** interprets webcam stream as animated skeleton capable of forming gestures for actuating one or more virtual cursors */
public class WebcamGestures extends Finger {

    private static final int BUTTONS = 1;

    protected WebcamGestures() {
        super(BUTTONS);
    }

    @Override
    public v2 posGlobal() {
        throw new TODO();
    }

    public static void main(String[] args) {


        window(new LazySurface(()-> {
            WebCam in = WebCam.the();
            VideoSource in2 = VideoTransform.the(in, f -> {
                return equalize(f);
            });
            VideoSource in4 = VideoTransform.the(in, new Function<>() {

                final IWorkArrays worker = new IWorkArrays();
                final GrayU8 g = new GrayU8(1,1);
                final GrayU8 h = new GrayU8(1,1);
                final Random rng = new XoRoShiRo128PlusRandom(1);

                @Override
                public BufferedImage apply(BufferedImage f) {
                    g.reshape(f.getWidth(), f.getHeight());
                    h.reshape(f.getWidth(), f.getHeight());

                    ConvertBufferedImage.convertFrom(f, g);

                    EnhanceImageOps.sharpen4(g, h);

                    return ConvertBufferedImage.extractBuffered(h);
                    //return ConvertBufferedImage.convertTo(g, new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB), true);
                }
            });
            VideoSource in3 = VideoTransform.the(in, new Function<>() {

                final IWorkArrays worker = new IWorkArrays();
//                final GrayU8 g = new GrayU8(1,1);
//                final GrayU8 h = new GrayU8(1,1);

                InterleavedU8 g = new InterleavedU8(1, 1, 3);
                InterleavedU8 h = new InterleavedU8(1, 1, 3);
                final Random rng = new XoRoShiRo128PlusRandom(1);

                @Override
                public BufferedImage apply(BufferedImage f) {
                    //ConvertBufferedImage.extractInterleavedU8(f)

                    g.reshape(f.getWidth(), f.getHeight());
                    h.reshape(f.getWidth(), f.getHeight());
                    ConvertBufferedImage.convertFromInterleaved(f, g, true);
                    ImageMiscOps.addUniform(g, rng, 1, 25);
//                    EnhanceImageOps.equalizeLocal(
//                            ConvertBufferedImage.convertFrom(f, g),
//                            4, h, 1, worker);
                    return ConvertBufferedImage.convertTo(g, new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB), true);
                }
            });

            return new Gridding(
                new VideoSurface(in),
                new VideoSurface(in2)
            );
        }), 1400, 800);
    }

    /** https://www.codeproject.com/Tips/1172662/Histogram-Equalisation-in-Java */
    static BufferedImage equalize(BufferedImage src){
        BufferedImage nImg = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster wr = src.getRaster();
        WritableRaster er = nImg.getRaster();
        int totpix= wr.getWidth()*wr.getHeight();
        int[] histogram = new int[256];

        for (int x = 0; x < wr.getWidth(); x++) {
            for (int y = 0; y < wr.getHeight(); y++) {
                histogram[wr.getSample(x, y, 0)]++;
            }
        }

        int[] chistogram = new int[256];
        chistogram[0] = histogram[0];
        for(int i=1;i<256;i++){
            chistogram[i] = chistogram[i-1] + histogram[i];
        }

        float[] arr = new float[256];
        for(int i=0;i<256;i++){
            arr[i] =  (float)((chistogram[i]*255.0)/(float)totpix);
        }

        for (int x = 0; x < wr.getWidth(); x++) {
            for (int y = 0; y < wr.getHeight(); y++) {
                int nVal = (int) arr[wr.getSample(x, y, 0)];
                er.setSample(x, y, 0, nVal);
            }
        }
        nImg.setData(er);
        return nImg;
    }
}
