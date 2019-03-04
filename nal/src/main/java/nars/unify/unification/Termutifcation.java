package nars.unify.unification;

import com.google.common.collect.Iterables;
import jcog.Util;
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
        pre.apply(base);
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

        u.tryMatches(termutes);

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
        public Term resolve(Variable x) {
            Term y = parent.resolve(x);
            if (y != null && y != x) {
                if (!(y instanceof Variable) || size == 0)
                    return y; //constant

                x = (Variable) y;   //recurse thru this resolver
            }

            return size > 0 ? super.resolve(x) : x;
        }

        @Override
        public boolean live() {
            return super.live() && discoveriesRemain > 0;
        }

        @Override
        protected void tryMatch() {

            --discoveriesRemain;

            Unification z = unification(false);
            if (z != Unification.Null) {
                if (z instanceof DeterministicUnification) {
                    if (Termutifcation.this.add((DeterministicUnification) z)) {
                        //TODO calculate max possible permutations from Termutes, and set done
                    }
                } else {
                    /*else {
                        return Iterators.getNext(a.apply(x).iterator(), null); //HACK
                    }*/
                }
            }
        }
    }
}
