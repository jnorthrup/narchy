package nars.term.util;

import jcog.Util;
import jcog.WTF;
import jcog.pri.ScalarValue;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.time.Tense;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * temporal target intermpolation
 */
public enum Intermpolate {;

    private static Term intermpolate(/*@NotNull*/ Compound a,  /*@NotNull*/ Compound b, float aProp, float curDepth, NAL nar) {

        if (a.equals(b))
            return a;

        if (!a.equalsRoot(b))
            return Null;

        Op ao = a.op();//, bo = b.op();
//        if (ao != bo)         return Null; //checked in equalRoot


        int len = a.subs();

        Subterms aa = a.subterms(), bb = b.subterms();


        boolean subsEqual = aa.equals(bb);

        if (ao == CONJ && !subsEqual) {
            //return new Conjterpolate(a, b, aProp, nar).term(); //root only: conj sequence merge
            return Null;
        }

        if (aa.subs() != bb.subs())
            return Null;

        int dt = ao.temporal ? chooseDT(a, b, aProp, nar) : DTERNAL;
        if (dt == XTERNAL)
            return Null;

        if (subsEqual) {
            return a.dt(dt);
        } else {

            Term[] ab = new Term[len];
            boolean change = false;
            for (int i = 0; i < len; i++) {
                Term ai = aa.sub(i), bi = bb.sub(i);
                if (ai.equals(bi)) {

                } else {

                    if (!(ai instanceof Compound) || !(bi instanceof Compound))
                        return Null;

                    Term y = intermpolate((Compound) ai, (Compound) bi, aProp, curDepth / 2f, nar);
                    if (y == Null)
                        return Null;
                    if (!ai.equals(y)) {
                        change = true;
                        ai = y;
                    }
                }
                ab[i] = ai;
            }

            return !change ? a : ao.the(dt, ab);
        }
    }

//    /**
//     * for merging CONJ or IMPL of equal subterms, so only dt is different
//     */
//    private static Term dtMergeTemporalDirect(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp,
//                                                           float depth, NAR nar) {
//        int dt = chooseDT(a, b, aProp, nar);
//        Subterms aa = a.subterms(), bb = b.subterms();
//        if (aa.equals(bb))
//            return a.dt(dt);
//        else {
//            Term[] ab = Util.map(aa.subs(), Term[]::new, i ->
//                intermpolate(aa.sub(i), bb.sub(i), aProp, depth / 2, nar)
//            );
//
//            return a.op().the(dt, ab);
//        }
//    }

    /** if returns XTERNAL, it is not possible */
    static int chooseDT(Term a, Term b, float aProp, NAL nar) {
        return chooseDT(a.dt(), b.dt(), aProp, nar);
    }

    public static int chooseDT(int adt, int bdt, float aProp, NAL nar) {
        int dt;

        if (adt == DTERNAL) adt = 0; if (bdt == DTERNAL) bdt = 0; //HACK

        if (adt == bdt) {
            dt = adt;
        } else if (adt == XTERNAL || bdt == XTERNAL) {

            dt = adt == XTERNAL ? bdt : adt; //the other one
            //dt = choose(adt, bdt, aProp);

//        } else if (adt == DTERNAL || bdt == DTERNAL) {
//
//            dt = 0;
//            //dt = adt == DTERNAL ? bdt : adt;
//            //dt = choose(adt, bdt, aProp, nar.random());
//
        } else {
            dt = merge(adt, bdt, aProp, nar);
        }


        return Tense.dither(dt, nar);
    }

    /**
     * merge delta
     */
    static int merge(int adt, int bdt, float aProp, NAL nar) {


        int range = Math.max(Math.abs(adt), Math.abs(bdt));
        int ab = Util.lerp(aProp, bdt, adt);
        int delta = Math.max(Math.abs(ab - adt), Math.abs(ab - bdt));
        float ratio = ((float) delta) / range;
        if (ratio < nar.intermpolationRangeLimit.floatValue()) {
            return ab;
        } else {
            //invalid
            return XTERNAL;
            //discard temporal information//return DTERNAL;
        }
    }

    public static Term intermpolate(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b, float aProp, NAL nar) {
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
        if (a instanceof Compound && b instanceof Compound)
            return dtDiff(a, b, 0);
        else
            return a.equals(b) ? 0 : Float.POSITIVE_INFINITY;
    }

    private static float dtDiff(Term a, Term b, int depth) {
        if (a.equals(b))
            return 0f;
        if (a instanceof Neg && b instanceof Neg) {
            a = a.unneg();
            b = b.unneg();
        }


        if (!(a instanceof Compound) && !(b instanceof Compound) || !a.equalsRoot(b))
            return Float.POSITIVE_INFINITY;

//        Op ao = a.op(), bo = b.op();
//        if (ao != bo)
//            return Float.POSITIVE_INFINITY;

        Subterms aa = a.subterms(), bb = b.subterms();
//        if (!aa.equalsRoot(bb))
//            return Float.POSITIVE_INFINITY;
//        if ((((aa.structure() != bb.structure() || (a.volume() != b.volume())) && !(aa.hasAny(CONJ) || bb.hasAny(CONJ))))) {
//            return Float.POSITIVE_INFINITY;
//        }


        float dSubterms = 0;
        if (!aa.equals(bb)) {
            Op ao = a.op();
            if (ao == CONJ) {
                if (a.dt() == XTERNAL || b.dt() == XTERNAL)
                    return 0;
                if (aa.hasAny(CONJ) || bb.hasAny(CONJ)) { // sub-conj of any type, include &| which is not a sequence:
                    //if ((Conj.isSeq(a) || Conj.isSeq(b))) {
                    //estimate difference
                    int ar = a.eventRange(), br = b.eventRange();
                    int av = a.volume(), bv = b.volume();
                    return (1 + (av + bv) / 2f) * (1 + Math.abs(av - bv)) * (1 + Math.abs(ar - br)); //heuristic

                }
            }

            int len = aa.subs();
            if (len != bb.subs())
                return Float.POSITIVE_INFINITY;

            for (int i = 0; i < len; i++) {
                float di = dtDiff(aa.sub(i), bb.sub(i), depth + 1);
                if (!Float.isFinite(di)) {
                    return Float.POSITIVE_INFINITY;
                }
                dSubterms += di;
            }

            dSubterms /= len;
        }


        float dDT;
        int adt = a.dt(), bdt = b.dt();

        if (adt == DTERNAL) adt = 0; if (bdt == DTERNAL) bdt = 0; //HACK

        if (adt != bdt) {
            if (adt == XTERNAL || bdt == XTERNAL) {
                //dDT = 0.25f; //undercut the DTERNAL case
                dDT = ScalarValue.EPSILONcoarse;
            } else {


                float range = 1 + Math.abs(adt) + Math.abs(bdt);
                if (range <= 0)
                    throw new WTF();
                dDT = Math.abs(adt - bdt) / (range);
            }

        } else {
            dDT = 0;
        }


        return dDT + dSubterms;
    }

}
