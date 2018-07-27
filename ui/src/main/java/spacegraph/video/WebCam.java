package spacegraph.video;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamEventType;
import com.github.sarxos.webcam.WebcamListener;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Ons;
import jcog.event.Topic;
import jcog.math.tensor.AsyncTensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.meta.ObjectSurface;

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

    private final Ons webcamListeners = new Ons();

    public final Topic<WebcamEvent> eventChange = new ListTopic();

    public final AsyncTensor tensor = new AsyncTensor();

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
        webcamListeners.add(new On() {
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
        private On on;

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

    public static void main(String[] args) {
        WebCam wc = the();
        window(new Splitting(new ObjectSurface(wc), new WebCamSurface(wc),  0.9f), 1000, 1000);
    }
}