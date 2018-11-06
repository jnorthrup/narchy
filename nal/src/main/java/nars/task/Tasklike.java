package nars.task;

import jcog.Util;
import jcog.pri.Prioritizable;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.time.Tense;

import static nars.Op.NEG;
import static nars.time.Tense.ETERNAL;

/**
 * Tasklike productions
 */
public class Tasklike  /* ~= Pair<Term, ByteLongPair> */ {
    public final Term term;

    public final byte punc;
    public final long when;

    private Tasklike(Term term, byte punc, long when) {
        this.punc = punc;

//            if (when == XTERNAL || when != ETERNAL &&
//                    (when < -9023372036854775808L || when > +9023372036854775808L))
//                throw new RuntimeException("detected invalid time");

        this.when = when;

        this.term = term;
    }

    /**
     * use this to create a tasklink seed shared by several different tasklinks
     * each with its own distinct priority
     */
    public static Tasklike seed(Term t, byte punc, long when) {
        //            //normalize images
//            //TEMPORARY
//            Term v = t.normalize();
//            if (!t.equals(v))
//                throw new WTF("what kind of task is " + t);

        assert (t.op().conceptualizable && t.op() != NEG);

        return new Tasklike(t, punc, when);
    }

    public static Tasklike seed(Task t, boolean polarizeBeliefsAndGoals, NAR n) {

        long when = t.isEternal() ? ETERNAL : Tense.dither(t.mid(), n);

        Term u = Param.TASKLINK_CONCEPT_TERM ? t.term().concept() : t.term();



        return seed(
                u
                        .negIf(
                                polarizeBeliefsAndGoals && t.isBeliefOrGoal() && t.isNegative()
                        ),
                t.punc(), when);
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
        if (!(o instanceof Tasklike)) return false;
        Tasklike oo = (Tasklike) o;
        return /*(hash == oo.hash) && */(punc == oo.punc) && (when == oo.when) && (term.equals(oo.term));
    }

    @Override
    public final int hashCode() {
        return Util.hashCombine(Util.hashCombine(term.hashCode(), punc), when);
    }

    public Task get(NAR n, Prioritizable link) {


        long start, end;
        if (when == ETERNAL) start = end = ETERNAL;
        else {
            int dur = n.dur();
            start = Tense.dither(when - dur/2, n);
            end = Tense.dither(when + dur/2, n);
        }

        long[] se = new long[] { start , end };

        Term t = term;//.unneg();
        Concept c =
                n.conceptualizeDynamic(t);

        Task task;
        if (c != null) {

            task = c.table(punc).sample(se[0], se[1], t, n);
//            if (task!=null) {
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
//            }

        } else {
//            //TODO if term supports dynamic truth, then possibly conceptualize and then match as above?
//
//            //form a question/quest task for the missing concept
//            byte punc;
//            switch (this.punc) {
//                case BELIEF:
//                    punc = QUESTION;
//                    break;
//                case GOAL:
//                    punc = QUEST;
//                    break;
//                case QUESTION:
//                case QUEST:
//                    punc = this.punc;
//                    break;
//                default:
//                    throw new UnsupportedOperationException();
//            }
//
//            task = new UnevaluatedTask(term, punc, null, n.time(), se[0], se[1], n.evidence());
//            if (Param.DEBUG)
//                task.log("Tasklinked");
//            task.pri(link.priElseZero());
            task = null;
        }


        //TEMPORARY
//        if (task!=null && task.isInput() && !(task instanceof SignalTask)) {
//            link.priMax(task.priElseZero()); //boost
//        }

        return task;

    }
}
