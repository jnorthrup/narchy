package nars.derive.op;

import jcog.WTF;
import jcog.data.set.ArrayHashRing;
import jcog.memoize.QuickMemoize;
import jcog.util.HashCachedPair;
import nars.$;
import nars.NAL;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompound;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.transform.AbstractTermTransform;
import nars.unify.Unification;
import nars.unify.unification.DeterministicUnification;
import nars.unify.unification.Termutifcation;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.$.$$;
import static nars.NAL.derive.TermUnifyForkMax;
import static nars.NAL.derive.TermutatorSearchTTL;

/**
 * Created by me on 5/26/16.
 */
public class Premisify extends AbstractPred<Derivation> {


    private final Term taskPat, beliefPat;
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

        //not working yet:
        //return substituteUnification(d);

        substituteDirect(d, TermUnifyForkMax);
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
                int searchTTL = TermutatorSearchTTL;
                if (((Termutifcation) u).isEmpty() || d.ttl > searchTTL) {
                    ((Termutifcation) u).discover(d, TermUnifyForkMax, searchTTL);
                }
            }

            return test(u, d);

        }
        return false;
    }

    /**
     * the original, direct method
     */
    private void substituteDirect(Derivation d, int forkLimit) {
        if (!unify(d, fwd, false))
            return;

        MatchFork mf = d.termBuilder;
        mf.reset(taskify, forkLimit);

        d.forEachMatch = mf;

        try {

            boolean unified = unify(d, !fwd, true);

        } finally {
            d.forEachMatch = null;
        }
    }

    protected boolean unify(Derivation d, boolean dir, boolean finish) {
        return d.unify(dir ? taskPat : beliefPat, dir ? d.taskTerm : d.beliefTerm, finish);
    }

    protected boolean test(Unification u, Derivation d) {

        if (u instanceof Termutifcation)
            taskify.test(((Termutifcation)u), d);

        else if (u instanceof DeterministicUnification)
            taskify.test((DeterministicUnification) u, d);

        else
            throw new WTF();

        return d.live(); //HACK
    }

    private static final Atomic UNIFY = $.the("unify");


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
                            TermUnifyForkMax, TermutatorSearchTTL);

                test(u, derivation);
            }

            return true;
        }
    }

    public static final class MatchFork extends LazyCompound implements Predicate<Derivation> {

        private int forkLimit = -1;
        final Set<Term> tried = new UnifiedSet();
        private Taskify taskify;

        public MatchFork() {
        }

        public void reset(Taskify taskify, int forkLimit) {
            this.taskify = taskify;
            this.forkLimit = forkLimit;
            tried.clear();
            clear();
        }

        @Override
        public boolean test(Derivation dd) {
            //assert(finalTTL[0]==-1);

            Term x = taskify.termify.pattern;
            Term y = AbstractTermTransform.transform(this, x,
                    dd.transform,
                    x.volume() +
                            NAL.derive.TERMIFY_VOLMAX_SCRATCH_BASE +
                            (int)Math.ceil(NAL.derive.TERMIFY_TERM_VOLMAX_SCRATCH_FACTOR * dd.termVolMax
                    )
            );

            if (!(y instanceof Bool) && y.unneg().op().taskable) {

                if (tried.add(y)) {

                    taskify.test(y, dd);

                    if (tried.size() >= forkLimit) {
                        return false;
                    }
                }
            }

            return true;
        }


    }
}
