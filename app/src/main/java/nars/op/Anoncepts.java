package nars.op;

import jcog.Paper;
import jcog.Skill;
import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.link.CauseLink;
import nars.term.Term;
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

    /** used also for termlinks */
    public final FloatRange conceptActivationRate = new FloatRange(1f, 0, 1f);

    public final FloatRange taskLinkActivationRate = new FloatRange(1f, 0, 1f);

    public Anoncepts(int taskCapacity, @NotNull NAR n) {
        super(taskCapacity, n);
        this.nar = n;



        conceptActivationRate.set(1f/taskCapacity);
        taskLinkActivationRate.set(1f/taskCapacity);
    }











    @Override
    protected float leak(Task task) {
        Term taskTerm = task.term().root();
        Term a = taskTerm.anon();
        if (a == null)  
            return 0;

        Concept c = nar.conceptualizeDynamic(a);
        if (c == null) {
            nar.concepts.set(a, c = new AnonConcept(a, nar));
        }

        float pri = task.priElseZero();
        float cr = conceptActivationRate.floatValue();
        nar.activate(c, pri * cr);

        short cid = in.id();

        
        c.termlinks().putAsync(new CauseLink.PriCauseLink<>(taskTerm, pri * cr, cid));

        return 1;
    }


    private static class AnonConcept extends NodeConcept {
        AnonConcept(Term a, NAR nar) {
            super(a, /* TODO AnonTermLinker */nar);
        }
    }


}
