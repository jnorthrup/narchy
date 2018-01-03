package nars.audio;

import com.github.sarxos.webcam.Webcam;
import jcog.Services;
import jcog.exe.Loop;
import nars.$;
import nars.NAR;
import nars.control.NARService;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.layout.Grid;
import spacegraph.widget.meter.WebCam;
import spacegraph.widget.text.Label;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.grid;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 11/29/16.
 */
public class NARAudioVideo extends NARService {


    final List<Services.Service> devices = new CopyOnWriteArrayList<>();

    static class AudioSourceService extends NARService {
        public final AudioSource audio;
        public final Supplier<WaveCapture> capture;
        private WaveCapture capturing;

        AudioSourceService(NAR nar, AudioSource audio, Supplier<WaveCapture> capture) {
            super(null, $.p($.the("audio"), $.the(audio.device)));
            this.audio = audio;
            this.capture = capture;
            nar.off(this); //default off
        }

        public Surface surface() {
            return capturing != null ? capturing.newMonitorPane() : new Label("not enabled try again"); //HACK
        }

        @Override
        protected void start(NAR x) {
            synchronized (audio) {
                capturing = capture.get();
            }
        }

        @Override
        protected void stopping(NAR nar) {
            synchronized (audio) {
                capturing.stop();
                capturing = null;
            }
        }
    }
    static class VideoSourceService extends NARService {

        private WebCam c;
        public final Webcam cam;
        Surface surface;
        private SpaceGraph surfaceWindow = null;

        VideoSourceService(NAR nar, Webcam cam) {
            super(null, $.p($.the("video"), $.the(cam.getName())));
            this.cam = cam;
            surface = new Grid(); //blank
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
                if (surfaceWindow!=null)
                    surfaceWindow.window.destroy();

                surface = new Grid();
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

    private Loop runFPS(float fps) {
        return new Loop(fps) {

            @Override
            public boolean next() {
                update();
                return true;
            }
        };
    }

    protected void update() {

    }


    public NARAudioVideo(NAR nar, float fps) {
        this(nar);
        runFPS(fps);
    }

    public NARAudioVideo(NAR nar) {
        super(nar);

        AudioSource.print();

        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();


        for (int device = 0; device < minfoSet.length; device++) {

            AudioSource audio = new AudioSource(device, 20);
            devices.add(new AudioSourceService(nar, audio,
                    () -> new WaveCapture(audio,
                            //new SineSource(128),
                            20)
            ));
        }

        Webcam.getWebcams().forEach(w -> {
            devices.add(new VideoSourceService(nar, w));
        });
    }


}
