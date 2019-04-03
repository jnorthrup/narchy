package nars.table.question;

import jcog.data.NumberX;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;
import nars.term.Term;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class HijackQuestionTable extends PriHijackBag<Task, Task> implements QuestionTable {

    public HijackQuestionTable(int cap, int reprobes) {
        super(cap, reprobes);
    }

    @Override
    protected Task merge(Task existing, Task incoming, NumberX overflowing) {
        return existing;
    }


    @Override
    public void match(Answer m) {
        sample(m.nar.random(), m.tasks.capacity(), m::tryAccept);
    }

    /** optimized for cases with zero and one stored tasks */
    @Override public Task match(long start, long end, Term template, int dur, NAR nar) {
        switch (size()) {
            case 0: return null;
            case 1:
                return next(0,t->false);
            default:
                return QuestionTable.super.match(start, end, template, dur, nar);
        }
    }

    @Override
    public final Task key(Task value) {
        return value;
    }

    @Override
    public final boolean isEmpty() {
        return super.isEmpty();
    }


    @Override
    public void remember(Remember r) {
        Task x = r.input;
        Task y = put(x, null);
        if (y == x) {
            r.remember(x);
        } else {
            if (y != null) {
                r.merge(y); //existing
            } else
                r.forget(x);
        }

        commit(forget(r.nar.attn.decay.floatValue() /* estimate */));

        //TODO track displaced questions
    }

    @Override
    public final int taskCount() {
        return size();
    }

    @Override
    public final void setTaskCapacity(int newCapacity) {
        setCapacity(newCapacity);
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        forEachKey(x);
    }

    @Override
    public boolean removeTask(Task x, boolean delete) {
        Task r = remove(x);
        if (r != null) {
            if (delete)
                r.delete();
            return true;
        }
        return false;
    }

    @Override
    public Stream<? extends Task> taskStream() {
        return stream();
    }

}
