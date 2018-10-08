package nars.table.question;

import jcog.TODO;
import jcog.data.map.MRUMap;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
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
    public synchronized void setCapacity(int newCapacity) {
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
    public synchronized boolean removeTask(Task x) {
        return remove(x) != null;
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

}
