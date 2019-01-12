package nars.truth.dynamic;

import jcog.Paper;
import jcog.WTF;
import jcog.data.set.MetalLongSet;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.truth.dynamic.DynamicConjTruth.ConjIntersection;

/**
 * Dynamic Taskify
 *
 * uses dynamic truth models and recursive dynamic belief evaluation to compute
 * accurately truthed, timed, and evidentially stamped composite/aggregate truths
 * with varying specified or unspecified internal temporal features described by
 * a template term.
 *
 * additionally tracks evidential overlap while being constructed, and provide the summation of evidence after*/
@Paper
public class DynTaskify extends DynEvi {

    private final AbstractDynamicTruth model;
    private final NAR nar;
    private final Term template;
    private MetalLongSet evi = null;

    final boolean beliefOrGoal;
    final Predicate<Task> filter;
    final boolean forceProjection;

    public DynTaskify(Term template, AbstractDynamicTruth model, boolean beliefOrGoal, Predicate<Task> filter, boolean forceProjection, NAR nar) {
        super(4 /* estimate */);
        this.beliefOrGoal = beliefOrGoal;

        this.template = template;

        this.model = model;
        this.nar = nar;

        this.filter = Param.DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER ?
                        Answer.filter(filter, this::doesntOverlap) :
                        filter;

        this.forceProjection = forceProjection;
    }

    @Nullable public static DynTaskify eval(Term template, AbstractDynamicTruth model, boolean beliefOrGoal, Answer a) {
        assert (template.op() != NEG);

        DynTaskify d = new DynTaskify(template, model, beliefOrGoal, a.filter, false, a.nar);

        return model.components(template, a.time.start, a.time.end, d::evalComponent) ? d : null;
    }

    @Nullable
    public static Task task(AbstractDynamicTruth model, boolean beliefOrGoal, Answer a) {
        Term template = a.template;
        DynTaskify d = eval(template, model, beliefOrGoal, a);
        if (d == null)
            return null;



        long s, e;


        int eternals = d.count(Task::isEternal);
        if (eternals == d.size()) {
            s = ETERNAL;
            e = ETERNAL;
        } else {
            if (eternals == 0) {
                s = d.minValue(Stamp::start);
                e = d.maxValue(Stamp::end);

            } else {
                s = d.minValue(t -> t.start() != ETERNAL ? t.start() : TIMELESS);
                e = d.maxValue(Stamp::end);
            }

//                //trim
//                if (a.time.start!=ETERNAL && Longerval.intersects(s, e, a.time.start, a.time.end)) {
//                    s = Math.max(a.time.start, s);
//                    if (a.time.end != ETERNAL)
//                        e = Math.min(a.time.end, e);
//                }

        }

        float eviFactor;
        if (model == ConjIntersection) {
            if (s != ETERNAL)
                e = s + (d.minValue(t -> t.isEternal() ? 1 : t.range()) - 1);
            eviFactor = 1;
        } else {

//        //HACK discount by estimated evidence loss due to time gaps
//        float maxEvi = yy.maxValue((Task t) -> t.evi());
//        TruthPolation p = Param.truth(s, e, nar.dur());
//        yy.forEach(p::add);
//        float eviMax = p.truth().evi();
//        eviFactor = Math.min(1, eviMax / maxEvi);

            //HACK estimate by time range only
            if (s != ETERNAL) {
                long range = (e - s) + 1;
                eviFactor = (float) (d.sumOfLong((Task x) -> x.isEternal() ? range : Math.min(range, x.range())) / (((double) range * d.size())));
                assert (eviFactor <= 1f);
            } else {
                eviFactor = 1;
            }
        }

        NAR nar = a.nar;
        Term term = model.reconstruct(template, d, nar, s, e);
        if (term == null || term instanceof Bool || !term.unneg().op().taskable) { //quick tests
            if (Param.DEBUG)
                throw new WTF("could not reconstruct: " + template + ' ' + d);
            return null;
        }

        Truth t = model.truth(d, nar);
        t = (t != null && eviFactor != 1) ? PreciseTruth.byEvi(t.freq(), t.evi() * eviFactor) : t;
        if (t == null)
            return null;


        Task y = d.task(term, t, d::stamp, beliefOrGoal, s, e, nar);
        if (y != null && eviFactor != 1) {
            y.priMult(eviFactor);
        }
        return y;
    }

    private boolean evalComponent(Term subTerm, long subStart, long subEnd) {
        Op so = subTerm.op();

        boolean negated = so == Op.NEG;
        if (!negated && !so.taskable)
            return false;

        if (negated)
            subTerm = subTerm.unneg();

        Concept subConcept = nar.conceptualizeDynamic(subTerm);
        if (!(subConcept instanceof TaskConcept))
            return false;

        BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);
        Task bt = forceProjection ?
                table.answer(subStart, subEnd, subTerm, filter, nar) :
                table.match(subStart, subEnd, subTerm, filter, nar);
        if (bt == null || !model.acceptComponent(template, bt.term(), bt))
            return false;

        /* project to a specific time, and apply negation if necessary */
        bt = Task.project(bt, subStart, subEnd, negated, forceProjection, false, nar);

        return bt != null && add(bt);
    }


    @Override
    public boolean add(Task newItem) {

        super.add(newItem);


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
            evi.addAll(newItem.stamp());
        }

        return true;
    }
    public boolean doesntOverlap(Task t) {
        return size ==0 ||
                doesntOverlap(t.stamp());
    }

    public boolean doesntOverlap(long[] stamp) {
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


//    public final Task task(Term term, Truth t, boolean beliefOrGoal, NAR n) {
//        return task(term, t, beliefOrGoal, false, n);
//
//    }

//    public final Truth truth(Term term, BiFunction<DynTruth, NAR, Truth> o, boolean beliefOrGoal, NAR n) {
//        return (Truth) eval(term, o, false, beliefOrGoal, n);
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
    public void clear() {
        super.clear();
        if (evi!=null)
            evi.clear();
    }
}
