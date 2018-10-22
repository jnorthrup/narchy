package nars.link;

import jcog.pri.PLinkHashCached;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.Tasklike;
import nars.term.Term;
import nars.time.Tense;

import static nars.time.Tense.ETERNAL;

/** seems to be important for Tasklink to NOT implement Termed when use with common Map's with Termlinks */
public interface TaskLink extends PriReference<Tasklike>/*, Termed*/ {

    /**
     * resolves a task in the provided NAR
     */
    Task get(NAR n);


    /**
     * dynamically resolves a task.
     * serializable and doesnt maintain a direct reference to a task.
     * may delete itself if the target concept is not conceptualized.
     */
    class GeneralTaskLink extends PLinkHashCached<Tasklike> implements TaskLink {


        public GeneralTaskLink(Task t, NAR n, float pri) {
            this(t, false, n, pri);
        }

        public GeneralTaskLink(Task t, boolean polarizeBeliefsAndGoals, NAR n, float pri) {
            this(seed(t, polarizeBeliefsAndGoals, n), pri);
        }

        public GeneralTaskLink(Term t, byte punc, long when, float pri) {
            this(seed(t, punc, when), pri);
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

        public Term term() {
            return id.term;
        }

        public byte punc() {
            return id.punc;
        }

        public long when() {
            return id.when;
        }


        @Override
        public Task get(NAR n) {
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
