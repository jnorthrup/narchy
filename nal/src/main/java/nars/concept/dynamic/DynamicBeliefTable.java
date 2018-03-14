package nars.concept.dynamic;

import jcog.decide.Roulette;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.signal.SignalTask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntFloatHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public abstract class DynamicBeliefTable extends DefaultBeliefTable {

    protected final boolean beliefOrGoal;

    protected final Term term;

    public DynamicBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        super(t);
        this.beliefOrGoal = beliefOrGoal;
        this.term = c;
    }

    /** generates a dynamic matching task */
    protected abstract Task taskDynamic(long start, long end, Term template, NAR nar);

    @Override
    @Nullable public final Truth truth(long start, long end, NAR nar) {
        Truth d = truthDynamic(start, end, nar);
        return Truth.maxConf(d, truthStored(start, end, nar));
    }

    /** generates a dynamic matching truth */
    @Nullable protected abstract Truth truthDynamic(long start, long end, NAR nar);

    @Override
    public boolean add(final Task input, TaskConcept concept, NAR nar) {

        if (Param.FILTER_DYNAMIC_MATCHES) {
            if (!(input instanceof SignalTask) && input.punc()==punc()  && !input.isInput()) {

                    PredictionFeedback.feedbackNewBelief(input, this, nar);
                    if (input.isDeleted())
                        return false;

            }
        }

//        if (Param.FILTER_DYNAMIC_MATCHES) {
//            if (!input.isInput()) {
//
//                long start, end;
//
//                Term inputTerm = input.term();
//                long[] inputStamp = input.stamp();
////                boolean[] foundEqual = new boolean[1];
//                Task matched = match(start = input.start(), end = input.end(), inputTerm, nar, (m) ->
////                        (foundEqual[0] |= (m.equals(input)))
////                                    ||
//                                (
//                        //one stamp is entirely contained within the other
////                        (inputStamp.length >= m.stamp().length && Stamp.overlapFraction(m.stamp(), inputStamp) >= 1f)
////                            &&
//                        m.term().equals(inputTerm) &&
//                        m.start() <= start &&
//                        m.end() >= end
//                );
//
//                if (matched == input)
//                    return true; //duplicate
//
//                //must be _during_ the same time and same term, same stamp, then compare Truth
//                if (matched != null) {
//
//                    float inputPri = input.priElseZero();
//
//                    if (matched instanceof DynTruth.DynamicTruthTask &&
//                            PredictionFeedback.absorb(matched, input, start, end, nar.dur(), nar.freqResolution.floatValue(), nar)) {
//                        Tasklinks.linkTask(matched, inputPri, concept, nar);
//                        return false;
//                    } else if (input.equals(matched)) {
//                        Tasklinks.linkTask(matched, inputPri, concept, nar);
//                        return true;
//                    }
//
//                    //otherwise it is unique (ex: frequency or conf)
//
//                }
//            }
//        }

        return super.add(input, concept, nar);
    }

    public final byte punc() {
        return beliefOrGoal ? Op.BELIEF : Op.GOAL;
    }


    public final Truth truthStored(long start, long end, NAR nar) {
        return super.truth(start, end, nar);
    }


    @Nullable
    protected abstract Term template(long start, long end, Term template, NAR nar);

    /**
     * returns an appropriate dt for the root term
     * of beliefs held in the table.  returns 0 if no other value can
     * be computed.
     */
    protected int matchDT(long start, long end, boolean commutive, NAR nar) {

        int s = size();
        if (s == 0)
            return 0;

        int dur = nar.dur();

        IntFloatHashMap dtEvi = new IntFloatHashMap(s);
        forEachTask(t -> {
            int tdt = t.dt();
            if (tdt != DTERNAL) {
                if (tdt == XTERNAL)
                    throw new RuntimeException("XTERNAL should not be present in " + t);
                if ((t.term().subs() > 2) == commutive)
                    dtEvi.addToValue(tdt, t.evi(start, end, dur)); //maybe evi
            }
        });
        int n = dtEvi.size();
        if (n == 0) {
            return 0;
        } else {
            MutableList<IntFloatPair> ll = dtEvi.keyValuesView().toList();
            int selected = n != 1 ?
                    Roulette.decideRoulette(ll.size(), (i) -> ll.get(i).getTwo(), nar.random()) : 0;
            return ll.get(selected).getOne();
        }
    }

    @Override
    public Task match(long start, long end, Term template, NAR nar, Predicate<Task> filter) {

        Task x = super.match(start, end, template, nar, filter);

        Task y = taskDynamic(start, end, template, nar);

        if (y == null || y.equals(x))
            return x;

        if (filter!=null && !filter.test(y))
            return x;


        boolean dyn;
        if (x == null) {
            dyn = true;
        } else {
            //choose higher confidence
            int dur = nar.dur();
            float xc = x.evi(start, end, dur);
            float yc = y.evi(start, end, dur);

            //prefer the existing task within a small epsilon lower for efficiency
            dyn = yc >= xc + Param.TRUTH_EPSILON;
        }

        if (dyn) {
            //Activate.activate(y, y.priElseZero(), nar);
            return y;
        } else {
            return x;
        }

    }
}
