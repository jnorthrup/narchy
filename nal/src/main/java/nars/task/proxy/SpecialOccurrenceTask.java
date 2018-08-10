package nars.task.proxy;

import nars.Task;
import nars.task.TaskProxy;

public class SpecialOccurrenceTask extends TaskProxy {
    protected final long start;
    protected final long end;

    public SpecialOccurrenceTask(Task task, long start, long end) {
        super(task);
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
