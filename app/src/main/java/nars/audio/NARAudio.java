package nars.audio;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.control.NARService;
import nars.control.NARServiceSet;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.widget.text.Label;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.function.Supplier;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 11/29/16.
 */
public class NARAudio extends NARServiceSet<NARAudio.AudioIn> {

    public NARAudio(NAR nar) {
        super(nar);

        //AudioSource.print();

        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();

        for (int device = 0; device < minfoSet.length; device++) {

            AudioSource audio = new AudioSource(device, 20);
            add(new AudioIn(nar, audio,
                    () -> new WaveCapture(audio)
                            //new SineSource(128),
            ));
        }

    }

    static class AudioIn extends NARService {
        public final AudioSource audio;
        public final Supplier<WaveCapture> capture;
        private WaveCapture capturing;
        private SpaceGraph surfaceWindow = null;

        /** target power level, as fraction of the sample depth */
        float autogain = 0.5f;
        private float fps = 20f;

        AudioIn(NAR nar, AudioSource audio, Supplier<WaveCapture> capture) {
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
                capturing.frame.on(this::update);
                capturing.runFPS(fps);
                surfaceWindow = window(surface(), 800, 600);
            }
        }

        private void update() {
            float targetAmp = autogain;
            WaveCapture c = capturing;
            if (targetAmp==targetAmp && c!=null) {
                //calculate signal peak
                float max = 0;
                for (float s : c.data) {
                    max = Math.max(max, Math.abs(s));
                }
                float a = ((AudioSource)capturing.source).gain.floatValue();
                if (max <= Float.MIN_NORMAL) {
                    //totally quiet
                    a = 1f;
                } else {
                    //HACK
                    if (max < targetAmp * 1f) {
                        a += 0.1f;
                    } else if (max > targetAmp * 1f) {
                        a = Math.max(0, a - 0.1f);
                    }
                }
                ((AudioSource)capturing.source).gain.set(a);
            }

        }

        @Override
        protected void stopping(NAR nar) {
            synchronized (audio) {
                surfaceWindow.off();
                capturing.stop();
                capturing = null;
            }
        }
    }

//    private Loop runFPS(float fps) {
//        return new Loop(fps) {
//
//            @Override
//            public boolean next() {
//                update();
//                return true;
//            }
//        };
//    }


    public static void main(String[] args) {
        NAR n = NARS.shell();
        new NARAudio(n).get($.p($.the("audio"), $.the(7))).start(n);
    }

}
