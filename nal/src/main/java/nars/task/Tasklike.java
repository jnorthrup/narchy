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

}
