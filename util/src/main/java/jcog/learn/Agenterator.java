package jcog.learn;

import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatSupplier;
import jcog.signal.Tensor;
import jcog.signal.tensor.TensorRing;
import jcog.signal.tensor.TensorSerial;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;

import java.util.List;
import java.util.function.IntConsumer;

/**
 *  analogous to Iterator<X>, this wraps an agent as a special "iterator" of
 *  markov-decision sensorimotor frames.
 *
 *  </X>constructs a 'compiled' / 'hardwired' iterable agent, only need to call next() on each iteration */
public class Agenterator implements FloatSupplier {

    /** TODO use Tensor */
    protected final TensorRing sensorsHistory;

    public final Agent agent;

    protected final IntConsumer action;

    protected final FloatSupplier reward;

    @Deprecated private final List<Tensor> _sensors;
    private final TensorSerial sensorsNow;


    public Agenterator(IntIntToObjectFunction<Agent> agentBuilder, List<Tensor> _sensors, FloatSupplier reward, List<IntObjectPair<? extends IntConsumer>> actions, boolean NOP_ACTION, IntConsumer action, int history) {

        this.reward = reward;

        this._sensors = _sensors;
        sensorsNow = new TensorSerial(_sensors);
        int numSensors = sensorsNow.volume();

        TensorRing sensors = new TensorRing(numSensors, history);
        this.sensorsHistory = sensors;

        int numActions = actions.stream().mapToInt(IntObjectPair::getOne).sum() + (NOP_ACTION ? 1 : 0);

        this.action = action;

        this.agent = agentBuilder.apply(numSensors * history, numActions);
    }

    /** returns the next reward value */
    @Override public float asFloat() {
        float r;

        /* @Deprecated  */ _sensors.forEach(Tensor::snapshot); sensorsNow.update(); sensorsHistory.setSpin(sensorsNow.snapshot()); //HACK bad

        action.accept(agent.act(r = reward.asFloat(), sensorsHistory));
        return r;
    }


}