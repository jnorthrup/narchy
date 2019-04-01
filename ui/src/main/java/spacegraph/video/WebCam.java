package spacegraph.video;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamEventType;
import com.github.sarxos.webcam.WebcamListener;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.signal.Tensor;
import jcog.signal.named.RGB;
import jcog.signal.tensor.TensorTopic;
import jcog.signal.wave2d.RGBBufImgBitmap2D;
import jcog.signal.wave2d.RGBToMonoBitmap2D;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import java.awt.*;
import java.awt.image.BufferedImage;

import static spacegraph.SpaceGraph.window;


/**
 * TODO
 * <p>
 * event listeners:
 * onClose - destroy surfaces
 */

public class WebCam {

    public int width = 0;
    public int height = 0;

    private final com.github.sarxos.webcam.Webcam webcam;

//    private final Offs webcamListeners = new Offs();

    public final Topic<WebcamEvent> eventChange = new ListTopic();

    public final TensorTopic<RGBBufImgBitmap2D> tensor = new TensorTopic();

//    private final static Logger logger = LoggerFactory.getLogger(WebCam.class);

    public volatile BufferedImage image;

    /** TODO async load */
    public WebCam(Webcam wc) {


        //logger.info("Webcam Devices: {} ", com.github.sarxos.webcam.Webcam.getWebcams());

        webcam = wc;
        if (!webcam.open(true))
            throw new RuntimeException("webcam not open");

        Dimension dim = webcam.getViewSize();
        width = (int) dim.getWidth();
        height = (int) dim.getHeight();


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


                        WebCam.this.image = nextImage;


                        eventChange.emit(we);

                        if (!tensor.isEmpty()) {
                            tensor.emit(new RGBBufImgBitmap2D(nextImage));
                        }
                    }
                } else {
                    width = height = 0;
                }

            }
        });


    }

    /**
     * returns the default webcam, or null if none exist
     */
    public static WebCam the() {
        Webcam defaultf = Webcam.getDefault();
        return defaultf == null ? null : new WebCam(defaultf);
    }

//    public static void convertFromInterleaved(BufferedImage src, InterleavedU8 dst) {
//
//        final int width = src.getWidth();
//        final int height = src.getHeight();
//
//        if (dst.getNumBands() == 3) {
//
//            byte[] dd = dst.data;
//            for (int y = 0; y < height; y++) {
//                int indexDst = dst.startIndex + y * dst.stride;
//                for (int x = 0; x < width; x++) {
//                    int argb = src.getRGB(x, y);
//
//                    dd[indexDst++] = (byte) (argb >>> 16);
//                    dd[indexDst++] = (byte) (argb >>> 8);
//                    dd[indexDst++] = (byte) argb;
//                }
//            }
//        } else if (dst.getNumBands() == 1) {
//
//
//            throw new IllegalArgumentException("Unsupported number of input bands");
//        }
//    }


//    public void on(WebcamListener wl) {
//        webcamListeners.add(new AbstractOff() {
//            {
//                webcam.addWebcamListener(wl);
//            }
//
//            @Override
//            public void off() {
//                webcam.removeWebcamListener(wl);
//            }
//        });
//
//    }


    static public class WebCamSurface extends AspectAlign {


        private final Tex ts;
        private final WebCam webcam;
        private Off on;

        public WebCamSurface(WebCam wc) {
            this(wc, new Tex());
        }

        WebCamSurface(WebCam webcam, Tex ts) {
            super(ts.view(), ((float) webcam.webcam.getViewSize().getHeight()) / ((float) webcam.webcam.getViewSize().getWidth()));
            this.ts = ts;
            this.webcam = webcam;
        }

        @Override
        protected void starting() {
            super.starting();
            on = webcam.eventChange.on(x -> {
                WebcamEventType t = x.getType();
                switch (t) {
                    case CLOSED:
                    case DISPOSED:
                        this.stop();
                        break;
                    case NEW_IMAGE:
                        ts.set(webcam.image);

                        break;
                }
            });
        }

        @Override
        protected void stopping() {
            on.off();
            on = null;
            super.stopping();
        }


    }

    public void stop() {

    }


    static class ChannelView extends Gridding {
        public final WebCam cam;
        private volatile Tensor current = null;

        public final RGB mix = new RGB(-1, +1);
        final RGBToMonoBitmap2D mixed = new RGBToMonoBitmap2D(mix);

        ChannelView(WebCam cam) {
            this.cam = cam;

            BitmapMatrixView bmp = new BitmapMatrixView(cam.width, cam.height, (x, y) -> {
                Tensor c = current;
                if (c != null) {
                    mixed.update((RGBBufImgBitmap2D) current);
                    float intensity = //c.get(x, y, channel);
                            mixed.get(x, y);
                    return Draw.rgbInt(intensity, intensity, intensity);
//                    switch (channel) {
//                        case 0:
//                            return Draw.rgbInt(intensity, 0, 0);
//                        case 1:
//                            return Draw.rgbInt(0, intensity, 0);
//                        case 2:
//                            return Draw.rgbInt(0, 0, intensity);
//                        default:
//                            throw new UnsupportedOperationException();
//
//                    }
                }
                return 0;
            });
            cam.tensor.on(x -> {
                current = x;

                bmp.updateIfShowing();
            });

            add(bmp);
            add(new ObjectSurface<>(mix));
        }
    }

    public static void main(String[] args) {
        WebCam wc = the();

        Gridding menu = new Gridding();
        menu.add(new PushButton("++").clicking(() -> {
            window(new ChannelView(wc), 400, 400);
        }));

        window(new Splitting(new Gridding(menu, new ObjectSurface(wc)), 0.9f, new WebCamSurface(wc)), 1000, 1000);
    }

}