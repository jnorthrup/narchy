package nars.concept.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.Prioritized;
import nars.*;
import nars.control.Cause;
import nars.task.NALTask;
import nars.task.TimeFusion;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
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
public final class DynTruth extends FasterList<TaskRegion> implements Prioritized, TaskRegion {

    public DynTruth() {
        super();
    }

    public float pri() {

        int s = size();
        assert (s > 0);

        if (s > 1) {
            //float f = 1f / s;
            //            for (Task x : e) {
            //                BudgetMerge.plusBlend.apply(b, x.budget(), f);
            //            }
            //            return b;
            //return e.maxValue(Task::priElseZero); //use the maximum of their truths

            //TODO sum weighted by evidence
            return meanValue(this::pri); //average value
        } else {
            return pri(get(0));
        }
    }

    private float pri(TaskRegion x) {
        if (x instanceof Prioritized)
            return ((Prioritized) x).priElseZero();

        //TODO ??

        return 0;
    }

    @Override
    public long start() {
        throw new TODO();
    }

    @Override
    public long end() {
        throw new TODO();
    }

    @Override @Nullable
    public short[] cause() {
        return Cause.zip(Param.causeCapacity.intValue(),
                Util.map(0, size(), x -> get(x).cause(), short[][]::new));
    }

    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        throw new TODO();
    }


    List<Stamp> evidence() {
        List<Stamp> s = $.newArrayList();
        for (TaskRegion x : this) {
            Stamp ss = stamp(x);
            if (ss != null)
                s.add(ss);
        }
        return s;
    }




    @Deprecated public static Stamp stamp(TaskRegion x) {
        if (x instanceof Task)
            return ((Task) x);
//        } else if (x instanceof DynTruth) {
//            return ((DynTruth)x).stamp();
//        }
        return null;
    }

    @Nullable
    public Truth truth(Term superterm, DynamicTruthModel m, @Nullable Consumer<NALTask> withBuiltTask, boolean beliefOrGoal, NAR nar) {

        Truth t = m.truth(this, nar);
        if (t == null)
            return null;

        float conf = t.conf();

        float evi = c2wSafe(conf);
        float eviMin = c2wSafe(nar.confMin.floatValue());
        if (evi < eviMin)
            return null;


        float freq = t.freq();

        float f;
        long start, end;
        if (withBuiltTask != null) {
            if (!superterm.op().temporal) {
                if (size() > 1) {
                    //dilute the evidence in proportion to temporal sparseness for non-temporal results
                    TimeFusion se = TimeFusion.the(this);
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
                    TaskRegion only = get(0);
                    start = only.start();
                    end = only.end();
                }
            } else {
                long min = Long.MAX_VALUE;
                for (int i = 0, thisSize = size(); i < thisSize; i++) {
                    long y = get(i).start();
                    if (y != ETERNAL) {
                        if (y < min)
                            min = y;
                    }
                }

                if (min == Long.MAX_VALUE)
                    min = ETERNAL; //all eternal

                start = end = min;
            }

            if (superterm.op() == NEG) {
                superterm = superterm.unneg(); //unneg if constructing a task, but dont if just returning the truth
                f = 1f - freq;
            } else {
                f = freq;
            }
        } else {
            start = end = XTERNAL; //not used
            f = freq;
        }


        //TODO compute max valid overlap to terminate the zip early
        ObjectFloatPair<long[]> ss = Stamp.zip(evidence(), Param.STAMP_CAPACITY);
        evi = Param.overlapEvidence(evi, ss.getTwo());
        if (evi < eviMin)
            return null;

        Truth tr = Truth.theDiscrete(f, evi, nar);
        if (tr == null)
            return null;
        if (withBuiltTask == null)
            return tr;

        float priority = pri();

        Term c = m.construct(superterm, this);
        if (c == null)
            return null;

        if (c.op()==NEG) {
            c = c.unneg();
            tr = tr.neg();
        }

        // then if the term is valid, see if it is valid for a task
        if (!Task.validTaskTerm(c, beliefOrGoal ? BELIEF : GOAL, true))
            return null;


        long[] stamp = ss.getOne();

        NALTask dyn = new NALTask(c, beliefOrGoal ? Op.BELIEF : Op.GOAL,
                tr, nar.time(), start, (start == ETERNAL || c.op().temporal) ? start : end, stamp);
        if (ss.getTwo() > 0) dyn.setCyclic(true);

        dyn.cause = cause();
        dyn.priSet(priority);

        if (Param.DEBUG)
            dyn.log("Dynamic");

        withBuiltTask.accept(dyn);

        return tr;
    }



    @Override
    public @Nullable Task task() {
        throw new TODO();
    }
}
