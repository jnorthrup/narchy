package nars.unify.mutate;

import nars.$;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

/**
 * AIKR choicepoint used in deciding possible mutations to apply in deriving new compounds
 */
@FunctionalInterface public interface Termutator {

    /**
     * match all termutations recursing to the next after each successful one
     */
    void mutate(Termutator[] chain, int current, Unify u);

    default int getEstimatedPermutations() {
        return -1; /* unknown */
    }

    /** @return null to terminate the entire chain (CUT);
     * this instance for no change
     * or a reduced version (or NullTermutator for NOP) */
    default @Nullable Termutator preprocess(Unify u) {
        return this;
    }

    abstract class AbstractTermutator extends ProxyTerm implements Termutator {

        AbstractTermutator(Atom klass, Term... keyComponents) {
            super($.pFast(klass, keyComponents.length == 1 ? keyComponents[0] :
                    $.pFast(keyComponents)));
        }

    }

    /** constant result for return from preprocess() call
     * */
    static Termutator result(boolean success) {
        return success ? Termutator.ELIDE : null;
    }

    Termutator[] CUT = new Termutator[0];

    Termutator ELIDE = new AbstractTermutator(Atomic.atom("ELIDE")) {
        @Override public void mutate(Termutator[] chain, int current, Unify u) {
            u.tryMutate(chain, current);
        }
    };
}
