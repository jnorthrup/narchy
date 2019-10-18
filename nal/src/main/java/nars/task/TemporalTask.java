package nars.task;

import nars.Task;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


/** generic NAL Task with stored start,end time */
public class TemporalTask extends ActualNALTask {

    private final long start;
    private final long end;

    public TemporalTask(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) throws TaskException {
        super(term, punc, truth, creation, start, end, stamp);

        this.start = start;
        this.end = end;
    }

    public TemporalTask(Term c, Task parent, Truth t, long creation) throws TaskException {
        this(c, parent.punc(), t, creation, parent.start(), parent.end(), parent.stamp());
        assert(!c.equals(parent.term()) || !Objects.equals(t, parent.truth())):
                "same parent term (" + c + " =?= " + parent.term() + ") and same truth (" + t + " =?= " + parent.truth();
    }


    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }

    @Deprecated public static class Unevaluated extends TemporalTask implements UnevaluatedTask {

        public Unevaluated(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) {
            super(term, punc, truth, creation, start, end, stamp);
        }


        public Unevaluated(Term c, Task xx, Truth t, long creation) {
            super(c, xx, t, creation);
        }
    }
}
