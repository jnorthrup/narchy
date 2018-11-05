package nars.link;

import jcog.data.MutableFloat;
import jcog.pri.*;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.Tasklike;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

import static nars.time.Tense.ETERNAL;

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
        if (task instanceof SignalTask) {
            return new DirectTaskLink(task, pri);
        } else {
            return new GeneralTaskLink(task, n, pri * n.taskLinkActivation.floatValue());
        }
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

    /**
     * create a batch of tasklinks, sharing common seed data
     */
    static void link(TaskLink tasklink, float pri, List<Concept> targets, @Nullable OverflowDistributor<Bag> overflow) {
        assert(!targets.isEmpty());

//        float pEach = Math.max(ScalarValue.EPSILON,
//                priTransferred / nTargets
//        );
        float pEach =
                //TODO abstract priority transfer function here
                //pri; //no division
                pri/targets.size(); //division


        for (Concept c : targets) {

            TaskLink tl = tasklink.clone(pEach);
            if (tl!=null) {
                link(tl, c.tasklinks(), overflow);
            }

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


        protected GeneralTaskLink(Task t, NAR n, float pri) {
            this(t, false, n, pri);
        }

        public GeneralTaskLink(Task t, boolean polarizeBeliefsAndGoals, NAR n, float pri) {
            this(seed(t, polarizeBeliefsAndGoals, n), pri);
        }

        public GeneralTaskLink(Term t, byte punc, long when, float pri) {
            this(seed(t, punc, when), pri);
        }

        @Override
        public TaskLink clone(float pri) {
            return new GeneralTaskLink(id, pri);
        }

        /**
         * use this to create a tasklink seed shared by several different tasklinks
         * each with its own distinct priority
         */
        public static Tasklike seed(Term t, byte punc, long when) {
            return new Tasklike(t, punc, when);
        }

        public static Tasklike seed(Task t, boolean polarizeBeliefsAndGoals, NAR n) {
            long when = t.isEternal() ? ETERNAL : Tense.dither(

                    !(t instanceof SeriesBeliefTable.SeriesTask) ? t.mid() : t.start() //use early alignment for stretchable SeriesTask otherwise it will spam the TaskLink bag whenever it stretches

                    , n);
            return seed(
                    (Param.TASKLINK_CONCEPT_TERM ? t.term().concept() : t.term())
                            .negIf(
                                    polarizeBeliefsAndGoals && t.isBeliefOrGoal() && t.isNegative()
                            ),
                    t.punc(), when);
        }


        public GeneralTaskLink(Tasklike seed, float pri) {
            super(seed, pri);
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
