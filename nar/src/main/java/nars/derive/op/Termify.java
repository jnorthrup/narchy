package nars.derive.op;

import nars.$;
import nars.derive.Derivation;
import nars.derive.DerivationFailure;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;

import static nars.derive.DerivationFailure.Success;

/**
 * Derivation target construction step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 * <p>
 * runtime instance. each leaf of each NAR's derivation tree should have
 * a unique instance, assigned the appropriate cause id by the NAR
 * at initialization.
 */
public final class Termify extends ProxyTerm {

    public final Term pattern;

    
    private final Occurrify.OccurrenceSolver time;
    private final Truthify truth;

    private static final Atom TERMIFY = Atomic.atom(Termify.class.getSimpleName());

    public Termify(Term pattern, Truthify truth, Occurrify.OccurrenceSolver time) {
        super($.funcFast(TERMIFY, pattern, truth));
        this.pattern = pattern;
        this.truth = truth;

        this.time = time;
    }

    public final void test(Term x, Taskify t, Derivation d) {

        d.nar.emotion.deriveTermify.increment();

        DerivationFailure fail = DerivationFailure.failure(x,
                (byte) 0 /* dont consider punc consequences until after temporalization */,
                d);

        if (fail == Success) {
            if (Occurrify.temporal(truth, d))
                Occurrify.temporalTask(x, time, t, d);
            else
                Occurrify.eternalTask(x, t, d);
        }

    }


}
