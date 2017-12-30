package nars.concept.dynamic;

import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.NALTask;
import nars.task.TimeFusion;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static nars.truth.TruthFunctions.c2wSafe;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth {

    @Nullable
    public final FasterList<Task> e;
    public Truthed truth;

    public float freq;
    public float conf; //running product

    Term term = null;

    /**
     * scratch for intermediate buffering
     */
    public List<Truth> truths = null;

    public DynTruth(FasterList<Task> e) {
        //this.t = t;
        this.e = e;
        this.truth = null;
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
    public short[] cause(NAR nar) {
        return e != null ? Cause.zip(nar.causeCapacity.intValue(), e.array(Task[]::new) /* HACK */) : ArrayUtils.EMPTY_SHORT_ARRAY;
    }


    @Nullable Truth truth(NAR nar) {
        return truth(null, false, nar);
    }

    @Nullable Truth truth(@Nullable Consumer<NALTask> withBuiltTask, boolean beliefOrGoal, NAR nar) {


        float evi = c2wSafe(conf);
        float eviMin = c2wSafe(nar.confMin.floatValue());
        if (evi < eviMin)
            return null;


        float f;
        Term c;
        long start, end;
        if (withBuiltTask != null) {
            c = this.term;
            if (!c.op().temporal) {
                if (e.size() > 1) {
                    //dilute the evidence in proportion to temporal sparseness for non-temporal results
                    TimeFusion se = TimeFusion.the(e);
                    if (se != null) {
                        evi *= se.factor;
                        if (evi < eviMin)
                            return null;
                        start = se.unionStart;
                        end = se.unionEnd;
                    } else {
                        start = end = ETERNAL;
                    }
                } else {
                    Task only = e.get(0);
                    start = only.start();
                    end = only.start();
                }
            } else {
                long min = Long.MAX_VALUE;
                for (int i = 0, thisSize = e.size(); i < thisSize; i++) {
                    long y = e.get(i).start(); //left-align
                    if (y != ETERNAL) {
                        if (y < min)
                            min = y;
                    }
                }

                if (min == Long.MAX_VALUE)
                    min = ETERNAL; //all eternal

                start = end = min;
            }

            if (c.op() == NEG) {
                c = c.unneg(); //unneg if constructing a task, but dont if just returning the truth
                f = 1f - freq;
            } else {
                f = freq;
            }
        } else {
            start = end = XTERNAL; //not used
            c = null; //not used
            f = freq;
        }


        Truth tr = Truth.the(f, evi, nar);
        if (tr == null)
            return null;
        if (withBuiltTask == null)
            return tr;

        float priority = budget();

        // then if the term is valid, see if it is valid for a task
        if (!Task.validTaskTerm(c,
                beliefOrGoal ? BELIEF : GOAL, true))
            return null;

        //TODO compute max valid overlap to terminate the zip early
        ObjectFloatPair<long[]> ss = Stamp.zip(e, Param.STAMP_CAPACITY);
//        float overlap = ss.getTwo();
//        if (overlap > 0) {
//            evi *= 1f-overlap;
////            if (evi > maxComponentEvi) {
////                evi = Util.lerp(overlap, evi, maxComponentEvi); //reduce to the maximum component evidence in proportion to the overlap
////            }
//        }
//        if (evi < eviMin)
//            return null;
        long[] stamp = ss.getOne();

        NALTask dyn = new NALTask(c, beliefOrGoal ? Op.BELIEF : Op.GOAL,
                tr, nar.time(), start, (start == ETERNAL || c.op().temporal) ? start : end, stamp);
        if (ss.getTwo() > 0) dyn.setCyclic(true);
        dyn.cause = cause(nar);
        dyn.priSet(priority);

        if (Param.DEBUG)
            dyn.log("Dynamic");

        withBuiltTask.accept(dyn);

        return tr;
    }


    void add(Task task, Truth sampled) {
        e.add(task);
    }
}
