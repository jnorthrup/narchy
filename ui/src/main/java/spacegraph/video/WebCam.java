package spacegraph.video;

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
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.AspectAlign;

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
    
    
    private com.github.sarxos.webcam.Webcam webcam;

    private final Topic<WebcamEvent> eventChange = new ListTopic();






    private final static Logger logger = LoggerFactory.getLogger(WebCam.class);
    public BufferedImage image;
    


    public WebCam() {
        this(Webcam.getDefault());
    }

    public WebCam(Webcam wc) {

        logger.info("Webcam Devices: {} ", com.github.sarxos.webcam.Webcam.getWebcams());

        
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
            
            
            throw new IllegalArgumentException("Unsupported number of input bands");
        }
    }


    private Ons webcamListeners = new Ons();
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















































































    public class WebCamSurface extends AspectAlign {


        private final Tex ts;
        private On on;

        WebCamSurface(Tex ts) {
            super(ts.view(), ((float)webcam.getViewSize().getHeight()) / ((float)webcam.getViewSize().getWidth()));
            this.ts = ts;
            layout();
        }

        @Override
        public boolean start(SurfaceBase parent) {
            if (super.start(parent)) {
                on = eventChange.on(x -> {
                    if (x.getType()==WebcamEventType.CLOSED || x.getType()==WebcamEventType.DISPOSED) {
                        this.stop();
                    } else if (x.getType()==WebcamEventType.NEW_IMAGE) {
                        ts.update(image);
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

    public Surface view() {

        return new WebCamSurface(new Tex());









    }


    public static void main(String[] args) {

        final WebCam w = new WebCam();
        SpaceGraph.window(
                
                w.view(), 1200, 1200);
        



















    }

    /**
     * after webcam input
     */
    protected static BufferedImage process(BufferedImage img) {
        return img;
    }

    public void stop() {

    }




















































































































































































































































































































}