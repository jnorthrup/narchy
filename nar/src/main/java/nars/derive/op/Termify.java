package nars.derive.op;

import nars.$;
import nars.derive.model.Derivation;
import nars.derive.model.DerivationFailure;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.transform.Retemporalize;

import static nars.derive.model.DerivationFailure.Success;

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

    /** conclusion template */
    public final Term pattern;

    /** fully eternalized conclusion template for completely non-temporal premises */
    public final Term patternEternal;

    
    private final Occurrify.OccurrenceSolver time;
    private final Truthify truth;

    private static final Atom TERMIFY = Atomic.atom(Termify.class.getSimpleName());

    public Termify(Term pattern, Truthify truth, Occurrify.OccurrenceSolver time) {
        super($.funcFast(TERMIFY, pattern, truth));

        this.pattern = pattern;

        this.patternEternal = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(pattern);

        assert (pattern.equals(patternEternal) || pattern.root().equals(patternEternal.root()));


        this.truth = truth;

        this.time = time;
    }

    public final void apply(Term x, Taskify t, Derivation d) {

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
