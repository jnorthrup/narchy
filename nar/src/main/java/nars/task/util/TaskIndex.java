package nars.task.util;

import nars.Task;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by me on 10/14/16.
 */
public interface TaskIndex {

    /**
     *
     * @param x
     * @return null if no existing task alredy present, non-null of the pre-existing one
     */
    @Nullable
    Task addIfAbsent( Task x);

    default  void remove( Task tt) {
        tt.delete();
        removeInternal(tt);
    }

    void removeInternal( Task tt);

    void clear();

    default void remove( List<Task> tt) {
        var s = tt.size();
        for (var aTt: tt) {
            this.remove(aTt);
        }
    }

    void forEach( Consumer<Task> each);








    default void addIfAbsent( List<Task> toAdd) {
        for (var aToAdd: toAdd) {
            addIfAbsent(aToAdd);
        }
    }

    boolean contains( Task t);
}
