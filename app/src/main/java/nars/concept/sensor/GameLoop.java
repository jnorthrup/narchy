package nars.concept.sensor;

import jcog.math.FloatRange;
import nars.agent.Game;
import nars.term.Termed;

/**
 * base interface for a repeatedly invoked procedure context
 * consisting of one or a group of concepts, sharing:
 *          resolution
 *          priority
 *          cause channel
 **/
public interface GameLoop extends Termed {

    void update(Game a);

    /** numeric resolution of scalar signals */
    FloatRange resolution();

    /** the components of the sensor, of which there may be one or more concepts */
    Iterable<Termed> components();


}
