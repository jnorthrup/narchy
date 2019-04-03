package nars.table.dynamic;

import jcog.Util;
import jcog.WTF;
import jcog.math.LongInterval;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.dynamic.AbstractDynamicTruth;
import nars.truth.dynamic.DynTaskify;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static nars.truth.dynamic.DynamicConjTruth.ConjIntersection;


/**
 * computes dynamic truth according to implicit truth functions
 * determined by recursive evaluation of the compound's sub-component's truths
 */
public final class DynamicTruthTable extends DynamicTaskTable {

    private final AbstractDynamicTruth model;

    public DynamicTruthTable(Term c, AbstractDynamicTruth model, boolean beliefOrGoal) {
        super(c, beliefOrGoal);
        this.model = model;
    }

    @Override
    public final void match(Answer a) {
        if (a.term() == null)
            a.template(term); //use default concept term

        DynTaskify d = new DynTaskify(model, beliefOrGoal, a);

        if (model.evalComponents(a, d::evalComponent))
            taskify(d);
    }

    private void taskify(DynTaskify d) {
        long s, e;

        Answer a = d.answer;

        long earliest;
        long latest = d.maxValue(Stamp::end);
        if (latest == LongInterval.ETERNAL) {
            //all are eternal
            s = e = LongInterval.ETERNAL;
            earliest = LongInterval.ETERNAL;
        } else {

            earliest = d.earliest();


            if (model == ConjIntersection) {
                //calculate the minimum range (ie. intersection of the ranges)
                s = earliest;
                long range = (d.minValue(t -> t.isEternal() ? 0 : t.range()-1));

                long ss = a.time.start;
                if (ss != LongInterval.ETERNAL && ss != XTERNAL) {
                    long ee = a.time.end;
                    s = Util.clampSafe(s, ss, ee); //project sequence to when asked
                }

                if (s != LongInterval.ETERNAL) {
                    e = s + range;
                } else {
                    e = LongInterval.ETERNAL;
                }

            } else {

                long[] u = Tense.merge(0, d);
                if (u == null)
                    return;

                s = u[0];
                e = u[1];

            }

        }


        NAR nar = a.nar;
        Term template = a.term();
        Term term1 = model.reconstruct(template, d, nar, s, e);
        if (term1 == null || !term1.unneg().op().taskable) { //quick tests
            //if (Param.DEBUG)
                throw new WTF("could not reconstruct: " + template + ' ' + d);
            //return;
        }


        boolean absolute = model != ConjIntersection || s == LongInterval.ETERNAL || earliest == LongInterval.ETERNAL;
        for (int i = 0, dSize = d.size(); i < dSize; i++) {
            Task x = d.get(i);
            long xStart = x.start();
            long shift = absolute || (xStart==ETERNAL) ? 0 : xStart - earliest;
            long ss = s + shift, ee = e + shift;
            if (xStart != ss || x.end() != ee) {
                Task tt = Task.project(x, ss, ee,
                        0, /* use no evidence threshold while accumulating sub-evidence */
                        false,
                        Param.DYNAMIC_TRUTH_TASK_TIME_DITHERING,
                        a.nar);
                if (tt == null)
                    return;
                d.setFast(i, tt);
            }
        }


        Truth t = model.truth(d, nar);
        //t = (t != null && eviFactor != 1) ? PreciseTruth.byEvi(t.freq(), t.evi() * eviFactor) : t;
        if (t == null)
            return;

        /** interpret the presence of truth dithering as an indication this is producng something for 'external' usage,
         *  and in which case, also dither time
         */
        boolean internalOrExternal = !a.ditherTruth;
        if (!internalOrExternal) {
            //dither and limit truth
            t = t.dither(a.nar);
            if (t == null)
                return;

            //dither time
            if (s!= LongInterval.ETERNAL) {
                int dtDither = a.nar.dtDither();
                s = Tense.dither(s, dtDither);
                e = Tense.dither(e, dtDither);
            }
        }

        Task y = d.merge(term1, t, d::stamp, beliefOrGoal, s, e, nar);
        if (y!=null)
            a.tryAccept(y);
    }


//    @Override
//    protected Truth truthDynamic(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
//
//        DynStampTruth d = model.eval(template, beliefOrGoal, start, end, filter, true, nar);
//
//        return d != null ? d.truth(template, model, beliefOrGoal, nar) : null;
//
//    }


}














































