package nars.task;

import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.Nullable;

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


    /**
     * note: the first null in the returned array will break the iteration because it means its the end of the list (all following it should also be null)
     */
    @Nullable Iterable<? extends ITask> run(NAR n);



    default byte punc() {
        return 0;
    }


    default boolean isInput() {
        return false;
    }

    default boolean persist() { return false; }

    /**
     * fluent form of setPri which returns this class
     */
    default ITask pri(float p) {
        priSet(p);
        return this;
    }
}
