package nars.game.sensor;

import jcog.data.list.FasterList;
import nars.game.Game;
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

//    /** numeric resolution of scalar signals */
//    @Deprecated FloatRange resolution();

    /** the components of the sensor, of which there may be one or more concepts
     * @return*/
    Iterable<? extends Termed> components();

    default Termed get(Random random) {
        Iterable<? extends Termed> cc = components();
        if (cc instanceof List) {
            List<Termed> ll = (List) cc;
            int s = ll.size();
            return s == 1 ? ll.get(0) : ll.get(random.nextInt(s));
        }
        FasterList<Termed> a = new FasterList(cc); //HACK
        return a.get(random);
    }

}
