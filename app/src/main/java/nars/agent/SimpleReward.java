package nars.agent;

import com.google.common.collect.Iterators;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import java.util.Iterator;

import static nars.Op.GOAL;

public class SimpleReward extends Reward {

    public final Signal concept;

    private final FloatFloatToObjectFunction<Truth> truther;

    protected final CauseChannel<ITask> in;

    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);
        NAR nar = nar();
        concept = new Signal(id, () -> reward, nar);

        in = a.nar().newChannel(this);

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
    public final Iterator<Concept> iterator() {
        return Iterators.singletonIterator(concept);
    }

    @Override
    public Term term() {
        return concept.term();
    }

    @Override
    public void update(long prev, long now, long next) {
        super.update(prev, now, next);
        in.input( concept.update(prev, now, truther, nar()) );
    }
}
