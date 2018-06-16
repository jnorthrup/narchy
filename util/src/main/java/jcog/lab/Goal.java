package jcog.lab;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.ToDoubleFunction;

/**
 * an "objective function"
 * the float value provided by the goal represents the degree to which
 * the condition is satisfied in the provided experiment at the given time point.
 * positive value = satisfied, negative value = dissatisfied.  neutral=0
 * NaN is unknown or not applicable
 * @param E experiment
 */
public class Goal<X> extends ProxySensor<X,Number> {

    public Goal(NumberSensor<X> f) {
        this("goal", f);
    }

    public Goal(String id, NumberSensor<X> goal) {
        super(id, goal);
    }

    public Goal(FloatFunction<X> goal) {
        super(NumberSensor.of("goal", goal));
    }
    public Goal(ToDoubleFunction<X> goal) {
        super(NumberSensor.of("goal", goal));
    }
}
