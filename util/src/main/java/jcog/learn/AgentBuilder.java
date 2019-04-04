package jcog.learn;

import jcog.data.list.FasterList;
import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatSupplier;
import jcog.signal.Tensor;
import jcog.signal.tensor.ScalarTensor;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.List;
import java.util.function.IntConsumer;

/** TODO test */
public class AgentBuilder {
    final List<Tensor> sensors = new FasterList();
    final List<IntObjectPair<? extends IntConsumer>> actions = new FasterList();
    final FloatSupplier reward;
//    float durations = 1f;

    /** whether to add an extra NOP action */
    private final static boolean NOP_ACTION = true;

    public AgentBuilder(FloatSupplier reward) {
        this.reward = reward;
    }

    /** builds the constructed agent */
    public Agenterator get(IntIntToObjectFunction<Agent> controller) {

        return new Agenterator(controller, this.sensors, reward, actions, NOP_ACTION, act, 6);
    }

    final IntConsumer act = (rawAction) -> {
        //deserialize the raw action id to the appropriate action group
        // TODO do this with a stored skip count int[]
        for (IntObjectPair<? extends IntConsumer> aa: actions) {
            int bb = aa.getOne();
            if (rawAction >= bb) {
                rawAction -= bb;
            } else {
                aa.getTwo().accept(rawAction);
                return;
            }
        }
    };

//    public AgentBuilder durations(float runEveryDurations) {
//        this.durations = runEveryDurations;
//        return this;
//    }

    public AgentBuilder in(FloatSupplier f) {
        sensors.add(new ScalarTensor(f));
        return this;
    }

    public AgentBuilder in(Tensor t) {
        sensors.add(t);
        return this;
    }

    public AgentBuilder out(int decisions, IntConsumer decision) {
        actions.add(PrimitiveTuples.pair(decisions, decision));
        return this;
    }


}
