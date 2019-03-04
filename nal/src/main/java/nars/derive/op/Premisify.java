package nars.derive.op;

import jcog.data.list.FasterList;
import jcog.memoize.QuickMemoize;
import jcog.util.HashCachedPair;
import nars.$;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.Unification;
import nars.unify.unification.DeterministicUnification;
import nars.unify.unification.PermutingUnification;

import java.util.function.BiFunction;

import static nars.$.$$;
import static nars.Param.TermutatorFanOut;
import static nars.Param.TermutatorSearchTTL;

/**
 * Created by me on 5/26/16.
 */
public class Premisify extends AbstractPred<Derivation> {


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

        return true;
    }


    /**
     * memoizable method
     */
    private void substituteUnification(Derivation d) {
        if (unify(d, fwd) && unify(d, !fwd)) {
            Unification u = d.unification(true, TermutatorFanOut, TermutatorSearchTTL);
            test(u, d);
        }
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
    protected boolean unify(Derivation d, boolean dir) {
        return d.unify(dir ? taskPatern : beliefPattern, dir ? d.taskTerm : d.beliefTerm, false);
    }

    public boolean test(Unification u,Derivation d) {

        if (u instanceof PermutingUnification)
            return taskify.test(((PermutingUnification)u), d);

        if (u instanceof DeterministicUnification)
            return taskify.test(((DeterministicUnification) u)::xy, d);


        return true;
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

    /** experimental unification caching */
    public static class CachingPremisify extends Premisify {

        QuickMemoize<HashCachedPair<Term,Term>, Unification> cache = new QuickMemoize<>(64);

        public CachingPremisify(Term taskPatern, Term beliefPattern, boolean fwd, Taskify taskify) {
            super(taskPatern, beliefPattern, fwd, taskify);
        }

        final BiFunction<HashCachedPair<Term, Term>, Derivation, Unification> builder = (HashCachedPair<Term, Term> p, Derivation d) -> {
            if (unify(d, fwd) && unify(d, !fwd))
                return d.unification(true);
            else
                return Unification.Null;
        };

        @Override
        public boolean test(Derivation derivation) {

            HashCachedPair premise = new HashCachedPair(derivation.taskTerm, derivation.beliefTerm);

            Unification u = cache.apply(premise, derivation, builder);

            if (u!=Unification.Null) {

                if (u instanceof PermutingUnification)
                    ((PermutingUnification) u).discover(derivation,
                            TermutatorFanOut, TermutatorSearchTTL);

                test(u, derivation);
            }

            return true;
        }
    }

}
