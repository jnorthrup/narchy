package nars.term.util;

import jcog.Util;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.Tense;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * temporal term intermpolation
 */
public enum Intermpolate {;

    private static Term intermpolate(/*@NotNull*/ Term a, @Deprecated long bOffset, /*@NotNull*/ Term b, float aProp, float curDepth, NAR nar) {

        if (a.equals(b)/* && bOffset == 0*/)
            return a;

        if (a instanceof Atomic || b instanceof Atomic)
            return Null; //atomics differ

        Op ao = a.op(), bo = b.op();
        if (ao != bo)
            return Null;


        int len = a.subs();
        assert len > 0;
//        if (len == 0) {
//            //WTF
//            if (a.op() == PROD) return a;
////            else
////                throw new WTF();
//        }

        if (!a.equalsRoot(b))
            return Null;

        Subterms aa = a.subterms(), bb = b.subterms();

//        if (!aa.equalsRoot(bb))
//            return Null;

        if (ao.temporal) {
                if (ao == CONJ) {// && Conj.isSeq(a) || Conj.isSeq(b)) {
                    return new Conjterpolate(a, b, aProp, nar.random()).term(); //root only: conj sequence merge
                }

            //            if (ao == CONJ && curDepth == 1) {

            return dtMergeTemporalDirect(a, b, aProp, curDepth, nar);
//            } else {
//            }
        }


//            if (aa.equals(bb))
//                return a;

        Term[] ab = new Term[len];
        boolean change = false;
        for (int i = 0; i < len; i++) {
            Term ai = aa.sub(i), bi = bb.sub(i);
            if (!ai.equals(bi)) {
                Term y = intermpolate(ai, 0, bi, aProp, curDepth / 2f, nar);
                if (y == Null)
                    return Null;

                if (!ai.equals(y)) {
                    change = true;
                    ai = y;
                }
            }
            ab[i] = ai;
        }

        return !change ? a : ao.the(ab);
    }

    /**
     * for merging CONJ or IMPL of equal subterms, so only dt is different
     */
    private static Term dtMergeTemporalDirect(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp,
                                                           float depth, NAR nar) {
        int dt = chooseDT(a, b, aProp, nar);
//        if (dt == DTERNAL) {
//            return $.disj(a, b); //OR
//        }

        Subterms aa = a.subterms(), bb = b.subterms();
        if (aa.equals(bb))
            return a.dt(dt);
        else {
            Term[] ab = Util.map(aa.subs(), Term[]::new, i -> {
                return intermpolate(aa.sub(i), 0, bb.sub(i), aProp, depth / 2, nar);
            });

            return a.op().the(dt, ab);
        }
    }

    public static int chooseDT(Term a, Term b, float aProp, NAR nar) {
        int adt = a.dt(), bdt = b.dt();
        return chooseDT(adt, bdt, aProp, nar);
    }

    public static int chooseDT(int adt, int bdt, float aProp, NAR nar) {
        int dt;
        if (adt == bdt) {
            dt = adt;
        } else if (adt == XTERNAL || bdt == XTERNAL) {

            dt = adt == XTERNAL ? bdt : adt;
            //dt = choose(adt, bdt, aProp);

        } else if (adt == DTERNAL || bdt == DTERNAL) {

            dt = DTERNAL;
            //dt = adt == DTERNAL ? bdt : adt;
            //dt = choose(adt, bdt, aProp, nar.random());

        } else {
            dt = merge(adt, bdt, aProp, nar);
        }


        return Tense.dither(dt, nar);
    }

    /**
     * merge delta
     */
    static int merge(int adt, int bdt, float aProp, NAR nar) {
        if (adt >= 0 == bdt >= 0) { //require same sign ?

            int range = //Math.max(Math.abs(adt), Math.abs(bdt));
                    Math.abs(adt - bdt);
            int ab = Tense.dither(Util.lerp(aProp, bdt, adt), nar);
            int delta = Math.max(Math.abs(ab - adt), Math.abs(ab - bdt));
            float ratio = ((float) delta) / range;
            if (ratio <= nar.intermpolationRangeLimit.floatValue()) {
                return ab;
            }
            return ab;
        }

        //discard temporal information by resorting to eternity
        return DTERNAL;
    }

    public static Term intermpolate(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, 0, b, aProp, nar);
    }

    /**
     * a is left aligned, dt is any temporal shift between where the terms exist in the callee's context
     */
    public static Term intermpolate(/*@NotNull*/ Term a, long dt, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, dt, b, aProp, 1, nar);
    }

    /**
     * heuristic representing the difference between the dt components
     * of two temporal terms.
     * 0 means they are identical or otherwise match.
     * > 0 means there is some difference.
     * <p>
     * this adds a 0.5 difference for && vs &| and +1 for each dt
     * XTERNAL matches anything
     */
    public static float dtDiff(Term a, Term b) {
        return dtDiff(a, b, 1);
    }

    private static float dtDiff(Term a, Term b, int depth) {
        if (a.equals(b))
            return 0f;

        if (!a.equalsRoot(b))
            return Float.POSITIVE_INFINITY;

        Op ao = a.op(), bo = b.op();
        if (ao != bo)
            return Float.POSITIVE_INFINITY;

        Subterms aa = a.subterms(), bb = b.subterms();
//        if (!aa.equalsRoot(bb))
//            return Float.POSITIVE_INFINITY;
//        if ((((aa.structure() != bb.structure() || (a.volume() != b.volume())) && !(aa.hasAny(CONJ) || bb.hasAny(CONJ))))) {
//            return Float.POSITIVE_INFINITY;
//        }


        float dSubterms = 0;
        if (!aa.equals(bb)) {

            if (ao == CONJ) {
                if (a.dt()==XTERNAL || b.dt()==XTERNAL)
                    return 0;
                if ((Conj.isSeq(a) || Conj.isSeq(b))) {
                    //estimate difference
                    int ar = a.eventRange(), br = b.eventRange();
                    int av = a.volume(), bv = b.volume();
                    return (1 + av + bv) / 2 * (1 + Math.abs(av - bv)) * ((1 + Math.abs(ar - br))); //heuristic

                }
            }

            int len = aa.subs();
            if (len != bb.subs())
                return Float.POSITIVE_INFINITY;

            for (int i = 0; i < len; i++) {
                Term ai = aa.sub(i);
                float di = dtDiff(ai, bb.sub(i), depth + 1);
                if (!Float.isFinite(di)) {
                    return Float.POSITIVE_INFINITY;
                }
                dSubterms += di;
            }

            dSubterms /= len;
        }


        float dDT;
        int adt = a.dt(), bdt = b.dt();
        if (adt != bdt) {
            if (adt == XTERNAL || bdt == XTERNAL) {
                //dDT = 0.25f; //undercut the DTERNAL case
                dDT = ScalarValue.EPSILONsqrt;
            } else {

                boolean ad = adt == DTERNAL, bd = bdt == DTERNAL;
                if (!ad && !bd) {
                    float range = Math.min(1 + Math.abs(adt), 1 + Math.abs(bdt));
                    assert (range > 0);
                    dDT = Math.abs(adt - bdt) / (range);
                } else {
                    //dDT = 0.5f; //one is dternal the other is not, record at least some difference (half time unit)
                    dDT = ScalarValue.EPSILONsqrt * 2;
                }

            }

        } else {
            dDT = 0;
        }


        return dDT + dSubterms;
    }

}
