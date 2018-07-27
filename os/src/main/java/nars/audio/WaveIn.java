package nars.audio;

import jcog.learn.pid.MiniPID;
import nars.NAR;
import nars.control.NARService;
import nars.term.Term;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;

/**
 * time domain waveform input, sampled in buffers.
 * runs as a Loop at a specific FPS that fills the buffer.
 * emits event on buffer fill.
 */
public class WaveIn extends NARService {

    final WaveCapture capture;


    /**
     * TODO make adjustable (FloatRange)
     */
    private final float fps = 20f;

    final MiniPID autogain = new MiniPID(1, 0.1, 0.4);

    WaveIn(NAR nar, Term id, WaveCapture capture) {
        super(id);
        this.capture = capture;
        nar.off(this);
    }


    @Override
    protected void starting(NAR x) {

        capture.frame.on(this::update);
        capture.setFPS(fps);
//        surfaceWindow = SpaceGraph.window(surface(), 800, 600);

    }

    private void update() {

        WaveCapture c = capture;
        if (autogain != null && c != null) {

            float max = 0;
            for (float s : c.data) {
                max = Math.max(max, Math.abs(s));
            }

            float a = (float) autogain.out(max, 0.9f /* target */);


            ((AudioSource) c.source).gain.set(a);
        }

    }

    @Override
    protected void stopping(NAR nar) {
        synchronized (this) {
            capture.stop();
        }
    }
}
