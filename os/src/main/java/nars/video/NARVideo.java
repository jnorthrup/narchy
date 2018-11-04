package nars.video;

import com.github.sarxos.webcam.Webcam;
import nars.$;
import nars.NAR;
import nars.control.NARService;
import nars.control.NARServiceSet;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.JoglSpace;
import spacegraph.video.WebCam;

public class NARVideo extends NARServiceSet<NARVideo.Video> {

    public NARVideo(NAR nar) {
        super(nar);

        Webcam.getWebcams().forEach(w -> add(new Video(nar, w)));
    }

    static class Video extends NARService {

        private WebCam c;
        public final Webcam cam;
        volatile Surface surface;
        volatile private JoglSpace surfaceWindow = null;

        Video(NAR nar, Webcam cam) {
            super($.p($.the("video"), $.the(cam.getName())));
            this.cam = cam;
            surface = new Gridding(); 
            nar.off(this); 
        }

        public Surface surface() {
            return surface;
        }

        @Override
        protected void starting(NAR x) {

            cam.open(true);
            c = new WebCam(cam);
            surface = new WebCam.WebCamSurface(c);
            surfaceWindow = SpaceGraph.window(surface, 800, 600);

        }

        @Override
        protected void stopping(NAR nar) {
            synchronized (cam) {
                if (surfaceWindow != null)
                    surfaceWindow.io.off();

                surface = new Gridding();
                c.stop();
                cam.close();
            }
        }
    }
































}
