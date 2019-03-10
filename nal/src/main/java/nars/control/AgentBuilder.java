package nars.control;

import jcog.learn.Agent;
import jcog.math.FloatSupplier;
import jcog.math.IntIntToObjectFunc;
import jcog.signal.Tensor;
import jcog.signal.tensor.ScalarTensor;
import nars.$;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class AgentBuilder {
    final List<Tensor> sensors = $.newArrayList();
    final List<IntObjectPair<? extends IntConsumer>> actions = $.newArrayList();
    final FloatSupplier reward;
    private final IntIntToObjectFunc<Agent> a;
    float durations = 1f;

    /** whether to add an extra NOP action */
    private final static boolean NOP_ACTION = true;

    public AgentBuilder(IntIntToObjectFunc<Agent> a, FloatSupplier reward) {
        this.a = a;
        this.reward = reward;
    }

    public AgentBuilder durations(float runEveryDurations) {
        this.durations = runEveryDurations;
        return this;
    }

    public WiredAgent get() {

        final int inputs = sensors.stream().mapToInt(Tensor::volume).sum();
        final int outputs = actions.stream().mapToInt(IntObjectPair::getOne).sum() + (NOP_ACTION ? 1 : 0);

        Consumer<float[]> inputter = (f) -> {
            int s = sensors.size();
            int j = 0;
            for (Tensor x: sensors) {
                x.writeTo(f, j);
                j += x.volume();
            }
            assert(j == f.length);
        };
        IntConsumer act = (c) -> {

            int s = actions.size();
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
        return new WiredAgent(a, inputs, inputter, reward, outputs, act);
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
        protected final Agent agent;
        protected final IntConsumer act;
        protected final FloatSupplier reward;
        /**
         * buffer where inputs to the agent are stored
         */
        protected final float[] in;


        public WiredAgent(IntIntToObjectFunc<Agent> agentBuilder, int inputs, Consumer<float[]> input, FloatSupplier reward, int outputs, IntConsumer act) {
            this.input = input;
            this.in = new float[inputs];
            this.reward = reward;
            this.agent = agentBuilder.apply(inputs, outputs);
            this.act = act;
        }

        public void next() {
            input.accept(in);
            act.accept(agent.act(reward.asFloat(), in));
        }
    }
}
