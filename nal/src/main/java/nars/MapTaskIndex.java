package nars;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static nars.concept.CompoundConcept.DuplicateMerge;

/**
 * Created by me on 8/14/16.
 */
public final class MapTaskIndex extends TaskIndex {

    final static Logger logger = LoggerFactory.getLogger(MapTaskIndex.class);

    @NotNull
    protected final Map<Task, Task> tasks;

    public MapTaskIndex() {
        this(16 * 1024);
    }

    public MapTaskIndex(int initialCapacity) {
        this.tasks =
                new ConcurrentHashMap<>(initialCapacity/* estimate TODO */);
                //new ConcurrentHashMapUnsafe(128 * 1024 /* estimate TODO */);

        //Caffeine.newBuilder()

//                .removalListener((k,v,cause) -> {
//                    if (cause != RemovalCause.EXPLICIT)
//                        logger.error("{} removal: {},{}", cause, k, v);
//                })

//                .build();
//        tasks.cleanUp();
//
//        this.tasksMap = tasks.asMap();
    }


    public void removeDeleted() {
        Iterator<Task> ii = tasks.values().iterator();
        while (ii.hasNext()) {
            Task x = ii.next();
            if (x.isDeleted()) {
                logger.error("lingering deleted task: {}", x);
                ii.remove();
            }
        }
    }


    @Override
    public Task add(@NotNull Task x) {
        return tasks.putIfAbsent(x,x);
    }


    @Override
    public final void remove(@NotNull Task tt) {
        tasks.remove(tt);
        tt.delete();
    }

    @Override
    public void clear() {
        tasks.clear();
    }

}
