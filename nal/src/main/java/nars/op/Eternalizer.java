package nars.op;

import jcog.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.CauseChannel;
import nars.exe.AbstractExec;
import nars.exe.Causable;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.ITask;
import nars.task.Revision;
import nars.task.TaskProxy;
import nars.task.util.TaskRegion;
import org.apache.commons.math3.stat.descriptive.MultivariateSummaryStatistics;

import java.util.Random;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

public class Eternalizer extends Causable {

    private final CauseChannel<ITask> in;
    private final double maxFreqStdev = 1;
    private final double maxDtDurStdev = 1;

    public Eternalizer(NAR nar) {
        super(nar);
        in = nar.newCauseChannel(this);
    }

    final int minBeliefs = 4;

    @Override
    protected int next(NAR n, int iterations) {

        Random rng = n.random();

        int dur = n.dur();

        /** freq, dt, ... */
        MultivariateSummaryStatistics stats = new MultivariateSummaryStatistics(2, true);
        FasterList<TaskRegion> tt = new FasterList() {
            @Override
            protected Object[] newArray(int newCapacity) {
                return new TaskRegion[newCapacity];
            }
        };
        FasterList<Task> built = new FasterList(iterations);

        for (int i = 0; i < iterations; i++) {
            Activate nextLink = ((AbstractExec) n.exe).active.sample(rng);
            if (nextLink == null)
                break;
            Concept next = nextLink.get();
            if (next==null)
                break;
            int beliefs = next.beliefs().size();
            if (beliefs < minBeliefs)
                continue;
            TemporalBeliefTable temporalBeliefs = ((DefaultBeliefTable) next.beliefs()).temporal;
            if (temporalBeliefs.size() < minBeliefs)
                continue;

            stats.clear();


            tt.clear();
            temporalBeliefs.forEachTask(t -> {
                double dt = t.dt();
                if (dt == DTERNAL) dt = 0;
                stats.addValue(new double[] { t.freq(), dt/dur });
                tt.add(t);
            });
            double[] stdev = stats.getStandardDeviation();
            if (stdev[0] < maxFreqStdev && stdev[1] < maxDtDurStdev) {

                //strength reduction: effectively takes the average
                float factor = 1f / tt.size();

                tt.replaceAll(x -> TaskProxy.eternalized((Task) x, factor));
                ((FasterList) tt).sortThisByFloat(t -> -((Task)t).evi());

                Task r = Revision.mergeTemporal(nar, ETERNAL, ETERNAL, tt);

                if (r!=null) {
                    built.add(r); //TODO buffer all generated, make a wrapper for in and use this in other classes like ConjClustering
                }
            } else {
                //too much variance
                //System.out.println(tt);
            }
        }
        int eternalizations = built.size();
        if (eternalizations > 0) {
            in.input(built);
        }
        return eternalizations;
    }

    @Override
    public float value() {
        return in.value();
    }

}
