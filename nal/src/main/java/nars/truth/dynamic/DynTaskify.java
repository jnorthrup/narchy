package nars.truth.dynamic;

import jcog.Paper;
import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.set.MetalLongSet;
import jcog.math.LongInterval;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.util.Answer;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import nars.time.event.WhenTimeIs;
import nars.truth.Stamp;
import nars.truth.Truth;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.XTERNAL;
import static nars.truth.dynamic.DynamicConjTruth.ConjIntersection;

/**
 * Dynamic Taskify
 *
 * uses dynamic truth models and recursive dynamic belief evaluation to compute
 * accurately truthed, timed, and evidentially stamped composite/aggregate truths
 * with varying specified or unspecified internal temporal features described by
 * a template target.
 *
 * additionally tracks evidential overlap while being constructed, and provide the summation of evidence after*/
@Paper
public class DynTaskify extends TaskList {

    private final AbstractDynamicTruth model;

    public final Answer answer;
    private final int dur;
    public final Task result;
    private MetalLongSet evi = null;

    final boolean beliefOrGoal;
    final Predicate<Task> filter;


    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, Answer a) {
        super(4 /* estimate */);

        this.answer = a;
        this.beliefOrGoal = beliefOrGoal;
        this.model = model;
        this.dur = a.dur;

        Term template = a.term();
        assert(template.op() != NEG);


        Predicate<Task> answerfilter = a.filter;
        this.filter = Param.DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER ?
                        Answer.filter(answerfilter, this::doesntOverlap) :
                        answerfilter;


        result = model.evalComponents(a, this::evalComponent) ? taskify() : null;
    }

    private Task taskify() {
        long s, e;

        Answer a = answer;

        long earliest;
        long latest = maxValue(Stamp::end);
        if (latest == LongInterval.ETERNAL) {
            //all are eternal
            s = e = LongInterval.ETERNAL;
            earliest = LongInterval.ETERNAL;
        } else {

            earliest = earliest();


            if (model == ConjIntersection) {
                //calculate the minimum range (ie. intersection of the ranges)
                s = earliest;
                long range = (minValue(t -> t.isEternal() ? 0 : t.range()-1));

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

                long[] u = Tense.merge(0, this);
                if (u == null)
                    return null;

                s = u[0];
                e = u[1];

            }

        }


        NAR nar = a.nar;
        Compound template = (Compound) a.term();
        Term term1 = model.reconstruct(template, this, nar, s, e);
        if (term1 == null || !term1.unneg().op().taskable) { //quick tests
            if (Param.test.DEBUG)
                throw new WTF("could not reconstruct: " + template + ' ' + this);
            return null;
        }


        boolean absolute = model != ConjIntersection || s == LongInterval.ETERNAL || earliest == LongInterval.ETERNAL;
        for (int i = 0, dSize = size(); i < dSize; i++) {
            Task x = get(i);
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
                    return null;
                setFast(i, tt);
            }
        }


        Truth t = model.truth(this, nar);
        //t = (t != null && eviFactor != 1) ? PreciseTruth.byEvi(t.freq(), t.evi() * eviFactor) : t;
        if (t == null)
            return null;

        /** interpret the presence of truth dithering as an indication this is producng something for 'external' usage,
         *  and in which case, also dither time
         */
        boolean internalOrExternal = !a.ditherTruth;
        if (!internalOrExternal) {
            //dither and limit truth
            t = t.dither(a.nar);
            if (t == null)
                return null;

            //dither time
            if (s!= LongInterval.ETERNAL) {
                int dtDither = a.nar.dtDither();
                s = Tense.dither(s, dtDither);
                e = Tense.dither(e, dtDither);
            }
        }

        Task y = merge(term1, t, this::stamp, beliefOrGoal, s, e, nar);
        return y;
    }


//    @Override
//    protected Truth truthDynamic(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
//
//        DynStampTruth d = model.eval(template, beliefOrGoal, start, end, filter, true, nar);
//
//        return d != null ? truth(template, model, beliefOrGoal, nar) : null;
//
//    }


    private boolean evalComponent(Term subTerm, long subStart, long subEnd) {
        Op so = subTerm.op();

        boolean negated = so == Op.NEG;
        if (negated) {
            subTerm = subTerm.unneg();
            so = subTerm.op();
        }
        if (!so.taskable)
            return false;

        NAR nar = answer.nar;

        Term st;
        if (!subTerm.isNormalized()) {
            if (Param.test.DEBUG) {
                throw new TODO("unnormalize the result for inclusion in the super-compound");
                //st = subTerm.normalize();
            }
            //HACK
            return false;

        } else
            st = subTerm;
        Concept subConcept = nar.conceptualizeDynamic(st);
        if (!(subConcept instanceof TaskConcept))
            return false;


        BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);
        Task bt =
                //table.answer(subStart, subEnd, subTerm, filter, nar);
                //table.match(subStart, subEnd, subTerm, filter, dur, nar);
                table.sample(WhenTimeIs.range(subStart, subEnd, this.answer), subTerm, filter);

        if (bt == null || !model.acceptComponent((Compound) template(), bt.term(), bt))
            return false;

        /* project to a specific time, and apply negation if necessary */
        //bt = Task.project(bt, subStart, subEnd, negated, forceProjection, false, nar);
        bt = negated ? Task.negated(bt) : bt;

        return bt != null && add(bt);
    }



    @Override
    public boolean add(Task x) {

        super.add(x);


        if (evi == null) {
            switch (size) {
                case 1: //dont create set now
//                case 2:
                    break;
                default: //more than 2:
                    long[] a = get(0).stamp(), b = get(1).stamp();
                    evi = Stamp.toSet(a.length + b.length, a, b);
                    break;
            }
        }

        if (evi!=null) {
            evi.addAll(x.stamp());
        }

        return true;
    }

    private boolean doesntOverlap(Task t) {
        return size ==0 ||
                doesntOverlap(t.stamp());
    }

    private boolean doesntOverlap(long[] stamp) {
        MetalLongSet e = this.evi;
        if (e != null) {
            long[] s = stamp;
            for (long x : s) {
                if (e.contains(x))
                    return false;
            }
        } else if (size > 0) {
            //delay creation of evi set one more item
            assert(size == 1);
            return !Stamp.overlapsAny(get(0).stamp(), stamp);
        }

        return true;
    }


//    public final Task task(Term target, Truth t, boolean beliefOrGoal, NAR n) {
//        return task(target, t, beliefOrGoal, false, n);
//
//    }

//    public final Truth truth(Term target, BiFunction<DynTruth, NAR, Truth> o, boolean beliefOrGoal, NAR n) {
//        return (Truth) eval(target, o, false, beliefOrGoal, n);
//    }

//    /**
//     * TODO make Task truth dithering optional
//     */
//    @Deprecated  public Truthed eval(Term superterm, @Deprecated BiFunction<DynTruth, NAR, Truth> truthModel, boolean taskOrJustTruth, boolean beliefOrGoal, NAR nar) {
//
//        Truth t = truthModel.apply(this, nar);
//        if (t == null)
//            return null;
//
//        //return eval(()->superterm, t, taskOrJustTruth, beliefOrGoal, freqRes, confRes, eviMin, nar);
//        if (taskOrJustTruth) {
//            return task(superterm, t, this::stamp, beliefOrGoal, nar);
//        } else {
//            return t;
//        }
//
//    }



    public long[] stamp(Random rng) {
        if (evi == null) {

            switch(size) {
                case 1:
                    return get(0).stamp();
                case 2:
                    //lazy calculated stamp
                    long[] a = get(0).stamp(), b = get(1).stamp();
                    return Stamp.sample(Param.STAMP_CAPACITY, Stamp.toSet(a.length + b.length, a, b), rng);
                case 0:
                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            return Stamp.sample(Param.STAMP_CAPACITY, this.evi, rng);
        }
    }


    @Override
    public boolean clearIfChanged() {
        if (super.clearIfChanged()) {
            evi = null;
            return true;
        }
        return false;
    }

    public final Term template() {
        return answer.term();
    }

    /** excludes ETERNALs */
    public long earliest() {
        return minValue(t -> {
            long ts = t.start();
            return ts != LongInterval.ETERNAL ? ts : LongInterval.TIMELESS;
        });
    }
}
