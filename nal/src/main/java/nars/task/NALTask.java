package nars.task;

import jcog.data.list.FasterList;
import jcog.data.map.CompactArrayMap;
import jcog.pri.UnitPri;
import jcog.pri.op.PriMerge;
import jcog.util.ArrayUtils;
import nars.Param;
import nars.Task;
import nars.control.CauseMerge;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * generic immutable Task implementation,
 * with mutable cause[] and initially empty meta table
 */
public abstract class NALTask extends UnitPri implements Task {
    protected final Term term;
    protected final Truth truth;
    protected final byte punc;
    protected final int hash;
    /*@Stable*/ protected final long[] stamp;
    public long creation;
    private /*volatile*/ short[] cause = ArrayUtils.EMPTY_SHORT_ARRAY;
    private volatile boolean cyclic;

    public static NALTask the(Term c, byte punct, Truth tr, long creation, long start, long end, long[] evidence) {
        if (start == ETERNAL) {
            return new EternalTask(c, punct, tr, creation, evidence);
        } else {
            return new GenericNALTask(c, punct, tr, creation, start, end, evidence);
        }
    }

    protected NALTask(Term term, byte punc, @Nullable Truth truth, long start, long end, long[] stamp, long creation) {
        super();
        this.term = term;
        this.truth = truth;
        this.punc = punc;
        this.creation = creation;
        this.stamp = stamp;
        this.hash = hashCalculate(start, end); //must be last
    }

    protected int hashCalculate(long start, long end) {
        return Task.hash(
                term,
                truth,
                punc,
                start, end, stamp);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object that) {
        return Task.equal(this, that);
    }

    @Override
    public boolean isCyclic() {
        return cyclic;
    }

    @Override
    public void setCyclic(boolean c) {
        this.cyclic = c;
    }

    /**
     * combine cause: should be called in all Task bags and belief tables on merge
     */
    public Task priCauseMerge(Task incoming, CauseMerge merge) {
        if (incoming == this) return this;

        /*(incoming.isInput() ? PriMerge.replace : Param.taskEquivalentMerge).*/
        PriMerge.max.merge(this, incoming);

        return causeMerge(incoming.cause(), merge);
    }

    public Task causeMerge(short[] c, CauseMerge merge) {

        int causeCap = Param.causeCapacity.intValue();

        //synchronized (this) {
            //HACK
            short[] prevCause = cause();
            short[] nextCause = merge.merge(prevCause, c, causeCap);
            if (prevCause == this.cause) //to avoid needing to synchronize, check again if the same previous result is still there.  otherwise just give up (but could try again)
                this.cause = nextCause;
        //}

        return this;
    }

    @Override
    public float freq(long start, long end) {
        return truth.freq();
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
    public abstract long start();

    @Override
    public void setCreation(long nextCreation) {
        creation = nextCreation;
    }

    @Override
    public abstract long end();

    @Override
    public long[] stamp() {
        return stamp;
    }

    @Override
    public short[] cause() {
        return cause;
    }

    /**
     * set the cause[]
     */
    public NALTask cause(short[] cause) {
        this.cause = cause;
        return this;
    }

    @Override
    public String toString() {
        return appendTo(null).toString();
    }

    @Override
    public float coordF(int dimension, boolean maxOrMin) {
        switch (dimension) {
            case 0:
                return maxOrMin ? end() : start();
            case 1:
                return freq();
            case 2:
                return conf();
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
            case 2:
                return 0;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * extended: with meta table
     */
    public static class NALTaskX extends GenericNALTask implements jcog.data.map.MetaMap {

        private final CompactArrayMap<String, Object> meta = new CompactArrayMap<>();

        NALTaskX(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) throws TaskException {
            super(term, punc, truth, creation, start, end, stamp);
        }

        @Override
        public @Nullable List log(boolean createIfMissing) {
            if (createIfMissing)
                return meta("!", (x) -> new FasterList(1));
            else
                return meta("!");
        }

        @Override
        public <X> X meta(String key, Function<String, X> valueIfAbsent) {
            CompactArrayMap<String, Object> m = this.meta;
            return m != null ? (X) m.computeIfAbsent(key, valueIfAbsent) : null;
        }

        @Override
        public Object meta(String key, Object value) {
            CompactArrayMap<String, Object> m = this.meta;
            if (m != null) m.put(key, value);
            return value;
        }

        @Override
        public <X> X meta(String key) {
            CompactArrayMap<String, Object> m = this.meta;
            return m != null ? (X) m.get(key) : null;
        }
    }
}
