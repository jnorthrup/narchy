package nars.link;

import jcog.data.MutableFloat;
import jcog.pri.Deleteable;
import jcog.pri.OverflowDistributor;
import jcog.pri.PLinkHashCached;
import jcog.pri.UnitPrioritizable;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.index.concept.AbstractConceptIndex;
import nars.task.Tasklike;
import nars.term.Term;
import org.eclipse.collections.impl.tuple.Tuples;
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

    static GeneralTaskLink the(Term src, Task task, boolean generify, boolean eternalize, float pri, NAR n) {
        return new TaskLink.GeneralTaskLink(src, Tasklike.seed(task, generify, eternalize, n), pri);
    }

    /** creates a copy, with a specific priority */
    TaskLink clone(Term src, float pri);

    /** concept term (source) where the link originates */
    Term source();

    /** task term (target) of the task linked */
    Term term();

    byte punc();

    /** main tasklink constructor
     * @param generify  if the task's target contains temporal information, discarding this coalesces the link with other similar tasklinks (weaker, more generic).
     *                   otherwise a unique tasklink is created
     *
     *      if concept only, then tasklinks are created for the concept() of the target.  this has consequences for temporal
     *      terms such that unique and specific temporal data is not preserved in the tasklink, thereby reducing
     *      the demand on tasklinks.
     *
     *      otherwise non-concept for temporal includes more temporal precision at the cost of more links.
     *
     * */
    static TaskLink tasklink(Term src, Task task, boolean generify, boolean eternalize, float pri, NAR n) {

        //assert(task.target().volume() < n.termVolumeMax.intValue());


//        if (task instanceof SignalTask) {
//            return new DirectTaskLink(task, pri);
//        } else {

        return new GeneralTaskLink(src, Tasklike.seed(task, generify, eternalize, n), pri);
        //}
    }


    static void link(TaskLink x, NAR nar) {
        link(x, nar, null);
    }

    static void link(TaskLink x, NAR nar, @Nullable OverflowDistributor<Bag> overflow) {

        Bag<TaskLink, TaskLink> b = ((AbstractConceptIndex)nar.concepts).active;

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


//    /** special tasklink for signals which can stretch and so their target time would not correspond well while changing */
//    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {
//
//        public DirectTaskLink(Task id, float p) {
//            super(id, p);
//        }
//
//        @Override
//        public Term term() {
//            return id.term();
//        }
//
//        @Override
//        public byte punc() {
//            return id.punc();
//        }
//
//        @Override
//        public Task apply(NAR n) {
//            return get();
//        }
//
//        @Override
//        public TaskLink clone(float pri) {
//            Task t = get();
//            return t!=null ? new DirectTaskLink(t, pri) : null;
//        }
//    }

    /**
     * dynamically resolves a task.
     * serializable and doesnt maintain a direct reference to a task.
     * may delete itself if the target concept is not conceptualized.
     */
    class GeneralTaskLink extends PLinkHashCached<org.eclipse.collections.api.tuple.Pair<Term,Tasklike>> implements TaskLink {

        public GeneralTaskLink(Term source, Tasklike seed, float pri) {
            super(Tuples.pair(source,seed), pri);
        }

        public GeneralTaskLink(Term source, Term t, byte punc, long when, float pri) {
            this(source.concept(), Tasklike.seed(t, punc, when), pri);
        }

        @Override
        public TaskLink clone(Term src, float pri) {
            return new GeneralTaskLink(src, id.getTwo(), pri);
        }

        @Override
        public String toString() {
            return toBudgetString() + ' ' + term() + ((char) punc()) + ':' + (when()!=ETERNAL ? when() : "ETE") + ":" + source();
        }

        @Override public Term source() {
            return id.getOne();
        }

        @Override public Term term() {
            return id.getTwo().target;
        }

        @Override public byte punc() {
            return id.getTwo().punc;
        }

        public long when() {
            return id.getTwo().when;
        }


        @Override
        public Task apply(NAR n) {
            return id.getTwo().get(n, this);
        }
    }

//    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {
//
//        public DirectTaskLink(Task id, float p) {
//            super(id, p);
//        }
//
//        @Override
//        public Term target() {
//            Task t = id;
//            if (t == null)
//                return null;
//            return t.target();
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
