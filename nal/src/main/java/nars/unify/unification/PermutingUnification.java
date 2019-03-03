package nars.unify.unification;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import jcog.data.set.ArrayHashSet;
import nars.Param;
import nars.term.Term;
import nars.unify.Unification;
import nars.unify.Unify;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.api.list.MutableList;

import java.util.List;
import java.util.Random;

/**
 * not thread-safe
 */
public class PermutingUnification extends ArrayHashSet<DeterministicUnification> implements Unification {

    private final Unify discovery;
    private final int restart;
    private final Termutator[] termutes;
    private int discoveriesRemain;


    public PermutingUnification(Unify u, DeterministicUnification pre, Termutator[] termutes) {
        super(0);
        this.termutes = termutes;

        this.discovery = new MyUnify(u);
        pre.apply(discovery);
        restart = discovery.size();
    }


    /**
     * returns how many TTL used
     */
    public int discover(int discoveriesMax, int ttl) {
        discovery.revert(restart);

        this.discoveriesRemain = discoveriesMax;
        discovery.setTTL(ttl);

        discovery.tryMatches(termutes);

        return Util.clamp(ttl - discovery.ttl, 0, ttl);
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
                        Iterables.transform(shuffle(this, discovery.random), a -> a.transform(x)),
                        z -> z != null
                                &&
                                z != Unification.Null
                );
        }
    }

    static private MutableList<DeterministicUnification> shuffle(ArrayHashSet<DeterministicUnification> fork, Random rng) {
        return fork.list.clone().shuffleThis(rng);
    }

    private final class MyUnify extends Unify.EmptyUnify {

        public MyUnify(Unify u) {
            super(u);
        }

        @Override
        public boolean live() {
            return super.live() && discoveriesRemain > 0;
        }

        @Override
        protected void tryMatch() {

            --discoveriesRemain;

            Unification z = unification(false);
            if (z instanceof DeterministicUnification) {
                if (PermutingUnification.this.add((DeterministicUnification) z)) {
                    //TODO calculate max possible permutations from Termutes, and set done
                }
            } else if (z instanceof PermutingUnification) {
                if (Param.DEBUG)
                    throw new TODO("recursive or-other 2nd-layer termute");
            }
        }

    }
}
