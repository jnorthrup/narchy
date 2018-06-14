package jcog.lab;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

/**
 * an "objective function"
 * the float value provided by the goal represents the degree to which
 * the condition is satisfied in the provided experiment at the given time point.
 * positive value = satisfied, negative value = dissatisfied.  neutral=0
 * NaN is unknown or not applicable
 * @param E experiment
 */
public class Goal<E> extends Sensor.FloatLambdaSensor<E> {

    public Goal(FloatFunction<E> f) {
        this("goal", f);
    }

    public Goal(String name, FloatFunction<E> f) {
        super(name, f);
    }

//    /** left-hand side is the variable, right-hand side is the desired value.
//     * resembles Pascal assignment operator */
//    public static final String GOAL_SYMBOL = ":=";
//
//    /** the id of the goal, describing what 'what' should be */
//    final String id;
//
//    /** concerning the sensor by this ID */
//    final String sensor;

//    public Goal(String sensor, String desc) {
//        super(sensor + GOAL_SYMBOL + desc);
//        this.sensor = sensor;
//        this.id = desc;
//    }
//
//    public Goal(Sensor<E, S> sensor, String desc) {
//        this(sensor.id, desc);
//    }

}
