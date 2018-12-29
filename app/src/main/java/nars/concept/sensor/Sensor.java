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

    void sense(long last, long now, NAR nar);

    /** numeric resolution of scalar signals */
    FloatRange resolution();

    /** the components of the sensor, of which there may be one or more concepts */
    Iterable<Termed> components();

}
