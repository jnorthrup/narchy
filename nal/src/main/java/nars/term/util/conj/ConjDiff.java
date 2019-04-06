package nars.term.util.conj;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

public class ConjDiff extends Conj {
    private final Conj exc;
    private final boolean invert;
    private final long[] excludeEvents;

    public static ConjDiff the(Term include, long includeAt, Term exclude, long excludeAt) {
        return the(include, includeAt, exclude, excludeAt, false);
    }


    public static ConjDiff the(Term include, long includeAt, Term exclude, long excludeAt, boolean invert) {
        return the(include, includeAt, exclude, excludeAt, invert, null);

    }
    /** warning: invert may not work the way you expect. it is designed for use in IMPL construction */
    private static ConjDiff the(Term include, long includeAt, Term exclude, long excludeAt, boolean invert, @Nullable Conj seed) {
        Conj e = seed == null ? new Conj() : Conj.newConjSharingTermMap(seed);
        e.add(excludeAt, exclude);

        return the(include, includeAt, invert, e);
    }

    private static ConjDiff the(Term include, long includeAt, boolean invert, Conj exclude) {
        if (exclude.eventCount(ETERNAL)>0 && exclude.event.size() > 1) {
            //has both eternal and temporal components;
            // mask the eternal components so they are not eliminated from the result
            exclude.removeAll(ETERNAL);
        }

        return new ConjDiff(include, exclude, includeAt, invert);
    }

    private ConjDiff(Term include, Conj exclude, long offset, boolean invert) {
        super(exclude.termToId, exclude.idToTerm);
        this.exc = exclude;
        this.excludeEvents = exc.event.keySet().toArray();
        this.invert = invert;
        add(offset, include);
        //distribute();
    }

//        @Override
//        protected boolean addConjEventFactored() {
//            //disable adding in factored form
//            return false;
//        }

    @Override
    protected int filterAdd(long at, byte id, Term x) {
        if (at == ETERNAL) {
            boolean hasAbsorb = false;
            for (long see : excludeEvents) {
                int f = test(see, id);
                if (f == -1) return -1;
                if (f == +1) hasAbsorb = true; //but keep checking for contradictions first
            }
            if (hasAbsorb)
                return +1; //ignore this target (dont repeat in the predicate)
        } else {
            int f = test(at, id);
            if (f == -1) return -1;
            if (f == +1) return +1; //ignore this target (dont repeat in the predicate)
        }
        return 0;
    }

    private int test(long at, byte id) {
        return exc.conflictOrSame(at, (byte) (id * (invert ? -1 : +1)));
    }
}
