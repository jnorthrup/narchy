package nars;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by me on 10/14/16.
 */
abstract public class TaskIndex {



    /**
     *
     * @param x
     * @return null if no existing task alredy present, non-null of the pre-existing one
     */
    @Nullable
    public abstract Task add(@NotNull Task x);

    public abstract void remove(@NotNull Task tt);

    public abstract void clear();

    public final void remove(@NotNull List<Task> tt) {
        int s = tt.size();
        for (int i = 0; i < s; i++) {
            this.remove(tt.get(i));
        }
    }

}
