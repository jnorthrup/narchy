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

    static final String blankContext = " ";

    final E experiment;
    private final BiConsumer<E, ExperimentRun<E>> procedure;

    /** data specific to this experiment; can be merged with multi-experiment
     * data collections later */
    public final ARFF data;

    /** enabled sensors */
    private final List<Sensor<E,?>> sensors;
    private long startTime;
    private long startNano;
    private long endTime;

    public ExperimentRun(BiConsumer<E, ExperimentRun<E>> procedure, E model, Iterable<Sensor<E,?>> sensors) {
        this.experiment = model;
        this.procedure = procedure;

        data = newData(sensors);
        this.sensors = new FasterList(sensors);
    }


    /** creates a new ARFF data with the headers appropriate for the sensors */
    public static <X> ARFF newData(Iterable<Sensor<X,?>> sensors) {
        ARFF data = new ARFF();
        data.defineNumeric("time");
        data.defineText("context");

        sensors.forEach(s -> {
            s.addToSchema(data);
        });
        return data;
    }

    @Override public void run() {
        startTime = System.currentTimeMillis();
        startNano = System.nanoTime();

        sense("start");

        try {
            procedure.accept(experiment, this);
        } catch (Throwable t) {
            sense(t.getMessage());
        }

        endTime = System.currentTimeMillis();

        sense("end");

        data.setComment(experiment + ": " + procedure +
                "\t@" + startTime + ".." + endTime + " (" + new Date(startTime) + " .. " + new Date(endTime) + ")");
    }

    /** records all sensors (blank context)*/
    public void sense() {

        sense(blankContext);
    }

    public void sense(String context) {
        synchronized (experiment) {
            long whenNano = System.nanoTime();

            Object row[] = new Object[sensors.size() + 2];
            int c = 0;
            row[c++] = whenNano - startNano;
            row[c++] = context;
            for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
                row[c++] = sensors.get(i).apply(experiment);
            }
            data.add(row);
        }
    }
}
