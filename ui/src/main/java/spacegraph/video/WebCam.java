package spacegraph.video;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDevice;
import com.google.common.base.Joiner;
import jcog.Log;
import jcog.signal.Tensor;
import jcog.signal.named.RGB;
import jcog.signal.wave2d.RGBBufImgBitmap2D;
import jcog.signal.wave2d.RGBToMonoBitmap2D;
import org.slf4j.Logger;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * TODO
 * <p>
 * event listeners:
 * onClose - destroy surfaces
 */

public class WebCam extends VideoSource implements WebcamListener {

    static final Logger logger = Log.logger(WebCam.class);

    static {
        com.github.sarxos.webcam.Webcam.setAutoOpenMode(false);
    }

    public final com.github.sarxos.webcam.Webcam webcam;

    public WebCam(Webcam wc) {
        this(wc, true);
    }

    public WebCam(Webcam wc, boolean auto) /* throws WebcamException */ {

        width = height = 1;
        webcam = wc;

        //Exe.invokeLater(() -> {

        if (!webcam.isOpen()) {

            Dimension[] sizes = webcam.getViewSizes();
            webcam.setViewSize(sizes[sizes.length - 1] /* assume largest is last */);

            if (!webcam.open(auto))
                throw new RuntimeException("webcam not open");
        }


        if (auto)
            webcam.addWebcamListener(this);
        //});
    }

    /**
     * returns the default webcam, or null if none exist
     */
    public static WebCam the() {
        logger.info("Webcam Devices:\n{} ", Joiner.on("\n").join(Webcam.getDriver().getDevices()));
        logger.info("Webcams:\n{} ", Joiner.on("\n").join(com.github.sarxos.webcam.Webcam.getWebcams().stream().map(
                new Function<Webcam, String>() {
                    @Override
                    public String apply(Webcam w) {
                        return w.getName() + "\t" + Arrays.toString(w.getViewSizes());
                    }
                }).iterator()));

//        new WebcamViewer();

        Webcam defaultf = Webcam.getDefault();
        return defaultf == null ? null : new WebCam(defaultf);
    }

    public static WebCam[] theFirst(int n) {
        assert (n > 0);
        WebCam[] wc = new WebCam[n];
        int j = 0;
        synchronized (WebCam.class) {
            for (Webcam w : com.github.sarxos.webcam.Webcam.getWebcams()) {
                try {
                    if (j == 0 || !deviceName(w).equals(deviceName(wc[j - 1].webcam))) {
                        wc[j++] = new WebCam(w);
                    }
                } catch (Throwable t) {
                    //try next
                }
            }
        }
        if (j < wc.length)
            wc = Arrays.copyOf(wc, j);
        return wc;
    }

    public static String deviceName(Webcam w) {
        return ((WebcamDefaultDevice) w.getDevice()).getDeviceRef().getNameStr();
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
        public final VideoSource cam;
        public final RGB mix = new RGB(-1.0F, (float) +1);
        final RGBToMonoBitmap2D mixed = new RGBToMonoBitmap2D(mix);
        private volatile Tensor current = null;

        ChannelView(VideoSource cam) {
            this.cam = cam;

            BitmapMatrixView bmp = new BitmapMatrixView(cam.width, cam.height, new BitmapMatrixView.ViewFunction2D() {
                @Override
                public int color(int x, int y) {
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
                }
            });
            cam.tensor.on(new Consumer<RGBBufImgBitmap2D>() {
                @Override
                public void accept(RGBBufImgBitmap2D x) {
                    current = x;

                    bmp.updateIfShowing();
                }
            });

            add(bmp);
            add(new ObjectSurface(mix));
        }


        public static void main(String[] args) {
            VideoSource wc = the();

            Gridding menu = new Gridding();
            menu.add(new PushButton("++").clicked(new Runnable() {
                @Override
                public void run() {
                    SpaceGraph.window(new ChannelView(wc), 400, 400);
                }
            }));

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