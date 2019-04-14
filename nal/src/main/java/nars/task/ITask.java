package nars.task;

import jcog.Log;
import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import nars.NAR;
import nars.attention.What;
import org.slf4j.Logger;

/**
 * generic abstract task used for commands and other processes
 * a procedure which can/may be executed.
 * competes for execution time among other
 * items
 * <p>
 * * controls the relative amount of effort spent in 3 main ways:
 * <p>
 * perception
 * processing input and activating its concepts
 * <p>
 * hypothesizing
 * forming premises
 * <p>
 * proving
 * exploring the conclusions derived from premises, which arrive as new input
 *
 * @param X identifier key
 */
public interface ITask extends Prioritizable {

    default ITask next(What w) {
        return next(w.nar);
    }

    /**
     * process the next stage; returns null if finished
     */
    @Deprecated ITask next(NAR n);


    byte punc();


    static void run(FasterList<ITask> t, NAR nar) {
        t.forEachWith(ITask::run, nar);
    }
    static void run(FasterList<ITask> t, What w) {
        t.forEachWith(ITask::run, w);
    }

    /**
     * continues executing the chain of returned tasks until the end
     * TODO rewrite as ForkJoin recursive task
     */
    @Deprecated static void run(ITask t, NAR nar) {
        ITask x = t;
        do {
            x = x.next(nar);
        } while (x != null);
    }

    /** TODO rewrite as ForkJoin recursive task */
    static void run(ITask t, What w) {
        ITask x = t;
        do {
            x = x.next(w);
        } while (x != null);
    }

    static void error(Prioritizable t, Prioritizable x, Throwable ee, NAR nar) {
        if (t == x)
            ITask.logger.error("{} {}", x, ee);
        else
            ITask.logger.error("{}->{} {}", t, x, ee);
    }

    Logger logger = Log.logger(ITask.class);

}
