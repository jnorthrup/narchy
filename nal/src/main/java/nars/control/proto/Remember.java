package nars.control.proto;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.NALTask;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public class Remember extends AbstractTask {
    public Task input;

    final FasterList<ITask> next = new FasterList(2);
    final FasterList<Task> remembered = new FasterList(2);
    public final FasterList<Task> forgotten = new FasterList(2);
    public final Concept concept;


    static final Logger logger = LoggerFactory.getLogger(Remember.class);

    @Nullable public static Remember the(Task input, NAR n) {
        if (!input.isCommand()) {
            try {
                TaskConcept concept = (TaskConcept) n.conceptualize(input);
                return concept != null ? new Remember(input, concept) : null;
            } catch (Throwable t) {
                if (Param.DEBUG)
                    logger.warn("not conceptualizable: {}", input);
            }
        }

        return null;
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

//        validate(n);

        try {

            input(n);

        } finally {

            commit();

            return AbstractTask.of(next);
        }

    }

    /** finalization and cleanup work */
    protected void commit() {
        if (!forgotten.isEmpty() || !remembered.isEmpty()) {
            next.add(new Commit(forgotten, remembered));
        }
    }

    /** attempt to insert into belief table */
    protected void input(NAR n) {
        ((TaskConcept) concept).add(this, n);
    }

//    private void validate(NAR n) {
        //verify dithering
//        if (Param.DEBUG) {
//            if (!input.isInput()) {
//                Truth t = input.truth();
//                if (t != null)
//                    t.ensureDithered(n);
//            }
//        }
//    }

    //TODO: private static final class ListTask extends FasterList<ITask> extends NativeTask {

    @Deprecated private static final class Commit extends AbstractTask {

        FasterList<Task> forgotten, remembered;

        public Commit(FasterList<Task> forgotten, FasterList<Task> remembered) {
            super();
            this.forgotten = forgotten; this.remembered = remembered;
        }

        @Override
        public ITask next(NAR n) {
            forgotten.forEach(Task::delete);

            return Reaction.the(remembered);
        }
    }


    public void forget(Task x) {
        if (remembered.removeInstance(x)) {
            //throw new TODO();
            //TODO filter next tasks with any involving that task
        }
        add(x, this.forgotten);
        x.delete();
        if (input == x)
            input = null;
    }

    public void remember(Task x) {
        if (add(x, this.remembered))
            next.add(new TaskLinkTask(x, concept));
    }


    public void merge(Task existing) {

        Task input = this.input;

        if (existing != input) {

            //assert (!input.isDeleted()); //dont delete just yet

            //TODO decide how much to re-activate
            //TODO consider forgetting rate

            if (existing instanceof NALTask)
                ((NALTask) existing).priCauseMerge(input);

            forget(input);
        }


        if (input.isInput())
            remember(existing); //link and emit
        else
            next(new TaskLinkTask(existing, concept)); //just link


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
            if (!f.isEmpty()) {
                if (f.containsInstance(x)) {
                    return false;
                }
            }

            f.add(x);
            return true;
        }
        return false;
    }


}
