package nars.concept.sensor;

import jcog.math.FloatRange;
import nars.NAR;
import nars.term.Termed;

/** base interface for a sensor that consists of one or a group of concepts, sharing:
 *          resolution
 *          priority
 *          cause channel
 *  */
public interface Sensor extends Termed {

    FloatRange resolution();
    FloatRange pri();

    void update(long last, long now, NAR nar);
}
