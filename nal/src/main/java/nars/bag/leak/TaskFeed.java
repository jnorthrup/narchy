package nars.bag.leak;

import nars.NAR;
import nars.Task;
import nars.control.CauseChannel;
import nars.exe.Causable;
import nars.task.ITask;

import java.util.List;

/** TODO */
public class TaskFeed extends Causable {

    private final CauseChannel<ITask> in;

    public TaskFeed(NAR nar, List<Task> inputs) {
        super(nar);
        this.in = nar.newCauseChannel(this);
    }

    @Override
    protected int next(NAR n, int iterations) {
        return 0;
    }

    @Override
    public float value() {
        return 0;
    }
}
