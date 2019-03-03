package nars.unify;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import jcog.data.set.ArrayHashSet;
import nars.Param;
import nars.term.Term;
import nars.unify.mutate.Termutator;
import org.eclipse.collections.api.list.MutableList;

import java.util.List;
import java.util.Random;

import static nars.term.atom.Bool.Null;

/**
 * not thread-safe
 */
public class PermutingUnification extends Unification {

    private final DeterministicUnification start;
    public final ArrayHashSet<DeterministicUnification> fork = new ArrayHashSet();

    public final Termutator[] termutes;
    private final Unify discovery;
    private final int restart;
    private int discoveriesRemain;


    public PermutingUnification(Unify x, DeterministicUnification start, Termutator[] termutes) {
        super();
        this.start = start;
        this.termutes = termutes;
        this.discovery = new Unify.LazyUnify(x) {

            @Override
            public boolean live() {
                return super.live() && discoveriesRemain > 0;
            }

            @Override
            protected void tryMatch() {

                --discoveriesRemain;

                Unification z = unification(false);
                if (z instanceof DeterministicUnification) {
                    if (fork.add((DeterministicUnification) z)) {
                        //TODO calculate max possible permutations from Termutes, and set done
                    }
                } else if (z instanceof nars.unify.PermutingUnification) {
                    if (Param.DEBUG)
                        throw new TODO("recursive or-other 2nd-layer termute");
                }
            }

        };
        start.apply(discovery);
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
    public int forkCount() {
        return fork.size();
    }

    @Override
    public Iterable<Term> apply(Term x) {
        switch (fork.size()) {
            case 0:
                return List.of();
            case 1:
                return fork.first().apply(x);
            default:
                //HACK could be better
                return Iterables.filter(
                        Iterables.transform(shuffle(fork, discovery.random), a -> a.transform(x)),
                        z -> z != null
                                &&
                                z != Null
                );
        }
    }

    static private MutableList<DeterministicUnification> shuffle(ArrayHashSet<DeterministicUnification> fork, Random rng) {
        return fork.list.clone().shuffleThis(rng);
    }
}
