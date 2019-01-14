package nars.unify.mutate;

import nars.$;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.unify.Unify;

/**
 * AIKR choicepoint used in deciding possible mutations to apply in deriving new compounds
 */
public interface Termutator {

    /**
     * match all termutations recursing to the next after each successful one
     */
    void mutate(Termutator[] chain, int current, Unify f);

    default int getEstimatedPermutations() {
        return -1; /* unknown */
    }

    abstract class AbstractTermutator extends ProxyTerm implements Termutator {

        AbstractTermutator(Atom klass, Term... keyComponents) {
            super($.pFast(klass, keyComponents.length == 1 ? keyComponents[0] :
                    $.pFast(keyComponents)));
        }

    }
}
