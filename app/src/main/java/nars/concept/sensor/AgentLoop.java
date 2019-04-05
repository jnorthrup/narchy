package nars.concept.sensor;

import jcog.math.FloatRange;
import nars.NAR;
import nars.agent.Game;
import nars.term.Termed;

/**
 * base interface for a repeatedly invoked procedure context
 * consisting of one or a group of concepts, sharing:
 *          resolution
 *          priority
 *          cause channel
 **/
public interface AgentLoop extends Termed {

    default void update(long last, long now, Game a) {
        update(last, now, a.nar());
    }

    /** run an update procedure, for the provided time period */
    @Deprecated default void update(long last, long now, NAR nar) {

    }

    /** numeric resolution of scalar signals */
    FloatRange resolution();

    /** the components of the sensor, of which there may be one or more concepts */
    Iterable<Termed> components();

}
