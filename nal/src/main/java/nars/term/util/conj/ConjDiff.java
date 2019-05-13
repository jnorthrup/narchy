package nars.term.util.conj;

import jcog.util.ArrayUtil;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

public final class ConjDiff extends Conj {
    private static final int ABSORB = +1;
    private static final int CONFLICT = -1;

    private final Conj exc;
    private final boolean invert;
    private final long[] excludeEventsAt;
    private final boolean excludeEventAtEternal;

    public static ConjBuilder the(Term include, long includeAt, Term exclude, long excludeAt) {
        return the(include, includeAt, exclude, excludeAt, false);
    }


    public static ConjBuilder the(Term include, long includeAt, Term exclude, long excludeAt, boolean invert) {
        return the(include, includeAt, exclude, excludeAt, invert, null);
    }

    /** warning: invert may not work the way you expect. it is designed for use in IMPL construction */
    private static ConjBuilder the(Term include, long includeAt, Term exclude, long excludeAt, boolean invert, @Nullable Conj seed) {
        Conj e = seed == null ? new Conj() : Conj.newConjSharingTermMap(seed);

        return e.add(excludeAt, exclude) ? the(include, includeAt, invert, e) : e;
    }

    private static ConjDiff the(Term include, long includeAt, boolean invert, Conj exclude) {
        if (exclude.eventCount(ETERNAL)>0 && exclude.event.size() > 1) {
            //has both eternal and temporal components;
            // mask the eternal components so they are not eliminated from the result
            exclude.removeAll(ETERNAL);
        }

        ConjDiff c = new ConjDiff(include, exclude, invert);
        c.add(includeAt, include);
        //distribute();
        return c;
    }

    private ConjDiff(Term include, Conj exclude, boolean invert) {
        super(exclude.termToId, exclude.idToTerm);
        this.exc = exclude;
        this.excludeEventsAt = exc.event.keySet().toArray();
        this.excludeEventAtEternal = ArrayUtil.indexOf(excludeEventsAt, ETERNAL)!=-1;
        this.invert = invert;
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
            for (long see : excludeEventsAt) {
                int f = test(see, id, x);
                if (f == -1) return -1;
                if (f == +1) hasAbsorb = true; //but keep checking for contradictions first
            }
            if (hasAbsorb)
                return +1; //ignore this target (dont repeat in the predicate)
        } else {
            int f = test(at, id, x);
            if (f == -1) return -1;
            if (f == +1) return +1; //ignore this target (dont repeat in the predicate)
        }
        return 0;
    }

//    @Override
//    protected int filterAdd(long at, byte id, Term x) {
//
//        if (at == ETERNAL) {
//            boolean absorbed = false;
//            for (long excludeAt : excludeEventsAt) {
//                int f = test(excludeAt, id);
//                if (f == CONFLICT) return CONFLICT;
//                if (f == ABSORB) absorbed = true; //but continue testing for contradictions before returning
//            }
//            if (absorbed)
//                return ABSORB;
//        } else {
//            int f = ArrayUtils.indexOf(excludeEventsAt, at)!=-1 ? test(at, id) : 0;
//            if (f == CONFLICT) return CONFLICT;
//            if (excludeEventAtEternal) {
//                int g = test(ETERNAL, id);
//                if (g == CONFLICT) return CONFLICT;
//                if (g == ABSORB) return ABSORB; //absorbed
//            }
//            if (f == ABSORB) return ABSORB; //asorbed
//        }
//        return 0;
//    }

    private int test(long at, byte id, Term x) {
        return exc.conflictOrSame(at, (byte) (id * (invert ? -1 : +1)), x);
    }
}
