package nars.video;

import com.github.sarxos.webcam.Webcam;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.JoglDisplay;
import spacegraph.video.VideoSource;
import spacegraph.video.VideoSurface;
import spacegraph.video.WebCam;

public class NARVideo extends NARPart {

    public NARVideo(NAR nar) {
        super(nar);

        //TODO better, with hotplug-able device selector
        Webcam.getWebcams().forEach(w -> nar.add(new Video(nar, w)));
    }

    static class Video extends NARPart {

        private VideoSource c;
        public final Webcam cam;
        volatile Surface surface;
        private volatile JoglDisplay surfaceWindow = null;

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
            surface = new VideoSurface(c);
            surfaceWindow = SpaceGraph.window(surface, 800, 600);

        }

        @Override
        protected void stopping(NAR nar) {
            synchronized (cam) {
                if (surfaceWindow != null)
                    surfaceWindow.delete();

                surface = new Gridding();
                try {
                    c.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cam.close();
            }
        }
    }
































}
