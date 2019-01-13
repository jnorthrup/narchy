package nars.term.util;

import jcog.Util;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.conj.Conj;
import nars.term.util.conj.Conjterpolate;
import nars.time.Tense;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * temporal term intermpolation
 */
public enum Intermpolate {;

    private static Term intermpolate(/*@NotNull*/ Term a,  /*@NotNull*/ Term b, float aProp, float curDepth, NAR nar) {

        if (a.equals(b))
            return a;

        if (!a.equalsRoot(b))
            return Null;



        Op ao = a.op();//, bo = b.op();
//        if (ao != bo)         return Null; //checked in equalRoot


        int len = a.subs();
        assert len > 0;
//        if (len == 0) {
//            //WTF
//            if (a.op() == PROD) return a;
////            else
////                throw new WTF();
//        }


        Subterms aa = a.subterms(), bb = b.subterms();

//        if (!aa.equalsRoot(bb))
//            return Null;

        if (ao.temporal) {
            if (aa.subs() == bb.subs() && aa.equals(bb)) {
                return dtMergeTemporalDirect(a, b, aProp, curDepth, nar);
            }

            if (ao == CONJ) {
//                if (Conj.isSeq(a) || Conj.isSeq(b)) {
                    return new Conjterpolate(a, b, aProp, nar.random()).term(); //root only: conj sequence merge
//                } else {
//                    throw new WTF();
//                }
            }

            //            if (ao == CONJ && curDepth == 1) {

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
                Term y = intermpolate(ai, bi, aProp, curDepth / 2f, nar);
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
                return intermpolate(aa.sub(i), bb.sub(i), aProp, depth / 2, nar);
            });

            return a.op().the(dt, ab);
        }
    }

    public static int chooseDT(Term a, Term b, float aProp, NAR nar) {
        return chooseDT(a.dt(), b.dt(), aProp, nar);
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
        //if (adt >= 0 == bdt >= 0) { //require same sign ?


            int range = Math.max(Math.abs(adt), Math.abs(bdt));
                    //Math.abs(adt - bdt);
            int ab = Util.lerp(aProp, bdt, adt);
            int delta = Math.max(Math.abs(ab - adt), Math.abs(ab - bdt));
            float ratio = ((float) delta) / range;
            if (ratio <= nar.intermpolationRangeLimit.floatValue()) {
                return Tense.dither(ab, nar);
            }
//        }
//
//        //discard temporal information by resorting to eternity
        return DTERNAL;
    }

    public static Term intermpolate(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, b, aProp, 0, nar);
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
                    float range = 1 + Math.abs(adt) + Math.abs(bdt);
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
