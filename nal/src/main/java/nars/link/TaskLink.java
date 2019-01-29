package nars.link;

import jcog.data.MutableFloat;
import jcog.pri.*;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.Tasklike;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static nars.time.Tense.ETERNAL;

/**
 * the function of a tasklink is to be a prioritizable strategy for resolving a Task in a NAR.
 * this does not mean that it must reference a specific Task but certain properties of it
 * that can be used ot dynamically match a Task on demand.
 *
 * note: seems to be important for Tasklink to NOT implement Termed when use with common Map's with Termlinks */
public interface TaskLink extends UnitPrioritizable, Function<NAR,Task>, Deleteable {

    /** dont use .apply() directly; use this */
    static Task task(TaskLink x, NAR n) {
        Task y = x.apply(n);
        if(y == null)
            x.delete();
        return y;
    }

    static GeneralTaskLink the(Task task, boolean generify, boolean eternalize, float pri, NAR n) {
        return new TaskLink.GeneralTaskLink(Tasklike.seed(task, generify, eternalize, n), pri);
    }

    /** creates a copy, with a specific priority */
    TaskLink clone(float pri);

    Term term();
    byte punc();

    /** main tasklink constructor
     * @param generify  if the task's term contains temporal information, discarding this coalesces the link with other similar tasklinks (weaker, more generic).
     *                   otherwise a unique tasklink is created
     *
     *      if concept only, then tasklinks are created for the concept() of the term.  this has consequences for temporal
     *      terms such that unique and specific temporal data is not preserved in the tasklink, thereby reducing
     *      the demand on tasklinks.
     *
     *      otherwise non-concept for temporal includes more temporal precision at the cost of more links.
     *
     * */
    static TaskLink tasklink(Task task, boolean generify, boolean eternalize, float pri, NAR n) {

        //assert(task.term().volume() < n.termVolumeMax.intValue());


//        if (task instanceof SignalTask) {
//            return new DirectTaskLink(task, pri);
//        } else {

        return new GeneralTaskLink(Tasklike.seed(task, generify, eternalize, n), pri);
        //}
    }


    static void link(TaskLink x, Concept c) {
        link(x, c.tasklinks(), null);
    }

    static void link(TaskLink x, Bag<?, TaskLink> b, @Nullable OverflowDistributor<Bag> overflow) {

        if (overflow != null) {
            MutableFloat o = new MutableFloat();

            TaskLink yy = b.put(x, o);
            if (o.floatValue() > EPSILON) {
                overflow.overflow(b, o.floatValue(),
                    (yy != null) ?
                        1f - yy.priElseZero()
                        :
                        1 //assume it needs as much as it can get
                );
            }
        } else {
            b.putAsync(x);
        }
    }


    /** special tasklink for signals which can stretch and so their target time would not correspond well while changing */
    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {

        public DirectTaskLink(Task id, float p) {
            super(id, p);
        }

        @Override
        public Term term() {
            return id.term();
        }

        @Override
        public byte punc() {
            return id.punc();
        }

        @Override
        public Task apply(NAR n) {
            return get();
        }

        @Override
        public TaskLink clone(float pri) {
            Task t = get();
            return t!=null ? new DirectTaskLink(t, pri) : null;
        }
    }

    /**
     * dynamically resolves a task.
     * serializable and doesnt maintain a direct reference to a task.
     * may delete itself if the target concept is not conceptualized.
     */
    class GeneralTaskLink extends PLinkHashCached<Tasklike> implements TaskLink {

        public GeneralTaskLink(Tasklike seed, float pri) {
            super(seed, pri);
        }

        public GeneralTaskLink(Term t, byte punc, long when, float pri) {
            this(Tasklike.seed(t, punc, when), pri);
        }

        @Override
        public TaskLink clone(float pri) {
            return new GeneralTaskLink(id, pri);
        }

        @Override
        public String toString() {
            return toBudgetString() + ' ' + term() + ((char) punc()) + ':' + (when()!=ETERNAL ? when() : "ETE");
        }

        @Override public Term term() {
            return id.term;
        }

        @Override public byte punc() {
            return id.punc;
        }

        public long when() {
            return id.when;
        }


        @Override
        public Task apply(NAR n) {
            return id.get(n, this);
        }
    }

//    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {
//
//        public DirectTaskLink(Task id, float p) {
//            super(id, p);
//        }
//
//        @Override
//        public Term term() {
//            Task t = id;
//            if (t == null)
//                return null;
//            return t.term();
//        }
//
//        @Override
//        public Task get(NAR n) {
//
//            return id;
//        }
//
//
//    }

}
