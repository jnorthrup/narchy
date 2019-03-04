package nars.derive.op;

import jcog.WTF;
import jcog.data.set.ArrayHashRing;
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
import nars.unify.unification.Termutifcation;

import java.util.function.BiFunction;

import static nars.$.$$;
import static nars.Param.TermutatorFanOut;
import static nars.Param.TermutatorSearchTTL;

/**
 * Created by me on 5/26/16.
 */
public class Premisify extends AbstractPred<Derivation> {


    public final Term taskPat, beliefPat;
    final boolean fwd;

    private static final Atomic FWD = Atomic.the("fwd");
    private static final Atomic REV = Atomic.the("rev");
    public final Taskify taskify;

    public Premisify(Term taskPat, Term beliefPat, boolean fwd, Taskify taskify) {
        super($.funcFast(UNIFY, $.pFast(taskPat, beliefPat), fwd ? FWD : REV));
        this.taskPat = taskPat;
        this.beliefPat = beliefPat;
        this.fwd = fwd;
        this.taskify = taskify;
    }

    @Override
    public boolean test(Derivation d) {

        //return substituteUnification(d);

        substituteDirect(d);
        return true;
    }


    /**
     * memoizable method
     * @return
     */
    private boolean substituteUnification(Derivation d) {
        if (unify(d, fwd, false) && unify(d, !fwd, false)) {

            Unification u = d.unification(true);

            if (u instanceof Termutifcation) {
                if (!((Termutifcation) u).discover(d, TermutatorFanOut, TermutatorSearchTTL))
                    return false;
            }

            if (u!=Unification.Null)
                return test(u, d);
        }
        return true;
    }

    /**
     * the original, direct method
     */
    private void substituteDirect(Derivation d) {
        if (!unify(d, fwd, false))
            return;

        d.forEachMatch = (dd) -> {
            Term y = AbstractTermTransform.transform(taskify.termify.pattern, dd.transform);
            if (!(y instanceof Bool) && y.unneg().op().taskable)
                return taskify.test(y, dd);
            else
                return true;
        };

        boolean unified = unify(d, !fwd, true);

        d.forEachMatch = null;

    }
    protected boolean unify(Derivation d, boolean dir, boolean finish) {
        return d.unify(dir ? taskPat : beliefPat, dir ? d.taskTerm : d.beliefTerm, finish);
    }

    protected boolean test(Unification u, Derivation d) {

        if (u instanceof Termutifcation)
            return taskify.test(((Termutifcation)u), d);

        else if (u instanceof DeterministicUnification)
            return taskify.test((DeterministicUnification) u, d);

        else
            throw new WTF();
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

    /** experimental unification caching (single thread only) */
    public static class CachingPremisify extends Premisify {

        final ArrayHashRing<HashCachedPair<Term,Term>> impossible = new ArrayHashRing<>(64);

        final QuickMemoize<HashCachedPair<Term,Term>, Unification> cache = new QuickMemoize<>(64) {
            @Override
            protected boolean store(HashCachedPair<Term, Term> p, Unification unification) {
                if (unification==Unification.Null) {
                    impossible.add(p);
                    return false;
                } else
                    return true;
            }
        };

        public CachingPremisify(Term taskPatern, Term beliefPattern, boolean fwd, Taskify taskify) {
            super(taskPatern, beliefPattern, fwd, taskify);
        }

        final BiFunction<HashCachedPair<Term, Term>, Derivation, Unification> builder = (HashCachedPair<Term, Term> p, Derivation d) -> {
            if (unify(d, fwd, false) && unify(d, !fwd, false))
                return d.unification(true);
            else
                return Unification.Null;
        };

        @Override
        public boolean test(Derivation derivation) {

            final HashCachedPair<Term,Term> premise = new HashCachedPair<>(derivation.taskTerm, derivation.beliefTerm);

            if (impossible.contains(premise)) {
                /** TODO log these wasted premises for most frequent offenders to cull the spam */
                return true;
            }


            Unification u = cache.apply(premise, derivation, builder);

            if (u!=Unification.Null) {

                if (u instanceof Termutifcation)
                    ((Termutifcation) u).discover(derivation,
                            TermutatorFanOut, TermutatorSearchTTL);

                test(u, derivation);
            }

            return true;
        }
    }

}
