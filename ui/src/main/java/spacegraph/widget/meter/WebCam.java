package spacegraph.widget.meter;

import boofcv.struct.image.InterleavedU8;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamEventType;
import com.github.sarxos.webcam.WebcamListener;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Ons;
import jcog.event.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.Surface;
import spacegraph.SurfaceBase;
import spacegraph.container.AspectAlign;
import spacegraph.render.JoglSpace;
import spacegraph.render.Tex;

import java.awt.*;
import java.awt.image.BufferedImage;


/** TODO
 *
 * event listeners:
 *      onClose - destroy surfaces
 */

public class WebCam {

    public int width;
    public int height;
    //private SourceDataLine mLine;
    // private ShortBuffer audioSamples;
    public com.github.sarxos.webcam.Webcam webcam;

    public final Topic<WebcamEvent> eventChange = new ListTopic();


//    @Range(min = 0, max = 1)
//    public final MutableFloat cpuThrottle = new MutableFloat(0.5f);


    final static Logger logger = LoggerFactory.getLogger(WebCam.class);
    public BufferedImage image;
    //public byte[] image;


    public WebCam() {
        this(Webcam.getDefault());
    }

    public WebCam(Webcam wc) {

        logger.info("Webcam Devices: {} ", com.github.sarxos.webcam.Webcam.getWebcams());

        // Open a webcam at a resolution close to 640x480
        webcam = wc;

        if (!webcam.open(true))
            throw new RuntimeException("webcam not open");

        Dimension dim = webcam.getViewSize();
        width = (int) dim.getWidth();
        height = (int) dim.getHeight();


        //default behavior:
        webcam.addWebcamListener(new WebcamListener() {

            @Override
            public void webcamOpen(WebcamEvent we) {

            }

            @Override
            public void webcamClosed(WebcamEvent we) {

            }

            @Override
            public void webcamDisposed(WebcamEvent we) {

            }

            @Override
            public void webcamImageObtained(WebcamEvent we) {


                Dimension viewSize = webcam.getViewSize();
                if (viewSize != null) {
                    width = (int) viewSize.getWidth();
                    height = (int) viewSize.getHeight();



                    BufferedImage nextImage = we.getImage();
                    if (nextImage != null) {
                        //if (iimage == null || iimage.width!=width || iimage.height!=height) {
                        //InterleavedU8 iimage = new InterleavedU8(width, height, 3);


                        //convertFromInterleaved(nextImage, iimage);

                        //int[] output = we.getImage().getRGB(0, 0, width, height, null, 0, width * 3);

                        //webcam.getgetImageBytes(image);


                        WebCam.this.image = nextImage;
                        ///WebCam.this.image = iimage.data;

                        eventChange.emit(we);
                    }
                } else {
                    width = height = 0;
                }

            }
        });


    }

    public static void convertFromInterleaved(BufferedImage src, InterleavedU8 dst) {

        final int width = src.getWidth();
        final int height = src.getHeight();

        if (dst.getNumBands() == 3) {

            byte[] dd = dst.data;
            for (int y = 0; y < height; y++) {
                int indexDst = dst.startIndex + y * dst.stride;
                for (int x = 0; x < width; x++) {
                    int argb = src.getRGB(x, y);

                    dd[indexDst++] = (byte) (argb >>> 16);
                    dd[indexDst++] = (byte) (argb >>> 8);
                    dd[indexDst++] = (byte) argb;
                }
            }
        } else if (dst.getNumBands() == 1) {
            //ConvertRaster.bufferedToGray(src, dst);
            //} else {
            throw new IllegalArgumentException("Unsupported number of input bands");
        }
    }


    Ons webcamListeners = new Ons();
    public void on(WebcamListener wl) {
        webcamListeners.add(new On() {
            {
                webcam.addWebcamListener(wl);
            }
            @Override public void off() {
                webcam.removeWebcamListener(wl);
            }
        });

    }

//    public void on(Consumer<BufferedImage> wl) {
//        on(new WebcamListener() {
//            @Override
//            public void webcamOpen(WebcamEvent we) {
//
//            }
//
//            @Override
//            public void webcamClosed(WebcamEvent we) {
//
//            }
//
//            @Override
//            public void webcamDisposed(WebcamEvent we) {
//
//            }
//
//            @Override
//            public void webcamImageObtained(WebcamEvent we) {
//                wl.accept(we.getImage());
//            }
//        });
//    }

//    public Thread loop(float fps) {
//        //try {
//
//            Thread t = new Thread(() -> {
//
//                while (running) {
//                    if (/*webcam.isOpen() && */webcam.isImageNew()) {
//
//                        image.rewind();
//
//                        Dimension viewSize = webcam.getViewSize();
//                        if (viewSize != null) {
//                            width = (int) viewSize.getWidth();
//                            height = (int) viewSize.getHeight();
//
//                            webcam.getImageBytes(image);
//
//                            image.rewind();
//
//                            eventChange.emit(this);
//
//                        } else {
//                            width = height = 0;
//                        }
//
//                    }
//
////                BufferedImage bimage = process(webcam.getImage());
////
////                //TODO blit the image directly, this is likely not be the most efficient:
////                image = SwingFXUtils.toFXImage(bimage, image);
////
////                WritableImage finalImage = process(image);
////                runLater(() -> view.setImage(finalImage));
////            }
//
//                    try {
//                        Thread.sleep((long) (1000.0f / fps));
//                    } catch (InterruptedException e) {
//                    }
//                }
//
//
//            });
//            t.start();
//            return t;
//
//        //} catch (Exception e) {
//            //getChildren().add(new Label(e.toString()));
//            //return null;
//        //}
//
//    }

    public class WebCamSurface extends AspectAlign {


        private final Tex ts;
        private On on;

        WebCamSurface(Tex ts) {
            super(ts.view(), ((float)webcam.getViewSize().getHeight()) / ((float)webcam.getViewSize().getWidth()));
            this.ts = ts;
            layout();
        }

        @Override
        public void start(SurfaceBase parent) {
            synchronized(this) {
                super.start(parent);
                on = eventChange.on(x -> {
                    if (x.getType()==WebcamEventType.CLOSED || x.getType()==WebcamEventType.DISPOSED) {
                        this.stop();
                    } else if (x.getType()==WebcamEventType.NEW_IMAGE) {
                        ts.update(image);
                    }
                });
            }
        }

        @Override
        public void stop() {
            synchronized(this) {
                on.off();
                on = null;
                super.stop();
            }
        }

    }

    public Surface view() {

        return new WebCamSurface(new Tex());

//
//        return new Splitting(ts.view(), new Gridding(new PushButton("snap").click(()->{
//            byte[] bytes = ((DataBufferByte)iimage.getRaster().getDataBuffer()).getData();
//            User.the().put(
//                    "(\"" + webcam.getName() +"\"," + System.currentTimeMillis() + ")",
//                    bytes
//                    );
//        })),0.1f);
    }


    public static void main(String[] args) {

        final WebCam w = new WebCam();
        JoglSpace.window(
                //new Cuboid(new WebcamSurface(320, 200),4,4), 1200, 1200);
                w.view(), 1200, 1200);
        //w.loop(10);


//        NARfx.run((a, b) -> {
//
//
//            Pane bv = new WebcamFX();
//
//
//            b.setWidth(1000);
//            b.setHeight(1000);
//            b.setScene(new Scene(bv));
//
//            b.show();
//
//            //b.sizeToScene();
//
//        });


    }

    /**
     * after webcam input
     */
    protected static BufferedImage process(BufferedImage img) {
        return img;
    }

    public void stop() {

    }

//    /**
//     * before display output
//     */
//    protected WritableImage process(WritableImage finalImage) {
//        return finalImage;
//    }


//    /**
//     * Example of how to open a webcam and track a user selected object.  Click and drag the mouse
//     * to select an object to track.
//     *
//     * @author Peter Abeles
//     */
//    public class WebcamShapes<T extends ImageBase> extends JPanel
//            implements MouseListener, MouseMotionListener {
//
//        TrackerObjectQuad<T> tracker;
//
//        // location of the target being tracked
//        Quadrilateral_F64 target = new Quadrilateral_F64();
//
//        // location selected by the mouse
//        Point2D_I32 point0 = new Point2D_I32();
//        Point2D_I32 point1 = new Point2D_I32();
//
//        int desiredWidth,desiredHeight;
//        volatile int mode;
//
//        BufferedImage workImage;
//
//        JFrame window;
//
//
//        // Polynomial fitting tolerances
//        static double toleranceDist = 8;
//        static double toleranceAngle= Math.PI/10;
//
//        /**
//         * Configures the tracking application
//         *
//         * @param tracker The object tracker
//         * @param desiredWidth Desired size of the input stream
//         * @param desiredHeight Desired height of the input stream
//         */
//        public WebcamShapes(TrackerObjectQuad<T> tracker,
//                            int desiredWidth , int desiredHeight)
//        {
//            this.tracker = tracker;
//            this.desiredWidth = desiredWidth;
//            this.desiredHeight = desiredHeight;
//
//            addMouseListener(this);
//            addMouseMotionListener(this);
//
//            window = new JFrame("Object Tracking");
//            window.setContentPane(this);
//            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        }
//
//        // used to select colors for each line
//        static Random rand = new XORShiftRandom();
//
//
//        /**
//         * Detects contours inside the binary image generated by canny.  Only the external contour is relevant. Often
//         * easier to deal with than working with Canny edges directly.
//         */
//        public static void fitCannyBinary(GrayF32 input, Graphics2D overlay ) {
//
//            BufferedImage displayImage = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
//            GrayU8 binary = new GrayU8(input.width,input.height);
//
//            int blurRadius = 2;
//
//            // Finds edges inside the image
//            CannyEdge<GrayF32,GrayF32> canny =
//                    FactoryEdgeDetectors.canny(blurRadius, false, true, GrayF32.class, GrayF32.class);
//
//            canny.process(input, 0.1f, 0.3f, binary);
//
//            java.util.List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
//
//
//            overlay.setStroke(new BasicStroke(4));
//
//
//            int iterations = 80;
//            for( Contour c : contours ) {
//                // Only the external contours are relevant.
//
////            System.out.println(c);
////            System.out.println(c.external);
//                java.util.List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(
//                        c.external, true,
//                        toleranceDist,
//                        toleranceAngle, iterations);
//
//                overlay.setColor(new Color(rand.nextInt()));
//                VisualizeShapes.drawPolygon(vertexes, true, overlay);
//
//            }
//
//            //ShowImages.showWindow(displayImage, "Canny Contour");
//        }
//
//        /**
//         * Invoke to start the main processing loop.
//         */
//        public void process() {
//            Webcam webcam = UtilWebcamCapture.openDefault(desiredWidth, desiredHeight);
//
//            // adjust the window size and let the GUI know it has changed
//            Dimension actualSize = webcam.getViewSize();
//            setPreferredSize(actualSize);
//            setMinimumSize(actualSize);
//            window.setMinimumSize(actualSize);
//            window.setPreferredSize(actualSize);
//            window.setVisible(true);
//
//            // create
//            T input = tracker.getImageType().createImage(actualSize.width,actualSize.height);
//
//            workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
//
//            GrayF32 inputFloat = new GrayF32(actualSize.width, actualSize.height);
//
//            //noinspection InfiniteLoopStatement
//            while( true ) {
//                BufferedImage buffered = webcam.getImage();
//                ConvertBufferedImage.convertFrom(webcam.getImage(), input, true);
//
//                // mode is read/written to by the GUI also
//                int mode = this.mode;
//
//                boolean success = false;
//                if( mode == 2 ) {
//                    Rectangle2D_F64 rect = new Rectangle2D_F64();
//                    rect.set(point0.x, point0.y, point1.x, point1.y);
//                    UtilPolygons2D_F64.convert(rect, target);
//                    success = tracker.initialize(input,target);
//                    this.mode = success ? 3 : 0;
//                } else if( mode == 3 ) {
//                    success = tracker.process(input,target);
//                }
//
//
//                synchronized( workImage ) {
//                    // copy the latest image into the work buffered
//                    Graphics2D g2 = workImage.createGraphics();
//
//                    g2.drawImage(buffered,0,0,null);
//
//                    ConvertBufferedImage.convertFrom(buffered, inputFloat);
//                    fitCannyBinary(inputFloat, g2);
//
//                    // visualize the current results
//                    if (mode == 1) {
//                        drawSelected(g2);
//                    } else if (mode == 3) {
//                        if( success ) {
//                            drawTrack(g2);
//                        }
//                    }
//                }
//
//                repaint();
//            }
//        }
//
//        @Override
//        public void paint (Graphics g) {
//            if( workImage != null ) {
//                // render the work image and be careful to make sure it isn't being manipulated at the same time
//                synchronized (workImage) {
//                    g.drawImage(workImage, 0, 0, null);
//                }
//            }
//        }
//
//        private void drawSelected( Graphics2D g2 ) {
//            g2.setColor(Color.RED);
//            g2.setStroke( new BasicStroke(3));
//            g2.drawLine(point0.getX(),point0.getY(),point1.getX(),point0.getY());
//            g2.drawLine(point1.getX(),point0.getY(),point1.getX(),point1.getY());
//            g2.drawLine(point1.getX(),point1.getY(),point0.getX(),point1.getY());
//            g2.drawLine(point0.getX(),point1.getY(),point0.getX(),point0.getY());
//        }
//
//        private void drawTrack( Graphics2D g2 ) {
//            g2.setStroke(new BasicStroke(3));
//            g2.setColor(Color.RED);
//            g2.drawLine((int)target.a.getX(),(int)target.a.getY(),(int)target.b.getX(),(int)target.b.getY());
//            g2.setColor(Color.BLUE);
//            g2.drawLine((int)target.b.getX(),(int)target.b.getY(),(int)target.c.getX(),(int)target.c.getY());
//            g2.setColor(Color.GREEN);
//            g2.drawLine((int)target.c.getX(),(int)target.c.getY(),(int)target.d.getX(),(int)target.d.getY());
//            g2.setColor(Color.DARK_GRAY);
//            g2.drawLine((int)target.d.getX(),(int)target.d.getY(),(int)target.a.getX(),(int)target.a.getY());
//        }
//
//        private void drawTarget( Graphics2D g2 ) {
//            g2.setColor(Color.RED);
//            g2.setStroke( new BasicStroke(2));
//            g2.drawLine(point0.getX(),point0.getY(),point1.getX(),point0.getY());
//            g2.drawLine(point1.getX(),point0.getY(),point1.getX(),point1.getY());
//            g2.drawLine(point1.getX(),point1.getY(),point0.getX(),point1.getY());
//            g2.drawLine(point0.getX(),point1.getY(),point0.getX(),point0.getY());
//        }
//
//        @Override
//        public void mousePressed(MouseEvent e) {
//            point0.set(e.getX(),e.getY());
//            point1.set(e.getX(),e.getY());
//            mode = 1;
//        }
//
//        @Override
//        public void mouseReleased(MouseEvent e) {
//            point1.set(e.getX(),e.getY());
//            mode = 2;
//        }
//
//        @Override public void mouseClicked(MouseEvent e) {mode = 0;}
//
//        @Override public void mouseEntered(MouseEvent e) {}
//
//        @Override public void mouseExited(MouseEvent e) {}
//
//        @Override public void mouseDragged(MouseEvent e) {
//            if( mode == 1 ) {
//                point1.set(e.getX(),e.getY());
//            }
//        }
//
//        @Override
//        public void mouseMoved(MouseEvent e) {}
//
//        public static void main(String[] args) {
//
//            ImageType<InterleavedU8> colorType = ImageType.il(3, InterleavedU8.class);
//
//            TrackerObjectQuad tracker =
//                    FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
//            //FactoryTrackerObjectQuad.sparseFlow(null,ImageUInt8.class,null);
////				FactoryTrackerObjectQuad.tld(null,ImageUInt8.class);
////				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), colorType);
////				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),colorType);
////				FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255, MeanShiftLikelihoodType.HISTOGRAM,colorType);
//
//
//            WebcamShapes app = new WebcamShapes(tracker,640,480);
//
//            app.process();
//        }
//    }


//
//    public enum WebcamTrack {
//        ;
//
//        public static void main(String[] args) {
//
//            // tune the tracker for the image size and visual appearance
//            ConfigGeneralDetector configDetector = new ConfigGeneralDetector(-1,8,1);
//            PkltConfig configKlt = new PkltConfig(3,new int[]{1,2,4,8});
//
//            PointTracker<GrayF32> tracker = FactoryPointTracker.klt(configKlt, configDetector, GrayF32.class, null);
//
//            // Open a webcam at a resolution close to 640x480
//            Webcam webcam = UtilWebcamCapture.openDefault(640, 480);
//
//            // Create the panel used to display the image and
//            ImagePanel gui = new ImagePanel();
//            gui.setPreferredSize(webcam.getViewSize());
//
//            ShowImages.showWindow(gui, "KLT Tracker");
//
//            int minimumTracks = 100;
//            //noinspection InfiniteLoopStatement
//            while( true ) {
//                BufferedImage image = webcam.getImage();
//                GrayF32 gray = ConvertBufferedImage.convertFrom(image, (GrayF32) null);
//
//                tracker.process(gray);
//
//                java.util.List<PointTrack> tracks = tracker.getActiveTracks(null);
//
//                // Spawn tracks if there are too few
//                if( tracks.size() < minimumTracks ) {
//                    tracker.spawnTracks();
//                    tracks = tracker.getActiveTracks(null);
//                    minimumTracks = tracks.size()/2;
//                }
//
//                // Draw the tracks
//                Graphics2D g2 = image.createGraphics();
//
//                for( PointTrack t : tracks ) {
//                    VisualizeFeatures.drawPoint(g2, (int) t.x, (int) t.y, Color.RED);
//                }
//
//                gui.setBufferedImageSafe(image);
//            }
//        }
//    }
}