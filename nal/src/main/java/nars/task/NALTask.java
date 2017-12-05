package nars.task;

import jcog.Util;
import jcog.data.map.CompactArrayMap;
import jcog.pri.Pri;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 2/24/17.
 */
public class NALTask extends Pri implements Task {

    public final Term term;
    public final DiscreteTruth truth;
    public final byte punc;

    private final long creation, start, end;

    public final long[] stamp;

    /** cause zero is reserved for unknown causes, as a catch-all */
    public static final short[] UnknownCause = { 0 };

    public short[] cause = UnknownCause;

    final int hash;

    public final CompactArrayMap<String,Object> meta = new CompactArrayMap();


    public NALTask(Term term, byte punc, @Nullable Truthed truth, long creation, long start, long end, long[] stamp) throws InvalidTaskException {
        super(0 /* 0 pri by default */);

        if ((punc == BELIEF) || (punc == GOAL)) {
            if (truth == null)
                throw new InvalidTaskException(term, "null truth");
        }

        Task.validTaskTerm(term, punc, null, false);

        this.term = term;

        this.truth =
                truth == null ? null :
                        truth instanceof DiscreteTruth ?
                                ((DiscreteTruth) truth) :
                                new DiscreteTruth(truth.freq(), truth.conf());

        this.punc = punc;


        assert (start == ETERNAL && end == ETERNAL) || (start != ETERNAL && start <= end) :
                "start=" + start + ", end=" + end + " is invalid task occurrence time";

//        //ensure that a temporal task is at least as long as the contained dt.
//        //bugs and rounding off-by-N errors may produce inexact results, this corrects it.
//        if (start != ETERNAL && term.op() == CONJ) {
//            int tdt = term.dtRange();
//            if (tdt > 0) {
//                if (tdt > (end - start)) {
//                    end = start + tdt; //keeps start (left)-aligned, end is stretched if necessary
//                }
//            } else if (tdt < 0) {
//                throw new RuntimeException("dt overflow");
//            }
//        }

        this.start = start;
        this.end = end;

        //EVIDENCE STAMP
        assert (punc == COMMAND || (stamp.length > 0)) : "non-command tasks must have non-empty stamp";
        this.stamp = stamp;

        //CALCULATE HASH
        int h = Util.hashCombine(
                term.hashCode(),
                punc,
                Arrays.hashCode(stamp)
        );

        if (stamp.length > 1) {

            if (start != ETERNAL) {
                h = Util.hashCombine(
                        Long.hashCode(start),
                        Long.hashCode(end), h
                );
            }

            DiscreteTruth t = this.truth;
            if (t != null)
                h = Util.hashCombine(t.hash, h);
        }

        this.hash = h;
        this.creation = creation;

        //READY
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object that) {
        if (this == that) return true;
        if (!(that instanceof Tasked)) return false;
        Task t = ((Tasked) that).task();
        if (this == t) return true;
        if (hash != t.hashCode()) return false;
        return Task.equal(this, t);
    }


    /**
     * combine cause: should be called in all Task bags and belief tables on merge
     */
    public void causeMerge(Task incoming) {
        if (incoming == this) return;
        int causeCap = Math.min(Param.CAUSE_LIMIT, incoming.cause().length + cause().length); //TODO use NAR's?
        this.cause = Cause.zip(causeCap, this, incoming);
        Param.taskMerge.merge(this, incoming);
    }

    @Nullable
    @Override
    public final Truth truth() {
        return truth;
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public long creation() {
        return creation;
    }


    @Override
    public Term term() {
        return term;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }


    @Override
    public @NotNull long[] stamp() {
        return stamp;
    }

    @Override
    public short[] cause() {
        return cause;
    }

    @Override
    public boolean delete() {
        if (super.delete()) {
            if (Param.DEBUG) {
                //dont clear meta if debugging
            } else {
                this.meta.clearExcept("@");
            }
            return true;
        }
        return false;
    }

    public boolean delete(@NotNull Task forwardTo) {
        if (super.delete()) {
            if (Param.DEBUG)
                meta.put("@", forwardTo);
            else
                meta.clearPut("@", forwardTo);

            return true;
        }
        return false;
    }


    @NotNull
    @Override
    @Deprecated
    public String toString() {
        return appendTo(null).toString();
    }

    @Override
    public <X> X meta(String key, Function<String,Object> valueIfAbsent) {
        return (X) meta.computeIfAbsent(key, valueIfAbsent);
    }

    @Override
    public void meta(String key, Object value) {
        meta.put(key, value);
    }

    @Override
    public <X> X meta(String key) {
        return (X) meta.get(key);
    }

//    @Nullable
//    public Task project(long newStart, int dur, float confMin) {
//        float newConf = conf(newStart, dur);
//        if (newConf < confMin)
//            return null;
//
//
//        ImmutableTask t = new ImmutableTask(term, punc, $.t(freq(), newConf), creation, newStart, newStart, stamp);
//        t.setPriority(this);
//        //t.meta
//        //t.log("Projected")
//        return t;
//    }

    public Task clone(Term x) {
        Task t = new NALTask(x, punc, truth, creation, start(), end(), stamp);
        t.priSet(this);
        return t;
    }

    @Override
    public final float freq() {
        return truth.freq;
    }

    @Override
    public final float conf() {
        return truth.conf;
    }

    @Override
    public final float evi() {
        return truth.evi();
    }


    @Override
    public double coord(boolean maxOrMin, int dimension) {
        return coordF(maxOrMin, dimension);
    }

    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        switch (dimension) {
            case 0:
                return maxOrMin ? end : start;
            case 1:
                return truth.freq;
            case 2:
                return truth.conf;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public double range(int dim) {
        switch (dim) {
            case 0:
                return end - start;
            case 1:
                return 0;
            case 2:
                return 0;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * rtree cost heuristic; constant result
     */
    @Override
    public float freqCost() {
        return 1;
    }

    /**
     * rtree cost heuristic; constant result
     */
    @Override
    public float confCost() {
        return 1;
    }
}
