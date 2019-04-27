package nars.task;

import nars.Task;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


/** generic NAL Task with stored start,end time */
public class TemporalTask extends ActualNALTask {

    private final long start, end;

    protected TemporalTask(Task copied, Term newContent, @Nullable Truth newTruth) throws TaskException {
        this(newContent, copied.punc(), newTruth, copied.creation(), copied.start(), copied.end(), copied.stamp());
    }

    protected TemporalTask(Term term, byte punc, @Nullable Truth truth, long creation, long start, long end, long[] stamp) throws TaskException {
        super(term, punc, truth, creation, start, end, stamp);

        this.start = start;
        this.end = end;
    }


    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }


}
