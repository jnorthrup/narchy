package nars.audio;

import nars.NAR;
import nars.control.NARService;
import nars.term.Term;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.widget.text.Label;

import java.util.function.Supplier;

import static spacegraph.SpaceGraph.window;

/** time domain waveform input, sampled in buffers.
 * runs as a Loop at a specific FPS that fills the buffer.
 * emits event on buffer fill. */
public class WaveIn extends NARService {

    final Supplier<WaveCapture> capture;

    private WaveCapture capturing;

    private SpaceGraph surfaceWindow = null;

    /**
     * target power level, as fraction of the sample depth
     */
    float autogain = 0.5f;
    private float fps = 20f;

    WaveIn(NAR nar, Term id, Supplier<WaveCapture> capture) {
        super(null, id);
        this.capture = capture;
        nar.off(this); //default off
    }

    public Surface surface() {
        return capturing != null ? capturing.newMonitorPane() : new Label("not enabled try again"); //HACK
    }

    @Override
    protected void start(NAR x) {
        synchronized (this) {
            capturing = capture.get();
            capturing.frame.on(this::update);
            capturing.runFPS(fps);
            surfaceWindow = window(surface(), 800, 600);
        }
    }

    private void update() {
        float targetAmp = autogain;
        WaveCapture c = capturing;
        if (targetAmp == targetAmp && c != null) {
            //calculate signal peak
            float max = 0;
            for (float s : c.data) {
                max = Math.max(max, Math.abs(s));
            }
            float a = ((AudioSource) capturing.source).gain.floatValue();
            if (max <= Float.MIN_NORMAL) {
                //totally quiet
                a = 1f;
            } else {
                //HACK this is very crude
                if (max < targetAmp * 1f) {
                    a = Math.min(1000f, a + 0.1f);
                } else if (max > targetAmp * 1f) {
                    a = Math.max(0, a - 0.1f);
                }
            }
            ((AudioSource) capturing.source).gain.set(a);
        }

    }

    @Override
    protected void stopping(NAR nar) {
        synchronized (this) {
            surfaceWindow.off();
            capturing.stop();
            capturing = null;
        }
    }
}
