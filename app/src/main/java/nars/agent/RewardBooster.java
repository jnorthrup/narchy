package nars.agent;

import jcog.TODO;
import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.action.ActionConcept;
import nars.control.channel.CauseChannel;
import nars.exe.Causable;
import nars.link.Activate;
import nars.task.ITask;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class RewardBooster extends Causable {

    private final CauseChannel<ITask> in;
    private final Concept reward;

    public RewardBooster(Reward r) {
        super();
        NAR n = r.agent.nar();
        in = n.newChannel(this);
        n.on(this);

        Iterator<Concept> rr = r.iterator();
        this.reward = rr.next();
        if (rr.hasNext())
            throw new TODO();
    }

    @Override
    protected void next(NAR n, BooleanSupplier kontinue) {


        long now = n.time();
        int dur = n.dur();
        long start = now - dur / 2, end = now + dur / 2;

        float c =
                //nn.confMin.floatValue() * 2;
                //nn.confDefault(GOAL);
                n.confDefault(GOAL) / 2;
        float confMin = n.confMin.floatValue();

        Random rng = n.random();
        float p = n.priDefault(GOAL); //* agent.pri?
        Truth b = reward.beliefs().truth(start, end, n);

        Set<Term> sampled = new UnifiedSet(128);

        if (b != null) {
            Truth g = reward.goals().truth(start, end, n);
            if (g != null) {
                //float df = g.freq() - b.freq();
                float sat = 1 - Math.abs(g.freq() - b.freq());

                //n.attn.active.stream().limit(samples).forEach(xx -> {
                while (kontinue.getAsBoolean()) {
                    Activate a = n.concepts.sample(rng);
                    if (a == null)
                        break;

                    Concept x = a.get();
                    if (x == reward || !(x instanceof TaskConcept) || x instanceof ActionConcept) continue;
                    if (!x.op().goalable) continue;

                    if (!sampled.add(x.term()))
                        continue;

                    Task tx = n.answer(x, BELIEF, start, end);
                    if (tx != null) {
                        Term xt = tx.term();

                        if (xt.hasXternal()) continue;

                        //TODO Use task answer() and get non-XTERNAL result
                        //TODO use stamp from answer

                        Truth t = tx.truth();
                        if (t != null) {
                            float yc = c * t.conf();
                            if (yc > confMin) {
                                float yf = Util.lerp(sat, 1 - t.freq(), t.freq());
                                UnevaluatedTask tt = new UnevaluatedTask(xt, GOAL, $.t(yf, yc), now, start, end, tx.stamp());
                                tt.pri(p * tx.priElseZero());
                                in.input(tt);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public float value() {
        return in.value();
    }

}
