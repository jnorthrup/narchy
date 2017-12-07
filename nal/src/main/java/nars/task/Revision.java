package nars.task;

import jcog.Util;
import jcog.list.FasterList;
import jcog.math.Interval;
import jcog.pri.Pri;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.control.Derivation;
import nars.term.Term;
import nars.term.container.Subterms;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static jcog.Util.lerp;
import static nars.Op.CONJ;
import static nars.Op.Null;
import static nars.time.Tense.*;
import static nars.truth.TruthFunctions.c2wSafe;

/**
 * Revision / Projection / Revection Utilities
 */
public class Revision {

    public static final Logger logger = LoggerFactory.getLogger(Revision.class);

    @Nullable
    public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b, float factor, float minEvi) {

        float w1 = a.evi() * factor;
//        if (w1 < Prioritized.EPSILON)
//            return null;

        float w2 = b.evi() * factor;
//        if (w2 < Prioritized.EPSILON)
//            return null;

        float w = (w1 + w2);


        return w <= minEvi ?
                null :
                new PreciseTruth(
                        (w1 * a.freq() + w2 * b.freq()) / w,
                        w,
                        false
                );
    }

//    @Nullable
//    public static Truth revise(/*@NotNull*/ Iterable<? extends Truthed> aa, float minConf) {
//        float f = 0;
//        float w = 0;
//        for (Truthed x : aa) {
//            float e = x.evi();
//            w += e;
//            f += x.freq() * e;
//        }
//        if (w <= 0)
//            return null;
//
//        float c = w2c(w);
//        return c < minConf ? null :
//                $.t(
//                        (f) / w,
//                        c
//                );
//    }

//    public static Truth merge(/*@NotNull*/ Truth newTruth, /*@NotNull*/ Truthed a, float aFrequencyBalance, /*@NotNull*/ Truthed b, float minConf, float confMax) {
//        float w1 = a.evi();
//        float w2 = b.evi();
//        float w = (w1 + w2) * evidenceFactor;

////        if (w2c(w) >= minConf) {
//            //find the right balance of frequency
//            float w1f = aFrequencyBalance * w1;
//            float w2f = (1f - aFrequencyBalance) * w2;
//            float p = w1f / (w1f + w2f);
//
//            float af = a.freq();
//            float bf = b.freq();
//            float f = lerp(p, bf, af);

//            //compute error (difference) in frequency TODO improve this
//            float fError =
//                    Math.abs(f - af) * w1f +
//                    Math.abs(f - bf) * w2f;
//
//            w -= fError;

//            float c = w2c(w);
//            if (c >= minConf) {
//                return $.t(f, Math.min(confMax, c));
//            }

////        }
//
//        return null;
//    }


    public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b) {
        return revise(a, b, 1f, 0f);
    }


    /*@NotNull*/
    static Term intermpolate(/*@NotNull*/ Term a, long dt, /*@NotNull*/ Term b, float aProp, float curDepth, /*@NotNull*/ Random rng, boolean mergeOrChoose) {

        Op ao = a.op();
        Op bo = b.op();
        if (ao != bo)
            return Null; //fail, why

//        assert (ao == bo) : a + " and " + b + " have different op";
//
//        if (ao == NEG) {
//            return intermpolate(a.unneg(), 0, b.unneg(),
//                    aProp, curDepth, rng, mergeOrChoose).neg();
//        }

        assert(dt!=XTERNAL);

        int len = a.subs();
        if (len > 0) {

            if (ao.temporal) {
                boolean dtCommutes = dt == 0 || dt == DTERNAL;

                if (a.equals(b) && dtCommutes) {
                    return a;
                }

                if (!mergeOrChoose && dtCommutes && !a.subterms().hasAny(Op.CONJ) && !b.subterms().hasAny(Op.CONJ) ) {
                    return choose(a, b, aProp, rng);
                } else {
                    switch (ao) {
                        case CONJ:
                            return dtMergeConjMerge(a, dt, b, aProp, curDepth, rng);
                        case IMPL:
                            return dtMergeImpl(a, b, aProp, curDepth, rng, mergeOrChoose);
                        default:
                            throw new RuntimeException();
                    }
                }
            } else {
                if (a.equals(b)) {
                    return a;
                }

                Term[] ab = new Term[len];
                boolean change = false;
                Subterms aa = a.subterms();
                Subterms bb = b.subterms();
                for (int i = 0; i < len; i++) {
                    Term ai = aa.sub(i);
                    Term bi = bb.sub(i);
                    if (!ai.equals(bi)) {
                        Term y = intermpolate(ai, 0, bi, aProp, curDepth / 2f, rng, mergeOrChoose);
                        if (!ai.equals(y)) {
                            change = true;
                            ai = y;
                        }
                    }
                    ab[i] = ai;
                }

                return !change ? a : ao.the(
                        choose(a, b, aProp, rng).dt()  /** this effectively chooses between && and &| in a size >2 case */,
                        ab
                );
            }

        }

        return choose(a, b, aProp, rng);

    }

    private static Term dtMergeConjMerge(Term a, long dt, Term b, float aProp, float v, Random rng) {
        FastList<LongObjectPair<Term>> ab = Op.conjMerge(a, 0, b, dt).eventList();

        //it may not be valid to choose subsets of the events, in a case like where >1 occurrences of $ must remain parent

        FastList<LongObjectPair<Term>> x = new FasterList(ab);
        int max = 1 + x.size() / 2; //HALF
        int all = x.size();
        int excess = all - max;
        if (excess > 0) {

            //decide on some items to remove
            //must keep the endpoints unless a shift and adjustment are reported
            //to the callee which decides this for the revised task

            //for now just remove some inner tasks
            if (all - excess < 2)
                return null; //retain the endpoints
            else if (all - excess == 2)
                x = new FasterList(2).addingAll(x.get(0), x.get(all - 1)); //retain only the endpoints
            else {
                for (int i = 0; i < excess; i++) {
                    x.remove(rng.nextInt(x.size() - 2) + 1);
                }
            }
        }

        return Op.conj(x);
    }


    /*@NotNull*/
    private static Term dtMergeImpl(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, float depth, /*@NotNull*/ Random rng, boolean mergeOrChoose) {

        int adt = a.dt();

        int bdt = b.dt();


//        if (adt!=bdt)
//            System.err.print(adt + " " + bdt);

        depth /= 2f;

        //        if (forwardSubterms(a, adt)) {
        Term a0 = a.sub(0);
        Term a1 = a.sub(1);
//        } else {
//            a0 = a.sub(1);
//            a1 = a.sub(0);
//            adt = -bdt;
//        }
        //        if (forwardSubterms(b, bdt)) {
        Term b0 = b.sub(0);
        Term b1 = b.sub(1);
//        } else {
//            b0 = b.sub(1);
//            b1 = b.sub(0);
//            bdt = -bdt;
//        }

        int dt;
        if (adt == DTERNAL)
            dt = bdt;
        else if (bdt == DTERNAL)
            dt = adt;
        else {
            dt = mergeOrChoose ?
                    lerp(aProp, bdt, adt) :
                    ((choose(a, b, aProp, rng) == a) ? adt : bdt);
        }


        if (a0.equals(b0) && a1.equals(b1)) {
            return a.dt(dt);
        } else {
            Term na = intermpolate(a0, 0, b0, aProp, depth, rng, mergeOrChoose);
            Term nb = intermpolate(a1, 0, b1, aProp, depth, rng, mergeOrChoose);
            return a.op().the(dt,
                    na,
                    nb);
        }

    }

//    private static boolean forwardSubterms(/*@NotNull*/ Term a, int adt) {
//        return a.op()!=CONJ || (adt >= 0) || (adt == DTERNAL);
//    }

    public static Term choose(Term a, Term b, float aBalance, /*@NotNull*/ Random rng) {
        return (rng.nextFloat() < aBalance) ? a : b;
    }

    /*@NotNull*/
    public static Term[] choose(/*@NotNull*/ Term[] a, Term[] b, float aBalance, /*@NotNull*/ Random rng) {
        int l = a.length;
        Term[] x = new Term[l];
        for (int i = 0; i < l; i++) {
            x[i] = choose(a[i], b[i], aBalance, rng);
        }
        return x;
    }


    /*@NotNull*/
    public static Task chooseByConf(/*@NotNull*/ Task t, @Nullable Task b, /*@NotNull*/ Derivation p) {

        if ((b == null) || !b.isBeliefOrGoal())
            return t;

        //int dur = p.nar.dur();
        float tw = t.conf();
        float bw = b.conf();

        //randomize choice by confidence
        return p.random.nextFloat() < tw / (tw + bw) ? t : b;

    }

    public static Term intermpolate(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, 0, b, aProp, nar);
    }

    /**
     * a is left aligned, dt is any temporal shift between where the terms exist in the callee's context
     */
    public static Term intermpolate(/*@NotNull*/ Term a, long dt, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, dt, b, aProp, 1, nar.random(), nar.dtMergeOrChoose.get());
    }


    @Nullable
    public static Task merge(/*@NotNull*/ Task a, /*@NotNull*/ Task b, long now, NAR nar) {
        if (a.op() == CONJ) {
            //avoid intermpolation of 2 conjunctions with opposite polarities
            if (!a.term().equals(b.term()) && (a.isPositive() ^ b.isPositive()) && (a.term().dtRange() != 0 || b.term().dtRange() != 0))
                return null;
        }

        long as, bs;
        if ((as = a.start()) > (bs = b.start())) {
            //swap so that 'a' is left aligned
            Task x = a;
            a = b;
            b = x;
        }
        assert (bs != ETERNAL);
        assert (as != ETERNAL);


        //            float ae = a.evi();
//            float aa = ae * (1 + ai.length());
//            float be = b.evi();
        //float bb = be * (1 + bi.length());
        //float p = aa / (aa + bb);

        float factor = 1f;

        //relate high frequency difference with low confidence
//        float freqDiscount =
//                0.5f + 0.5f * (1f - Math.abs(a.freq() - b.freq()));
//        factor *= freqDiscount;
//        if (factor < Prioritized.EPSILON) return null;


//            float temporalOverlap = timeOverlap==null || timeOverlap.length()==0 ? 0 : timeOverlap.length()/((float)Math.min(ai.length(), bi.length()));
//            float confMax = Util.lerp(temporalOverlap, Math.max(w2c(ae),w2c(be)),  1f);
//
//
//            float timeDiscount = 1f;
//            if (timeOverlap == null) {
//                long separation = Math.max(a.timeDistance(b.start()), a.timeDistance(b.end()));
//                if (separation > 0) {
//                    long totalLength = ai.length() + bi.length();
//                    timeDiscount =
//                            (totalLength) /
//                                    (separation + totalLength)
//                    ;
//                }
//            }


        //width will be the average width
//        long width = (ai.length() + bi.length()) / 2; //TODO weight
//        long mid = (ai.mid() + bi.mid()) / 2;  //TODO weight

//            Truth expected = table.truth(mid, now, dur);
//            if (expected == null)
//                return null;


        TaskTimeJoint joint = new TaskTimeJoint(as, a.end(), bs, b.end(), nar);
        factor *= joint.factor;
        if (factor < Prioritized.EPSILON) return null;

        int dur = nar.dur();
//        float intermvalDistance = dtDiff(a.term(), b.term()) /
//                ((1 + Math.max(a.term().dtRange(), b.term().dtRange())) * dur);
//        factor *= (1f / (1f + intermvalDistance));
//        if (factor < Prioritized.EPSILON) return null;

        float confMin =
                0;
                //nar.confMin.floatValue();
        Truth rawTruth = revise(a, b, factor, c2wSafe(confMin));
        if (rawTruth == null)
            return null;

        float maxEviAB = Math.max(a.evi(), b.evi());
        float evi = rawTruth.evi();
        if (maxEviAB < evi + Pri.EPSILON) {
            //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
            //TODO weight the contributed overlap amount by the relative confidence provided by each task
            float overlapDiscount = Stamp.overlapFraction(a.stamp(), b.stamp());
            //        factor *= overlapDiscount;
            //        if (factor < Prioritized.EPSILON) return null;

            float eviDiscount = (evi - maxEviAB) * overlapDiscount;
            float newEvi = evi - eviDiscount;
            if (!Util.equals(evi, newEvi, Pri.EPSILON)) {
                rawTruth = rawTruth.withEvi(newEvi);
            }

        }


        Truth finalTruth = rawTruth.dither(nar);
        if (finalTruth == null)
            return null;


        //TODO maybe delay dithering until after the negation has been determined below

//            float conf = w2c(expected.evi() * factor);
//            if (conf >= Param.TRUTH_EPSILON)
//                newTruth = new PreciseTruth(expected.freq(), conf);
//            else
//                newTruth = null;


        assert (a.punc() == b.punc());

        long start = joint.unionStart;
        long end = joint.unionEnd;

        float aw = a.isQuestOrQuestion() ? 0 : a.conf(start, end, dur); //question
        float bw = b.conf(start, end, dur);
        float aProp = aw / (aw + bw);

        Term cc = null;

        Term at = a.term();
        Term bt = b.term();

        //Term atConceptual = at.conceptual();
        //if (Param.DEBUG) assert(bt.conceptual().equals(atConceptual)): at + " and " + bt + " may not belong in the same concept";

        for (int i = 0; i < Param.MAX_TERMPOLATE_RETRIES; i++) {
            Term t;
            if (at.equals(bt)) {
                t = at;
                i = Param.MAX_TERMPOLATE_RETRIES; //no need to retry
            } else {
                try {
                    long dt = bs - as;
                    t = intermpolate(at, dt, bt, aProp, nar);
                    if (t == null || !t.op().conceptualizable)
                        continue;

                } catch (Throwable ett) {
                    //TODO this probably means conjunction need to be merged by events
                    if (Param.DEBUG_EXTRA)
                        logger.warn("{} + {} ==> {}", at, bt, ett.getMessage());
                    continue;
                    //return null;
                }
            }


            ObjectBooleanPair<Term> ccp = Task.tryContent(t, a.punc(), true);
            if (ccp != null) {

                cc = ccp.getOne();
                assert (cc.isNormalized());

                if (ccp.getTwo())
                    finalTruth = finalTruth.neg();
                break;
            }
        }

        if (cc == null)
            return null;


        //        if (cc.op() == CONJ) {
//            long mid = Util.lerp(aProp, b.mid(), a.mid());
//            long range = cc.op() == CONJ ?
//                    cc.dtRange() :
//                    (Util.lerp(aProp, b.range(), a.range()));
//            start = mid - range / 2;
//            end = start + range;
//        } else {
//            if (u > s) {
//                start = end = Util.lerp(aProp, b.mid(), a.mid());
//            } else {

//            }
//        }


        if (equivalent(a, finalTruth, start, end, cc, nar))
            return a;
        if (equivalent(b, finalTruth, start, end, cc, nar))
            return b;

        NALTask t = new NALTask(cc, a.punc(),
                finalTruth,
                now, start, end,
                Stamp.zip(a.stamp(), b.stamp(), aProp) //get a stamp collecting all evidence from the table, since it all contributes to the result
        );

        t.priSet(Util.lerp(aProp, b.priElseZero(), a.priElseZero()));

        //t.setPri(a.priElseZero() + b.priElseZero());

        t.cause = Cause.zip(nar.causeCapacity.intValue(), a, b);

        if (Param.DEBUG)
            t.log("Revection Merge");
        return t;
    }

    public static boolean equivalent(Task input, Truth output, long start, long end, Term cc, NAR nar) {
        Truth bt = input.truth();
        if (input.conf() >= output.conf() && Util.equals(input.freq(), output.freq(), nar.freqResolution.asFloat())) {
            return cc.equals(input.term()) && start == input.start() && end == input.end();
        }
        return false;
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

    static float dtDiff(Term a, Term b, int depth) {
        if (a.equals(b)) return 0f;

        if (!a.isTemporal() || !b.isTemporal())
            return 0f;

        Op ao = a.op();
        Op bo = b.op();
        if (ao != bo)
            return Float.POSITIVE_INFINITY; //why?


        Subterms aa = a.subterms();
        int len = aa.subs();
        Subterms bb = b.subterms();

        float d = 0;

        int blen = bb.subs();
        if (a.op() == CONJ && (len > 2 || blen > 2)) {
            if (len > 2 && blen == len) {

                //parallel, eternal, or xternal commutive
                for (int i = 0; i < len; i++)
                    d += dtDiff(aa.sub(i), bb.sub(i), depth + 1);

            } else {
                //hard way: break down into events because there is a combination of parallel and seq

                //a) inter-term distance if both are non-XTERNAL
                if (a.dt() != XTERNAL && b.dt() != XTERNAL) {

                    ObjectLongHashMap<Term> ab = new ObjectLongHashMap(len);
                    a.events((tw) -> ab.put(tw.getTwo().root(), tw.getOne()));
                    b.events((tw) -> ab.addToValue(tw.getTwo().root(), -tw.getOne()));

                    final float[] xd = {0};
                    ab.forEachValue(x -> xd[0] += Math.abs(x));
                    d += xd[0];
                }

                Map<Term, Term[]> ab = new HashMap(len);

                //TODO this collapses duplicates. ignore for now
                a.events((tw) -> {
                    Term tww = tw.getTwo();
                    if (tww.isTemporal())
                        ab.put(tww.root(), new Term[]{tww, null});
                });

                b.events((tw) -> {
                    Term tww = tw.getTwo();
                    if (tww.isTemporal()) {
                        Term[] x = ab.computeIfAbsent(tww.root(), (nn) -> new Term[2]);
                        x[1] = tww;
                    }
                });

                for (Term[] xy : ab.values()) {
                    if (xy[0] != null && xy[1] != null) {
                        if (!xy[0].equals(xy[1])) {
                            if (!((!xy[0].equals(a) && !xy[1].equals(b)) || (!xy[1].equals(a) && !xy[0].equals(b)))) //for prevneting an infinite recursion here that i dont understand yet
                                d += dtDiff(xy[0], xy[1], depth + 1);
                        }
                    } //else ?
                }
            }

        } else {
            int adt = a.dt();
            int bdt = b.dt();
            if (adt == XTERNAL) adt = bdt;
            if (bdt == XTERNAL) bdt = adt;
            if (adt != bdt && adt != DTERNAL && bdt != DTERNAL) {

//            if (adt == DTERNAL) {
//                adt = 0;
//                dLocal += 0.5f;
//            }
//            if (bdt == DTERNAL) {
//                bdt = 0;
//                dLocal += 0.5f;
//            }

                d += Math.abs(adt - bdt);
            }

            for (int i = 0; i < len; i++)
                d += dtDiff(aa.sub(i), bb.sub(i), depth + 1);
        }

        return d / depth;
    }

    public static final class TaskTimeJoint {

        public final float factor;
        public final long unionStart;
        public final long unionEnd;

        public TaskTimeJoint(long as, long ae, long bs, long be, NAR nar) {

            Interval ai = new Interval(as, ae);
            Interval bi = new Interval(bs, be);

            Interval uu = ai.union(bi);
            this.unionStart = uu.a;
            this.unionEnd = uu.b;

            long u = uu.length();
            Interval ii = ai.intersection(bi);
            if (ii == null) {
                //no intersection
                this.factor = 0;
            } else {
                //partial intersection, weaken the union
                this.factor = (1+ii.length()) / (1+((float)u));
            }

//            int separation = ai.
//            int al = (int) ai.length();
//            int bl = (int) bi.length();
//            int s = al + bl;
//
//
//            float factor = 1f;
//            if (u > s) {
//
//                /** account for how much the merge stretches the truth beyond the range of the inputs */
//                long separation = u - s;
//                if (separation > 0) {
//                    //int hd = nar.dur();
//                    int minterval = Math.min(al, bl);// + hd; //+hd to pad the attention surrounding point0like events
//                    if (separation < minterval) {
//                        factor = 1f - separation / ((float) minterval);
//                    } else {
//                        factor = 0; //too separate
//                    }
//
//
////                    factor = 1f - (separation / (separation + (float) u));
//
//                }
//            }
//
//            this.factor = factor;

        }
    }
}

//    /** get the task which occurrs nearest to the target time */
//    /*@NotNull*/ public static Task closestTo(/*@NotNull*/ Task[] t, long when) {
//        Task best = t[0];
//        long bestDiff = Math.abs(when - best.occurrence());
//        for (int i = 1; i < t.length; i++) {
//            Task x = t[i];
//            long o = x.occurrence();
//            long diff = Math.abs(when - o);
//            if (diff < bestDiff) {
//                best = x;
//                bestDiff = diff;
//            }
//        }
//        return best;
//    }


//    public static float temporalIntersection(long now, long at, long bt, float window) {
//        return window == 0 ? 1f : BeliefTable.relevance(Math.abs(now-at) + Math.abs(now-bt), window);
//    }

//    @Nullable
//    public static Truth revisionTemporalOLD(/*@NotNull*/ Task ta, /*@NotNull*/ Task tb, long target, float match, float confThreshold) {
//        Truth a = ta.truth();
//        Truth b = tb.truth();
//
//        long at = ta.occurrence();
//        long bt = tb.occurrence();
//
//        //temporal proximity balancing metric (similar to projection)
//        long adt = 1 + Math.abs(at-target);
//        long bdt = 1 + Math.abs(bt-target);
//        float closeness = (adt!=bdt) ? (bdt/(float)(adt+bdt)) : 0.5f;
//
//        //float w1 = c2w(a.conf()) * closeness;
//        //float w2 = c2w(b.conf()) * (1-closeness);
//        float w1 = a.conf() * closeness;
//        float w2 = b.conf() * (1-closeness);
//
//        final float w = (w1 + w2);
////        float newConf = w2c(w) * match *
////                temporalIntersection(target, at, bt,
////                    Math.abs(a.freq()-b.freq()) //the closer the freq are the less that difference in occurrence will attenuate the confidence
////                );
////                //* TruthFunctions.temporalProjectionOld(at, bt, now)
//
//        float newConf = UtilityFunctions.or(w1,w2) * match *
//                temporalIntersection(target, at, bt,
//                        Math.abs(a.freq()-b.freq()) //the closer the freq are the less that difference in occurrence will attenuate the confidence
//                );
//
//        if (newConf < confThreshold)
//            return null;
//
//
//        float f1 = a.freq();
//        float f2 = b.freq();
//        return new DefaultTruth(
//                (w1 * f1 + w2 * f2) / w,
//                newConf
//        );
//    }


//    @Nullable
//    public static Budget budgetRevision(/*@NotNull*/ Truth revised, /*@NotNull*/ Task newBelief, /*@NotNull*/ Task oldBelief, /*@NotNull*/ NAR nar) {
//
//        final Budget nBudget = newBelief.budget();
//
//
////        Truth bTruth = oldBelief.truth();
////        float difT = revised.getExpDifAbs(nTruth);
////        nBudget.andPriority(1.0f - difT);
////        nBudget.andDurability(1.0f - difT);
//
////        float cc = revised.confWeight();
////        float proportion = cc
////                / (cc + Math.min(newBelief.confWeight(), oldBelief.confWeight()));
//
////		float dif = concTruth.conf()
////				- Math.max(nTruth.conf(), bTruth.conf());
////		if (dif < 0) {
////			String msg = ("Revision fault: previous belief " + oldBelief
////					+ " more confident than revised: " + conclusion);
//////			if (Global.DEBUG) {
////				throw new RuntimeException(msg);
//////			} else {
//////				System.err.println(msg);
//////			}
//////			dif = 0;
////		}
//
//        float priority =
//                proportion * nBudget.pri();
//                //or(dif, nBudget.pri());
//        int durability =
//                //aveAri(dif, nBudget.dur());
//                proportion * nBudget.dur();
//        float quality = BudgetFunctions.truthToQuality(revised);
//
//		/*
//         * if (priority < 0) { memory.nar.output(ERR.class, new
//		 * RuntimeException(
//		 * "BudgetValue.revise resulted in negative priority; set to 0"));
//		 * priority = 0; } if (durability < 0) { memory.nar.output(ERR.class,
//		 * new RuntimeException(
//		 * "BudgetValue.revise resulted in negative durability; set to 0; aveAri(dif="
//		 * + dif + ", task.getDurability=" + task.getDurability() +") = " +
//		 * durability)); durability = 0; } if (quality < 0) {
//		 * memory.nar.output(ERR.class, new RuntimeException(
//		 * "BudgetValue.revise resulted in negative quality; set to 0"));
//		 * quality = 0; }
//		 */
//
//        if (BudgetFunctions.valid(durability, nar)) {
//            return new UnitBudget(priority, durability, quality);
//        }
//        return null;
//    }

//    /**
//     * assumes the compounds are the same except for possible numeric metadata differences
//     */
//    public static /*@NotNull*/ Compound intermpolate(/*@NotNull*/ Termed<Compound> a, /*@NotNull*/ Termed<Compound> b, float aConf, float bConf, /*@NotNull*/ TermIndex index) {
//        /*@NotNull*/ Compound aterm = a.term();
//        if (a.equals(b))
//            return aterm;
//
//        float aWeight = c2w(aConf);
//        float bWeight = c2w(bConf);
//        float aProp = aWeight / (aWeight + bWeight);
//
//        /*@NotNull*/ Compound bterm = b.term();
//
//        int dt = DTERNAL;
//        int at = aterm.dt();
//        if (at != DTERNAL) {
//            int bt = bterm.dt();
//            if (bt != DTERNAL) {
//                dt = lerp(at, bt, aProp);
//            }
//        }
//
//
//        Term r = index.the(a.op(), dt, aterm.terms());
//        return !(r instanceof Compound) ? choose(aterm, bterm, aProp) : (Compound) r;
//    }

//    @Nullable
//    public static ProjectedTruth project(/*@NotNull*/ Truth t, long target, long now, long occ, boolean eternalizeIfWeaklyTemporal) {
//
//        if (occ == target)
//            return new ProjectedTruth(t, target);
//
//        float conf = t.conf();
//
//        float nextConf;
//
//
//        float projConf = nextConf = conf * projection(target, occ, now);
//
//        if (eternalizeIfWeaklyTemporal) {
//            float eternConf = eternalize(conf);
//
//            if (projConf < eternConf) {
//                nextConf = eternConf;
//                target = ETERNAL;
//            }
//        }
//
//        if (nextConf < Param.TRUTH_EPSILON)
//            return null;
//
//        float maxConf = 1f - Param.TRUTH_EPSILON;
//        if (nextConf > maxConf) //clip at max conf
//            nextConf = maxConf;
//
//        return new ProjectedTruth(t.freq(), nextConf, target);
//    }
//    private static void failIntermpolation(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b) {
//        throw new RuntimeException("interpolation failure: different or invalid internal structure and can not be compared:\n\t" + a + "\n\t" + b);
//    }
//
//    private static int dtCompare(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b, float aProp, float depth, @Nullable Random rng) {
//        int newDT;
//        int adt = a.dt();
//        if (adt != b.dt()) {
//
//            int bdt = b.dt();
//            if (adt != DTERNAL && bdt != DTERNAL) {
//
//                accumulatedDifference.add(Math.abs(adt - bdt) * depth);
//
//                //newDT = Math.round(Util.lerp(adt, bdt, aProp));
//                if (rng != null)
//                    newDT = choose(adt, bdt, aProp, rng);
//                else
//                    newDT = aProp > 0.5f ? adt : bdt;
//
//
//            } else if (bdt != DTERNAL) {
//                newDT = bdt;
//                //accumulatedDifference.add(bdt * depth);
//
//            } else if (adt != DTERNAL) {
//                newDT = adt;
//                //accumulatedDifference.add(adt * depth);
//            } else {
//                throw new RuntimeException();
//            }
//        } else {
//            newDT = adt;
//        }
//        return newDT;
//    }

//    static int choose(int x, int y, float xProp, /*@NotNull*/ Random random) {
//        return random.nextFloat() < xProp ? x : y;
//    }

//    private static Compound failStrongest(Compound a, Compound b, float aProp) {
//        //logger.warn("interpolation failure: {} and {}", a, b);
//        return strongest(a, b, aProp);
//    }


//    /**
//     * heuristic which evaluates the semantic similarity of two terms
//     * returning 1f if there is a complete match, 0f if there is
//     * a totally separate meaning for each, and in-between if
//     * some intermediate aspect is different (ex: temporal relation dt)
//     * <p>
//     * evaluates the terms recursively to compare internal 'dt'
//     * produces a tuple (merged, difference amount), the difference amount
//     * can be used to attenuate truth values, etc.
//     * <p>
//     * TODO threshold to stop early
//     */
//    public static FloatObjectPair<Compound> dtMerge(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b, float aProp, Random rng) {
//        if (a.equals(b)) {
//            return PrimitiveTuples.pair(0f, a);
//        }
//
//        MutableFloat accumulatedDifference = new MutableFloat(0);
//        Term cc = dtMerge(a, b, aProp, accumulatedDifference, 1f, rng);
//
//
//        //how far away from 0.5 the weight point is, reduces the difference value because less will have changed
//        float weightDivergence = 1f - (Math.abs(aProp - 0.5f) * 2f);
//
//        return PrimitiveTuples.pair(accumulatedDifference.floatValue() * weightDivergence, cc);
//
//
////            int at = a.dt();
////            int bt = b.dt();
////            if ((at != bt) && (at!=DTERNAL) && (bt!=DTERNAL)) {
//////                if ((at == DTERNAL) || (bt == DTERNAL)) {
//////                    //either is atemporal but not both
//////                    return 0.5f;
//////                }
////
//////                boolean symmetric = aop.isCommutative();
//////
//////                if (symmetric) {
//////                    int ata = Math.abs(at);
//////                    int bta = Math.abs(bt);
//////                    return 1f - (ata / ((float) (ata + bta)));
//////                } else {
//////                    boolean ap = at >= 0;
//////                    boolean bp = bt >= 0;
//////                    if (ap ^ bp) {
//////                        return 0; //opposite direction
//////                    } else {
//////                        //same direction
////                        return 1f - (Math.abs(at - bt) / (1f + Math.abs(at + bt)));
//////                    }
//////                }
////            }
////        }
////        return 1f;
//    }


//    /**
//     * computes a value that indicates the amount of difference (>=0) in the internal 'dt' subterm structure of 2 temporal compounds
//     */
//    /*@NotNull*/
//    public static float dtDifference(@Nullable Termed<Compound> a, /*@NotNull*/ Termed<Compound> b) {
//        if (a == null) return 0f;
//
//        MutableFloat f = new MutableFloat(0);
//        dtDifference(a.term(), b.term(), f, 1f);
//        return f.floatValue();
//    }

//    /*@NotNull*/
//    private static void dtDifference(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float depth) {
//        if (a.op() == b.op()) {
//            if (a.size() == 2 && b.size() == 2) {
//
//                if (a.equals(b))
//                    return; //no difference
//
//                Compound aa = ((Compound) a);
//                Compound bb = ((Compound) b);
//
//                dtCompare(aa, bb, 0.5f, accumulatedDifference, depth, null);
//            }
////            if (a.size() == b.size())
////
////                Term a0 = aa.term(0);
////                if (a.size() == 2 && b0) {
////                    Term b0 = bb.term(0);
////
////                    if (a0.op() == b0.op()) {
////                        dtCompare((Compound) a0, (Compound) b0, 0.5f, accumulatedDifference, depth / 2f, null);
////                    }
////
////                    Term a1 = aa.term(1);
////                    Term b1 = bb.term(1);
////
////                    if (a1.op() == b1.op()) {
////                        dtCompare((Compound) a1, (Compound) b1, 0.5f, accumulatedDifference, depth / 2f, null);
////                    }
////
////                }
////
////            }
//        } /* else: can not be compared anyway */
//    }
//
