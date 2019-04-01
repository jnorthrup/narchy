package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.Op;
import nars.Param;
import nars.task.util.TaskException;
import nars.term.Term;

import static jcog.Util.assertFinite;
import static nars.Task.i;

public abstract class AbstractTaskLink implements TaskLink {
    /**
     * source,target as a 2-ary subterm
     */
    final Term from, to;
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
        if (Param.DEBUG) {
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
        mergeComponent(punc, pri, Param.tasklinkMerge, true);
        return this;
    }

    public final TaskLink priMerge(byte punc, float pri, PriMerge merge) {
        priMergeGetValue(punc, pri, merge);
        return this;
    }

    protected float priMergeGetValue(byte punc, float pri, PriMerge merge) {
        return mergeComponent(punc, pri, merge, true);
    }

    public final float priMergeGetDelta(byte punc, float pri, PriMerge merge) {
        return mergeComponent(punc, pri, merge, false);
    }


    /**
     * calculates the priority from the components, to be cached until invalidation
     */
    protected final float _pri() {
        return priSum() / 4;
    }

    protected abstract float priSum();

    protected abstract float merge(int ith, float pri, FloatFloatToFloatFunction componentMerge, boolean valueOrDelta);

    protected abstract void fill(float pri);

    public float getAndSetPriPunc(byte punc, float next) {
        return mergeComponent(punc, next, PriMerge.replace, true);
    }

    @Override abstract public String toString();
    public final void priSet(byte punc, float pri) {
        priMergeGetValue(punc, pri, PriMerge.replace);
    }

    @Override
    public /* HACK */ float merge(TaskLink incoming, PriMerge merge) {
        if (incoming instanceof AtomicTaskLink) {
            float delta = 0;
            for (byte i = 0; i < 4; i++) {
                float p = incoming.priIndex(i);
                delta += merge(i, p, merge, false);
            }
            return delta / 4;
        } else {
            throw new TODO();
        }
    }

    private float mergeComponent(byte punc, float pri, PriMerge merge, boolean valueOrDelta) {
        return merge(i(punc), pri, merge, valueOrDelta);
    }

    private float merge(int ith, float pri, PriMerge merge, boolean valueOrDelta) {
        assertFinite(pri);
        float y = merge(ith, pri, merge::mergeUnitize, valueOrDelta);
        if (valueOrDelta || y != 0)
            invalidate();
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
                float d = merge(i, X, mult, false);
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



}
