package nars.task.util;

import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.event.On;
import jcog.event.Ons;
import nars.NAR;
import nars.Task;
import nars.derive.Deriver;
import nars.derive.impl.SimpleDeriver;

import java.util.Collection;
import java.util.stream.Collectors;

public class DialogTask {

    final ConcurrentFastIteratingHashSet<Task> tasks = new ConcurrentFastIteratingHashSet<>(new Task[0]);
    private final Deriver deriver;
    private final Ons ons;
    private final On monitor;
    private final NAR nar;

    public void add(Task t) {
        tasks.add(t);
    }

    public DialogTask(NAR n,Task... input) {
        this.nar = n;
        for (Task i : input) {
            add(i);
        }

        ons= new Ons(

            deriver = SimpleDeriver.forConcepts(n, tasks.asList().stream().map(t -> {

                nar.input(t);

                if (t != null)
                    return nar.concept(t.term(), true);
                else
                    return null;

            }).collect(Collectors.toList())),

            monitor = n.onTask(this::onTask)
        );
    }

    public void off() {
        ons.off();
    }

    protected void onTask(Collection<Task> x) {
        x.removeIf(t -> !this.onTask(t));
        nar.input(x);
    }

    /** return false to filter this task from input */
    protected boolean onTask(Task x) {
        return true;
    }

}
