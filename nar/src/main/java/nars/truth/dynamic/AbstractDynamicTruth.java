package nars.truth.dynamic;

import jcog.Util;
import jcog.math.LongInterval;
import jcog.util.ObjectLongLongPredicate;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Compound;
import nars.term.Term;
import nars.time.When;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;

import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;
import static nars.truth.dynamic.DynamicConjTruth.ConjIntersection;
import static nars.truth.dynamic.DynamicStatementTruth.Impl;

/**
 * Created by me on 12/4/16.
 */
abstract public class AbstractDynamicTruth {

    abstract public Truth truth(DynTaskify d /* eviMin, */);


    public abstract boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic target from some or all components
     */
    abstract public Term reconstruct(Compound superterm, long start, long end, DynTaskify d);


    /** default subconcept Task resolver */
    public Task subTask(TaskConcept subConcept, Term subTerm, long subStart, long subEnd, Predicate<Task> filter, DynTaskify d) {
        BeliefTable table = (BeliefTable) subConcept.table(d.beliefOrGoal ? BELIEF : GOAL);
        return subTask(table, subTerm, subStart, subEnd, filter, d);
    }

    public Task subTask(BeliefTable table, Term subTerm, long subStart, long subEnd, Predicate<Task> filter, DynTaskify d) {
        NAR nar = d.nar;
        float dur = d.dur;
        Task bt;
        switch (NAL.DYN_TASK_MATCH_MODE) {
            case 0:
                //may be too aggressive in evidence collection, preventing other components from succeeding
                bt = table.matchExact(subStart, subEnd, subTerm, filter, dur, nar);
                break;
            case 1:
                //may be too aggressive in evidence collection, preventing other components from succeeding
                bt = table.match(subStart, subEnd, subTerm, filter, dur, nar);
                break;
            case 2:
                bt = table.sample(new When<>(subStart, subEnd, dur, nar), subTerm, filter);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return bt;
    }

    public static ObjectBooleanToObjectFunction<Term, BeliefTable[]> table(AbstractDynamicTruth... models) {
        return (Term t, boolean beliefOrGoal) ->
                Util.map(m -> new DynamicTruthTable(t, m, beliefOrGoal), new BeliefTable[models.length], models);
    }


    /** estimates number of components, for allocation purposes */
    abstract public int componentsEstimate();

    public Task task(Compound template, long earliest, long s, long e, DynTaskify d) {
        Term y = reconstruct(template, s, e, d);
        if (y==null || !y.unneg().op().taskable /*|| y.hasXternal()*/) { //quick tests
            if (NAL.DEBUG) {
                //TEMPORARY for debug
//                  model.evalComponents(answer, (z,start,end)->{
//                      System.out.println(z);
//                      nar.conceptualizeDynamic(z).beliefs().match(answer);
//                      return true;
//                  });
//                  model.reconstruct(template, this, s, e);
//                throw new TermException("DynTaskify template not reconstructed: " + this, template);
            }
            return null;
        }


        boolean absolute = (this!=Impl && this != ConjIntersection) || s == LongInterval.ETERNAL || earliest == LongInterval.ETERNAL;
        for (int i = 0, dSize = d.size(); i < dSize; i++) {
            Task x = d.get(i);
            long xStart = x.start();
            if (xStart!=ETERNAL) {
                long shift = absolute ? 0 : xStart - earliest;
                long ss = s + shift, ee = e + shift;
                if (xStart != ss || x.end() != ee) {
                    Task tt = Task.project(x, ss, ee,
                            NAL.truth.EVI_MIN, //minimal truth threshold for accumulating evidence
                            false,
                            1, //no need to dither truth or time here.  maybe in the final calculation though.
                            d.dur,
                            d.nar);
                    if (tt == null)
                        return null;
                    d.setFast(i, tt);
                }
            }
        }


        Truth t = this.truth(d);
        if (t == null)
            return null;

        /** interpret the presence of truth dithering as an indication this is producng something for 'external' usage,
         *  and in which case, also dither time
         */

        if (d.ditherTruth) {
            //dither and limit truth
            t = t.dither(d.nar);
            if (t == null)
                return null;
        }

//        if (ditherTime) {
//            if (s!= LongInterval.ETERNAL) {
//                int dtDither = nar.dtDither();
//                s = Tense.dither(s, dtDither, -1);
//                e = Tense.dither(e, dtDither, +1);
//            }
//        }
        d.trimToSize();
        return DynTaskify.merge(d::array, y, t, d.stamp(d.nar.random()), d.beliefOrGoal, s, e, d.nar);
    }
}