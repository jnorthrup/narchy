package nars.task;

import jcog.data.map.CompactArrayMap;
import jcog.pri.Pri;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


public class NALTask extends Pri implements Task {

    public final Term term;
    public final Truth truth;
    public final byte punc;

    private final long creation, start, end;

    private final long[] stamp;

    public volatile short[] cause = ArrayUtils.EMPTY_SHORT_ARRAY;

    final int hash;

    private final CompactArrayMap<String,Object> meta;

    private volatile boolean cyclic;

    public NALTask(Term term, byte punc, @Nullable Truthed truth, long creation, long start, long end, long[] stamp) throws InvalidTaskException {
        super();

        if (truth == null ^ (!((punc == BELIEF) || (punc == GOAL))))
            throw new InvalidTaskException(term, "null truth");

        if ((start == ETERNAL && end != ETERNAL) || (start != ETERNAL && start > end))
            throw new RuntimeException("start=" + start + ", end=" + end + " is invalid task occurrence time");

        if (Param.DEBUG_EXTRA)
            Task.validTaskTerm(term, punc, false);

        this.term = term;

        this.truth = truth!=null ? truth.truth() : null;

        this.punc = punc;

        this.start = start;
        this.end = end;
        this.creation = creation;

        this.stamp = stamp;

        this.hash = Task.hash(term, this.truth, punc, start, end, stamp);

        this.meta = new CompactArrayMap();
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
//        } else if (that instanceof Tasked) {
//
//            t = ((Tasked) that).task();
//            if (this == that) return true;
//            if (hash != that.hashCode()) return false;
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
        this.cause = Cause.sample(causeCap, this, incoming);
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
    public long[] stamp() {
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
                m.clearExcept("@");
            }
            return true;
        }
        return false;
    }

    public boolean delete(Task forwardTo) {
        return delete();

        //return delete(forwardTo.term().concept(), forwardTo.punc(), forwardTo.mid());

//        if (super.delete()) {
//            if (meta!=null) {
//                if (Param.DEBUG)
//                    meta.put("@", forwardTo);
//                else
//                    meta.clearPut("@", forwardTo);
//            }
//
//            return true;
//        }
//        return false;
    }

//    public boolean delete(Term forwardTo, byte punc, long when) {
//        return delete(new TaskLink.GeneralTaskLink(forwardTo, punc, when, 0));
//    }
//
//    public boolean delete(TaskLink forwardLink) {
//        if (super.delete()) {
//            if (meta!=null) {
//                if (Param.DEBUG)
//                    meta.put("@", forwardLink);
//                else
//                    meta.clearPut("@", forwardLink);
//            }
//
//            return true;
//        }
//        return false;
//    }

    @Override
    public String toString() {
        return appendTo(null).toString();
    }

    @Override
    public <X> X meta(String key, Function<String,Object> valueIfAbsent) {
        if (key.equals("@")) return null; //HACK filter these for now
        CompactArrayMap<String, Object> m = this.meta;
        return m != null ? (X) m.computeIfAbsent(key, valueIfAbsent) : null;
    }

    @Override
    public void meta(String key, Object value) {
        if (key.equals("@")) return; //HACK filter these for now
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
