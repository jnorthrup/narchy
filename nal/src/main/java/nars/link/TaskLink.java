package nars.link;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Priority;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

public interface TaskLink extends Priority, Termed {

    /**
     * resolves a task in the provided NAR
     */
    Task get(NAR n);


    /**
     * Tasklike productions
     */
    class Tasklike  /* ~= Pair<Term, ByteLongPair> */ {
        final Term term;

        final byte punc;
        final long when;
        private final int hash;

        public Tasklike(Term term, byte punc, long when) {
            this.punc = punc;

            if (when == XTERNAL || when != ETERNAL &&
                    (when < -9023372036854775808L || when > +9023372036854775808L))
                throw new RuntimeException("detected invalid time");

            this.when = when;

            this.term = term;
            assert (term.op().conceptualizable && term.op() != NEG);


            this.hash = Util.hashCombine(term.hashCode(), punc, Long.hashCode(when));
        }

        public final Term term() {

            return term;
        }

        @Override
        public String toString() {
            return term.toString() +
                    (char) punc +
                    (when != ETERNAL ? ("@" + when) : "");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;


            Tasklike tasklike = (Tasklike) o;
            if (hash != tasklike.hash) return false;

            return punc == tasklike.punc && when == tasklike.when &&

                    term.equals(tasklike.term);
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        public Task get(NAR n, Priority link) {

            Term t = term.unneg();

            long[] se =
                    //n.timeFocus(when);
                    new long[] { when, when };

            Concept c = n.conceptualizeDynamic(t);
            Task r;
            if (c != null) {
                r = c.table(punc).sample(se[0], se[1], t, n);

                //r = result == null || result.isDeleted() ? null : result;
            } else {
                //TODO if term supports dynamic truth, then possibly conceptualize and then match as above?

                //form a question/quest task for the missing concept
                byte punc;
                switch (this.punc) {
                    case BELIEF:
                        punc = QUESTION;
                        break;
                    case GOAL:
                        punc = QUEST;
                        break;
                    case QUESTION:
                    case QUEST:
                        punc = this.punc;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                r = new UnevaluatedTask(term, punc, null, n.time(), se[0], se[1], n.evidence());
                if (Param.DEBUG)
                    r.log("Tasklinked");
                r.pri(link.priElseZero());
            }

//            if (c != null && r != null && !t.hasAny(Op.VAR_QUERY /* ineligible to be present in actual belief/goal */)) {
//                if (r.isQuestionOrQuest()) {
//                    BeliefTable answers = c.tableAnswering(r.punc());
//                    if (answers instanceof DynamicBeliefTable) {
//                        //match an answer emulating a virtual self-termlink being matched during premise formation
//                        Task a = answers.answer(r, n);
//                        if (a != null) {
//                            n.input(a);
//                        }
//                    }
//                }
//            }

            return r;

        }
    }

    /**
     * dynamically resolves a task.
     * serializable and doesnt maintain a direct reference to a task.
     * may delete itself if the target concept is not conceptualized.
     */
    class GeneralTaskLink extends PLink<Tasklike> implements TaskLink {


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

                    t.mid()
                    //+ tt.dtRange() / 2
                    , n);
            return seed(
                    t.term()
                            //.concept()
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
            return toBudgetString() + " " + term() + ((char) punc()) + ":" + when();
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


        @Override
        public Task get(NAR n) {
            Task t = id.get(n, this);
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
            Task t = id;
            if (t == null)
                return null;
            return t.term();
        }

        @Override
        public Task get(NAR n) {

            return id;
        }


    }

}
