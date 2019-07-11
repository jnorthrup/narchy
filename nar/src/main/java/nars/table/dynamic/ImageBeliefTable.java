package nars.table.dynamic;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.op.Remember;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.Answer;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.Image;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.INH;

public class ImageBeliefTable extends DynamicTaskTable {

    /**
     * the target of the concept which this relies on for storage of beliefs, and thus remains consistent with
     */
    @Deprecated public final Term normal;



    public ImageBeliefTable(Term image, boolean beliefOrGoal) {
        super(image, beliefOrGoal);

        Term imageNormalized = Image.imageNormalize(image);
        if (image.isNormalized())
            imageNormalized = imageNormalized.normalize();
        assert (!image.equals(imageNormalized) && imageNormalized.op() == INH);
        this.normal = imageNormalized;
    }

    @Override
    public void remember(Remember r) {

        Task imaged = r.input;
        Term normal = Image.imageNormalize(imaged.term());

        TaskConcept c = (TaskConcept) r.nar.conceptualize(normal);
        if (c == null)
            return;

        Task normalized = SpecialTermTask.the(imaged, normal, true);

        if (r.store) {
            r.link = r.notify = false; //proxy store
            r.input = normalized;
            c.table(normalized.punc()).remember(r);
        }

        if (r.remembered!=null && r.remembered.containsInstance(normalized)) {
//        if (!r.store || r.remembered!=null) {
            r.store = false;
            r.link = r.notify = true;
            r.remember(imaged);
        }
    }

    /**
     * wraps resulting task as an Image proxy
     */
    @Override
    public @Nullable Task match(long start, long end, boolean forceProject, @Nullable Term template, Predicate<Task> filter, float dur, NAR nar, boolean ditherTruth) {
        Task t = super.match(start, end, forceProject, template, filter, dur, nar, ditherTruth);
        return transformFromTemplate(t);
    }

    /**
     * wraps resulting task as an Image proxy
     */
    @Override
    public Task sample(When<NAR> when, @Nullable Term template, @Nullable Predicate<Task> filter) {
        Task t = super.sample(when, template, filter);
        return transformFromTemplate(t);
    }

    @Nullable private Task transformFromTemplate(Task t) {
        return t != null ? SpecialTermTask.the(t, transformTermFromTemplate(t), false) : null;
    }

    private Term transformTermFromTemplate(Termed t) {
        return Image.transformFromTemplate(t.term(), this.term, this.normal);
    }

    @Override
    public void match(Answer t) {
//        if (t.term()==null)
//            throw new WTF();

        //forward to the host concept's appropriate table
        Concept h = t.nar.conceptualizeDynamic(normal);
        if (h == null)
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

}
