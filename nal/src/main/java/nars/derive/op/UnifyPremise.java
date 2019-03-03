package nars.derive.op;

import nars.$;
import nars.Param;
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

/**
 * Created by me on 5/26/16.
 */
public final class UnifyPremise extends AbstractPred<Derivation> {


    public final Term taskPatern, beliefPattern;
    final boolean fwd;

    private static final Atomic FWD = Atomic.the("fwd");
    private static final Atomic REV = Atomic.the("rev");
    public final Taskify taskify;

    public UnifyPremise(Term taskPatern, Term beliefPattern, boolean fwd, Taskify taskify) {
        super($.funcFast(UNIFY, $.pFast(taskPatern, beliefPattern), fwd ? FWD : REV));
        this.taskPatern = taskPatern;
        this.beliefPattern = beliefPattern;
        this.fwd = fwd;
        this.taskify = taskify;
    }

    @Override
    public boolean test(Derivation d) {
        //first component
        if (d.unify(fwd ? taskPatern : beliefPattern, fwd ? d.taskTerm : d.beliefTerm, false)) {

            //second component

            substituteUnification(d);

            //substituteDirect(d);

        }

        return d.live();
    }

    /** the original, direct method */
    private void substituteDirect(Derivation d) {
        d.forEachMatch = (dd)->{
            Term y = AbstractTermTransform.transform(taskify.termify.pattern, dd.transform);
            if (!(y instanceof Bool) && y.unneg().op().taskable)
                taskify.test(y, dd);
            return true;
        };

        boolean unified = d.unify(fwd ? beliefPattern : taskPatern, fwd ? d.beliefTerm: d.taskTerm, true);

        d.forEachMatch = null;

    }

    /** memoizable method */
    private void substituteUnification(Derivation d) {

        d.forEachMatch = (x) -> true; //HACK

        if (d.unify(fwd ? beliefPattern : taskPatern, fwd ? d.beliefTerm: d.taskTerm, false)) {

            Unification u = d.unification(true, TermutatorFanOut, Param.TermutatorSearchTTL);

            taskify.test(u, d);
        }
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
