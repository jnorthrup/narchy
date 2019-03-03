package nars.derive.op;

import nars.$;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.Unification;

import static nars.$.$$;
import static nars.Param.TermutatorFanOut;
import static nars.Param.TermutatorSearchTTL;

/**
 * Created by me on 5/26/16.
 */
public final class Premisify extends AbstractPred<Derivation> {


    public final Term taskPatern, beliefPattern;
    final boolean fwd;

    private static final Atomic FWD = Atomic.the("fwd");
    private static final Atomic REV = Atomic.the("rev");
    public final Taskify taskify;

    public Premisify(Term taskPatern, Term beliefPattern, boolean fwd, Taskify taskify) {
        super($.funcFast(UNIFY, $.pFast(taskPatern, beliefPattern), fwd ? FWD : REV));
        this.taskPatern = taskPatern;
        this.beliefPattern = beliefPattern;
        this.fwd = fwd;
        this.taskify = taskify;
    }

    @Override
    public boolean test(Derivation d) {

        substituteUnification(d);

        //substituteDirect(d);

        return d.live();
    }

    /**
     * the original, direct method
     */
    private void substituteDirect(Derivation d) {
        if (!unify(d, fwd))
            return;

        d.forEachMatch = (dd) -> {
            Term y = AbstractTermTransform.transform(taskify.termify.pattern, dd.transform);
            if (!(y instanceof Bool) && y.unneg().op().taskable)
                taskify.test(y, dd);
            return true;
        };

        boolean unified = unify(d, !fwd);

        d.forEachMatch = null;

    }

    /**
     * memoizable method
     */
    private void substituteUnification(Derivation d) {
        if (!unify(d, fwd))
            return;

        d.forEachMatch = (x) -> true; //HACK

        if (unify(d, !fwd)) {

            Unification u = d.unification(true,
                    TermutatorFanOut, TermutatorSearchTTL);

            taskify.test(u, d);
        }
    }

    private boolean unify(Derivation d, boolean dir) {
        return d.unify(dir ? taskPatern : beliefPattern, dir ? d.taskTerm : d.beliefTerm, false);
    }


    protected static final Atomic UNIFY = $.the("unify");


    public final static PREDICATE<Derivation> preUnify = new AbstractPred<>($$("preUnify")) {
        @Override
        public boolean test(Derivation d) {
            d.clear();
            d.retransform.clear();
            d.forEachMatch = null;
            return true;
        }
    };
}
