package nars.task;

import jcog.data.map.CompactArrayMap;
import jcog.pri.Pri;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.DiscreteTruth;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.c2wSafe;


public class NALTask extends Pri implements Task {

    public final Term term;
    public final Truth truth;
    public final byte punc;

    private final long creation, start, end;

    public final long[] stamp;

//    /** cause zero is reserved for unknown causes, as a catch-all */
//    public static final short[] UnknownCause = { 0 };

    public short[] cause =
            ArrayUtils.EMPTY_SHORT_ARRAY;
            //UnknownCause;

    final int hash;

    public final CompactArrayMap<String,Object> meta;
    private boolean cyclic;


    public NALTask(Term term, byte punc, @Nullable Truthed truth, long creation, long start, long end, long[] stamp) throws InvalidTaskException {
        super(0 /* 0 pri by default */);

        if ((punc == BELIEF) || (punc == GOAL)) {
            if (truth == null)
                throw new InvalidTaskException(term, "null truth");
        }

        if (Param.DEBUG)
            Task.validTaskTerm(term, punc, false);

        this.term = term;

        this.truth = truth!=null ? truthify(truth.truth()) : null;

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

        this.hash = Task.hash(term, this.truth, punc, start, end, stamp);
        this.creation = creation;

        this.meta = new CompactArrayMap();
        //READY
    }

    /** get an appropriate representation of the truth for use as an instance in a new NALTask */
    static Truth truthify(@Nullable Truth truth) {
        if (truth instanceof PreciseTruth)
            return new DiscreteTruth(truth.freq(), truth.conf());
        else
            return truth; //already DiscreteTruth, or Truthlet
    }


    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object that) {
        if (this == that) return true;
        Task t;
        if (that instanceof Task) {
            if (hash != that.hashCode()) return false;
            t = (Task) that;
        } else if (that instanceof Tasked) {

            t = ((Tasked) that).task();
            if (this == that) return true;
            if (hash != that.hashCode()) return false;
        } else {
            return false;
        }
        return Task.equal(this, t);
    }

    @Override
    public void setCyclic(boolean c) {
        this.cyclic = c;
    }

    @Override
    public boolean isCyclic() {
        return cyclic;
    }


    /**
     * combine cause: should be called in all Task bags and belief tables on merge
     */
    public void causeMerge(Task incoming) {
        if (incoming == this) return;
        if (!Arrays.equals(cause(), incoming.cause())) {
            return; //dont merge if they are duplicates, it's pointless here
        }

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
                CompactArrayMap<String, Object> m = this.meta;
                if (m!=null)
                    m.clearExcept("@");
            }
            return true;
        }
        return false;
    }

    public boolean delete(Task forwardTo) {
        if (super.delete()) {
            if (meta!=null) {
                if (Param.DEBUG)
                    meta.put("@", forwardTo);
                else
                    meta.clearPut("@", forwardTo);
            }

            return true;
        }
        return false;
    }


    @Override
    @Deprecated
    public String toString() {
        return appendTo(null).toString();
    }

    @Override
    public <X> X meta(String key, Function<String,Object> valueIfAbsent) {
        CompactArrayMap<String, Object> m = this.meta;
        return m != null ? (X) m.computeIfAbsent(key, valueIfAbsent) : null;
    }

    @Override
    public void meta(String key, Object value) {
        CompactArrayMap<String, Object> m = this.meta;
        if (m!=null) m.put(key, value);
    }

    @Override
    public <X> X meta(String key) {
        CompactArrayMap<String, Object> m = this.meta;
        return m!=null ? (X) m.get(key) : null;
    }

    @Override
    public float freq() {
        return truth.freq();
    }

    @Override
    public float conf() {
        return truth.conf();
    }

    @Override
    public float evi() {
        return c2wSafe(conf());
    }


    @Override
    public double coord(boolean maxOrMin, int dimension) {
        return coordF(maxOrMin, dimension);
    }

    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        switch (dimension) {
            case 0:
                return maxOrMin ? end() : start();
            case 1:
                return truth.freq();
            case 2:
                return truth.conf();
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public final double range(int dim) {
        switch (dim) {
            case 0:
                return end() - start();
            case 1:
                return 0;
            case 2:
                return 0;
            default:
                throw new UnsupportedOperationException();
        }
    }


}
