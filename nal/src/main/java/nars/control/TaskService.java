package nars.control;

import nars.NAR;
import nars.Task;

import java.util.function.BiConsumer;

/**
 * Service which reacts to NAR TaskProcess events
 */
abstract public class TaskService extends NARService implements BiConsumer<NAR, Task> {

    @Override
    protected void starting(NAR nar) {
        ons.add(nar.onTask((t) -> accept(nar, t)));
    }

    protected TaskService(NAR nar) {
        super(nar);
    }

}
