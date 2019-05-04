package nars.table.dynamic;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.Answer;
import nars.term.Term;
import nars.term.util.Image;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.INH;

public class ImageBeliefTable extends DynamicTaskTable {

    /**
     * the target of the concept which this relies on for storage of beliefs, and thus remains consistent with
     */
    public final Term normal;



    public ImageBeliefTable(Term image, boolean beliefOrGoal) {
        super(image, beliefOrGoal);

        Term imageNormalized = Image.imageNormalize(image);
        assert(!image.equals(imageNormalized) && imageNormalized.op()==INH);
        this.normal = imageNormalized;
    }

    @Override
    public @Nullable Task match(long start, long end, boolean forceProject, @Nullable Term template, Predicate<Task> filter, int dur, NAR nar) {
        Task t = super.match(start, end, forceProject, template, filter, dur, nar);
        if (t!=null) {
            //wrap the result as an image
            return new ImageTermTask(this.term, t);
        }
        return null;
    }

    @Override
    public void match(Answer t) {
        //forward to the host concept's appropriate table
        Concept h = host(t.nar, false);
        if (h==null)
            return;

        if (!(h instanceof TaskConcept))
            return; //TODO if this happens: may be a NodeConcept in certain cases involving $ vars.  investigate

        (beliefOrGoal ? h.beliefs() : h.goals()).match(t);
    }

    //
//        @Nullable private BeliefTable table(NAR n, boolean conceptualize) {
//            Concept h = host(n, conceptualize);
//            if (!(h instanceof TaskConcept))
//                return null; //TODO if this happens: may be a NodeConcept in certain cases involving $ vars.  investigate
//            else
//                return beliefOrGoal ? h.beliefs() : h.goals();
//        }

    @Nullable private Concept host(NAR n, boolean conceptualize) {
        return n.concept(normal, conceptualize);
    }

    public static class ImageTermTask extends SpecialTermTask {
        public ImageTermTask(Term t, Task x) {
            super(t, x);
        }

        @Override
        protected boolean inheritCyclic() {
            return true; //cyclic propagation is fine here but only here
        }
    }
}