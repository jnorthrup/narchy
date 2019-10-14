package nars.game.sensor;

import nars.game.Game;
import nars.term.Termed;

import java.util.function.Consumer;

/**
 * base interface for a repeatedly invoked procedure context
 * consisting of one or a group of concepts, sharing:
 *          resolution
 *          priority
 *          cause channel
 **/
public interface GameLoop extends Termed, Consumer<Game> {

    /** the components of the sensor, of which there may be one or more concepts
     * @return*/
    Iterable<? extends Termed> components();

}
