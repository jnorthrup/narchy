package spacegraph.space2d.dyn2d;


import boofcv.alg.background.BackgroundModelStationary;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import jcog.exe.Loop;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.Tex;
import spacegraph.video.WebCam;

import java.awt.image.BufferedImage;

public class ExampleBackgroundRemovalStationary {
    public static void main(String[] args) {

        WebCam c = new WebCam();

        Tex output = new Tex();
        SpaceGraph.window(new Gridding(
            c.view(),
            output.view()
        ), 800, 800);

//		String fileName = UtilIO.pathExample("background/horse_jitter.mp4"); // degraded performance because of jitter
//		String fileName = UtilIO.pathExample("tracking/chipmunk.mjpeg"); // Camera moves.  Stationary will fail here

        // Comment/Uncomment to switch input image type
        ImageType imageType = ImageType.single(GrayF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedU8.class);

        // Configuration for Gaussian model.  Note that the threshold changes depending on the number of image bands
        // 12 = gray scale and 40 = color
        ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(40, 0.0005f);
        configGaussian.initialVariance = 100;
        configGaussian.minimumDifference = 10f;

        // Comment/Uncomment to switch algorithms
        BackgroundModelStationary background =
//				FactoryBackgroundModel.stationaryBasic(new ConfigBackgroundBasic(35, 0.005f), imageType);
                FactoryBackgroundModel.stationaryGaussian(configGaussian, imageType);



        // Declare storage for segmented image.  1 = moving foreground and 0 = background
        GrayU8 segmented = new GrayU8(c.width, c.height);
        GrayF32 input = new GrayF32(c.width, c.height);



        BufferedImage segmentedVis = new BufferedImage(c.width, c.height, BufferedImage.TYPE_INT_RGB);

        new Loop(10f) {
            @Override
            public boolean next() {
                BufferedImage img = c.image;
                if (img != null) {


                    ConvertBufferedImage.convertFrom(img, input, true);

//                    long before = System.nanoTime();
                    background.segment(input, segmented);

                    background.updateBackground(input);

                    byte[] b = segmented.data;
                    for (int i = 0; i < b.length; i++) {
                        if (b[i]!=0)
                            b[i] = 127;
                    }

                    output.update(
                        //segmented
                        ConvertBufferedImage.convertTo(segmented, segmentedVis)
                    );
                }
//            VisualizeBinaryData.renderBinary(segmented, false, visualized);
////            gui.setImage(0, 0, (BufferedImage) video.getGuiImage());
////            gui.setImage(0, 1, visualized);
////            gui.repaint();
//            System.out.println("FPS = " + fps);
//
//            try {
//                Thread.sleep(5);
//            } catch (InterruptedException e) {

//            }
                return true;
            }
        };
    }
}