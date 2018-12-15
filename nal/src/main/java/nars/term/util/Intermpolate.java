package nars.term.util;

import jcog.Util;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.time.Tense;

import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/** temporal term intermpolation */
public enum Intermpolate { ;

    private static Term intermpolate(/*@NotNull*/ Term a, long bOffset, /*@NotNull*/ Term b, float aProp, float curDepth, NAR nar) {

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


        if (ao.temporal) {
//            if (ao == CONJ && curDepth == 1) {
//                return Conj.conjIntermpolate(a, b, aProp, bOffset); //root only: conj sequence merge
//            } else  {
                return dtMergeDirect(a, b, aProp, curDepth, nar);
//            }
        } else {

            Subterms aa = a.subterms(), bb = b.subterms();
//            if (aa.equals(bb))
//                return a;

            Term[] ab = new Term[len];
            boolean change = false;
            for (int i = 0; i < len; i++) {
                Term ai = aa.sub(i), bi = bb.sub(i);
                if (!ai.equals(bi)) {
                    Term y = intermpolate(ai, 0, bi, aProp, curDepth / 2f, nar);
                    if (y instanceof Bool)
                        return Null;

                    if (!ai.equals(y)) {
                        change = true;
                        ai = y;
                    }
                }
                ab[i] = ai;
            }

            return !change ? a : ao.the(chooseDT(a, b, aProp, nar), ab);
        }

    }

    /*@NotNull*/
    private static Term dtMergeDirect(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, float depth, NAR nar) {


        Term a0 = a.sub(0), a1 = a.sub(1), b0 = b.sub(0), b1 = b.sub(1);

        int dt = chooseDT(a, b, aProp, nar);
        if (a0.equals(b0) && a1.equals(b1)) {
            return a.dt(dt);
        } else {

            depth /= 2f;

            Term na = intermpolate(a0, 0, b0, aProp, depth, nar);
            if (na == Null || na == Bool.False) return na;

            Term nb = intermpolate(a1, 0, b1, aProp, depth, nar);
            if (nb == Null || nb == Bool.False) return nb;

            return a.op().the(dt, na, nb);
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
        /*if (adt >= 0 == bdt >= 0)*/ { //require same sign ?

            int range = //Math.max(Math.abs(adt), Math.abs(bdt));
                        Math.abs(adt - bdt);
            int ab = Util.lerp(aProp, bdt, adt);
            int delta = Math.max(Math.abs(ab-adt), Math.abs(ab- bdt));
            float ratio = ((float) delta) / range;
            if (ratio <= nar.intermpolationRangeLimit.floatValue()) {
                return ab;
            }
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
        Term term = intermpolate(a, dt, b, aProp, 1, nar);

        if (term.volume() > nar.termVolumeMax.intValue())
            return Null;

        return term;
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
        float d = dtDiff(a, b, 1);
        //return Util.assertUnitized(d);
        return d;
    }

    private static float dtDiff(Term a, Term b, int depth) {
        if (a.equals(b))
            return 0f;

        Op ao = a.op(), bo = b.op();
        if ((ao != bo) || (a.volume() != b.volume()) || (a.structure() != b.structure()))
            return Float.POSITIVE_INFINITY;

        Subterms aa = a.subterms(), bb = b.subterms();
//        int len = bb.subs();

        float d = 0;

        //        if (a.op() == CONJ && !aSubsEqualsBSubs) {
//
//            Conj c = new Conj();
//            String as = Conj.sequenceString(a, c).toString();
//            String bs = Conj.sequenceString(b, c).toString();
//
//            int levDist = Texts.levenshteinDistance(as, bs);
//            float seqDiff = (float) levDist / Math.min(as.length(), bs.length());
//
//
//            float rangeDiff = Math.max(1f, Math.abs(a.dtRange() - b.dtRange()));
//
//            d += (1f + rangeDiff) * (1f + seqDiff);
//
//            return Float.POSITIVE_INFINITY;
//
//        } else {
        if (!aa.equals(bb)) {

            if (aa.subs() != bb.subs())
                return Float.POSITIVE_INFINITY;

            d = dtDiff(aa, bb, true, depth);

//            if (!Float.isFinite(d) && len == 2 && ao.commutative && aa.hasAny(Op.Temporal) ) {
//                //try reversing
//                d = dtDiff(aa, bb, false, depth);
//            }


        } else {

            int adt = a.dt(), bdt = b.dt();
            if (adt != bdt) {
                if (adt == XTERNAL || bdt == XTERNAL) {
                    //zero, match
                    int other = adt == XTERNAL ? b.dt() : a.dt();
                    float range = other!=DTERNAL ? Math.max(1, Math.abs(other)) : 0.5f;
                    d += 0.25f / range; //undercut the DT option
                } else {

                    boolean ad = adt == DTERNAL;
                    boolean bd = bdt == DTERNAL;
                    int range;
                    float delta;
                    if (!ad && !bd) {
                        range = Math.max(Math.abs(adt), Math.abs(bdt));
                        delta = Math.abs(Math.abs(adt - bdt));
                    } else {
                        range = Math.max(1, Math.abs(ad ? b.dt() : a.dt()));
                        delta = 0.5f; //one is dternal the other is not, record at least some difference (half time unit)
                    }
                    assert(delta > 0 && range > 0);
                    d += delta / range;
                }
            }

        }

        return d / depth;
    }

    private static float dtDiff(Subterms aa, Subterms bb, boolean parity, int depth) {
        float d = 0;
        int len = aa.subs();
        for (int i = 0; i < len; i++) {
            Term ai = aa.sub(i);
            float dx = dtDiff(ai, bb.sub(parity ? i : (len-1)-i), depth + 1);
            if (!Float.isFinite(dx)) {
                return Float.POSITIVE_INFINITY;
            }
            d += dx;
        }
        return d;
        //return d/len; // avg
    }
}
