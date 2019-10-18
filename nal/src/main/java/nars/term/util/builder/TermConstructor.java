package nars.term.util.builder;

import nars.Op;
import nars.subterm.*;
import nars.term.Term;
import nars.term.anon.Intrin;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

import static nars.time.Tense.DTERNAL;

/** lowest-level (raw, possibly un-checked) compound construction interface */
@FunctionalInterface public interface TermConstructor {


    default Subterms subterms(@Nullable Op inOp, Term... t) {
        return TermConstructor.theSubterms(true, t);
    }

    default /*final*/ Subterms subterms(Term... s) {
        return subterms(null, s);
    }

    default /* final */ Term compound(Op o, Term... u) {
        return compound(o, DTERNAL, u);
    }

    Term compound(Op op, int dt, Term... subterms);

    default Term compound(Op op, int dt, Subterms t) {
        return compound(op, dt, t.arrayShared());
    }

    static Subterms theSubterms(boolean tryAnon, Term... t) {
        final int n = t.length;
        if (n == 0)
            return Op.EmptySubterms;

        if (tryAnon && Intrin.intrinsic(t))
            return new IntrinSubterms(t);
        else {
            Term t0 = t[0];
            switch (t.length) {
//                case 0: throw new UnsupportedOperationException();

                case 1: return new UniSubterm(t0);

                case 2: return new BiSubterm(t0, t[1]);

                default: {
                    //TODO Param.SUBTERM_BYTE_KEY_CACHED_BELOW_VOLUME
                    boolean different = false;
                    for (int i = 1; i < t.length; i++) {
                        if (!t[i].equals(t[i - 1])) {
                            different = true;
                            break;
                        }
                    }
                    //if (t[i] != t[i - 1]) {

                    return different ?
                        new ArrayTermVector(t) :
                        new RemappedSubterms.RepeatedSubterms<>(t0, t.length);
                }
            }
        }
    }

}
