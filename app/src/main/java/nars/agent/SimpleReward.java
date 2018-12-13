package nars.agent;

import com.google.common.collect.Iterators;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.AttNode;
import nars.concept.sensor.Signal;
import nars.control.channel.CauseChannel;
import nars.op.mental.Inperience;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;

import java.util.Iterator;

import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

public class SimpleReward extends Reward {

    public final Signal concept;

    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);
        NAR nar = nar();
        concept = new Signal(id, () -> reward, nar) {
            @Override
            protected CauseChannel<ITask> newChannel(NAR n) {
                return in;
            }
        };
        concept.attn.parent(attn);

        alwaysWantEternally(concept.term);
//        agent.//alwaysWant
//                alwaysWantEternally
//                    (concept.term, nar.confDefault(GOAL));
//        agent.alwaysQuestionDynamic(()->{
//            int dt =
//                    //0;
//                    nar.dur();
//            Random rng = nar.random();
//            return IMPL.the(agent.actions.get(rng).term().negIf(rng.nextBoolean()), dt, concept.term());
//        }, true);
    }

    public void alwaysWantEternally(Term goal) {
        alwaysWantEternally(goal, nar().confDefault(GOAL));
    }

    public void alwaysWantEternally(Term goal, float conf) {
        Task t = new NALTask(goal, GOAL, $.t(1f, conf), nar().time(),
                ETERNAL, ETERNAL,
                nar().evidence()
                //Stamp.UNSTAMPED
        );

        AttNode a = new AttNode($.func(Inperience.want, this.term(), goal)) {
            @Override
            protected float childDemand(NAR nar) {
                return (1f - nar.concepts.pri(t, 0));
            }

            @Override
            public void update(NAR nar) {
                super.update(nar);
                t.take(supply, Math.min(supply.priElseZero(), demand.floatValue()), true, false);
                in.input(t);
            }
        };
        a.parent(attn);
    }


    @Override
    public final Iterator<Signal> iterator() {
        return Iterators.singletonIterator(concept);
    }

    @Override
    public Term term() {
        return concept.term();
    }


    @Override
    protected void updateReward(long prev, long now, long next) {
        concept.update(prev, now, next, nar());
    }
}
