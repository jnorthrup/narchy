package nars.task;

import jcog.Util;
import jcog.WTF;
import jcog.pri.Prioritizable;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.time.Tense;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * Tasklike productions
 */
public final class Tasklike  /* ~= Pair<Term, ByteLongPair> */ {
    public final Term target;

    public final byte punc;
    public final long when;

    private Tasklike(Term target, byte punc, long when) {
        this.punc = punc;

//            if (when == XTERNAL || when != ETERNAL &&
//                    (when < -9023372036854775808L || when > +9023372036854775808L))
//                throw new RuntimeException("detected invalid time");

        this.when = when;

        this.target = target;
    }

    /**
     * use this to create a tasklink seed shared by several different tasklinks
     * each with its own distinct priority
     */
    public static Tasklike seed(Term tgt, byte punc, long when) {
        //            //normalize images
//            //TEMPORARY
//            Term v = t.normalize();
//            if (!t.equals(v))
//                throw new WTF("what kind of task is " + t);

        if (!(tgt.op().conceptualizable && tgt.op() != NEG))
            throw new WTF();
        //assert(t.isNormalized());

        return new Tasklike(tgt, punc, when);
    }

    public static Tasklike seed(Task tgt, boolean conceptRoot, boolean eternalize, NAR n) {

        long when = eternalize || tgt.isEternal() ? ETERNAL : Tense.dither(tgt.mid(), n);

        Term tt = tgt.term();

        return seed(
                conceptRoot ? tt.concept() : tt,
//                        .negIf(
//                                polarizeBeliefsAndGoals && t.isBeliefOrGoal() && t.isNegative()
//                        ),
                tgt.punc(), when);
    }

    @Override
    public String toString() {
        return target.toString() +
                (char) punc +
                (when != ETERNAL ? ( when) : "");
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tasklike)) return false;
        Tasklike oo = (Tasklike) o;
        return /*(hash == oo.hash) && */(target.equals(oo.target)) && (punc == oo.punc) && (when == oo.when);
    }

    @Override
    public final int hashCode() {
        return Util.hashCombine(Util.hashCombine(target.hashCode(), punc), when);
    }

    public Task get(NAR n, Prioritizable link) {

//        long[] se = new long[] { start , end };

        Term t = target;//.unneg();
        Concept c =
                //n.concept(t);
                punc == BELIEF || punc == GOAL ? n.conceptualizeDynamic(t) : n.concept(t);

        if (c != null) {

            long start, end;
            start = end = when;

            return c.table(punc).
                    sample
                    //match
                        (start, end, t, n);

//            if (task!=null) {
//                    byte punc = task.punc();
//                    //dynamic question answering
//                    Term taskTerm = task.target();
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
        }
//        } else {
//            //TODO if target supports dynamic truth, then possibly conceptualize and then match as above?
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
//            task = new UnevaluatedTask(target, punc, null, n.time(), se[0], se[1], n.evidence());
//            if (Param.DEBUG)
//                task.log("Tasklinked");
//            task.pri(link.priElseZero());

//        }


        //TEMPORARY
//        if (task!=null && task.isInput() && !(task instanceof SignalTask)) {
//            link.priMax(task.priElseZero()); //boost
//        }

        return null;

    }
}
