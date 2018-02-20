package nars.video;

import com.github.sarxos.webcam.Webcam;
import nars.$;
import nars.NAR;
import nars.control.NARService;
import nars.control.NARServiceSet;
import spacegraph.Surface;
import spacegraph.layout.Gridding;
import spacegraph.render.JoglSpace;
import spacegraph.widget.meter.WebCam;

import static spacegraph.render.JoglPhysics.window;

public class NARVideo extends NARServiceSet<NARVideo.Video> {

    public NARVideo(NAR nar) {
        super(nar);

        Webcam.getWebcams().forEach(w -> add(new Video(nar, w)));
    }

    static class Video extends NARService {

        private WebCam c;
        public final Webcam cam;
        Surface surface;
        private JoglSpace surfaceWindow = null;

        Video(NAR nar, Webcam cam) {
            super($.p($.the("video"), $.the(cam.getName())));
            this.cam = cam;
            surface = new Gridding(); //blank
            nar.off(this); //default off
        }

        public Surface surface() {
            return surface;
        }

        @Override
        protected void start(NAR x) {
            synchronized (cam) {
                cam.open(true);
                c = new WebCam(cam);
                surface = c.surface();
                surfaceWindow = window(surface, 800, 600);
            }
        }

        @Override
        protected void stopping(NAR nar) {
            synchronized (cam) {
                if (surfaceWindow != null)
                    surfaceWindow.off();

                surface = new Gridding();
                c.stop();
                cam.close();
            }
        }
    }

//    public static void main(String[] args) {
//
//        //init();
//
//        NAR n = NARS.tmp();
//        n.log();
//        NARHear a = new NARHear(n);
//        a.runFPS(1f);
//        Loop loop = n.startFPS(10);
//
//        SpaceGraph.window(
//                grid(
//                        row(
//                                a.devices.get(7).surface()
//                        )
////                    new MatrixView(ae.xx, (v, gl) -> { Draw.colorBipolar(gl, v); return 0; }),
////                    new MatrixView(ae.y, (v, gl) -> { Draw.colorBipolar(gl, v); return 0; })
//                        //new MatrixView(ae.W.length, ae.W[0].length, MatrixView.arrayRenderer(ae.W)),
//                        //Vis.conceptLinePlot(nar, freqInputs, 64)
//                ),
//                1200, 1200);
//
////        this.loop = nar.exe.loop(fps, () -> {
////            if (enabled.get()) {
////                this.now = nar.time();
////                senseAndMotor();
////                predict();
////            }
////        });
//
//    }
}
