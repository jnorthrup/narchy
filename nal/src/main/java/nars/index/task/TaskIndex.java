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
        for (Task aTt: tt) {
            this.remove(aTt);
        }
    }

    void forEach(@NotNull Consumer<Task> each);








    default void addIfAbsent(@NotNull List<Task> toAdd) {
        for (Task aToAdd: toAdd) {
            addIfAbsent(aToAdd);
        }
    }

    boolean contains(@NotNull Task t);
}
