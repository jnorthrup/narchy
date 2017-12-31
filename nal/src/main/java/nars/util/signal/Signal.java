package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.task.signal.*;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * Manages the creation of a stream of tasks for a changing Truth value
 * input signal
 */
public class Signal {


    FloatSupplier pri;


    /**
     * quantizes the input value
     * quantization of the output value, ie. truth epsilon is separately applied according to the NAR's parameter
     */
    public final FloatSupplier resolution;

//    public final AtomicBoolean busy = new AtomicBoolean(false);

//    boolean inputIfSame;
//    int maxTimeBetweenUpdates;
//    int minTimeBetweenUpdates;

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

//        if (!busy.compareAndSet(false, true))
//            return last;
//        try {


            TruthletTask last = this.current.get();
            if (last == null && nextTruth == null)
                return null;

            @Nullable PreciseTruth tt = nextTruth != null ? nextTruth.dither(nar) : null;


            TruthletTask next;
            if (tt == null) {
                //no signal
                next = null;
            } else {


                if (last == null ||
                        last.isDeleted() ||
                        (!last.truth(last.end()).equals(tt, nar) ||
                                (Param.SIGNAL_LATCH_TIME_MAX != Integer.MAX_VALUE && now - last.start() >= dur * Param.SIGNAL_LATCH_TIME_MAX)
                        )) {

                    //TODO move the task construction out of this critical update section?
                    next = taskStart(c, last,
                            tt,
                            now, now + lookAheadDurs * dur,
                            stamper.getAsLong(), dur);

                } else {

                    next = last; //nothing, keep as-is

                }


            }


            if (last == next) {
                if (last != null) {
                    last.pri(pri.asFloat());
                    last.updateEnd(c, now + lookAheadDurs * dur);
                }
                return null;  //dont re-input the task, just stretch it where it is in the temporal belief table
            } else {
                if (current.compareAndSet(last, next)) {
                    if (last != null) {
                        last.updateEnd(c, Math.max(last.start(), (next!=null ? now-1 : now))); //one cycle ago so as not to overlap during the new task's start time
                    }
                    return next; //new or null input; stretch will be assigned on first insert to the belief table (if this happens)
                } else
                    return get(); //the next latest
            }

//        } finally {
//            busy.set(false);
//        }

    }

    public TruthletTask taskStart(Concept c, SignalTask last, Truth t, long start, long end, long stamp, int dur) {


        boolean smooth = false;

        float fNext = t.freq();
        if (last != null) {
            //use a sloped connector to the previous task if it happens soon enough again (no temporal gap between them) and if its range is right
            long gap = Math.abs(last.end() - start);
            if ((smooth || gap > 0) && (gap <= dur / 2f)) {
                float lastTruth = last.truth(last.end(), dur).freq();
                long r = last.range();
                float fPrevEnd;
                if (smooth && (r > dur && r < dur*2))
                    fPrevEnd = fNext; //smoothing
                else
                    fPrevEnd = lastTruth;

                if (gap > 0 || Math.abs(lastTruth-fPrevEnd) >= Param.TRUTH_EPSILON) {
                    ((TruthletTask) last).update(c, (tt) -> {
                        //((LinearTruthlet)tt.truthlet).freqEnd = fNext;
                        LinearTruthlet l = (LinearTruthlet) ((((ProxyTruthlet) tt.truthlet)).ref);
                        l.freqEnd = fPrevEnd;
                        l.end = start;
                    });
                }
            }
        }

        TruthletTask s = new TruthletTask(c.term(), punc,
                    new SustainTruthlet(
                            new LinearTruthlet(start, fNext, end, fNext, t.evi()
                            ), 1)
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
