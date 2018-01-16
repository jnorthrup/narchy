package nars.audio;

import nars.$;
import nars.NAR;
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
public class NARAudio extends NARServiceSet {

    public NARAudio(NAR nar) {
        super(nar);

        //AudioSource.print();

        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();

        for (int device = 0; device < minfoSet.length; device++) {

            AudioSource audio = new AudioSource(device, 20);
            add(new Audio(nar, audio,
                    () -> new WaveCapture(audio,20)
                            //new SineSource(128),
            ));
        }

    }

    static class Audio extends NARService {
        public final AudioSource audio;
        public final Supplier<WaveCapture> capture;
        private WaveCapture capturing;
        private SpaceGraph surfaceWindow = null;

        Audio(NAR nar, AudioSource audio, Supplier<WaveCapture> capture) {
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
                surfaceWindow = window(surface(), 800, 600);
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

}
