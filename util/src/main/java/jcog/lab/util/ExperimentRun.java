package jcog.lab.util;

import jcog.io.arff.ARFF;
import jcog.lab.Sensor;
import jcog.list.FasterList;

import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

/**
 *  an instance of an experiment, conducting the experiment, collecting data as it runs;
 *  the integration of a subject, a repeatable procedure, and measurement schema
 *
 *  contains:
 *     -all or some of the Lab's sensors
 *     -executable procedure for applying the starting conditions to the subject via
 *      some or all of the variables
 *     -executable schedule for recording sensor measurements, with at least
 *      the start and ending state enabled by default. TODO
 */
public class ExperimentRun<E> implements Runnable {

    final E experiment;
    private final BiConsumer<E, ExperimentRun<E>> procedure;

    /** data specific to this experiment; can be merged with multi-experiment
     * data collections later */
    public final ARFF data;

    /** enabled sensors */
    private final List<Sensor<E,?>> sensors;
    private long startTime;
    private long endTime;

    public ExperimentRun(E model, ARFF data, List<Sensor<E, ?>> sensors, BiConsumer<E, ExperimentRun<E>> procedure) {
        this.experiment = model;
        this.procedure = procedure;
        this.data = data;
        this.sensors = sensors;
    }

    public ExperimentRun(E model, Iterable<Sensor<E,?>> sensors, BiConsumer<E, ExperimentRun<E>> procedure) {
        this(model, newData(sensors), new FasterList(sensors), procedure);
    }


    /** creates a new ARFF data with the headers appropriate for the sensors */
    public static <E> ARFF newData(Iterable<Sensor<E,?>> sensors) {
        ARFF data = new ARFF();
        sensors.forEach(s -> s.addToSchema(data));
        return data;
    }

    @Override public void run() {
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


    /** records all sensors ()*/
    public void record() {
        synchronized (experiment) {
            Object row[] = new Object[sensors.size()];
            int c = 0;
            for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
                row[c++] = sensors.get(i).apply(experiment);
            }
            data.add(row);
        }
    }
}
