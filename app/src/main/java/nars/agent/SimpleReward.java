package nars.agent;

import com.google.common.collect.Iterators;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.concept.sensor.Signal;
import nars.table.dynamic.SeriesBeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import java.util.Iterator;
import java.util.stream.Stream;

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
    public final Iterator<Signal> iterator() {
        return Iterators.singletonIterator(concept);
    }

    @Override
    public Term term() {
        return concept.term();
    }


    @Override
    protected Stream<SeriesBeliefTable.SeriesRemember> updateReward(long prev, long now, long next) {
        return Stream.of(concept.update(prev, now, truther, nar()));
    }
}
