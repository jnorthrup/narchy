package nars.link;

import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.Termed;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.BELIEF;

public abstract class DynamicTaskLink extends AtomicTaskLink {

    public DynamicTaskLink(Term src) {
        super(src);
    }

    @Override
    public @Nullable Task get(byte punc, When<NAR> w, Predicate<Task> filter) {
        Termed t = src(w);
        if (t!=null) {
            TaskTable table = w.x.tableDynamic(t, BELIEF);
            if (table != null) {
                return table.sample(w, null, filter);
            }
        }
        return null;
    }

    protected abstract Termed src(When<NAR> when);

    @Override
    abstract public Term target(Task task, Derivation d);

}
