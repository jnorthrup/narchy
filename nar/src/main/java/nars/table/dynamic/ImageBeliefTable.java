package nars.table.dynamic;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.op.Remember;
import nars.task.proxy.ImageTask;
import nars.task.util.Answer;
import nars.term.Term;
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

        Task original = r.input;
        Term normal = Image.imageNormalize(original.term());

        TaskConcept c = (TaskConcept) r.nar.conceptualize(normal);
        if (c == null)
            return;


        Task input;
        if (original instanceof ImageTask)
            input = ((ImageTask) original).task; //unwrap existing
        else {
            input = Task.withContent(original, normal);
            input.take(original, 0.5f, false, false); //share 50% priority with the normalized version


            boolean cyclic = original.isCyclic();
            if (cyclic)
                input.setCyclic(true); //inherit cyclic
        }

        r.input = input;

        c.table(input.punc()).remember(r);

    }

    /**
     * wraps resulting task as an Image proxy
     */
    @Override
    public @Nullable Task match(long start, long end, boolean forceProject, @Nullable Term template, Predicate<Task> filter, float dur, NAR nar, boolean ditherTruth) {
        Task t = super.match(start, end, forceProject, template, filter, dur, nar, ditherTruth);
        return t != null ? new ImageTask(transformFromTemplate(t), t) : null;
    }

    private Term transformFromTemplate(Task t) {
        return Image.transformFromTemplate(t.term(), this.term, this.normal);
    }


    /**
     * wraps resulting task as an Image proxy
     */
    @Override
    public Task sample(When<NAR> when, @Nullable Term template, @Nullable Predicate<Task> filter) {
        Task t = super.sample(when, template, filter);
        return t != null ? new ImageTask(transformFromTemplate(t), t) : null;
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
