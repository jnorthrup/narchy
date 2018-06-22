package nars.util;

import java.util.Random;

/** implementations provide a time-bound context which supports
 *  awareness of temporal determinism */
public interface TimeAware {

    int dur();

    Random random();

    long time();

}
