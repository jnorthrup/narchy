package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.util.FloatFloatToFloatFunction;
import nars.Op;
import nars.NAL;
import nars.task.util.TaskException;
import nars.term.Term;

import static jcog.Util.assertFinite;
import static jcog.pri.op.PriReturn.Void;
import static jcog.pri.op.PriReturn.*;
import static nars.Task.i;

public abstract class AbstractTaskLink implements TaskLink {
    /**
     * source,target as a 2-ary subterm
     */
    private final Term from;
    private final Term to;
    private final int hash;
    /**
     * cached; NaN means invalidated
     */
    private volatile float pri = 0;

    protected AbstractTaskLink(Term self) {
        this(self.concept(), null);
    }

    protected AbstractTaskLink(Term source, Term target) {

        source = source.concept();
        target = target == null ? source : target.concept();

        Op so = source.op();
        if (!so.taskable)
            throw new TaskException(source, "source term not taskable");
        if (!so.conceptualizable)
            throw new TaskException(source, "source term not conceptualizable");
        if (NAL.DEBUG) {
            if (!source.isNormalized())
                throw new TaskException(source, "source term not normalized");
        }

        this.from = source;
        this.to = target;
        this.hash = Util.hashCombine(from, to);
    }

    @Override
    final public TaskLink id() {
        return this;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof TaskLink) {
            if (hashCode() == obj.hashCode()) {
                TaskLink t = (TaskLink) obj;
                if (from().equals(t.from()))
                    return to().equals(t.to());
            }
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final Term from() {
        return from;
    }

    @Override
    public final Term to() {
        return to;
    }

    @Override
    public float pri() {
        float p = this.pri;
        if (p != p)
            return this.pri = _pri(); //update cached value
        return p;
    }




    protected void invalidate() {
        this.pri = Float.NaN;
    }

    @Override
    public float take(byte punc, float howMuch) {
        return Math.max(ScalarValue.EPSILON,
                //-priMergeGetDelta(punc, howMuch, PriMerge.minus)
                -priMergeGetDelta(punc, -howMuch, PriMerge.plus)
        );
    }

    @Override
    public void delete(byte punc) {
        priSet(i(punc), 0);
    }

    @Override
    public boolean delete() {
        fill(0);
        return true;
    }

    @Override
    public final boolean isDeleted() {
        return false;
    }

    public final TaskLink priMerge(byte punc, float pri) {
        mergeComponent(punc, pri, NAL.tasklinkMerge);
        return this;
    }

    public final TaskLink priMerge(byte punc, float pri, PriMerge merge) {
        priMergeGetValue(punc, pri, merge);
        return this;
    }

    protected float priMergeGetValue(byte punc, float pri, PriMerge merge) {
        return mergeComponent(punc, pri, merge, Post);
    }

    public final float priMergeGetDelta(byte punc, float pri, PriMerge merge) {
        return mergeComponent(punc, pri, merge, Delta);
    }


    /**
     * calculates the priority from the components, to be cached until invalidation
     */
    protected final float _pri() {
        return priSum() / 4;
    }

    protected abstract float priSum();

    protected abstract float merge(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning);

    protected abstract void fill(float pri);

    public float getAndSetPriPunc(byte index, float next) {
        return merge(index, next, PriMerge.replace, Post);
    }

    @Override abstract public String toString();

    private void priSet(byte index, float next) {
        float before = merge(index, next, PriMerge.replace, Post);
        if (Math.abs(before-next) >= Float.MIN_NORMAL)
            invalidate();
    }

    @Override
    public float merge(TaskLink incoming, PriMerge merge, PriReturn returning) {


        if (incoming instanceof AtomicTaskLink) {
            switch (returning) {
                case Overflow:
                    float o = 0;
                    for (byte i = 0; i < 4; i++)
                        o += merge(i, incoming.priIndex(i), merge, Overflow);
                    return o/4;
                case Delta:
                    float delta = 0;
                    for (byte i = 0; i < 4; i++)
                        delta += merge(i, incoming.priIndex(i), merge, Delta);
                    return delta/4;
                case Void:
                    for (byte i = 0; i < 4; i++)
                        merge(i, incoming.priIndex(i), merge, Void);
                    return Float.NaN;
            }
        }

        throw new TODO();
    }

    private void mergeComponent(byte punc, float pri, PriMerge merge) {
        mergeComponent(punc, pri, merge, PriReturn.Void);
    }

    private float mergeComponent(byte punc, float pri, PriMerge merge, PriReturn returning) {
        return merge(i(punc), pri, merge, returning);
    }

    private float merge(int ith, float pri, PriMerge merge, PriReturn returning) {
        assertFinite(pri);
        float y = merge(ith, pri, merge::mergeUnitize, returning);

        if (returning == PriReturn.Delta && y == 0) {
            //no need to invalidate
        } else {
            invalidate();
        }

        return y;
    }


    @Override
    public float pri(float p) {
        throw new TODO();
    }

    @Override
    public void priAdd(float a) {
        throw new TODO();
    }

    @Override
    public float priMult(float X) {
        assertFinite(X);
        if (!Util.equals(X, 1)) {
            //HACK not fully atomic but at least consistent
            FloatFloatToFloatFunction mult = PriMerge.and::mergeUnitize;
            boolean changed = false;
            for (int i = 0; i < 4; i++) {
                float d = merge(i, X, mult, Delta);
                changed |= d!=0;
            }
            if (changed)
                invalidate();
        }
        return pri();
    }

    @Override
    public float pri(FloatFloatToFloatFunction update, float scalar) {
        throw new UnsupportedOperationException();

        //TODO make fully atomic:
//            float prev = this.pri();
//            float next = update.apply(prev, x);
//            if (next == next) {
//                next = Util.unitizeSafe(next);
//                if (prev != prev) {
//                    punc.fill(next); //flat
//                } else {
//                    if (!Util.equals(next, prev)) {
//                        //renormalize
//                        renormalize(next);
//                    }
//                }
//            } else {
//                punc.fill(0);
//                next = 0;
//            }
//            invalidate();
//            return next;

//            }, _x);

//            return y;
    }

//    /** holds value for one punctuation only */
//    public static class SimpleTaskLink extends AbstractTaskLink {
//
//        @Override
//        protected float priSum() {
//            return 0;
//        }
//
//        @Override
//        protected float merge(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning) {
//            return 0;
//        }
//
//        @Override
//        protected void fill(float pri) {
//
//        }
//
//        @Override
//        public String toString() {
//            return null;
//        }
//
//        @Override
//        public float priIndex(byte index) {
//            return 0;
//        }
//    }

}
