package nars.derive.op;

import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashRing;
import jcog.memoize.QuickMemoize;
import jcog.util.HashCachedPair;
import nars.derive.model.Derivation;
import nars.term.Term;
import nars.unify.Unification;
import nars.unify.unification.DeterministicUnification;
import nars.unify.unification.Termutifcation;

import java.util.List;
import java.util.function.BiFunction;

import static nars.NAL.derive.Termify_Forks;
import static nars.NAL.derive.TermutatorSearchTTL;

/** experimental unification caching (single thread only) */
public class CachingPremisify extends Premisify {

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
                        Termify_Forks, TermutatorSearchTTL);

            test(u, derivation);
        }

        return true;
    }

    protected boolean test(Unification u, Derivation d) {

        if (u instanceof Termutifcation)
            apply(((Termutifcation)u), d);

        else if (u instanceof DeterministicUnification)
            apply((DeterministicUnification) u, d);

        else
            throw new WTF();

        return d.live(); //HACK
    }


    public final void apply(DeterministicUnification xy, Derivation d) {
//        assert(d.transform.xy == null);
//            int start = d.size();
        d.transform.xy = xy::xy;
        d.retransform.clear();
        Term y = taskify.pattern(d).transform(d.transform);
//      d.revert(start);
        d.transform.xy = null;
        taskify.apply(y, d);
    }

    public final boolean apply(Termutifcation u, Derivation d) {
        List<DeterministicUnification> ii = u.listClone();
        int s = ii.size();
        if (s > 0) {
            if (s > 1)
                ((FasterList) ii).shuffleThis(d.random);

            int fanOut = Math.min(s, Termify_Forks);
            for (int i = 0; i < fanOut; i++) {
                apply(ii.get(i), d);
                if (!d.live())
                    return false;
            }
        }
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
                    ((Termutifcation) u).discover(d, Termify_Forks, searchTTL);
                }
            }

            return test(u, d);

        }
        return false;
    }
}
