package jcog.lab;

/**
 * an "objective function"
 * the float value provided by the goal represents the degree to which
 * the condition is satisfied in the provided experiment at the given time point.
 * positive value = satisfied, negative value = dissatisfied.  neutral=0
 * NaN is unknown or not applicable
 * @param E experiment
 */
abstract public class Goal<E, S> extends Sensor<E,Float> {

    /** left-hand side is the variable, right-hand side is the desired value.
     * resembles Pascal assignment operator */
    public static final String GOAL_SYMBOL = ":=";

    /** the id of the goal, describing what 'what' should be */
    final String id;

    /** concerning the sensor by this ID */
    final String sensor;

    public Goal(String sensor, String shouldBe) {
        super(sensor + GOAL_SYMBOL + shouldBe);
        this.sensor = sensor;
        this.id = shouldBe;
    }

    public Goal(Sensor<E, S> sensor, String shouldBe) {
        this(sensor.id, shouldBe);
    }

}
