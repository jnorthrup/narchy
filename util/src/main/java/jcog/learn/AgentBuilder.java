package jcog.learn;

import jcog.data.list.FasterList;
import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatSupplier;
import jcog.signal.Tensor;
import jcog.signal.tensor.ScalarTensor;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;

import java.util.List;
import java.util.function.IntConsumer;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/** TODO test */
public class AgentBuilder {
    final List<Tensor> sensors = new FasterList();
    final List<IntObjectPair<? extends IntConsumer>> actions = new FasterList();
    final FloatSupplier reward;
//    float durations = 1f;

    /** allow history individually for each input using Rolling tensor */
    @Deprecated int history = 4;

    /** whether to add an extra NOP action */
    boolean NOP_ACTION = false;

    public AgentBuilder(FloatSupplier reward) {
        this.reward = reward;
    }

    /** builds the constructed agent */
    public Agenterator get(IntIntToObjectFunction<Agent> controller) {

        return new Agenterator(controller, this.sensors, reward, actions, NOP_ACTION, act, history);
    }

    final IntConsumer act = new IntConsumer() {
        @Override
        public void accept(int rawAction) {
            //deserialize the raw action id to the appropriate action group
            // TODO do this with a stored skip count int[]
            int rawAction1 = rawAction;
            for (IntObjectPair<? extends IntConsumer> aa : actions) {
                int bb = aa.getOne();
                if (rawAction1 >= bb) {
                    rawAction1 -= bb;
                } else if (rawAction1 >= 0) {
                    aa.getTwo().accept(rawAction1);
                }
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

    public AgentBuilder out(Runnable decision) {
        actions.add(pair(1, new IntConsumer() {
            @Override
            public void accept(int x) {
                decision.run();
            }
        }));
        return this;
    }

    public AgentBuilder out(int decisions, IntConsumer decision) {
        actions.add(pair(decisions, decision));
        return this;
    }

    @Override
    public String toString() {
        return "AgentBuilder{" +
            "sensors=" + sensors +
            ", actions=" + actions +
            ", reward=" + reward +
            ", act=" + act +
            '}';
    }
}
