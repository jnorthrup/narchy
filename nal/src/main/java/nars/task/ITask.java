package nars.task;

import jcog.pri.Prioritizable;
import nars.NAR;

/**
 * generic abstract task used for commands and other processes
 * a procedure which can/may be executed.
 * competes for execution time among other
 * items
 *
 *  * controls the relative amount of effort spent in 3 main ways:
 *
 *      perception
 *         processing input and activating its concepts
 *
 *      hypothesizing
 *         forming premises
 *
 *      proving
 *         exploring the conclusions derived from premises, which arrive as new input
 *
 * @param X identifier key
 */
public interface ITask extends Prioritizable {

    /** process the next stage; returns null if finished */
    ITask next(NAR n);

    byte punc();

    /** continues executing the chain of returned tasks until the end */
    static void run(ITask t, NAR nar) {
        ITask x = t;
        do {
            try {

                x = x.next(nar);

            } catch (Throwable ee) {
                error(t, x, ee, nar);
                break;
            }
        } while (x != null) ;
    }

    static void error(ITask t, ITask x, Throwable ee, NAR nar) {
        if (t==x)
            nar.logger.error("{} {}", x, ee);
        else
            nar.logger.error("{}->{} {}", t, x, ee);
    }

}
