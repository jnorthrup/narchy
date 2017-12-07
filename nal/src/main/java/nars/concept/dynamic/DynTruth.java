package nars.concept.dynamic;

import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth implements Truthed {

    @Nullable
    public final FasterList<Task> e;
    public Truthed truth;

    public float freq;
    public float conf; //running product

    final Term template;
    Term concrete = null;

    public DynTruth(Term template, FasterList<Task> e) {
        //this.t = t;
        this.e = e;
        this.truth = null;
        this.template = template;
    }

    public void setTruth(Truthed truth) {
        this.truth = truth;
    }

    public float budget() {

        int s = e.size();
        assert (s > 0);

        if (s > 1) {
            //float f = 1f / s;
            //            for (Task x : e) {
            //                BudgetMerge.plusBlend.apply(b, x.budget(), f);
            //            }
            //            return b;
            //return e.maxValue(Task::priElseZero); //use the maximum of their truths
            return e.meanValue(Task::priElseZero); //average value
        } else {
            return e.get(0).priElseZero();
        }
    }

    @Nullable
    public long[] evidence() {
        return Stamp.zip(e.array(Stamp[]::new), Param.STAMP_CAPACITY);
    }

    @Nullable
    public short[] cause(NAR nar) {
        return e != null ? Cause.zip(nar.causeCapacity.intValue(), e.array(Task[]::new) /* HACK */) : ArrayUtils.EMPTY_SHORT_ARRAY;
    }

    @Override
    @Nullable
    public PreciseTruth truth() {
        return conf == conf && conf <= 0 ? null : new PreciseTruth(freq, conf);
    }


    NALTask task(boolean beliefOrGoal, NAR nar) {

        Term c = this.concrete;

        Truth tr0 = truth();
        if (tr0 == null)
            return null;

        if (c.op() == NEG) {
            c = c.unneg();
            tr0 = tr0.neg();
        }

        long[] se = Task.range(e);
        long start = se[0];
        long end = se[1];
        long eviRange = end - start;
        int termRange = c.dtRange();

        float rangeCoherence = eviRange==termRange ? 1f :
                1f - ((float)Math.abs(eviRange - termRange))/Math.max(eviRange, termRange)/nar.dur();

        Truth tr = tr0.dither(nar, rangeCoherence);
        if (tr == null)
            return null;

        float priority = budget();


        // then if the term is valid, see if it is valid for a task
        if (!Task.validTaskTerm(c,
                beliefOrGoal ? BELIEF : GOAL, true))
            return null;

        NALTask dyn = new NALTask(c, beliefOrGoal ? Op.BELIEF : Op.GOAL,
                tr, nar.time(), start, end /*+ dur*/, evidence());
        dyn.cause = cause(nar);
        dyn.priSet(priority);

        if (Param.DEBUG)
            dyn.log("Dynamic");

        return dyn;
    }
}
