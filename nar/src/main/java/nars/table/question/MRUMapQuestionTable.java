package nars.table.question;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.map.MRUMap;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;

import java.util.function.BiFunction;
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
    public synchronized void setTaskCapacity(int newCapacity) {
        super.setCapacity(newCapacity);
    }

    @Override
    public final int taskCount() {
        return size();
    }

    @Override
    public void remember( Remember r) {
        Task t = r.input;
        synchronized (this) {
            Task u = merge(t, t, new BiFunction<Task, Task, Task>() {
                @Override
                public Task apply(Task prev, Task next) {
                    r.merge(prev);
                    return next;
                }
            });
        }

    }
    @Override
    public void match(Answer a) {
        //sample(m.nar.random(), size(), m::tryAccept);
        throw new TODO();
    }

    @Override
    public Stream<? extends Task> taskStream() {
        return ArrayIterator.stream(taskArray());
    }

    public Task[] taskArray() {

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
        Task[] t = taskArray();
        for (Task y : t) {
            if (y == null)
                continue;
            if (y.isDeleted()) {
                removeTask(y, false);
            } else {
                x.accept(y);
            }
        }
    }

    @Override
    public synchronized boolean removeTask(Task x, boolean delete) {
        Task r = remove(x);
        if (r != null) {
            if (delete)
                r.delete();
            return true;
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

}
