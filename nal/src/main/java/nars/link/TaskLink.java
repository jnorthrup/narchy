package nars.link;

import jcog.Util;
import jcog.pri.PLinkHashCached;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Priority;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

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

//            if (when == XTERNAL || when != ETERNAL &&
//                    (when < -9023372036854775808L || when > +9023372036854775808L))
//                throw new RuntimeException("detected invalid time");

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
                    (when != ETERNAL ? ( when) : "");
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;

            Tasklike oo = (Tasklike) o;
            return (hash == oo.hash) && (punc == oo.punc) && (when == oo.when) && (term.equals(oo.term));
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        public Task get(NAR n, Priority link) {


            long start, end;
            if (when == ETERNAL) start = end = ETERNAL;
            else {
                int dur = n.dur();
                start = Tense.dither(when - dur/2, n);
                end = Tense.dither(when + dur/2, n);
            }

            long[] se = new long[] { start , end };

            Term t = term.unneg();
            Concept c = n.conceptualizeDynamic(t);
            Task task;
            if (c != null) {

                task = c.table(punc).sample(se[0], se[1], t, n);
                if (task!=null) {
//                    byte punc = task.punc();
//                    //dynamic question answering
//                    Term taskTerm = task.term();
//                    if ((punc==QUESTION || punc == QUEST) && !taskTerm.hasAny(Op.VAR_QUERY /* ineligible to be present in actual belief/goal */)) {
//
//                        BeliefTables aa = (BeliefTables) c.tableAnswering(punc);
//                        /*@Nullable DynamicTaskTable aa = answers.tableFirst(DynamicTaskTable.class);
//                        if (aa!=null)*/ {
//
//                            //match an answer emulating a virtual self-termlink being matched during premise formation
//                            Task q = task;
//                            Task a = aa.answer(q.start(), q.end(), taskTerm, null, n);
//                            if (a != null) {
//
//
//
//                                //decrease tasklink too?
//
//                                q.onAnswered(a, n);
//                                n.input(a);
//                            }
//                        }
//                    }

                }

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

                task = new UnevaluatedTask(term, punc, null, n.time(), se[0], se[1], n.evidence());
                if (Param.DEBUG)
                    task.log("Tasklinked");
                task.pri(link.priElseZero());
            }



            return task;

        }
    }

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
