package nars.derive.op;

import nars.$;
import nars.derive.model.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;

/**
 *
 */
abstract public class Premisify extends AbstractPred<Derivation> {


    private final Term taskPat, beliefPat;
    final boolean fwd;

    private static final Atomic FWD = Atomic.the("fwd");
    private static final Atomic REV = Atomic.the("rev");
    public final Taskify taskify;

    public Premisify(Term taskPat, Term beliefPat, boolean fwd, Taskify taskify) {
        super($.func(UNIFY, $.p(taskPat, beliefPat), fwd ? FWD : REV));
        this.taskPat = taskPat;
        this.beliefPat = beliefPat;
        this.fwd = fwd;
        this.taskify = taskify;
    }


    protected final boolean unify(Derivation d, boolean dir, boolean finish) {
        return d.unify(dir ? taskPat : beliefPat, dir ? d.taskTerm : d.beliefTerm, finish);
    }


    private static final Atomic UNIFY = $.the("unify");

}
