package nars.truth.dynamic;

import jcog.Paper;
import jcog.TODO;
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
import nars.term.Term;
import nars.time.event.WhenTimeIs;
import nars.truth.Stamp;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;

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
    private MetalLongSet evi = null;

    final boolean beliefOrGoal;
    final Predicate<Task> filter;


    public DynTaskify(AbstractDynamicTruth model, boolean beliefOrGoal, Answer a) {
        super(4 /* estimate */);

        this.answer = a;

        this.beliefOrGoal = beliefOrGoal;

        Term template = a.term();
        assert(template.op() != NEG);

        this.model = model;

        Predicate<Task> answerfilter = a.filter;
        this.filter = Param.DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER ?
                        Answer.filter(answerfilter, this::doesntOverlap) :
                        answerfilter;

        this.dur = a.dur;
    }


    public boolean evalComponent(Term subTerm, long subStart, long subEnd) {
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
            if (Param.DEBUG) {
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

        if (bt == null || !model.acceptComponent(template(), bt.term(), bt))
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
