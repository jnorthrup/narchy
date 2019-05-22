package spacegraph.input.finger.impl;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.core.image.GConvertImage;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import jcog.TODO;
import jcog.math.v2;
import org.ejml.ops.ConvertMatrixData;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.SubFinger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.LazySurface;
import spacegraph.video.*;

import java.awt.image.BufferedImage;

import static spacegraph.SpaceGraph.window;

/**
 * interprets webcam stream as animated skeleton capable of forming gestures for actuating one or more virtual cursors
 */
public class WebcamGestures extends Finger {

    private static final int BUTTONS = 1;

    protected WebcamGestures() {
        super(BUTTONS);
    }

    public static void main(String[] args) {

        VideoSource in = WebCam.the();

        OrthoSurfaceGraph g = window(new LazySurface(() -> {

            VideoSource in2 = new VideoEqualizer(in);

            VideoSource in3 = new VideoBackgroundRemoval(in2);

//            VideoSource in4 = VideoTransform.the(in, new Function<>() {
//
//                final IWorkArrays worker = new IWorkArrays();
//                final GrayU8 g = new GrayU8(1, 1);
//                final GrayU8 h = new GrayU8(1, 1);
//                final Random rng = new XoRoShiRo128PlusRandom(1);
//
//                @Override
//                public BufferedImage apply(BufferedImage f) {
//                    g.reshape(f.getWidth(), f.getHeight());
//                    h.reshape(f.getWidth(), f.getHeight());
//
//                    ConvertBufferedImage.convertFrom(f, g);
//
//                    EnhanceImageOps.sharpen4(g, h);
//
//                    return ConvertBufferedImage.extractBuffered(h);
//                    //return ConvertBufferedImage.convertTo(g, new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB), true);
//                }
//            });
//            VideoSource in3 = VideoTransform.the(in, new Function<>() {
//
//                final IWorkArrays worker = new IWorkArrays();
////                final GrayU8 g = new GrayU8(1,1);
////                final GrayU8 h = new GrayU8(1,1);
//                final Random rng = new XoRoShiRo128PlusRandom(1);
//                final InterleavedU8 g = new InterleavedU8(1, 1, 3);
//                final InterleavedU8 h = new InterleavedU8(1, 1, 3);
//
//                @Override
//                public BufferedImage apply(BufferedImage f) {
//                    //ConvertBufferedImage.extractInterleavedU8(f)
//
//                    g.reshape(f.getWidth(), f.getHeight());
//                    h.reshape(f.getWidth(), f.getHeight());
//                    ConvertBufferedImage.convertFromInterleaved(f, g, true);
//                    ImageMiscOps.addUniform(g, rng, 1, 25);
////                    EnhanceImageOps.equalizeLocal(
////                            ConvertBufferedImage.convertFrom(f, g),
////                            4, h, 1, worker);
//                    return ConvertBufferedImage.convertTo(g, new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB), true);
//                }
//            });

            return new Gridding(
                    new VideoSurface(in),
                    new VideoSurface(in2),
                    new VideoSurface(in3)
            );
        }), 1400, 800);

//        Finger f = g.fingers.get(0);
//        g.addFinger(new MyPolarSubFinger(f, new VideoSurface(in)));
//        g.addFinger(new MyPolarSubFinger(f, SignalViewTest.newSignalView()));
    }



    @Override
    public v2 posGlobal() {
        throw new TODO();
    }

    @Override
    protected void start(SpaceGraph x) {

    }

    @Override
    protected void stop(SpaceGraph x) {

    }

    /** http://boofcv.org/index.php?title=Example_Background_Moving_Camera */
    private static class VideoBackgroundRemoval extends VideoTransform {
        // storage for segmented image.  Background = 0, Foreground = 1
        final GrayU8 segmented = new GrayU8(1, 1);
        // Grey scale image that's the input for motion estimation
        final GrayF32 grey = new GrayF32(1,1);//segmented.width,segmented.height);
        private final ImageMotion2D motion2D;
        private final BackgroundModelMoving background;
        private final Homography2D_F32 firstToCurrent32;
        private final ImageType imageType;
        private BufferedImage visualized;

        GrayF32 input = null;

        public VideoBackgroundRemoval(VideoSource in) {
            super(in);

            // Comment/Uncomment to switch input image type
            //imageType = ImageType.single(GrayU8.class);
            imageType = ImageType.single(GrayF32.class);
//		imageType = ImageType.il(3, InterleavedF32.class);
///		imageType = ImageType.il(3, InterleavedU8.class);

            // Configure the feature detector
            ConfigGeneralDetector confDetector = new ConfigGeneralDetector();
            confDetector.threshold = 10;
            confDetector.maxFeatures = 300;
            confDetector.radius = 4;

            // Use a KLT tracker
            PointTracker tracker = FactoryPointTracker.klt(new int[]{1, 2, 4, 8}, confDetector, 3,
                    imageType.getImageClass(), null);

            // This estimates the 2D image motion
            motion2D =
                    FactoryMotion2D.createMotion2D(500, 0.5, 3, 100, 0.6, 0.5,
                            false, tracker, new Homography2D_F64());


            background = FactoryBackgroundModel.movingBasic(new ConfigBackgroundBasic(30, 0.005f), new PointTransformHomography_F32(), imageType);

            // Configuration for Gaussian model.  Note that the threshold changes depending on the number of image bands
            // 12 = gray scale and 40 = color
//            ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(12,0.001f);
//            configGaussian.initialVariance = 64;
//            configGaussian.minimumDifference = 5;
//            background = FactoryBackgroundModel.movingGaussian(configGaussian, new PointTransformHomography_F32(), imageType);

            // Note that GMM doesn't interpolate the input image. Making it harder to model object edges.
            // However it runs faster because of this.
//            background = FactoryBackgroundModel.movingGmm(configGmm,new PointTransformHomography_F32(), imageType);
//            ConfigBackgroundGmm configGmm = new ConfigBackgroundGmm();
//            configGmm.initialVariance = 1600;
//            configGmm.significantWeight = 1e-1f;

            background.setUnknownValue(1);

//            MediaManager media = DefaultMediaManager.INSTANCE;
//            SimpleImageSequence video =
//                    media.openVideo(fileName, background.getImageType());
////				media.openCamera(null,640,480,background.getImageType());

            //====== Initialize Images



            // coordinate frames


//            ImageGridPanel gui = new ImageGridPanel(1,2);
//            gui.setImages(visualized, visualized);

//            ShowImages.showWindow(gui, "Detections", true);
            firstToCurrent32 = new Homography2D_F32();
        }


        @Override
        protected synchronized BufferedImage apply(BufferedImage image) {

            int W = image.getWidth(), H = image.getHeight();
            if (segmented.getWidth()!=W || segmented.getHeight()!=H) {

                segmented.reshape(W, H);
                grey.reshape(W, H);

                Homography2D_F32 homeToWorld = new Homography2D_F32();
                homeToWorld.a13 = grey.width/2;
                homeToWorld.a23 = grey.height/2;

                // Create a background image twice the size of the input image.  Tell it that the home is in the center
                background.initialize(W * 2, H * 2, homeToWorld);

            }

            input = ConvertBufferedImage.convertFrom(image, input);

            GConvertImage.convert(input, grey);

            motion2D.process(grey);

            /*if( motion2D.process(grey) )*/ {
                Homography2D_F64 firstToCurrent64 = (Homography2D_F64) motion2D.getFirstToCurrent();
                ConvertMatrixData.convert(firstToCurrent64, firstToCurrent32);

                background.segment(firstToCurrent32, input, segmented);
                background.updateBackground(firstToCurrent32,input);


                if (visualized==null || (visualized.getWidth()!=segmented.width || visualized.getHeight()!=segmented.height)) {
                    visualized = new BufferedImage(segmented.width,segmented.height,BufferedImage.TYPE_BYTE_GRAY);
                }

                VisualizeBinaryData.renderBinary(segmented,false,visualized);
            }
            //else throw new TODO(); //input was probably blank


            return visualized;
        }
    }

    private static class MyPolarSubFinger extends SubFinger.PolarSubFinger {
        private final Surface vx;

        public MyPolarSubFinger(Finger f, Surface vx) {
            super(f);
            theta = (float)(Math.random() * 3.14*2);
            this.vx = vx;
        }

        @Override
        public Surface cursorSurface() {
            CursorSurface c = new CursorSurface(this);
            c.renderer = null;
            c.set(vx);
            return c;
        }
    }
}
