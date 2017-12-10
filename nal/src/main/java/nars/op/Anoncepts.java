package nars.op;

import jcog.Paper;
import jcog.Skill;
import jcog.bloom.StableBloomFilter;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.concept.Concept;
import nars.term.Term;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 *  Anonymizing spider which creates anonymous meta-concepts
 *  with task and term links to their instances.  it acts like a
 *  fuzzy index that links groups of structurally related concepts.
 *
 *  We take away the face and leave only the message.
 *  Behind the mask we could be anyone,
 *  which is why we are judged by what we say and do,
 *  not who we are or what we have.
 *
 **/
@Paper
@Skill({"Data_compression","Graph_theory","Graph_matching"})
public class Anoncepts extends LeakBack {

    private final NAR nar;
    private final StableBloomFilter<Task> filter;

    /** used also for termlinks */
    public final MutableFloat conceptActivationRate = new MutableFloat(1f);

    public final MutableFloat taskLinkActivationRate = new MutableFloat(1f);

    public Anoncepts(int taskCapacity, @NotNull NAR n) {
        super(taskCapacity, n);
        this.nar = n;

        filter = Task.newBloomFilter(1024 /* small */, n.random());

        conceptActivationRate.set(1f/taskCapacity);
        taskLinkActivationRate.set(1f/taskCapacity);
    }

    @Override
    protected boolean preFilter(Task next) {
        if (filter.addIfMissing(next)) {
            return super.preFilter(next);
        }
        return false;
    }

    @Override
    protected float leak(Task task) {
        Term taskTerm = task.term().root();
        Term a = taskTerm.anonymous();
        if (a == null)  //?<- why would this, if it does
            return 0;

        float pri = task.priElseZero();

        float cr = conceptActivationRate.floatValue();
        Concept c = nar.activate(a, pri * cr);
        if (c == null)
            return 0;  //???

        c.tasklinks().putAsync(new PLinkUntilDeleted<>(task, pri * taskLinkActivationRate.floatValue()));
        c.termlinks().putAsync(new PLink<>(taskTerm, pri * cr));

        return 1;
    }
}
