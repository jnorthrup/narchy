package nars.audio;

import jcog.learn.pid.MiniPID;
import nars.NAR;
import nars.control.NARService;
import nars.term.Term;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.text.Label;
import spacegraph.video.JoglSpace;

import java.util.function.Supplier;

/** time domain waveform input, sampled in buffers.
 * runs as a Loop at a specific FPS that fills the buffer.
 * emits event on buffer fill. */
public class WaveIn extends NARService {

    final Supplier<WaveCapture> capture;

    private WaveCapture capturing;

    private JoglSpace surfaceWindow = null;

    /**
     * target power level, as fraction of the sample depth
     */
    
    private final float fps = 20f;
    final MiniPID autogain = new MiniPID(1, 0.1, 0.4);

    WaveIn(NAR nar, Term id, Supplier<WaveCapture> capture) {
        super(id);
        this.capture = capture;
        nar.off(this); 
    }

    public Surface surface() {
        return capturing != null ? capturing.view() : new Label("not enabled try again"); 
    }

    @Override
    protected void starting(NAR x) {

        capturing = capture.get();
        capturing.frame.on(this::update);
        capturing.setFPS(fps);
        surfaceWindow = SpaceGraph.window(surface(), 800, 600);

    }

    private void update() {

        WaveCapture c = capturing;
        if (autogain!=null && c != null) {
            
            float max = 0;
            for (float s : c.data) {
                max = Math.max(max, Math.abs(s));
            }

            float a = (float) autogain.out(max, 0.9f /* target */);

            











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
