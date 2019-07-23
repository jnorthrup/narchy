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
public interface Termutator {


    /**
     * match all termutations recursing to the next after each successful one
     */
    void mutate(Termutator[] chain, int current, Unify u);

    default int getEstimatedPermutations() {
        return -1; /* unknown */
    }

    /** @return null to terminate the entire chain; this for no change; or a reduced version (or NullTermutator for NOP) */
    @Nullable
    default Termutator preprocess(Unify u) {
        return this;
    }

    abstract class AbstractTermutator extends ProxyTerm implements Termutator {

        AbstractTermutator(Atom klass, Term... keyComponents) {
            super($.pFast(klass, keyComponents.length == 1 ? keyComponents[0] :
                    $.pFast(keyComponents)));
        }

    }

    Termutator[] TerminateTermutator = new Termutator[0];
    Termutator NullTermutator = new AbstractTermutator(Atomic.atom("NullTermutator")) {

        @Override
        public void mutate(Termutator[] chain, int current, Unify u) {
            u.tryMutate(chain, current);
        }
    };

    /** constant result for return from preprocess() call */
    static Termutator result(boolean b) {
        if (b)
            return Termutator.NullTermutator; //success
        else
            return null; //fail
    }

}
