package nars.task;

import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import nars.NAR;

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

    /**
     * process the next stage; returns null if finished
     */
    ITask next(NAR n);

    byte punc();

    static void run(Iterable<ITask> t, NAR nar) {
        for (ITask tt: t)
            run(tt, nar);
    }

    static void run(FasterList<ITask> t, NAR nar) {
        t.forEachWith(ITask::run, nar);
    }

    /**
     * continues executing the chain of returned tasks until the end
     */
    static void run(ITask t, NAR nar) {
        ITask x = t;
        try {
            do {
                x = x.next(nar);
            } while (x != null);
        } catch (Throwable ee) {
            error(t, x, ee, nar);
        }
    }

    static void error(ITask t, ITask x, Throwable ee, NAR nar) {
        if (t == x)
            nar.logger.error("{} {}", x, ee);
        else
            nar.logger.error("{}->{} {}", t, x, ee);
    }

}
