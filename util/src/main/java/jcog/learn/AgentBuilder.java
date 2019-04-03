package jcog.learn;

import jcog.data.list.FasterList;
import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatSupplier;
import jcog.signal.Tensor;
import jcog.signal.tensor.ScalarTensor;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** TODO test */
public class AgentBuilder {
    final List<Tensor> sensors = new FasterList();
    final List<IntObjectPair<? extends IntConsumer>> actions = new FasterList();
    final FloatSupplier reward;
    float durations = 1f;

    /** whether to add an extra NOP action */
    private final static boolean NOP_ACTION = true;

    public AgentBuilder(FloatSupplier reward) {
        this.reward = reward;
    }

    /** builds the constructed agent */
    public WiredAgent get(IntIntToObjectFunction<Agent> controller) {

        final int inputs = sensors.stream().mapToInt(Tensor::volume).sum();
        final int outputs = actions.stream().mapToInt(IntObjectPair::getOne).sum() + (NOP_ACTION ? 1 : 0);

        Consumer<float[]> inputter = (f) -> {
//            int s = sensors.size();
            int j = 0;
            for (Tensor x: sensors) {
                x.writeTo(f, j);
                j += x.volume();
            }
            assert(j == f.length);
        };
        IntConsumer act = (c) -> {
//            int s = actions.size();
            for (IntObjectPair<? extends IntConsumer> aa: actions) {
                int bb = aa.getOne();
                if (c >= bb) {
                    c -= bb;
                } else {
                    aa.getTwo().accept(c);
                    return;
                }
            }
        };
        return new WiredAgent(controller, inputs, inputter, reward, outputs, act);
    }

    public AgentBuilder durations(float runEveryDurations) {
        this.durations = runEveryDurations;
        return this;
    }

    public AgentBuilder in(FloatSupplier f) {
        sensors.add(new ScalarTensor(f));
        return this;
    }

    public AgentBuilder in(Tensor t) {
        sensors.add(t);
        return this;
    }

    public AgentBuilder out(int actions, IntConsumer action) {
        return out(PrimitiveTuples.pair(actions, action));
    }

    public AgentBuilder out(IntObjectPair<? extends IntConsumer> action) {
        actions.add(action);
        return this;
    }

    /** constructs a 'compiled' / 'hardwired' iterable agent, only need to call next() on each iteration */
    public static class WiredAgent implements Serializable {

        protected final Consumer<float[]> input;
        public final Agent agent;
        protected final IntConsumer act;
        protected final FloatSupplier reward;
        /**
         * buffer where inputs to the agent are stored
         */
        protected final float[] in;


        public WiredAgent(IntIntToObjectFunction<Agent> agentBuilder, int inputs, Consumer<float[]> input, FloatSupplier reward, int outputs, IntConsumer act) {
            this.input = input;
            this.in = new float[inputs];
            this.reward = reward;
            this.agent = agentBuilder.apply(inputs, outputs);
            this.act = act;
        }

        /** returns the next reward value */
        public float next() {
            input.accept(in);
            float r;
            act.accept(agent.act(r = reward.asFloat(), in));
            return r;
        }
    }
}
