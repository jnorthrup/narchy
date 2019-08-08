package nars.concept.sensor;

import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import nars.agent.Game;
import nars.term.Termed;

import java.util.List;
import java.util.Random;

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

    default Termed get(Random random) {
        Iterable<Termed> cc = components();
        if (cc instanceof List) {
            List<Termed> ll = (List) cc;
            if (ll.size() == 1)
                return ll.get(0);
            else
                return ll.get(random.nextInt(ll.size()));
        }
        FasterList<Termed> a = new FasterList(cc); //HACK
        return a.get(random);
    }

}
