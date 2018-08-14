package nars.table.dynamic;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.EmptyBeliefTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** does not store tasks but only generates them on query */
public abstract class DynamicTaskTable extends EmptyBeliefTable {

    final boolean beliefOrGoal;

    protected final Term term;

    protected DynamicTaskTable(Term c, boolean beliefOrGoal) {
        this.beliefOrGoal = beliefOrGoal;
        this.term = c;
    }

    /** this is very important:  even if size==0 this must return false */
    @Override public final boolean isEmpty() {
        return false;
    }

    @Override
    public void match(Answer t) {
        t.accept(taskDynamic(t.time.start, t.time.end, t.template, ((Answer)t).nar ));
    }


    /**
     * generates a dynamic matching task
     */
    protected abstract Task taskDynamic(long start, long end, Term template, NAR nar);

    /**
     * generates a dynamic matching truth
     */
    @Nullable
    protected abstract Truth truthDynamic(long start, long end, Term template, NAR nar);


    protected final byte punc() {
        return beliefOrGoal ? Op.BELIEF : Op.GOAL;
    }


//    @Override
//    @Nullable
//    public Truth truth(long start, long end, Term template, NAR nar) {
//        //TODO calculate with evidence, adding strongest distinct evidence sources like Truthpolation does
//
//        Truth d = truthDynamic(start, end, template, nar);
//        if (d != null && dynamicOverrides()) {
//            return d;
//        }
//
//        Truth e = truthStored(start, end, template, nar);
//        if (d == null)
//            return e;
//        if (e == null || d.equals(e))
//            return d;
//
//
//        return Truth.stronger(d, e);
//    }



//    /**
//     * default implementation
//     */
//    void sampleDynamic(long start, long end, Consumer<Task> n, NAR nar) {
//        Task x = taskDynamic(start, end, term, nar);
//        if (x != null)
//            n.accept(x);
//    }

//    @Override
//    public void match(TaskMatchRank m, NAR nar, Consumer<Task> target) {
//        long s = m.timeMin();
//        long e = m.timeMax();
//        FloatFunction<Task> value = m.value();
//
//        Top2<Task> ss = new Top2<>(value);
//
//        sampleDynamic(s, e, ss::add, nar);
//
//        boolean dynOnly = dynamicOverrides();
//        if (!dynOnly)
//            eternal.match(m, nar, ss::add);
//
//        if (ss.isEmpty()) {
//            temporal.match(m, nar, target);
//        } else {
//            if (!dynOnly)
//                temporal.match(m, nar, ss::add);
//
//
//            if (ss.size() == 1) {
//                target.accept(ss.a);
//            } else {
//                if (m.limit() > 1)
//                    throw new TODO();
//                ss.sample(target, m.value(), nar.random());
//            }
//        }
//    }


//    /**
//     * an implementation can allow this to return true to cause
//     * a dynamic match, if exist, to totally override any possible
//     * otherwise stored temporal or eternal tasks.
//     */
//    protected boolean dynamicOverrides() {
//        return false;
//    }

//    @Override
//    public Task match(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
//
//        boolean anyTemplate;
//        if (template == null || template.op() != term.op()) {
//            template = term;
//            anyTemplate = true;
//        } else {
//            anyTemplate = !term.hasAny(Op.Temporal);
//        }
//
//        //TODO apply filter to dynamic results
//        Task y = taskDynamic(start, end, template, nar);
//        if (y != null && dynamicOverrides())
//            return y;
//
//        Task x = taskStored(start, end, template, filter, nar);
//        if (x == null)
//            return y;
//        if (y == null)
//            return x;
//
//        if (!anyTemplate) {
//            //choose betch match to template term
//            float xt = Revision.dtDiff(x.term(), template);
//            float yt = Revision.dtDiff(y.term(), template);
//            if (xt < yt)
//                return x;
//            else if (yt < xt)
//                return y;
//        }
//
//        return Revision.mergeOrChoose(x, y, start, end, filter, nar);
//    }

//    public final Task taskStored(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
//        return super.match(start, end, template, filter, nar);
//    }

//    public final Truth truthStored(long start, long end, Term template, NAR nar) {
//        return super.truth(start, end, template, nar);
//    }

}
