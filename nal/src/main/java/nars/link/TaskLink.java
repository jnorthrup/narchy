package nars.link;

import jcog.data.MutableFloat;
import jcog.pri.OverflowDistributor;
import jcog.pri.PLinkHashCached;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.UnitPrioritizable;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.task.Tasklike;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * the function of a tasklink is to be a prioritizable strategy for resolving a Task in a NAR.
 * this does not mean that it must reference a specific Task but certain properties of it
 * that can be used ot dynamically match a Task on demand.
 *
 * note: seems to be important for Tasklink to NOT implement Termed when use with common Map's with Termlinks */
public interface TaskLink extends UnitPrioritizable, Function<NAR,Task> {

    /** creates a copy, with a specific priority */
    @Nullable TaskLink clone(float pri);

    Term term();
    byte punc();

    /** main tasklink constructor */
    static TaskLink tasklink(Task task, float pri, NAR n) {

        if (pri > 0)
            pri = pri * n.taskLinkActivation.floatValue();

//        if (task instanceof SignalTask) {
//            return new DirectTaskLink(task, pri);
//        } else {
            return new GeneralTaskLink(Tasklike.seed(task, n), pri);
        //}
    }


    static void link(TaskLink x, Bag<?, TaskLink> b, @Nullable OverflowDistributor<Bag> overflow) {

        if (overflow != null) {
            MutableFloat o = new MutableFloat();

            TaskLink yy = b.put(x, o);

            if (o.floatValue() > EPSILON) {
                float headRoom;
                if (yy != null)
                    headRoom = 1f - yy.priElseZero();
                else
                    headRoom = 1; //assume it needs as much as it can get

                overflow.overflow(b, o.floatValue(), headRoom);
            }
//            if (yy != xx && yy != null) {
//
//
//            }
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
            return get(); //includes deletion test
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
            return toBudgetString() + ' ' + term() + ((char) punc()) + ':' + when();
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
            Task t = id.get(n, this);
            if (t == null) {
                delete();
            }
            return t;
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
