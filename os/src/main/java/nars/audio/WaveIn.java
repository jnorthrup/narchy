package nars.audio;

import jcog.math.FloatRange;
import jcog.signal.wave1d.SignalReading;
import nars.NAR;
import nars.control.NARService;
import nars.term.Term;

/**
 * time domain waveform input, sampled in buffers.
 * runs as a Loop at a specific FPS that fills the buffer.
 * emits event on buffer fill.
 *
 */
public class WaveIn extends NARService {

    final SignalReading in;

    /** updates per time unit */
    private final FloatRange rate = new FloatRange(30, 0.5f, 120);


    WaveIn(Term id, SignalReading in, float rate) {
        super(id);
        this.in = in;
        this.rate.set(rate);
    }

    @Override
    protected void stopping(NAR nar) {
        in.stop();
        super.stopping(nar);
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        in.setPeriodMS(Math.round(1000f/rate.floatValue()));
    }

}
//    float TARGET_GAIN = 0.5f;
//
//    final MiniPID autogain = new MiniPID(0.5, 0.5, 0.5);
//
//        if (autogain != null) {
//            in.wave.on((w) -> {
//
//                float max = 0;
//                for (float s : w.data) {
//                    max = Math.max(max, Math.abs(s));
//                }
//
//
//                float a = (float) autogain.out(max, TARGET_GAIN /* target */);
//
//
//                in.gain.set(a);
//            });
//        }