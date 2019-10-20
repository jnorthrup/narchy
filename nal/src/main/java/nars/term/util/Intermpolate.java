package nars.term.util;

import jcog.Util;
import jcog.pri.ScalarValue;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.util.conj.ConjList;
import nars.time.Tense;

import static jcog.Util.assertUnitized;
import static nars.Op.CONJ;
import static nars.term.atom.IdempotentBool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * temporal target intermpolation
 */
public enum Intermpolate {;

    private static Term intermpolate(/*@NotNull*/ Compound a,  /*@NotNull*/ Compound b, float aProp, float curDepth, NAL nar) {

        if (a.equals(b))
            return a;

        if (a instanceof Neg) {
            if (!(b instanceof Neg)) return Null;
            Term au = a.unneg();
            if (!(au instanceof Compound)) return Null;
            Term bu = b.unneg();
            if (!(bu instanceof Compound)) return Null;
            return intermpolate((Compound)au, (Compound)bu, aProp, curDepth, nar).neg();
        }

        if (!a.equalsRoot(b))
            return Null;

        Op ao = a.op();//, bo = b.op();
//        if (ao != bo)         return Null; //checked in equalRoot


        int len = a.subs();

        Subterms aa = a.subterms(), bb = b.subterms();


        boolean subsEqual = aa.equals(bb);

        if (ao == CONJ && !subsEqual) {
            return intermpolateSeq(a, b, aProp, nar);
        }


        if (!subsEqual && aa.subs() != bb.subs())
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
                if (!ai.equals(bi)) {
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

            return !change ? a : Util.maybeEqual(ao.the(dt, ab), a, b);
        }
    }

    private static float dtDiffSeq(Compound a, Compound b, int depth) {
//        int v = a.volume();
//        if (v !=b.volume()) {
//            return Float.POSITIVE_INFINITY; //is there a solution here?
//            //return (1+Math.abs(a.eventRange()-b.eventRange())) * (1 + Math.abs(a.volume()-b.volume())) ; //HACK finite, but nonsensical
//        }

        //return (1 + dtDiff(a.eventRange(), b.eventRange()) ) * Math.max(a.subs(), b.subs()); //HACK estimate

        //exhaustive test:
        ConjList aa = ConjList.events(a);
        ConjList bb = ConjList.events(b);
        int n = aa.size();
        if (n !=bb.size())
            return 1.0F;//Float.POSITIVE_INFINITY;

        long aar = aa.eventRange(), bbr = bb.eventRange(); //save these before changing sort

        aa.sortThisByValue(); bb.sortThisByValue();

//        for (int i = 0; i < n; i++) {
//            Term aai = aa.get(i);
//            if (aai instanceof Neg) {
//                Term bbi = bb.get(i);
//                if (bbi instanceof Neg) {
//                    aa.set(i, aai.unneg());
//                    bb.set(i, bbi.unneg());
//                }
//            }
//        }
//        if (!Arrays.equals(aa.array(), 0, n, bb.array(), 0, n))
//            return 1;//Float.POSITIVE_INFINITY;

        //same events, sum timing differences
        long dtDiff = 0L;
        long[] aaw = aa.when, bbw = bb.when;
        float subDiff = (float) 0;
        for (int i = 0; i < n; i++) {
            dtDiff += Math.abs(aaw[i] - bbw[i]); //TODO fail early if dtErr becomes excessive that dtDiff=1
            subDiff += dtDiff(aa.get(i), bb.get(i), depth+1);
        }

        float dtErr;
        long r = Math.max(aar, bbr);
        if (r == 0L) {
            if (dtDiff > 0L) return 1.0F; //can this happen?
            dtErr = (float) 0;
        } else {
            dtErr = (((float) dtDiff) / (float) r) / (float) n;
        }
        return Util.or(dtErr, subDiff/ (float) n);
    }

    private static Term intermpolateSeq(Compound a, Compound b, float aProp, NAL nar) {
        if (aProp < 0.5f-Float.MIN_NORMAL) {
            //swap so that a is the larger proportion
            Compound c = a;
            a = b;
            b = c;
            aProp = 1.0F - aProp;
        }

        if (a.volume()!=b.volume())
            return Null; //is there a solution here?

        ConjList ae = ConjList.events(a);
        ConjList be = ConjList.events(b);
        int s = ae.size();
        if (be.size()!=s)
            return Null; //?

        //canonical order
        ae.sortThisByValue();
        be.sortThisByValue();

//        //TODO could be conjunction in believe() etc which differs
//        if (!Arrays.equals(ae.array(), 0, s, be.array(), 0, s))
//            return Null; //wtf?

        int dtDither = nar.dtDither();
        boolean changed = false;
        long[] aw = ae.when;
        long[] bw = be.when;
        for (int i = 0; i < s; i++) {
            long ai = aw[i], bi = bw[i];
            if (ai!=bi) {
                long abi = Tense.dither(Util.lerpLong(aProp, ai, bi), dtDither);
                if (abi != ai) {
                    changed = true;
                    aw[i] = abi;
                }
            }
        }
        if (!changed)
            return a;

//        ae.sortThis();
        return Util.maybeEqual(ae.term(), a, b);
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

    static int chooseDT(int adt, int bdt, float aProp, NAL nar) {

        if (adt == DTERNAL) adt = 0; if (bdt == DTERNAL) bdt = 0; //HACK

        int dt;
        if (adt == bdt)
            dt = adt;
        else if (adt == XTERNAL || bdt == XTERNAL)
            dt = adt == XTERNAL ? bdt : adt; //the other one
        else
            dt = merge(adt, bdt, aProp);

        return dt!=0 ? Tense.dither(dt, nar) : 0;
    }

    /**
     * merge delta
     */
    static int merge(int adt, int bdt, float aProp) {


        int range = Math.max(Math.abs(adt), Math.abs(bdt));
        int ab = Util.lerpInt(aProp, bdt, adt);
        int delta = Math.max(Math.abs(ab - adt), Math.abs(ab - bdt));
        float ratio = ((float) delta) / (float) range;
		//nar.intermpolationRangeLimit.floatValue()) {
		//            //invalid
		//            //discard temporal information//return DTERNAL;
		return ratio < 1.0F ? ab : XTERNAL;
    }

    public static Term intermpolate(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b, float aProp, NAL nar) {
        return intermpolate(a, b, aProp, (float) 0, nar);
    }


    /**
     * heuristic representing the difference between the dt components
     * of two temporal terms.
     * 0 means they are identical or otherwise match.
     * 1 means entirely different
     * between 0 and 1 means some difference
     * <p>
     * XTERNAL matches anything
     */
    public static float dtDiff(Term a, Term b) {
        float d = dtDiff(a, b, 0);
        if (NAL.DEBUG)
            assertUnitized(d);
        d = Util.unitize(d);
        return d;
    }

    private static float dtDiff(Term a, Term b, int depth) {
        if (a.equals(b))
            return 0f;

        if (a instanceof Neg && b instanceof Neg) {
            a = a.unneg();
            b = b.unneg();
        }

        if (!(a instanceof Compound) || !(b instanceof Compound))
            return 1.0F;

        if (!a.equalsRoot(b))
            return 1.0F;

        if (a.opID() == (int) CONJ.id)
            return dtDiffSeq((Compound)a, (Compound)b, depth);

        Subterms as = a.subterms(), bs = b.subterms();
        float dDT = dtDiff((long) a.dt(), (long) b.dt());
        if (as.equals(bs)) {
            return dDT;
        } else {
            float dSubterms = dtDiff(as, bs, depth);
            if (!Float.isFinite(dSubterms))
                return 1.0F;

            //return dDT + dSubterms;
            return Util.or(dDT, dSubterms);
        }



    }

    public static float dtDiff(long adt, long bdt) {
        if (adt == (long) DTERNAL) adt = 0L; if (bdt == (long) DTERNAL) bdt = 0L; //HACK

        if (adt == bdt)
            return (float) 0;

        if (adt == (long) XTERNAL || bdt == (long) XTERNAL) {
            //dDT = 0.25f; //undercut the DTERNAL case
            return ScalarValue.EPSILONcoarse;
        } else {
            float range = (float) Math.max(1L, Math.abs(adt) + Math.abs(bdt));
//            return Math.abs(adt - bdt) / (range);
//            float mean = (adt+bdt)/2f;
            //float range = Math.max(Math.abs(adt), Math.abs(bdt));
//            float range = Math.min(Math.abs(adt), Math.abs(bdt));
            //return Math.max( Math.abs(adt - mean), Math.abs(bdt - mean)) / (range);
            return (float) Math.abs(adt - bdt) / (range);
        }
    }

    private static float dtDiff(Subterms aa, Subterms bb, int depth) {

        int len = aa.subs();
        if (len != bb.subs())
            return 1.0F; //Float.POSITIVE_INFINITY;

        float dSubterms = (float) 0;
        for (int i = 0; i < len; i++) {
            float di = dtDiff(aa.sub(i), bb.sub(i), depth + 1);
//            if (!Float.isFinite(di)) {
//                return 1; //Float.POSITIVE_INFINITY;
//            }
            dSubterms += di;
        }

        return dSubterms/ (float) len;
    }


}
