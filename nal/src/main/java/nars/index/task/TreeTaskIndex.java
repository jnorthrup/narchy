package nars.index.task;

import jcog.data.byt.AbstractBytes;
import jcog.tree.radix.MyRadixTree;
import nars.Task;
import nars.term.util.key.TermBytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by me on 10/14/16.
 */
public class TreeTaskIndex implements TaskIndex {


    public final MyRadixTree<Task> tasks = new MyRadixTree<>();

    @Override
    public boolean contains(Task t) {
        return tasks.getValueForExactKey(key(t))!=null;
    }

    @Override
    public @Nullable final Task addIfAbsent(@NotNull Task x) {

        Task y = tasks.putIfAbsent(key(x), x);
        
        if (y == x)
            return null;
        else {





            return y;
        }
    }

    @Override
    public final void removeInternal(@NotNull Task tt) {
        tasks.remove(key(tt));
    }

    @NotNull
    static AbstractBytes key(@NotNull Task x) {
        return new TermBytes(x);
    }


    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<Task> each) {
        tasks.forEach(each);
    }
}
