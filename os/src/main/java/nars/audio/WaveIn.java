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

    float TARGET_GAIN = 0.5f;

    /**
     * TODO make adjustable (FloatRange)
     */
    private final float fps = 20f;

    final MiniPID autogain = new MiniPID(0.5, 0.5, 0.5);

    WaveIn(NAR nar, Term id, WaveCapture capture) {
        super(id);
        this.capture = capture;
        nar.off(this);

        if (autogain != null) {
            capture.wave.on((w) -> {

                float max = 0;
                for (float s : w.data) {
                    max = Math.max(max, Math.abs(s));
                }


                float a = (float) autogain.out(max, TARGET_GAIN /* target */);


                ((AudioSource) capture.source).gain.set(a);
            });
        }

    }


    @Override
    protected void starting(NAR x) {

        capture.frame.on(this::update);
        capture.setFPS(fps);
//        surfaceWindow = SpaceGraph.window(surface(), 800, 600);

    }

    private void update() {

    }

    @Override
    protected void stopping(NAR nar) {
        synchronized (this) {
            capture.stop();
        }
    }
}
