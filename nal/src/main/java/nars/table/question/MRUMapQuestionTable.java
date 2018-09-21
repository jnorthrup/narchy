package nars.table.question;

import jcog.data.map.MRUMap;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * unsorted, MRU policy.
 * this impl sucks actually
 * TODO make one based on ArrayHashSet
 */
public class MRUMapQuestionTable extends MRUMap<Task, Task> implements QuestionTable {


    public MRUMapQuestionTable() {
        super(0);
    }

    @Override
    public void setCapacity(int newCapacity) {
        synchronized (this) {
            super.setCapacity(newCapacity);


        }
    }


    @Override
    public void add(/*@NotNull*/ Remember r, NAR n) {
        Task u;
        Task t = r.input;
        synchronized (this) {
            u = merge(t, t, (prev, next) -> {
                r.merge(prev);
                return next;
            });
        }

    }


    @Override
    public Stream<? extends Task> streamTasks() {
        Task[] t = toArray();
        return t.length > 0 ? Stream.of(t) : Stream.empty();
    }

    public Task[] toArray() {

        int s = size();
        if (s == 0) {
            return Task.EmptyArray;
        } else {
            synchronized (this) {
                return values().toArray(new Task[s]);
            }
        }

    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        Task[] t = toArray();
        for (Task y : t) {
            if (y == null)
                continue;
            if (y.isDeleted()) {
                removeTask(y);
            } else {
                x.accept(y);
            }
        }
    }

    @Override
    public boolean removeTask(Task x) {
        synchronized (this) {
            return remove(x) != null;
        }
    }

    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
        }
    }

}
