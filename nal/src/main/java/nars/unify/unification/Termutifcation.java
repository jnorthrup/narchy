package nars.unify.unification;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unification;
import nars.unify.Unify;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Random;

/**
 * Termutator-permuted Unification
 *      not thread-safe
 */
public class Termutifcation extends ArrayHashSet<DeterministicUnification> implements Unification {

    private final Unify base;
    private final Termutator[] termutes;

    public Termutifcation(Unify u, DeterministicUnification pre, Termutator[] termutes) {
        super(0);
        this.termutes = termutes;

        UnifiedMap<Term, Term> baseMap = new UnifiedMap<>(4, 1f);
        Unify.ContinueUnify base = new Unify.ContinueUnify(u, baseMap);
        boolean applied = pre.apply(base);
        assert(applied);
        baseMap.trimToSize();
        this.base = base;


//        restart = discovery.size();
    }


    /**
     * returns how many TTL used
     */
    public void discover(Unify ctx, int discoveriesMax, int ttl) {


        Discovery u = new Discovery(this.base, discoveriesMax);
        u.setTTL(ttl);

        u.matches(termutes);

        int spent = Util.clamp(ttl - u.ttl, 0, ttl);

        ctx.use(spent);
    }

    @Override
    public final int forkKnown() {
        return size();
    }

    @Override
    public Iterable<Term> apply(Term x) {
        switch (size()) {
            case 0:
                return List.of();
            case 1:
                return first().apply(x);
            default:
                //HACK could be better
                /* equals between Term and Unification:
                Reports calls to .equals() where the target and argument are of incompatible types. While such a call might theoretically be useful, most likely it represents a bug. */
                return Iterables.filter(
                        Iterables.transform(shuffle(this, base.random), a -> a.transform(x)),
                        z -> z != null
                                &&
                                z != Unification.Null
                );
        }
    }

    static private MutableList<DeterministicUnification> shuffle(ArrayHashSet<DeterministicUnification> fork, Random rng) {
        return fork.list.clone().shuffleThis(rng);
    }

    public List<DeterministicUnification> listClone() {
        FasterList<DeterministicUnification> l = list;
        switch (l.size()) {
            case 0: return List.of();
            case 1: return List.of(l.getOnly());
            default: return list.clone();
        }
    }


    private class Discovery extends Unify.ContinueUnify {

        private final Unify parent;
        private int discoveriesRemain;

        /**
         * if xy is null then inherits the Map<Term,Term> from u
         * otherwise, no mutable state is shared between parent and child
         *
         * @param parent
         * @param xy
         */
        public Discovery(Unify parent, int discoveriesRemain) {
            super(parent, new UnifiedMap<>(0));
            this.parent = parent;
            this.discoveriesRemain = discoveriesRemain;
        }

        @Override
        public Term resolveVar(Variable x) {
            Term y = parent.resolveVar(x);
            if (y != null && y != x) {
                if (size==0 || !var(y))
                    return y; //constant

                x = (Variable) y;   //recurse thru this resolver
            }

            return size > 0 ? super.resolveVar(x) : x;
        }

//        @Override
//        public boolean live() {
//            return super.live() && discoveriesRemain > 0;
//        }

        @Override
        protected boolean match() {

            Unification z = unification(false);
            if (z != Unification.Null) {
                if (z!=Self && z instanceof DeterministicUnification) {
                    if (Termutifcation.this.add((DeterministicUnification) z)) {
                        //TODO calculate max possible permutations from Termutes, and set done
                    }
                } else {
                    /*else {
                        return Iterators.getNext(a.apply(x).iterator(), null); //HACK
                    }*/
                }
            }

            return --discoveriesRemain > 0;
        }
    }
}
