package nars.term.util.conj;

import jcog.TODO;
import jcog.data.bit.MetalBitSet;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.Image;
import nars.unify.UnifyTransform;

import java.util.concurrent.ThreadLocalRandom;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.XTERNAL;

public enum ConjMatch { ;


    /** TODO as argument */
    @Deprecated static final int varBits =
        VAR_DEP.bit | VAR_INDEP.bit | VAR_QUERY.bit;
        //VAR_DEP.bit | VAR_INDEP.bit;
        //VAR_INDEP.bit | VAR_QUERY.bit;
        //VAR_INDEP.bit;
        //VAR_DEP.bit;

    public static final Atom BEFORE = Atomic.atom("conjBefore");
    public static final Atom CONJ_WITHOUT_UNIFY = Atomic.atom("conjWithoutUnify");
    public static final Atom AFTER = Atomic.atom("conjAfter");

    /**
     * returns the prefix or suffix sequence of a specific matched subevent
     * TODO configurable dtTolerance for matches
     */
    public static Term beforeOrAfter(Term conj, Term event, boolean includeBefore, boolean includeMatched, boolean includeAfter,  UnifyTransform s, int ttl /*, unifyOrEquals, includeMatchedEvent */) {
        if (!(conj instanceof Compound) || conj.opID() != CONJ.id || conj.dt()==XTERNAL || conj.equals(event))
            return Null;

        if (!event.op().eventable)
            return Null;

        event = Image.imageNormalize(event);

        int varBits = ConjMatch.varBits;
        if (!Term.commonStructure( event.structure()&(~varBits), conj.subStructure()&(~varBits)))
            return Null;

        return beforeOrAfterSeq(conj, event, includeBefore, includeMatched, includeAfter, varBits, s, ttl);

    }

    private static Term beforeOrAfterSeq(Term conj, Term event, boolean includeBefore, boolean includeMatched, boolean includeAfter, int varBits, UnifyTransform s, int ttl) {

        boolean eVar = event.hasAny(varBits);
        boolean unify = eVar || conj.hasAny(varBits);

        if (!unify && (!(event instanceof Compound) || event.opID()!=CONJ.id)) {
            if (!Conj.isSeq(conj)) {
                if (!includeMatched) {
                    //simple parallel remove match case
                    Subterms cs = conj.subterms();
                    Subterms csNext = cs.remove(event);
                    if (csNext==cs)
                        return Null;
                    return cs != csNext ? (csNext.subs() > 1 ? CONJ.the(csNext) : csNext.sub(0)) : Null;
                } else
                    throw new TODO();
            } else {
                if (!conj.containsRecursively(event))
                    return Null; //quick fail test for simple event case
            }
        }

        //TODO only include events that actually can be returned
        ConjList seq = ConjList.events(conj);

        int n = seq.size();


        MetalBitSet matches = includeMatched ? null : MetalBitSet.bits(n); //only necessary if they are to be removed
        s.clear(varBits, false);
        s.setTTL(ttl);

        boolean forward = (includeAfter != includeBefore) ? includeAfter : ThreadLocalRandom.current().nextBoolean();

        ConjList ee = ConjList.events(event);
        EventUnifier u = unify ? new EventUnifier(s) : null;

        int[] at = seq.contains(ee, unify ? u : Term::equals, 1, forward, matches, s.dtTolerance);
        if (at.length == 0)
            return Null;

        long matchStart = at[0], matchEnd = (forward ? matchStart + event.eventRange() : matchStart - event.eventRange());

        if (!includeMatched)
            seq.removeAll(matches);

        if (!includeBefore)
            seq.removeIf((w,x)-> w < matchEnd);

        if (!includeAfter)
            seq.removeIf((w,x)-> w > matchStart);


        Term ss = seq.term();
        if (u!=null && ss.hasAny(varBits)) {
            ss = u.apply(ss); //transform any unified vars
            if (u.u instanceof UniSubst.MyUnifyTransform)
                ((UniSubst.MyUnifyTransform)u.u).commitSubst(); //HACK
        }

        return ss;
    }

//    private static boolean removeExtreme(Term event, ConjList x, long[] matchedTime, BiPredicate<Term, Term> eq, boolean beforeOrAfter) {
//        int s = x.size();
//
//        for (int j = 0; j < s; j++) {
//            int i = beforeOrAfter ? (s - 1 - j) : j; //proceed from opposite direction
//            Term what = x.get(i);
//            if (eq.test(event, what)) {
//                long when = x.when(i);
//                matchedTime[0] = Math.min(matchedTime[0], when);
//                matchedTime[1] = Math.max(matchedTime[1], when);
//                x.removeThe(i);
//                return true;
//            }
//        }
//        return false;
//
//    }
//
//
//    /** removes one matching event at random, visiting them in a fairly shuffled order */
//    private static boolean removeAny(Term event, ConjList x, long[] matchedTime, Random random) {
//
//        int s = x.size();
//        int i = s > 1 ? random.nextInt(s) : 0; //random starting point
//        for (int j = 0; j < s; j++) {
//            Term what = x.get(i);
//            if (what.equals(event)) {
//                long when = x.when(i);
//                matchedTime[0] = Math.min(matchedTime[0], when);
//                matchedTime[1] = Math.max(matchedTime[1], when);
//                x.removeThe(i);
//                return true;
//            }
//            if (++i == s) i = 0;
//        }
//        return false;
//    }

}
