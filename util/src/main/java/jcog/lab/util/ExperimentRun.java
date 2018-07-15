package jcog.lab.util;

import jcog.data.list.FasterList;
import jcog.io.arff.ARFF;
import jcog.lab.Lab;
import jcog.lab.Sensor;

import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * an instance of an experiment, conducting the experiment, collecting data as it runs;
 * the integration of a subject, a repeatable procedure, and measurement schema
 * <p>
 * contains:
 * -all or some of the Lab's sensors
 * -executable procedure for applying the starting conditions to the subject via
 * some or all of the variables
 * -executable schedule for recording sensor measurements, with at least
 * the start and ending state enabled by default. TODO
 */
public class ExperimentRun<E> implements Runnable {


    /**
     * data specific to this experiment; can be merged with multi-experiment
     * data collections later
     */
    public final ARFF data;
    private final BiConsumer<E, ExperimentRun<E>> procedure;
    /**
     * enabled sensors
     */
    private final List<Sensor<E, ?>> sensors;
    private final E experiment;
    private long startTime;
    private long endTime;

    public ExperimentRun(E experiment, ARFF data, List<Sensor<E, ?>> sensors, BiConsumer<E, ExperimentRun<E>> procedure) {
        this.experiment = experiment;
        this.procedure = procedure;
        this.data = data;
        this.sensors = sensors;
    }

    public ExperimentRun(E experiment, Iterable<Sensor<E,?>> sensors, BiConsumer<E, ExperimentRun<E>> procedure) {
        this(experiment, newData(sensors), new FasterList<>(sensors), procedure);
    }


    /**
     * creates a new ARFF data with the headers appropriate for the sensors
     */
    public static <X> ARFF newData(Iterable<Sensor<X,?>> sensors) {
        ARFF data = new ARFF();
        sensors.forEach(s -> s.addToSchema(data));
        return data;
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis();

        try {
            procedure.accept(experiment, this);
        } catch (Throwable t) {
            //sense(t.getMessage());
            t.printStackTrace();
        }

        endTime = System.currentTimeMillis();

        data.setComment(experiment + ": " + procedure +
                "\t@" + startTime + ".." + endTime + " (" + new Date(startTime) + " .. " + new Date(endTime) + ")");
    }

    public Object[] record() {
        return Lab.record(experiment, data, sensors);
    }

}
