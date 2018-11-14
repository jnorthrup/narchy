package nars.budget;

import jcog.pri.bag.Bag;
import nars.NAR;
import nars.concept.Concept;
import nars.link.TaskLink;
import nars.task.Tasklike;

import java.util.function.Consumer;

/** TODO abstract */
public class Forgetting {

    public void update(NAR n) {

    }

    public void update(Concept c, NAR n) {


        int dur = n.dur();

        Consumer<TaskLink> tasklinkUpdate;
        Bag<Tasklike, TaskLink> tasklinks = c.tasklinks();

        long curTime = n.time();
        Long prevCommit = c.meta("C", curTime);
        if (prevCommit != null) {
            if (curTime - prevCommit > 0) {

                double deltaDurs = ((double) (curTime - prevCommit)) / dur;

                //deltaDurs = Math.min(deltaDurs, 1);

                float forgetRate = (float) (1 - Math.exp(-deltaDurs / n.memoryDuration.doubleValue()));

                //System.out.println(deltaDurs + " " + forgetRate);
                tasklinkUpdate = tasklinks.forget(forgetRate);

            } else {
                //dont need to commit, it already happened in this cycle
                return;
            }
        } else {
            tasklinkUpdate = null;

        }

        tasklinks.commit(tasklinkUpdate);

    }
}
