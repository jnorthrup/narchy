package nars.util;

import java.util.Random;

/** implementations provide a time-bound context which supports
 *  awareness of temporal determinism */
public interface Timed {

    float dur();

    Random random();

    long time();

}
