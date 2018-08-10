package nars.table.dynamic;

import jcog.TODO;
import jcog.sort.Top2;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.DefaultBeliefTable;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.Revision;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.task.TaskMatch;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class DynamicBeliefTable extends DefaultBeliefTable {

    final boolean beliefOrGoal;

    protected final Term term;

    protected DynamicBeliefTable(Term c, boolean beliefOrGoal, EternalTable e, TemporalBeliefTable t) {
        super(e, t);
        this.beliefOrGoal = beliefOrGoal;
        this.term = c;
    }

    /**
     * generates a dynamic matching task
     */
    protected abstract Task taskDynamic(long start, long end, Term template, NAR nar);

    @Override
    @Nullable
    public Truth truth(long start, long end, Term template, NAR nar) {
        Truth d = truthDynamic(start, end, template, nar);
        if (d!=null && dynamicOverrides()) {
            return d;
        }

        Truth e = truthStored(start, end, template, nar);
        if (d == null)
            return e;
        if (e == null || d.equals(e))
            return d;

        
        return Truth.stronger(d, e); 
    }

    /**
     * generates a dynamic matching truth
     */
    @Nullable
    protected abstract Truth truthDynamic(long start, long end, Term template, NAR nar);


















































    protected final byte punc() {
        return beliefOrGoal ? Op.BELIEF : Op.GOAL;
    }


    public Truth truthStored(long start, long end, Term template, NAR nar) {
        return super.truth(start, end, template, nar);
    }

    @Override
    public Task sample(long start, long end, Term template, NAR nar) {
        return matchThe(TaskMatch.sampled(start, end, null, nar.random()), nar);
    }

    /** default implementation */
    void sampleDynamic(long start, long end, Consumer<Task> n, NAR nar) {
        Task x = taskDynamic(start, end, term, nar);
        if (x != null)
            n.accept(x);
    }

    @Override
    public void match(TaskMatch m, NAR nar, Consumer<Task> target) {
        long s = m.start();
        long e = m.end();
        FloatFunction<Task> value = m.value();

        Top2<Task> ss = new Top2<>(value);

        sampleDynamic(s, e, ss::add, nar);

        boolean dynOnly = dynamicOverrides();
        if (!dynOnly)
            eternal.match(m, nar, ss::add);

        if (ss.isEmpty()) {
            temporal.match(m, nar, target);
        } else {
            if (!dynOnly)
                temporal.match(m, nar, ss::add);

            
            if (ss.size()==1) {
                target.accept(ss.a); 
            } else {
                if (m.limit() > 1)
                    throw new TODO();
                ss.sample(target, m.value(), m.random());
            }
        }
    }


    /** an implementation can allow this to return true to cause
     * a dynamic match, if exist, to totally override any possible
     * otherwise stored temporal or eternal tasks.
     */
    protected boolean dynamicOverrides() {
        return false;
    }

    @Override
    public Task match(long start, long end, Term template, Predicate<Task> filter, NAR nar) {

        boolean anyTemplate;
        if (template==null || template.op()!=term.op()) {
            template = term;
            anyTemplate = true;
        } else {
            anyTemplate = false;
        }

        //TODO apply filter to dynamic results
        Task y = taskDynamic(start, end, template, nar);
        if (y!=null && dynamicOverrides())
            return y;

        Task x = taskStored(start, end, template, filter, nar);
        if (x == null)
            return y;
        if (y == null)
            return x;

        if (!anyTemplate) {
            //choose betch match to template term
            float xt = Revision.dtDiff(x.term(), template);
            float yt = Revision.dtDiff(y.term(), template);
            if (xt < yt)
                return x;
            else if (yt < xt)
                return y;
        }

        return Revision.mergeOrChoose(x, y, start, end, filter, nar);
    }

    public final Task taskStored(long start, long end, Term template, Predicate<Task> filter, NAR nar) {
        return super.match(start, end, template, filter, nar);
    }


}
