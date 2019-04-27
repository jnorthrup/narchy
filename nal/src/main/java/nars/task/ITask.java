package nars.task;

import jcog.Log;
import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
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
@Deprecated public interface ITask extends Prioritizable {

    byte punc();

    /**
     * process the next stage; returns null if finished
     */
    @Deprecated ITask next(Object n);

    @Deprecated static <W> void run(FasterList<ITask> t, W w) {
        t.forEachWith(ITask::run, w);
    }

    /** TODO rewrite as ForkJoin recursive task */
    @Deprecated static <W> void run(ITask t, W w) {
        ITask x = t;
        do {
            x = x.next(w);
        } while (x != null);
    }

    static void error(Prioritizable t, Prioritizable x, Throwable ee) {
        if (t == x)
            ITask.logger.error("{} {}", x, ee);
        else
            ITask.logger.error("{}->{} {}", t, x, ee);
    }

    Logger logger = Log.logger(ITask.class);

}
