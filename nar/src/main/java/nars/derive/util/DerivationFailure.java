package nars.derive.util;

import nars.NAR;
import nars.Op;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public enum DerivationFailure {


    VolMax {
        @Override
        public void record(NAR n) {
            n.emotion.deriveFailVolLimit.increment();
        }
    },
    Taskable,

    /** term contains query vars, applies only to belief and goals */
    QueryVar,

    /** term contains xternal, applies only to belief and goals */
    Xternal;


    @Nullable
    public static DerivationFailure failure(Term x, byte punc, int volMax) {

        x = x.unneg();

        if (!x.op().taskable)
            return Taskable;

        if (x.hasAny(Op.VAR_PATTERN))
            throw new TermException("Termify result contains VAR_PATTERN", x);

        if (volMax > 0 && x.volume() > volMax)
            return VolMax;

        if (punc == BELIEF || punc == GOAL) {
            if(x.hasVarQuery())
                return QueryVar;
            if(x.hasXternal())
                return Xternal;
        }

        return null; //OK
    }

    public static DerivationFailure failure(Term x, Derivation d) {
        return failure(x, (byte) 0, d);
    }

    public static DerivationFailure failure(Term x, byte punc, Derivation d) {
        DerivationFailure f = failure(x, punc, d.termVolMax);
        return f;
    }

    public static boolean failure(Term x, byte punc) {
        return failure(x, punc, 0) != null;
    }

    /** default misc failure */
    public void record(NAR n) {
        n.emotion.deriveFail.increment(/*() ->
                rule + " |\n\t" + d.xy + "\n\t -> " + c1e
        */);
    }
}
