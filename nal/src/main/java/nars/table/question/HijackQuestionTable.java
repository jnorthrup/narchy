package nars.table.question;

import jcog.data.NumberX;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
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
    public void clear() {
        forEachTask(ScalarValue::delete);
        super.clear();
    }

    @Override
    public void match(Answer m) {
        sample(m.nar.random(), size(), m::tryAccept);
    }

    /** optimized for cases with zero and one stored tasks */
    @Override public Task match(long start, long end, Term template, NAR nar) {
        switch (size()) {
            case 0: return null;
            case 1:
                return next(0,(t)->false);
            default:
                return QuestionTable.super.match(start, end, template, nar);
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
    public void add(Remember r, NAR n) {
        Task x = put(r.input, null);
        if (x != r.input) {
            if (x != null) {
                assert (x.equals(r.input));
                r.merge(x); //existing
            } else
                r.reject();
        } else {
            r.remember(x);
            commit();
        }
        //TODO track displaced questions
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        forEachKey(x);
    }

    @Override
    public boolean removeTask(Task x) {
        return remove(x) != null;
    }

    @Override
    public Stream<? extends Task> streamTasks() {
        return stream();
    }

}
