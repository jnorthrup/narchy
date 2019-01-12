package nars.term.util.conj;

import nars.term.Term;

import static nars.time.Tense.ETERNAL;

public class ConjDiff extends Conj {
    private final Conj se;
    private final boolean invert;
    private final long[] seEvents;

    public static ConjDiff the(long subjAt, Term subj, long offset, Term subtractWith, boolean invert) {
        Conj subtractFrom = new Conj(subjAt, subj);

        if (subtractFrom.eventCount(ETERNAL)>0 && subtractFrom.event.size() > 1) {
            //has both eternal and temporal components; mask the eternal components so they are not eliminated from the result
            subtractFrom.removeAll(ETERNAL);
        }

        return new ConjDiff(subtractFrom, offset, subtractWith, invert);
    }

    ConjDiff(Conj subtractFrom, long offset, Term subtractWith, boolean invert) {
        super(subtractFrom.termToId, subtractFrom.idToTerm);
        this.se = subtractFrom;
        this.seEvents = se.event.keySet().toArray();
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
    protected int addFilter(long at, Term x, byte id) {
        if (at == ETERNAL) {
            boolean hasAbsorb = false;
            for (long see : seEvents) {
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
