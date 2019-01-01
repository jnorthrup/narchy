package nars.agent;

import com.google.common.collect.Iterators;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.attention.AttBranch;
import nars.concept.sensor.Signal;
import nars.term.Term;

import java.util.Iterator;

public class SimpleReward extends Reward {

    public final Signal concept;

    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);
        NAR nar = nar();
        concept = new Signal(id, () -> reward, nar) {
            @Override
            protected AttBranch newAttn(Term term) {
                return new AttBranch(term, this.components()) {
//
//                    @Override
//                    public float elementPri(NAR nar) {
//                        return nar.priDefault(GOAL); /* feedback beliefs pri = goal NOT belief */
//                    }
                };
            }

        };
        //concept.attn.parent(attn);

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




    @Override
    public final Iterator<Signal> iterator() {
        return Iterators.singletonIterator(concept);
    }

    @Override
    public Term term() {
        return concept.term();
    }


    @Override
    protected void updateReward(long prev, long now) {
        concept.sense(prev, now, nar());
    }
}
