package spacegraph.video;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import jcog.exe.Exe;
import jcog.signal.Tensor;
import jcog.signal.named.RGB;
import jcog.signal.wave2d.RGBBufImgBitmap2D;
import jcog.signal.wave2d.RGBToMonoBitmap2D;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;


/**
 * TODO
 * <p>
 * event listeners:
 * onClose - destroy surfaces
 */

public class WebCam extends VideoSource implements WebcamListener {

    private final com.github.sarxos.webcam.Webcam webcam;


    /**
     * TODO async load
     */
    public WebCam(Webcam wc) {

        width = height = 1;
        webcam = wc;

        Exe.invokeLater(() -> {
            if (!webcam.open(true))
                throw new RuntimeException("webcam not open");

            webcam.addWebcamListener(this);
        });
    }

    /**
     * returns the default webcam, or null if none exist
     */
    public static WebCam the() {
        //logger.info("Webcam Devices: {} ", com.github.sarxos.webcam.Webcam.getWebcams());
        Webcam defaultf = Webcam.getDefault();
        return defaultf == null ? null : new WebCam(defaultf);
    }


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


        if (!tensor.isEmpty() && (setAndGet(we.getImage())) != null) {
        } /*else {
                width = height = 1;
            }*/

    }

    @Override
    public void close() {
        webcam.close();
    }

    static class ChannelView extends Gridding {
        public final WebCam cam;
        public final RGB mix = new RGB(-1, +1);
        final RGBToMonoBitmap2D mixed = new RGBToMonoBitmap2D(mix);
        private volatile Tensor current = null;

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


        public static void main(String[] args) {
            WebCam wc = the();

            Gridding menu = new Gridding();
            menu.add(new PushButton("++").clicked(() -> SpaceGraph.window(new ChannelView(wc), 400, 400)));

            SpaceGraph.window(new Splitting(new Gridding(menu, new ObjectSurface(wc)), 0.9f, new VideoSurface(wc)), 1000, 1000);
        }
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
}