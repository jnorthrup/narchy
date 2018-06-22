package nars.control.proto;

import jcog.TODO;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.NativeTask;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public final class Remember extends NativeTask {


    public final Task input;

    final FasterList<ITask> next = new FasterList(2);
    final FasterList<Task> remembered = new FasterList(2);
    public final FasterList<Task> forgotten = new FasterList(2);
    public final Concept concept;

    @Nullable public static Remember the(Task input, NAR n) {


        Concept concept = input.concept(n, true);
        if (concept == null) {
            return null;
        } else if (!(concept instanceof TaskConcept)) {
            input.delete();
            if (Param.DEBUG_EXTRA && input.isBeliefOrGoal()) {
                throw new TODO("why?: " + input + " does not resolve a TaskConcept:\n" + concept);
            } else
                return null;
        }

        return new Remember(input, concept);
    }

    public Remember(Task task, Concept c) {
        this.input = task;
        this.concept = c;
    }

    @Override
    public String toString() {
        return "Remember(" + input + ")";
    }

    @Override
    public final ITask next(NAR n) {

        validate(n);

        try {

            ((TaskConcept) concept).add(this, n);

        } finally {
            if (!forgotten.isEmpty() || !remembered.isEmpty()) {
                next.add(new Commit(forgotten, remembered));
            }
            return NativeTask.of(next);
        }

    }

    private void validate(NAR n) {
        //verify dithering
        if (Param.DEBUG) {
            if (!input.isInput()) {
                Truth t = input.truth();
                if (t != null)
                    t.ensureDithered(n);
            }
        }
    }

    //TODO: private static final class ListTask extends FasterList<ITask> extends NativeTask {

    private static final class Commit extends NativeTask {

        FasterList<Task> forgotten, remembered;

        public Commit(FasterList<Task> forgotten, FasterList<Task> remembered) {
            super();
            this.forgotten = forgotten; this.remembered = remembered;
        }

        @Override
        public ITask next(NAR n) {
            forgotten.forEach(Task::delete);
            remembered.forEach(n.eventTask::emit);
            return null;
        }
    }


    public void forget(Task x) {
        if (remembered.removeInstance(x)) {
            throw new TODO();
            //TODO filter next tasks with any involving that task
        }
        add(x, this.forgotten);
    }

    public void remember(Task x) {
        if (forgotten.removeInstance(x)) {
            throw new TODO();
            //TODO filter next tasks with any involving that task
        }
        if (add(x, this.remembered))
            next.add(new TaskLinkTask(x, concept));
    }


    public void merge(Task existing) {
        if (existing != input) {

            assert (!input.isDeleted()); //dont delete just yet

            //TODO decide how much to re-activate
            //TODO consider forgetting rate

            if (existing instanceof NALTask)
                ((NALTask) existing).causeMerge(input);
            forget(input);
        }


        next(new TaskLinkTask(existing, concept));


    }

    public void next(ITask n) {
        if (n!=null)
            next.add(n);
    }

    public final void reject() {
        forget(input);
    }

    public final boolean isEternal() {
        return input.isEternal();
    }
    public final byte punc() {
        return input.punc();
    }

    private static boolean add(Task x, FasterList<Task> f) {
        if (x!=null) {
            if (!f.isEmpty() && f.containsInstance(x))
                return false;

            f.add(x);
            return true;
        }
        return false;
    }


}
