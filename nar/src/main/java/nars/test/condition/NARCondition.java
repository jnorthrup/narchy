package nars.test.condition;

import org.slf4j.Logger;

import java.io.Serializable;

/**
 * a condition which can be observed as being true or false
 * in the observed behavior of a NAR
 */
public interface NARCondition extends Serializable {





    boolean isTrue();



    /** max possible cycle time in which this condition could possibly be satisfied. */
    long getFinalCycle();

    default void log(Logger logger) {
        String s = toString();
        if (isTrue())
            logger.info(s);
        else
            logger.warn(s);
    }


}
