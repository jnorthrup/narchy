package nars.concept.dynamic;

import jcog.sort.Top;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.Revision;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public abstract class DynamicBeliefTable extends DefaultBeliefTable {

    protected final boolean beliefOrGoal;

    protected final Term term;

    public DynamicBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        super(t);
        this.beliefOrGoal = beliefOrGoal;
        this.term = c;
    }

    /**
     * generates a dynamic matching task
     */
    protected abstract Task taskDynamic(long start, long end, Term template, NAR nar);

    @Override
    @Nullable
    public final Truth truth(long start, long end, NAR nar) {
        Truth d = truthDynamic(start, end, nar);
        Truth e = truthStored(start, end, nar);
        if (e == null || d == e)
            return d;
        if (d == null)
            return e;

        //return Revision.revise(d, e); //<- this is optimistic that the truths dont overlap
        return Truth.maxConf(d, e); //<- this is conservative disallowing any overlap
    }

    /**
     * generates a dynamic matching truth
     */
    @Nullable
    protected abstract Truth truthDynamic(long start, long end, NAR nar);

//    @Override
//    public boolean add(final Task input, TaskConcept concept, NAR nar) {
//
////        if (Param.FILTER_DYNAMIC_MATCHES) {
////            if (!input.isInput()) {
////
////                long start, end;
////
////                Term inputTerm = input.term();
////                long[] inputStamp = input.stamp();
//////                boolean[] foundEqual = new boolean[1];
////                Task matched = match(start = input.start(), end = input.end(), inputTerm, nar, (m) ->
//////                        (foundEqual[0] |= (m.equals(input)))
//////                                    ||
////                                (
////                        //one stamp is entirely contained within the other
//////                        (inputStamp.length >= m.stamp().length && Stamp.overlapFraction(m.stamp(), inputStamp) >= 1f)
//////                            &&
////                        m.term().equals(inputTerm) &&
////                        m.start() <= start &&
////                        m.end() >= end
////                );
////
////                if (matched == input)
////                    return true; //duplicate
////
////                //must be _during_ the same time and same term, same stamp, then compare Truth
////                if (matched != null) {
////
////                    float inputPri = input.priElseZero();
////
////                    if (matched instanceof DynTruth.DynamicTruthTask &&
////                            PredictionFeedback.absorb(matched, input, start, end, nar.dur(), nar.freqResolution.floatValue(), nar)) {
////                        Tasklinks.linkTask(matched, inputPri, concept, nar);
////                        return false;
////                    } else if (input.equals(matched)) {
////                        Tasklinks.linkTask(matched, inputPri, concept, nar);
////                        return true;
////                    }
////
////                    //otherwise it is unique (ex: frequency or conf)
////
////                }
////            }
////        }
//
//        return super.add(input, concept, nar);
//    }

    public final byte punc() {
        return beliefOrGoal ? Op.BELIEF : Op.GOAL;
    }


    protected final Truth truthStored(long start, long end, NAR nar) {
        return super.truth(start, end, nar);
    }


    @Override
    public Task match(long start, long end, Term template, NAR nar, Predicate<Task> filter) {

        Task x = super.match(start, end, template, nar, filter);

        Task y = taskDynamic(start, end, template, nar);

        if (x == null && y == null)
            return null;

        if (filter !=null) {
            if (x != null && !filter.test(x))
                x = null;
            if (y != null && !filter.test(y))
                y = null;
        }

        if (y == null)
            return x;

        if (x == null)
            return y;

        if (x.equals(y))
            return x;

        int dur = nar.dur();

        //choose highest confidence
        Top<Task> top = new Top<>(t->t.evi(start,end,dur));


        if (x.term().equals(y.term()) && !Stamp.overlapping(x, y)) {
            //try to revise
            Task xy = Revision.mergeTemporal(nar, x, y);
            if (xy != null && (filter==null || filter.test(xy)))
                top.accept(xy);
        }
        top.accept(x);
        top.accept(y);

        return top.the;
    }


}
