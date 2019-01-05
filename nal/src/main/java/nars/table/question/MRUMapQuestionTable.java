package nars.table.question;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.map.MRUMap;
import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;

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
    public void match(Answer m) {
        //sample(m.nar.random(), size(), m::tryAccept);
        throw new TODO();
    }

    @Override
    public Stream<? extends Task> streamTasks() {
        return ArrayIterator.stream(toArray());
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
