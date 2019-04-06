package nars.video;

import com.github.sarxos.webcam.Webcam;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.JoglDisplay;
import spacegraph.video.WebCam;

public class NARVideo extends NARPart {

    public NARVideo(NAR nar) {
        super(nar);

        //TODO better, with hotplug-able device selector
        Webcam.getWebcams().forEach(w -> nar.start(new Video(nar, w)));
    }

    static class Video extends NARPart {

        private WebCam c;
        public final Webcam cam;
        volatile Surface surface;
        volatile private JoglDisplay surfaceWindow = null;

        Video(NAR nar, Webcam cam) {
            super($.p($.the("video"), $.the(cam.getName())));
            this.cam = cam;
            surface = new Gridding(); 
            //nar.remove(this);
        }

        public Surface surface() {
            return surface;
        }

        @Override
        protected void starting(NAR x) {

            cam.open(true);
            c = new WebCam(cam);
            surface = new WebCam.WebCamSurface(c);
            surfaceWindow = SpaceGraph.surfaceWindow(surface, 800, 600);

        }

        @Override
        protected void stopping(NAR nar) {
            synchronized (cam) {
                if (surfaceWindow != null)
                    surfaceWindow.video.off();

                surface = new Gridding();
                c.stop();
                cam.close();
            }
        }
    }
































}
