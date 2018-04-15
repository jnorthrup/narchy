package nars.task;

import jcog.pri.Priority;
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
public interface ITask extends Priority {

    /** process the next stage; returns null if finished */
    ITask next(NAR n);

    byte punc();

    /** continues executing the chain of returned tasks until the end */
    default void run(NAR nar) {
        ITask x = this;
        while ((x = x.next(nar)) != null) ;
    }

}
