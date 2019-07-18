package nars.sensor.util;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.derive.model.Derivation;
import nars.link.AtomicTaskLink;
import nars.link.TaskLink;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.Termed;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.BELIEF;

public abstract class DynamicSensorTaskLink extends AtomicTaskLink {

    public DynamicSensorTaskLink(Term src) {
        super(src);
    }

    @Override
    public @Nullable Task get(byte punc, When<NAR> when, Predicate<Task> filter) {
        Termed t = src(when);
        if (t!=null) {
            TaskTable table = when.x.tableDynamic(t, BELIEF);
            if (table != null) {
                return table.sample(when, null, filter);
            }
        }
        return null;
    }

    protected abstract Termed src(When<NAR> when);

    @Override
    abstract public Term target(Task task, Derivation d);

    @Override
    public @Nullable Term forward(Term target, TaskLink link, Task task, Derivation d) {
        //return task.term();
        return null;
    }

}
