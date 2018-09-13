package spacegraph.video;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamEventType;
import com.github.sarxos.webcam.WebcamListener;
import jcog.event.*;
import jcog.signal.Tensor;
import jcog.signal.named.RGB;
import jcog.signal.tensor.AsyncTensor;
import jcog.signal.wave2d.RGBBitmap2DTensor;
import jcog.signal.wave2d.RGBToGrayBitmap2DTensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Gridding;
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

    public int width;
    public int height;


    private com.github.sarxos.webcam.Webcam webcam;

    private final Offs webcamListeners = new Offs();

    public final Topic<WebcamEvent> eventChange = new ListTopic();

    public final AsyncTensor<RGBBitmap2DTensor> tensor = new AsyncTensor();

    private final static Logger logger = LoggerFactory.getLogger(WebCam.class);

    public volatile BufferedImage image;

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
                            tensor.emit(new RGBBitmap2DTensor(nextImage));
                        }
                    }
                } else {
                    width = height = 0;
                }

            }
        });


    }

    /** returns the default webcam, or null if none exist */
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


    public void on(WebcamListener wl) {
        webcamListeners.add(new AbstractOff() {
            {
                webcam.addWebcamListener(wl);
            }

            @Override
            public void off() {
                webcam.removeWebcamListener(wl);
            }
        });

    }


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
        public boolean start(SurfaceBase parent) {
            if (super.start(parent)) {
                on = webcam.eventChange.on(x -> {
                    WebcamEventType t = x.getType();
                    switch (t) {
                        case CLOSED:
                        case DISPOSED:
                            this.stop();
                            break;
                        case NEW_IMAGE:
                            ts.update(webcam.image);
                            break;
                    }
                });
                return true;
            }
            return false;
        }

        @Override
        public boolean stop() {
            if (super.stop()) {
                on.off();
                on = null;
                return true;
            }
            return false;
        }

    }

    public void stop() {

    }


    static class ChannelView extends Gridding {
        public final WebCam cam;
        private volatile Tensor current = null;

        public final RGB mix = new RGB(-1, +1);
        final RGBToGrayBitmap2DTensor mixed = new RGBToGrayBitmap2DTensor(mix);

        ChannelView(WebCam cam) {
            this.cam = cam;

            BitmapMatrixView bmp = new BitmapMatrixView(cam.width, cam.height, (x, y)->{
                Tensor c = current;
                if (c!=null) {
                    mixed.update((RGBBitmap2DTensor) current);
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

               bmp.update();
            });

            add(bmp);
            add(new ObjectSurface<>(mix));
        }
    }

    public static void main(String[] args) {
        WebCam wc = the();

        Gridding menu =new Gridding();
        menu.add(new PushButton("++").click(()->{
            window(new ChannelView(wc), 400, 400);
        }));

        window(new Splitting(new Gridding(menu, new ObjectSurface(wc)), new WebCamSurface(wc),  0.9f), 1000, 1000);
    }

}