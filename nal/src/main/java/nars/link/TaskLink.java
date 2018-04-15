package nars.link;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Priority;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

public interface TaskLink extends Priority, Termed {

    /**
     * resolves a task in the provided NAR
     */
    Task get(NAR n);
//
//    /**
//     * after it has been deleted, give an opportunity to re-insert
//     * any forwarded task in a bag where it was just removed
//     */
//    void reincarnate(TaskLinkCurveBag taskLinks);

    /** Tasklike productions */
    class Tasklike  /* ~= Pair<Term, ByteLongPair> */ {
        final Term term;
        //final byte[] term;
        final byte punc;
        final long when;
        private final int hash;

        public Tasklike(Term term, byte punc, long when) {
            this.punc = punc;

            assert(when!=XTERNAL);
            this.when = when;

            this.term = term;
            //this.term = IO.termToBytes(term);
            this.hash = Util.hashCombine(term.hashCode(), punc, Long.hashCode(when));
        }

        public final Term term() {
            //return IO.termFromBytes(term);
            return term;
        }

        @Override
        public String toString() {
            return term().toString() +
                   Character.valueOf((char)punc) +
                   (when!=ETERNAL ? ("@" + when) : "");
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;

            //if (o == null || getClass() != o.getClass()) return false;
            Tasklike tasklike = (Tasklike) o;
            if (hash != tasklike.hash) return false;

            return punc == tasklike.punc && when == tasklike.when &&
                    //Arrays.equals(term, tasklike.term);
                    term.equals(tasklike.term);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        public Task get(NAR n) {

            Term t = term().unneg();
            Concept c = n.conceptualize(t);
            if (c == null)
                return null;

            long[] se = n.timeFocus(when);
            Task result = c.table(punc).sample(se[0], se[1], t, n);

            return result == null || result.isDeleted() ? null : result;

        }
    }

    /**
     * dynamically resolves a task.
     * serializable and doesnt maintain a direct reference to a task.
     * may delete itself if the target concept is not conceptualized.
     */
    class GeneralTaskLink extends PLink<Tasklike> implements TaskLink {

//        public GeneralTaskLink(Task template, NAR nar, float pri) {
//            this(template.term().concept(), template.punc(), Tense.dither(template.mid(), nar), pri);
//        }

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
            Term tt = t.term();
            return seed(
                    tt
                        .root()
                        .negIf(
                            polarizeBeliefsAndGoals && t.isBeliefOrGoal() && t.isNegative()
                        ),
                    t.punc(), Tense.dither(
                        //t.mid()
                       t.mid() + tt.dtRange()/2
                    , n));
        }


        public GeneralTaskLink(Tasklike seed, float pri) {
            super(seed, pri);
        }


        @Override
        public String toString() {
            return toBudgetString() + " " + term() + ((char)punc()) + ":" + when();
        }

        public Term term() {
            return id.term();
        }

        public byte punc() {
            return id.punc;
        }

        public long when() {
            return id.when;
        }

//        @Override
//        public final void reincarnate(TaskLinkCurveBag taskLinks) {
//            //N/A
//        }

        @Override
        public Task get(NAR n) {
            Task t = get().get(n);
            if (t == null) {
                delete();
            }
            return t;
        }
    }

    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {

        public DirectTaskLink(Task id, float p) {
            super(id, p);
        }

        @Override
        public Term term() {
            Task t = get();
            if (t == null)
                return null;
            return t.term();
        }

        @Override
        public Task get(NAR n) {
            //TODO may want to check if the task is still active in the NAR
            return get();
        }

//        @Override
//        public void reincarnate(TaskLinkCurveBag bag) {
//            float p = this.priBeforeDeletion;
//            if (p == p) {
//                // this link was deleted due to the referent being deleted,
//                // not because the link was deleted.
//                // so see if a forwarding exists
//
//                Task x = this.get();
//                Task px = x;
//                Task y;
//
//                //TODO maybe a hard limit should be here for safety in case anyone wants to create loops of forwarding tasks
//                int hopsRemain = Param.MAX_TASK_FORWARD_HOPS;
//                do {
//                    y = x.meta("@");
//                    if (y != null)
//                        x = y;
//                } while (y != null && --hopsRemain > 0);
//
//                if (x != px && !x.isDeleted()) {
//                    Tasklinks.linkTask(x, p, bag);
//                }
//            }
//
//        }
    }

}
