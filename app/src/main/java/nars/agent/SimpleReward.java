package nars.agent;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.GOAL;

public class SimpleReward extends Reward {

    public final Signal concept;

    private final FloatFloatToObjectFunction<Truth> truther;

    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);
        NAR nar = nar();
        concept = new Signal(id, () -> reward, nar);
        truther = truther();
        agent.//alwaysWant
                alwaysWantEternally
                    (concept.term, nar.confDefault(GOAL));
//        agent.alwaysQuestionDynamic(()->{
//            int dt =
//                    //0;
//                    nar.dur();
//            Random rng = nar.random();
//            return IMPL.the(agent.actions.get(rng).term().negIf(rng.nextBoolean()), dt, concept.term());
//        }, true);
    }

    @Override
    public void update(long prev, long now, long next) {
        super.update(prev, now, next);
        concept.update(prev, now, truther, nar());
    }
}
