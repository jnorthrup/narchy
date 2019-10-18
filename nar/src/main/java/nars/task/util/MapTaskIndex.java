package nars.task.util;

import nars.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 8/14/16.
 */
public final class MapTaskIndex implements TaskIndex {

    private static final Logger logger = LoggerFactory.getLogger(MapTaskIndex.class);

    
    private final Map<Task, Task> tasks;

    private MapTaskIndex(boolean concurrent) {
        this(concurrent, 1);
    }

    private MapTaskIndex(boolean concurrent, int initialCapacity) {
        this.tasks =
                concurrent ?
                    new ConcurrentHashMap<>(initialCapacity/* estimate TODO */):
                        
                    new HashMap<>(initialCapacity);

        










    }

    @Override
    public final boolean contains(Task t) {
        return tasks.containsKey(t);
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
    public Task addIfAbsent( Task x) {
        return tasks.putIfAbsent(x,x);
    }


    @Override
    public final void removeInternal( Task tt) {
        tasks.remove(tt);
    }

    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public void forEach( Consumer<Task> each) {
        for (Map.Entry<Task, Task> entry : tasks.entrySet()) {
            Task k = entry.getKey();
            Task v = entry.getValue();
            each.accept(v);
        }
    }
}
