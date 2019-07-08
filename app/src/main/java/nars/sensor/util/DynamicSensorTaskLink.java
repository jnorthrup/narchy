package nars.sensor.util;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.derive.model.Derivation;
import nars.link.AtomicTaskLink;
import nars.link.TaskLink;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

public abstract class DynamicSensorTaskLink extends AtomicTaskLink {



    public DynamicSensorTaskLink(Term src) {
        super(src);
    }

    @Override
    public @Nullable Task get(byte punc, When<NAR> when, Predicate<Task> filter) {
        Concept t = src(when);
        return t!=null ? t.beliefs().match(when, null, filter, false) : null;
    }

    protected abstract Concept src(When<NAR> when);

    @Override
    abstract public Term target(Task task, Derivation d);

    @Override
    public @Nullable Term forward(Term target, TaskLink link, Task task, Derivation d) {
        //return task.term();
        return null;
    }


}
