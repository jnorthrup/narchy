package nars.task.util;

import jcog.Util;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import org.apache.commons.math3.stat.Frequency;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by me on 10/31/16.
 */
public class TaskStatistics {
    private final AtomicInteger i = new AtomicInteger(0);

    private final Frequency clazz = new Frequency();
    private final Frequency volume = new Frequency();
    private final Frequency rootOp = new Frequency();
    private final Frequency punc = new Frequency();
    private final Frequency eviLength = new Frequency();

    private final Frequency freq = new Frequency();
    private final Frequency conf = new Frequency();
    private final Frequency pri = new Frequency();

    public TaskStatistics add(NAR nar) {
        nar.tasks().forEach(this::add);
        return this;
    }

    public TaskStatistics add(Concept c) {
        c.tasks(true, true, true, true).forEach(this::add);
        return this;
    }

    public TaskStatistics add(Iterable<Task> c) {
        c.forEach(this::add);
        return this;
    }

    private void add(Task t) {

        if (t.isDeleted())
            return;

        i.incrementAndGet();
        
        volume.addValue(t.volume());
        rootOp.addValue(t.op());
        clazz.addValue(t.getClass().toString());
        punc.addValue(t.punc());
        eviLength.addValue(t.stamp().length);

        if (t.isBeliefOrGoal()) {
            freq.addValue(Util.round(t.freq(), 0.1f));
            conf.addValue(Util.round(t.conf(), 0.1f));
        }
        pri.addValue(Util.round(t.pri(), 0.1f));

    }

    private void print(PrintStream out) {
        out.println("-------------------------------------------------");
        out.println("Total Tasks:\n" + i.get());

        out.println("\npunc:\n" + punc);
        out.println("\nrootOp:\n" + rootOp);
        out.println("\nvolume:\n" + volume);
        out.println("\nevidence:\n" + eviLength);
        out.println("\nclass:\n" + clazz);

        out.println("\nfreq:\n" + freq);
        out.println("\nconf:\n" + conf);
        out.println("\npri:\n" + pri);

    }


    public void print() {
        print(System.out);
    }
}
