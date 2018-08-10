package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.task.signal.LinearTruthlet;
import nars.task.signal.SignalTask;
import nars.task.signal.SustainTruthlet;
import nars.task.signal.TruthletTask;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * Manages the creation of a stream of tasks for a changing Truth value
 * input signal
 */
@Deprecated public class Signal {


    private FloatSupplier pri;


    /**
     * quantizes the input value
     * quantization of the output value, ie. truth epsilon is separately applied according to the NAR's parameter
     */
    public final FloatSupplier resolution;







    final byte punc;

    private static final int lookAheadDurs = 0;
    private final AtomicReference<TruthletTask> current = new AtomicReference(null);


    public Signal(byte punc, FloatSupplier resolution) {
        pri(() -> 1);
        this.punc = punc;
        this.resolution = resolution;
    }

    public SignalTask get() {
        return current.get();
    }

    public Task set(Concept c, @Nullable Truth nextTruth, LongSupplier stamper, long now, int dur, NAR nar) {


        @Nullable Truth tt =
                
                nextTruth != null ? nextTruth.dither(nar) : null;

        final boolean[] keep = {false};
        Task cur = current.updateAndGet((last)-> {

            if (last == null && nextTruth == null)
                return null;


            TruthletTask next;
            int gapFillTolerance = dur * 2;
            if (tt == null) {
                
                next = null;
            } else {


                if (last == null ||
                        last.isDeleted() ||
                        ((!tt.equalsIn(last.truth(last.end()), nar) || (now - last.end() > gapFillTolerance /* fill gap up to n*dur tolerance */)) ||
                                (Param.SIGNAL_LATCH_DUR_MAX != Integer.MAX_VALUE && now - last.start() >= dur * Param.SIGNAL_LATCH_DUR_MAX)
                        )) {

                    long beforeNow = (last!=null && (now-last.end())<gapFillTolerance) ? last.end() : now;
                    
                    next = taskStart(c, last,
                            tt,
                            beforeNow, now + lookAheadDurs * dur,
                            stamper.getAsLong(), dur);

                } else {

                    next = last; 

                }


            }


            if (last == next) {
                if (last != null) {
                    last.pri(pri.asFloat());
                    last.updateEnd(c, now + lookAheadDurs * dur);
                    keep[0] = true;
                }
                return last;  
            } else {





                return next; 

            }




        });
        return keep[0] ? null : cur;
    }

    public TruthletTask taskStart(Concept c, SignalTask last, Truth t, long start, long end, long stamp, int dur) {

        

        float fNext = t.freq();























        TruthletTask s = new TruthletTask(c.term(), punc,
                new SustainTruthlet(
                        new LinearTruthlet(start, fNext, end, fNext, t.evi())
                , 1)
                ,start,stamp
        );


        s.priMax(pri.asFloat());
        return s;
    }










































    public Signal pri(FloatSupplier p) {
        this.pri = p;
        return this;
    }




}
