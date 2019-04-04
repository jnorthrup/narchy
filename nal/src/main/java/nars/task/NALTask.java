package nars.task;

import jcog.data.list.FasterList;
import jcog.data.map.CompactArrayMap;
import jcog.pri.Prioritized;
import jcog.pri.UnitPri;
import jcog.pri.op.PriMerge;
import nars.Param;
import nars.Task;
import nars.control.CauseMerge;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.time.When;
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
    private final Truth truth;
    protected final byte punc;
    protected final int hash;
    public long creation;

    private volatile boolean cyclic;

    public static NALTask the(Term c, byte punct, Truth tr, When when) {
        return the(c, punct, tr, when.nar.time(), when.start, when.end, when.newStamp());
    }

    public static NALTask the(Term c, byte punct, Truth tr, When when, long[] evidence) {
        return the(c, punct, tr, when.nar.time(), when.start, when.end, evidence);
    }

    public static NALTask the(Term c, byte punct, Truth tr, long creation, long start, long end, long[] evidence) {
        if (start == ETERNAL) {
            return new EternalTask(c, punct, tr, creation, evidence);
        } else {
            return new GenericNALTask(c, punct, tr, creation, start, end, evidence);
        }
    }

    protected NALTask(Term term, byte punc, @Nullable Truth truth, long start, long end, long creation, long[] stamp) {
        super();
        this.term = term;
        this.truth = truth;
        this.punc = punc;
        this.creation = creation;
        this.hash = hashCalculate(start, end, stamp); //must be last
    }

    protected int hashCalculate(long start, long end, long[] stamp) {
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
        PriMerge.max.merge(this, ((Prioritized) incoming).pri(), PriMerge.MergeResult.Overflow);

        return causeMerge(incoming.cause(), merge);
    }

//    @Override
//    public boolean delete() {
//        return super.delete();
//    }

    /**
     * set the cause[]
     */
    public NALTask cause(short[] ignored) {
        return this;
    }

    public Task causeMerge(short[] c, CauseMerge merge) {

        int causeCap = Param.causeCapacity.intValue();

        //synchronized (this) {
            //HACK
            short[] prevCause = cause();
            short[] nextCause = merge.merge(prevCause, c, causeCap);
            if (prevCause == this.cause())
                this.cause(nextCause);
        //}

        return this;
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
    public String toString() {
        return appendTo(null).toString();
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
