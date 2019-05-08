package nars.derive.model;

import nars.NAR;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.TermException;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public enum DerivationFailure {

    Success {
        @Override
        protected void record(NAR n) {
            /* nothing */
        }
    },

    /** boolean result */
    Null,

    VolMax {
        @Override
        protected void record(NAR n) {
            n.emotion.deriveFailVolLimit.increment();
        }
    },
    Taskable,

    /** term contains query vars, applies only to belief and goals */
    QueryVar,

    /** term contains xternal, applies only to belief and goals */
    Xternal;


    public static DerivationFailure failure(Term x, byte punc, int volMax) {
        if (x == null)
            throw new NullPointerException();

        if (x instanceof Bool)
            return Null;

        x = x.unneg();

        if (x.hasAny(Op.VAR_PATTERN))
            throw new TermException("Termify result contains VAR_PATTERN", x);

        if (volMax > 0 && x.volume() > volMax)
            return VolMax;

        if (!x.op().taskable)
            return Taskable;

        if (punc == BELIEF || punc == GOAL) {
            if(x.hasVarQuery())
                return QueryVar;
            if(x.hasXternal())
                return Xternal;
        }

        return Success; //OK
    }

    public static DerivationFailure failure(Term x, Derivation d) {
        return failure(x, (byte) 0, d);
    }

    public static DerivationFailure failure(Term x, byte punc, Derivation d) {
        DerivationFailure f = failure(x, punc, d.termVolMax);
        f.record(d.nar);
        return f;
    }

    public static boolean failure(Term x, byte punc) {
        return failure(x, punc, 0) == Success;
    }

    /** default misc failure */
    protected void record(NAR n) {
        n.emotion.deriveFail.increment(/*() ->
                rule + " |\n\t" + d.xy + "\n\t -> " + c1e
        */);
    }
}
