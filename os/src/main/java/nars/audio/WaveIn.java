package nars.audio;

import jcog.learn.pid.MiniPID;
import nars.NAR;
import nars.control.NARService;
import nars.term.Term;
import spacegraph.audio.AudioBuffer;

/**
 * time domain waveform input, sampled in buffers.
 * runs as a Loop at a specific FPS that fills the buffer.
 * emits event on buffer fill.
 *
 */
public class WaveIn extends NARService {

    final AudioBuffer in;

    float TARGET_GAIN = 0.5f;

    final MiniPID autogain = new MiniPID(0.5, 0.5, 0.5);

    WaveIn(Term id, AudioBuffer in) {
        super(id);
        this.in = in;

        if (autogain != null) {
            in.wave.on((w) -> {

                float max = 0;
                for (float s : w.data) {
                    max = Math.max(max, Math.abs(s));
                }


                float a = (float) autogain.out(max, TARGET_GAIN /* target */);


                in.source().gain.set(a);
            });
        }

    }


    @Override
    protected void starting(NAR x) {

        in.setPeriodMS(0); //run ASAP


    }


    @Override
    protected void stopping(NAR nar) {
        synchronized (this) {
            in.stop();
        }
    }
}
