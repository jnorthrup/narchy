package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAL;
import nars.term.Term;

import static jcog.Util.assertFinite;
import static jcog.pri.op.PriReturn.Void;
import static jcog.pri.op.PriReturn.*;
import static jcog.signal.tensor.AtomicFixedPoint4x16bitVector.SHORT_TO_FLOAT_SCALE;
import static nars.Task.i;

public abstract class AbstractTaskLink implements TaskLink {

    //private static final AtomicFloatFieldUpdater<AbstractTaskLink> PRI = new AtomicFloatFieldUpdater<AbstractTaskLink>(AbstractTaskLink.class, "pri");

    /**
     * source,target as a 2-ary subterm
     */
    public final Term src;
    public final Term tgt;
    public final int hash;

    static final FloatFloatToFloatFunction plus = PriMerge.plus::mergeUnitize;
    static final FloatFloatToFloatFunction mult = PriMerge.and::mergeUnitize;

    /**
     * cached; NaN means invalidated
     */
    private volatile float pri = 0;


    protected AbstractTaskLink(Term source, Term target, int hash) {
        this.src = source;
        this.tgt = target;
        this.hash = hash;
    }

    protected AbstractTaskLink(Term source, Term target) {
        this(source, target,
            //TODO construct hash as 16bit+16bit so that the short hash can be compared from external iterations
            source==target ? source.hashCode() : Util.hashCombine(source, target)
        );
    }

    @Override
    public final TaskLink id() {
        return this;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof TaskLink && hash == obj.hashCode()) {
            TaskLink t = (TaskLink) obj;
            return from().equals(t.from()) && to().equals(t.to());
        }
        return false;
    }

    @Override
    public boolean isSelf() {
        return src==tgt;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final Term from() {
        return src;
    }

    @Override
    public final Term to() {
        return tgt;
    }

    @Override
    public float pri() {
        float p = this.pri;
        if (p != p)
            return this.pri = priSum() / 4; //update cached value
        else
            return p;
    }

    protected void invalidate() {
        this.pri = Float.NaN;
    }


    @Override
    public float take(byte punc, float howMuch) {
        return Math.max(ScalarValue.EPSILON,
                -priMergeGetDelta(punc, -howMuch, PriMerge.plus)
        );
    }

    @Override
    public void delete(byte punc) {
        priSet(punc, 0);
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


    protected abstract float priSum();

    protected abstract float merge(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning);

    protected abstract void fill(float pri);

    public float getAndSetPriPunc(byte index, float next) {
        return merge(index, next, PriMerge.replace, Post);
    }

    @Override abstract public String toString();

    public final AbstractTaskLink priSet(byte punc, float puncPri) {
        if (puncPri==puncPri)
            priMerge(punc, puncPri, PriMerge.replace);
        return this;
    }
    public final AbstractTaskLink priMax(byte punc, float puncPri) {
        if (puncPri==puncPri)
            priMerge(punc, puncPri, PriMerge.max);
        return this;
    }


    @Override
    public float merge(TaskLink incoming, PriMerge merge, PriReturn returning) {


        if (incoming instanceof AtomicTaskLink) {
            switch (returning) {
                case Overflow:
                case Delta:
                    float o = 0;
                    for (byte i = 0; i < 4; i++)
                        o += merge(i, incoming.priIndex(i), merge, returning);
                    return o/4;
                case Void:
                    for (byte i = 0; i < 4; i++)
                        merge(i, incoming.priIndex(i), merge, Void);
                    return Float.NaN;
            }
        }

        throw new TODO();
    }

    protected void mergeComponent(byte punc, float pri, PriMerge merge) {
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
        //TODO fully atomic
        float e = pri();
        if (e > 1f/SHORT_TO_FLOAT_SCALE * 4) { return priMult(  p/ e); } else { fill(p); return p; }
    }

    @Override
    public void priAdd(float a) {
        throw new TODO();
    }



    @Override
    public float priMult(float X) {
        assertFinite(X);
        if (!Util.equals(X, 1)) {


            boolean changed = false;
            //HACK not fully atomic but at least consistent
            for (int i = 0; i < 4; i++)
                changed |= merge(i, X, mult, Changed) != 0;

            if (changed)
                invalidate();
        }
        return pri();
    }

    @Override
    public void priMult(float belief, float goal, float question, float quest) {
        boolean changed;
        changed = merge(0, belief, mult, Changed) != 0;
        changed |= merge(1, goal, mult, Changed) != 0;
        changed |= merge(2, question, mult, Changed) != 0;
        changed |= merge(3, quest, mult, Changed) != 0;
        if (changed)
            invalidate();
    }

//    @Override
//    public float pri(FloatFloatToFloatFunction update, float scalar) {
//        throw new UnsupportedOperationException();
//
//        //TODO make fully atomic:
////            float prev = this.pri();
////            float next = update.apply(prev, x);
////            if (next == next) {
////                next = Util.unitizeSafe(next);
////                if (prev != prev) {
////                    punc.fill(next); //flat
////                } else {
////                    if (!Util.equals(next, prev)) {
////                        //renormalize
////                        renormalize(next);
////                    }
////                }
////            } else {
////                punc.fill(0);
////                next = 0;
////            }
////            invalidate();
////            return next;
//
////            }, _x);
//
////            return y;
//    }

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
