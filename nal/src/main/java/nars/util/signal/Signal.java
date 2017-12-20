package nars.util.signal;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.signal.LinearTruthlet;
import nars.task.signal.RangeTruthlet;
import nars.task.signal.SignalTask;
import nars.task.signal.TruthletTask;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

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
    private SignalTask last;


    public Signal(byte punc, FloatSupplier resolution) {
        pri(() -> 1);
        this.punc = punc;
        this.resolution = resolution;
    }

    public SignalTask get() {
        return last;
    }

    public Task set(Term term, @Nullable Truth nextTruth, LongSupplier stamper, long now, int dur, NAR nar) {

//        if (!busy.compareAndSet(false, true))
//            return last;
//        try {
        synchronized (resolution) {

            SignalTask last = this.last;
            if (last == null && nextTruth == null)
                return null;

            @Nullable PreciseTruth tt = nextTruth != null ? nextTruth.dither(nar) : null;


            SignalTask next;
            if (tt == null) {
                //no signal
                next = null;
            } else {


                if (last == null ||
                        last.isDeleted() ||
                        (!last.truth.equals(tt, nar.freqResolution.floatValue()) ||
                                (Param.SIGNAL_LATCH_TIME_MAX != Integer.MAX_VALUE && now - last.start() >= dur * Param.SIGNAL_LATCH_TIME_MAX)
                        )) {

                    //TODO move the task construction out of this critical update section?
                    next = taskStart(last,
                            term, tt,
                            now, now + lookAheadDurs * dur,
                            stamper.getAsLong());

                } else {

                    next = last; //nothing, keep as-is

                }


            }


            if (last == next) {
                if (last != null) {
                    last.pri(pri.asFloat());
                    last.grow(now);
                }
                return null;  //dont re-input the task, just stretch it where it is in the temporal belief table
            } else {
                if (last != null) {
                    last.end(Math.max(last.start(), (next!=null ? now-1 : now))); //one cycle ago so as not to overlap during the new task's start time
                }
                return this.last = next; //new or null input; stretch will be assigned on first insert to the belief table (if this happens)
            }

//        } finally {
//            busy.set(false);
//        }
        }
    }

    public SignalTask taskStart(SignalTask last, Term term, Truth t, long start, long end, long stamp) {


        float fStart;
        float fNext = t.freq();
        if (last == null || Math.abs(last.end() - start) > 1) {
            fStart = fNext;
        } else {
            Truth le = last.truth(last.end(), 1);
            if (le != null) {
                fStart = le.freq();
                ((LinearTruthlet)(((TruthletTask)last).truthlet)).freqEnd = fNext;
            } else fStart = fNext;
        }

        SignalTask s = new TruthletTask(term, punc,
                    new LinearTruthlet(start, fStart, end, fNext, t.evi()),
                    stamp);

        s.priMax(pri.asFloat());
        return s;
    }

    public Signal pri(FloatSupplier p) {
        this.pri = p;
        return this;
    }




}
