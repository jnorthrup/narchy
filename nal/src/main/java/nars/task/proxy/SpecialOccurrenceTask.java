package nars.task.proxy;

import nars.Task;
import nars.task.ProxyTask;

public class SpecialOccurrenceTask extends ProxyTask {
    protected final long start;
    protected final long end;

    public SpecialOccurrenceTask(Task task, long[] startEnd) {
        this(task, startEnd[0], startEnd[1]);
        assert(startEnd.length == 2);
    }

    public SpecialOccurrenceTask(Task task, long start, long end) {
        super(task instanceof SpecialOccurrenceTask ? ((SpecialOccurrenceTask)task).task : task);
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
