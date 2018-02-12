package nars.op;

import jcog.Paper;
import jcog.Skill;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.link.CauseLink;
import nars.term.Term;
import nars.term.Termed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

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
//    private final StableBloomFilter<Task> filter;

    /** used also for termlinks */
    public final MutableFloat conceptActivationRate = new MutableFloat(1f);

    public final MutableFloat taskLinkActivationRate = new MutableFloat(1f);

    public Anoncepts(int taskCapacity, @NotNull NAR n) {
        super(taskCapacity, n);
        this.nar = n;

//        filter = Task.newBloomFilter(1024 /* small */, n.random());

        conceptActivationRate.set(1f/taskCapacity);
        taskLinkActivationRate.set(1f/taskCapacity);
    }

    @Override
    protected boolean preFilter(Task next) {
//        if (filter.addIfMissing(next)) {
            return super.preFilter(next);
//        }
//        return false;
    }

    @Override
    protected float leak(Task task) {
        Term taskTerm = task.term().root();
        Term a = taskTerm.anon();
        if (a == null)  //?<- why would this, if it does
            return 0;

        Concept c = nar.concept(a); //HACK
        if (c == null) {
            nar.concepts.set(a, c = new AnonConcept(a, nar));
        }

        float pri = task.priElseZero();
        float cr = conceptActivationRate.floatValue();
        c = nar.activate(c, pri * cr);
        if (c == null)
            return 0;  //???

        short cid = out.id;
        c.tasklinks().putAsync(new CauseLink.CauseLinkUntilDeleted<>(task, pri * taskLinkActivationRate.floatValue(), cid));
        c.termlinks().putAsync(new CauseLink.PriCauseLink<>(taskTerm, pri * cr, cid));

        return 1;
    }

    private static class AnonConcept extends NodeConcept {
        AnonConcept(Term a, NAR nar) {
            super(a, nar);
        }

        @Override protected List<Termed> buildTemplates(Term term) {
            return Collections.emptyList();
        }
    }


}
