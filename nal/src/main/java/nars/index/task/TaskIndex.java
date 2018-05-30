package nars.index.task;

import nars.Task;
import org.jetbrains.annotations.NotNull;
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
    Task addIfAbsent(@NotNull Task x);

    default  void remove(@NotNull Task tt) {
        tt.delete();
        removeInternal(tt);
    }

    void removeInternal(@NotNull Task tt);

    void clear();

    default void remove(@NotNull List<Task> tt) {
        int s = tt.size();
        for (int i = 0; i < s; i++) {
            this.remove(tt.get(i));
        }
    }

    void forEach(@NotNull Consumer<Task> each);








    default void addIfAbsent(@NotNull List<Task> toAdd) {
        for (int i = 0, toAddSize = toAdd.size(); i < toAddSize; i++) {
            addIfAbsent(toAdd.get(i));
        }
    }

    boolean contains(@NotNull Task t);
}
