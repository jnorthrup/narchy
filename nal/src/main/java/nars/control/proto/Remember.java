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
public class Remember extends NativeTask {


    public final Task input;

    final FasterList<ITask> next = new FasterList(2);
    final FasterList<Task> remembered = new FasterList(2);
    public final FasterList<Task> forgotten = new FasterList(2);
    public final Concept concept;

    @Nullable public static Remember the(Task input, NAR n) {

        //verify dithering
        if (Param.DEBUG) {
            if (!input.isInput()) {
                Truth t = input.truth();
                if (t != null)
                    t.ensureDithered(n);
            }
        }


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

    private Remember(Task task, Concept c) {
        this.input = task;
        this.concept = c;
    }

    @Override
    public String toString() {
        return "Remember(" + input + ")";
    }

    @Override
    public final ITask next(NAR n) {

        try {



            /* the tasks pri may change after starting insertion, so cache here */
            float pri = input.pri();
            if (pri != pri)
                return null; //got deleted somehow


            ((TaskConcept) concept).add(this, n);

            if (!forgotten.containsIdentity(input)) {
                n.emotion.onInput(input, n);
                next.add(new TaskLinkTask(input, concept));
            }

        } finally {
            forgotten.forEach(Task::delete);
            remembered.forEach(n.eventTask::emit);
        }

        return NativeTask.of(next);
    }



    public void forget(Task x) {
        if (!forgotten.isEmpty() && forgotten.containsIdentity(x))
            return;

        forgotten.add(x);
    }

    public void remember(Task x) {
        if (x!=null)
            remembered.add(x);
    }

    public void merge(Task existing) {
        if (existing == input)
            return; //same instance, do nothing

        assert(!input.isDeleted()); //dont delete just yet

        //TODO decide how much to re-activate
        //TODO consider forgetting rate

        if (existing instanceof NALTask)
            ((NALTask) existing).causeMerge(input);

        float pri = existing.priElseZero();
        next(new TaskLinkTask(existing, pri, concept));


        forget(input);
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
}
