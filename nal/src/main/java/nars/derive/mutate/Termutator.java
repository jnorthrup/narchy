package nars.derive.mutate;

import nars.$;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.subst.Unify;

/**
 * AIKR choicepoint used in deciding possible mutations to apply in deriving new compounds
 */
public interface Termutator {

    /**
     * match all termutations recursing to the next after each successful one
     */
    void mutate(Unify f, Termutator[] chain, int current);

    default int getEstimatedPermutations() {
        return -1; /* unknown */
    }

    abstract class AbstractTermutator extends ProxyTerm implements Termutator {

        AbstractTermutator(Term... keyComponents) {
            super(keyComponents.length == 1 ? keyComponents[0] : $.pFast(keyComponents));
        }

    }
}
