package nars.op.mental;

import jcog.math.FloatRange;
import jcog.pri.bag.Sampler;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.control.Causable;
import nars.link.TaskLink;
import nars.task.ITask;
import nars.task.proxy.SpecialPuncTermAndTruthTask;
import nars.term.Term;
import nars.time.When;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BooleanSupplier;

import static nars.Op.BELIEF;

/** improved imperience impl which samples directly from taskbag and summarizes
 * snapshots of belief table aggregates, rather than individual tasks
 */
public class Inperience2 extends Causable {

    public final FloatRange priFactor = new FloatRange(0.5f, 0, 2f);

    private final CauseChannel<ITask> in;

    public Inperience2(NAR n) {
        super(n);
        this.in = n.newChannel(id);
    }

    @Override
    public void next(NAR n, BooleanSupplier kontinue) {

        long now = n.time();
        int dur = n.dur();
        double window = 2.0;

        //int dither = n.dtDither();
        long start = /*Tense.dither*/((long)Math.floor(now - window * dur/2));//, dither);
        long end = /*Tense.dither*/((long)Math.ceil(now + window * dur/2));//, dither);
        When when = new When(start, end, dur, n);

        int volMax = n.termVolumeMax.intValue();
        int volMaxPre = (int) Math.max(1, Math.ceil(volMax * 0.5f));
        float beliefConf = n.confDefault(BELIEF);
        Random rng = n.random();
        n.attn.links.sample(rng, (TaskLink tl) -> {

            Task t = tl.get(when);
            if (t != null && t.term().volume() <= volMaxPre) {

                Task u = null;
                if (t.isBeliefOrGoal()) {
                    Term r = Inperience.reifyBeliefOrGoal(t, n);
                    r = r.normalize();
                    if (validReification(r, volMax)) {
                        boolean neg = t.isNegative();
                        u = new InperienceTask(r, $.t(1, beliefConf * t.truth().negIf(neg).expectation()), t);
                    }
                } else {
                    Term r = Inperience.reifyQuestion(t.term(), t.punc(), n);
                    r = r.normalize();
                    if (validReification(r, volMax))
                        u = new InperienceTask(r, $.t(1, beliefConf * 0.5f), t);
                }

                if (u != null) {
                    Task.deductComplexification(t, u, priFactor.floatValue(),true);
                    in.input(u);
                }
            }

            return kontinue.getAsBoolean() ? Sampler.SampleReaction.Next : Sampler.SampleReaction.Stop;
        });

    }

    private boolean validReification(Term r, int volMax) {
        return r.op().taskable && r.volume() <= volMax && Task.validTaskTerm(r, BELIEF, true);
    }

    @Override
    public float value() {
        return in.value();
    }

    private static class InperienceTask extends SpecialPuncTermAndTruthTask {
        public InperienceTask(Term r, @Nullable Truth tr, Task t) {
            super(r, Op.BELIEF, tr, t);
        }

        @Override
        public ITask next(NAR n) {
            return Remember.the(this, n); //copied from UnevaluatedTask
        }
    }
}
