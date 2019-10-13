package nars.table.dynamic;

import nars.NAL;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.op.Remember;
import nars.subterm.Subterms;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.Answer;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.term.util.TermException;
import nars.term.util.TermTransformException;
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
        //if (!(imageNormalized.op() == INH && !image.equals(imageNormalized)))
        if (imageNormalized.unneg().op() != INH)
            throw new TermException("ImageBeliefTable", INH, image, imageNormalized);

        //assert (imageNormalized.op() == INH && !image.equals(imageNormalized));
//        if (imageNormalized.hasXternal())
//            Util.nop(); //throw new TermException("contains XTERNAL", imageNormalized);
        this.normal = imageNormalized;
    }

    @Override
    public void remember(Remember r) {

        assert(r.link && r.notify): "TODO save these to tmp var";

        Task imaged = r.input;
        Term normal = Image.imageNormalize(imaged.term());

        Task normalized;

        if (r.store) {
            //r.link = r.notify = false; //proxy store
            TaskConcept c = (TaskConcept) r.nar.conceptualize(normal);
            if (c == null)
                return;
            r.input = normalized = SpecialTermTask.the(imaged, normal, true);
            c.table(normalized.punc()).remember(r);
        }

//        if (r.result!=null && !r.result.isDeleted() && normalized.equals(r.result)) {
//            r.store = false;
//            r.link = r.notify = true;
//            if (normalized!=r.result) {
//                Task.merge(imaged, r.result, PriMerge.replace);
//            }
//            r.remember(imaged);
//        }
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
    public Task sample(When<What> when, @Nullable Term template, @Nullable Predicate<Task> filter) {
        return transformFromTemplate(super.sample(when, template, filter));
    }

//    @Override
//    public @Nullable Answer sampleSome(When<NAR> when, @Nullable Term template, @Nullable Predicate<Task> filter) {
//        Answer a = super.sampleSome(when, template, filter);
//        if (a!=null)
//            a.tasks.replace(this::transformFromTemplate);
//        return a;
//    }

    @Nullable private Task transformFromTemplate(@Nullable Task x) {
        if (x == null) return null;
        try {
            Term xx = x.term();
            Term y = transformTermFromTemplate(xx);
            if (y instanceof Bool)
                throw new TermException("invalid recursive image", xx);

            return SpecialTermTask.the(x, y, false);
        } catch (TermException t) {
            //HACK
            if (NAL.DEBUG) throw new RuntimeException(t);
            else return null;
        }
    }

    private Term transformTermFromTemplate(Term t) {
        if (t.equals(this.normal))
            return this.term;

        assert(/*this.term.op()==INH && */t.op()==INH);

//        if (!x.hasAny(Op.Temporal))
//            return template; //template should equal the expected result

        Subterms tt = this.term.subterms();
        Term subj = tt.sub(0), pred = tt.sub(1);
        if (subj.contains(Op.ImgInt)) {
            //Term y = x.sub(1).sub(normal.sub(1).subIndexFirst(z -> z.equals(pred)));
            //return Image.imageInt(x, y);
            return Image.imageInt(t, pred);
        } else if (pred.contains(Op.ImgExt)) {
            //Term y = x.sub(0).sub(normal.sub(0).subIndexFirst(z -> z.equals(subj)));
            //return Image.imageExt(x, y);
            return Image.imageExt(t, subj);
        } else
            throw new TermTransformException(t, this.term, "could not infer Image transform from template");
    }

    @Override
    public void match(Answer a) {
        //forward to the host concept's appropriate table
        Concept h = a.nar.conceptualizeDynamic(normal);
        if (!(h instanceof TaskConcept))
            return; //TODO if this happens: may be a NodeConcept in certain cases involving $ vars.  investigate

        Term termOriginal = a.term();  a.term(normal); //push

        (beliefOrGoal ? h.beliefs() : h.goals()).match(a);

        a.term(termOriginal); //pop
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
