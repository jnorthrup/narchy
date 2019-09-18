package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.util.FloatFloatToFloatFunction;
import nars.Op;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.assertFinite;
import static jcog.pri.op.PriReturn.Void;
import static jcog.pri.op.PriReturn.*;
import static nars.Task.i;

public abstract class AbstractTaskLink implements TaskLink {

    //private static final AtomicFloatFieldUpdater<AbstractTaskLink> PRI = new AtomicFloatFieldUpdater<AbstractTaskLink>(AbstractTaskLink.class, "pri");

    /**
     * source,target as a 2-ary subterm
     */
    public final Term from;
    public final Term to;
    public final int hash;

    static final FloatFloatToFloatFunction plus = PriMerge.plus::mergeUnitize;
    static final FloatFloatToFloatFunction mult = PriMerge.and::mergeUnitize;

    /**
     * cached; NaN means invalidated
     */
    private volatile float pri = 0;


    protected AbstractTaskLink(Term source, Term target, int hash) {
        this.from = source;
        this.to = target;
        this.hash = hash;
    }

    protected AbstractTaskLink(Term source, Term target) {
        this(source, Util.maybeEqual(source,target),
            //TODO construct hash as 16bit+16bit so that the short hash can be compared from external iterations
            hash(source, target)
        );

        if (source instanceof Bool)
            throw new TermException("source bool", source);
        if (target instanceof Bool)
            throw new TermException("target bool", target);

        Op so = source.op();
        if (!so.taskable)
            throw new TaskException(source, "source term not taskable");
        if (!so.conceptualizable)
            throw new TaskException(source, "source term not conceptualizable");
//        if (NAL.DEBUG) {
//            if (!source.isNormalized())
//                throw new TaskException(source, "source term not normalized and can not name a task");
//        }
    }

    private static int hash(Term source, Term target) {
        int s = source.hashCodeShort();
        int t = source!=target ? target.hashCodeShort() : s;
        int hash = (t << 16) | s;
//        if (t!=(hash >>> 16))
//            throw new WTF();
//        if (s!=(hash & 0xffff))
//            throw new WTF();
        return hash;
    }

    @Override
    public @Nullable final Term other(Term x, int xHashShort, boolean reverse) {
        //return x.equals(reverse ? to() : from()) ? (reverse ? from() : to()) : null;
        boolean hashMatch = xHashShort == (reverse ? toHash() : fromHash());
//        if ((other(x,reverse) !=null ) != hashMatch)
//            throw new WTF();
        return hashMatch ? other(x, reverse) : null;
    }



    public final int toHash() {
        return (hash >>> 16);
    }
    public final int fromHash() {
        return (hash & 0xffff);
    }

    @Override
    public final boolean isSelf() {
        return from == to;
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
            return from.equals(t.from()) && to.equals(t.to());
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

    /** merge a component; used internally.  does not invalidate so use the high-level methods like merge() */
    protected abstract float apply(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning);

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

    protected float merge(int ith, float pri, PriMerge merge, PriReturn returning) {
        assertFinite(pri);
        float y = apply(ith, pri, merge::mergeUnitize, returning);

        if (returning == PriReturn.Delta && y == 0) {
            //no need to invalidate
        } else {
            invalidate();
        }

        return y;
    }


    @Override
    public AbstractTaskLink pri(float p) {
//        //TODO fully atomic
//        float e = pri();
//        priMult(  p/ e); } else { fill(p);  }
//        return this;
        throw new UnsupportedOperationException();
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
                changed |= apply(i, X, mult, Changed) != 0;

            if (changed)
                invalidate();
        }
        return pri();
    }

    @Override
    public void priMult(float belief, float goal, float question, float quest) {
        boolean changed;
        changed = apply(0, belief, mult, Changed) != 0;
        changed |= apply(1, goal, mult, Changed) != 0;
        changed |= apply(2, question, mult, Changed) != 0;
        changed |= apply(3, quest, mult, Changed) != 0;
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
