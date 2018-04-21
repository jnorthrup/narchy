package nars.concept.dynamic;

import jcog.TODO;
import jcog.sort.Top;
import jcog.sort.Top2;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.DefaultBeliefTable;
import nars.table.TaskMatch;
import nars.table.TemporalBeliefTable;
import nars.task.Revision;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class DynamicBeliefTable extends DefaultBeliefTable {

    protected final boolean beliefOrGoal;

    protected final Term term;

    protected DynamicBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
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
    public final Truth truth(long start, long end, Term template, NAR nar) {
        Truth d = truthDynamic(start, end, template, nar);
        Truth e = truthStored(start, end, template, nar);
        if (d == null)
            return e;
        if (e == null || d.equals(e))
            return d;

        //return Revision.revise(d, e); //<- this is optimistic that the truths dont overlap
        return Truth.stronger(d, e); //<- this is conservative disallowing any overlap
    }

    /**
     * generates a dynamic matching truth
     */
    @Nullable
    protected abstract Truth truthDynamic(long start, long end, Term template, NAR nar);

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


    protected final Truth truthStored(long start, long end, Term template, NAR nar) {
        return super.truth(start, end, template, nar);
    }

    @Override
    public Task sample(long start, long end, Term template, NAR nar) {
        return matchThe(TaskMatch.sampled(start, end, nar.random()), nar);
    }

    abstract public void sampleDynamic(long start, long end, Consumer<Task> n, NAR nar);

    @Override
    public void match(TaskMatch m, NAR nar, Consumer<Task> target) {
        long s = m.start();
        long e = m.end();
        FloatFunction<Task> value = m.value();
        Top2<Task> ss = new Top2<>(value);
        //TODO include eternal
        sampleDynamic(s, e, ss::add, nar);
        eternal.match(m, nar, ss::add);
        if (ss.isEmpty()) {
            temporal.match(m, nar, target);
        } else {
            temporal.match(m, nar, ss::add);

            //combine results from sensor series and from the temporal table
            if (ss.size()==1) {
                target.accept(ss.a); //simple case
            } else {
                if (m.limit() > 1)
                    throw new TODO();
                ss.sample(target, m.value(), m.random());
            }
        }
    }


    @Override
    public Task match(long start, long end, Term template, Predicate<Task> filter, NAR nar) {

        Task x = super.match(start, end, template, filter, nar);

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

        //choose highest confidence
        Top<Task> top = new Top<>(t->Revision.eviAvg(t, start, end, 1));

        if (x.term().equals(y.term()) && !Stamp.overlapsAny(x, y)) {
            //try to revise
            Task xy = Revision.mergeTasks(nar, start, end, x, y);
            if (xy != null && (filter==null || filter.test(xy)))
                top.accept(xy);
        }
        top.accept(x);
        top.accept(y);

        return top.the;
    }


}
