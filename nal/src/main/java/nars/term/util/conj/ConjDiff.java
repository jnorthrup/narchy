package nars.term.util.conj;

import nars.term.Term;

import static nars.time.Tense.ETERNAL;

public class ConjDiff extends Conj {
    private final Conj se;
    private final boolean invert;
    private final long[] excludeEvents;

    public static ConjDiff the(Term include, long includeAt, Term exclude, long excludeAt) {
        return the(include, includeAt, exclude, excludeAt);
    }

    /** warning: invert may not work the way you expect. it is designed for use in IMPL construction */
    public static ConjDiff the(Term include, long includeAt, Term exclude, long excludeAt, boolean invert) {
        Conj subtractFrom = new Conj(excludeAt, exclude);

        if (subtractFrom.eventCount(ETERNAL)>0 && subtractFrom.event.size() > 1) {
            //has both eternal and temporal components;
            // mask the eternal components so they are not eliminated from the result
            subtractFrom.removeAll(ETERNAL);
        }

        return new ConjDiff(subtractFrom, includeAt, include, invert);
    }

    ConjDiff(Conj subtractFrom, long offset, Term subtractWith, boolean invert) {
        super(subtractFrom.termToId, subtractFrom.idToTerm);
        this.se = subtractFrom;
        this.excludeEvents = se.event.keySet().toArray();
        this.invert = invert;
        add(offset, subtractWith);
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
                return +1; //ignore this term (dont repeat in the predicate)
        } else {
            int f = test(at, id);
            if (f == -1) return -1;
            int f2 = (at == ETERNAL) ? f : test(ETERNAL, id);
            if (f2 == -1) return -1;
            if (f == +1 || f2 == +1) return +1; //ignore this term (dont repeat in the predicate)
        }
        return 0;
    }

    private int test(long at, byte id) {
        return se.conflictOrSame(at, (byte) (id * (invert ? -1 : +1)));
    }
}
