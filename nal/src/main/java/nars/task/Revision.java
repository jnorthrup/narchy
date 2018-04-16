package nars.task;

import jcog.Texts;
import jcog.Util;
import jcog.list.FasterList;
import jcog.math.Longerval;
import jcog.pri.Priority;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.subterm.Subterms;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.util.Conj;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.polation.TruthPolation;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.TreeSet;

import static jcog.Util.lerp;
import static nars.Op.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Revision / Projection / Revection Utilities
 */
public class Revision {

    public static final Logger logger = LoggerFactory.getLogger(Revision.class);

    @Nullable
    public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b, float factor, float minEvi) {

        float ae = a.evi();
        float be = b.evi();
        float w = ae + be;
        float e = w * factor;

        return e <= minEvi ?
                null :
                new PreciseTruth(
                        (ae * a.freq() + be * b.freq()) / w,
                        e,
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
    static Term intermpolate(/*@NotNull*/ Term a, long bOffset, /*@NotNull*/ Term b, float aProp, float curDepth, NAR nar) {

        if (a.equals(b) && bOffset == 0) {
            return a;
        }

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

        int len = a.subs();
        if (len > 0) {

            if (ao.temporal) {


                boolean mergeOrChoose = nar.dtMergeOrChoose.get();
                if (ao == CONJ) {

                    return dtMergeConjEvents(a, bOffset, b, aProp, curDepth, mergeOrChoose, nar.random(), nar.dtDitherCycles());

                } else if (ao == IMPL) {
                    return dtMergeDirect(a, b, aProp, curDepth, nar, mergeOrChoose);
                } else
                    throw new UnsupportedOperationException();
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
                        Term y = intermpolate(ai, 0, bi, aProp, curDepth / 2f, nar);
                        if (y instanceof Bool && (!(ai instanceof Bool)))
                            return Null; //failure

                        if (!ai.equals(y)) {
                            change = true;
                            ai = y;
                        }
                    }
                    ab[i] = ai;
                }

                return !change ? a : ao.the(
                        choose(a, b, aProp, nar.random()).dt()  /** this effectively chooses between && and &| in a size >2 case */,
                        ab
                );
            }

        }

        return choose(a, b, aProp, nar.random());

    }

    /**
     * TODO handle common ending suffix, not just prefix
     * TODO maybe merge based on the event separation time, preserving the order. currently this may shuffle the order though when events are relative to the sequence beginning this can makes sense
     */
    private static Term dtMergeConjEvents(Term a, long bOffset, Term b, float aProp, float curDepth, boolean mergeOrChoose, Random rng, int dither) {

        FasterList<LongObjectPair<Term>> ae = a.eventList(0, dither);
        FasterList<LongObjectPair<Term>> be = b.eventList(bOffset, dither);


        int as = ae.size();
        int bs;
        if (as != (bs = be.size())) {
            int pnn = Math.min(as, bs);
            int pn = pnn - 1;
            FasterList<LongObjectPair<Term>> shorter = (as < bs) ? ae : be; /* prefer the shorter for the reconstruction */
            if (pn > 0) {
                //decide forward or reverse
                int matchesForward = 0, matchesReverse = 0;
                for (int i = 0; i < pnn; i++) {
                    if (ae.get(i).getTwo().equals(be.get(i).getTwo()))
                        matchesForward++;
                    else
                        break;
                }
                if (matchesForward < pnn) {
                    for (int i = 0; i < pnn; i++) {
                        Term aa = ae.get((as - 1) - i).getTwo();
                        Term bb = be.get((bs - 1) - i).getTwo();
                        if (aa.equals(bb))
                            matchesReverse++;
                        else
                            break;
                    }
                }

                if (matchesForward > matchesReverse) {
                    Term prefix = conjMergeSeqEqualEvents(ae, be, aProp, mergeOrChoose, rng, pn, dither);
                    if (prefix == Null) return Null;
                    Term suffix = Op.conjEternalize(shorter, pn, shorter.size());
                    if (suffix == Null) return Null;
                    return mergeConjSeq(ae, be, pn, aProp, mergeOrChoose, rng, suffix, prefix, dither);
                } else {
                    TreeSet<Term> ete = new TreeSet();
                    long lastA = 0, lastB = 0;

                    if (ae.size() != be.size()) {
                        boolean alarger = ae.size() > be.size();
                        FasterList<LongObjectPair<Term>> longer = alarger ? ae : be;
                        int toRemove = Math.abs(ae.size() - be.size());
                        for (int i = 0; i < toRemove; i++) {
                            LongObjectPair<Term> lr = longer.remove(0);
                            long lro = lr.getOne();
                            if (alarger)
                                lastA = Math.max(lastA, lro);
                            else
                                lastB = Math.max(lastB, lro);
                            ete.add(lr.getTwo());
                        }
                        assert (ae.size() == be.size());
                    }
                    while (ae.size() > matchesReverse) {
                        if (!ae.isEmpty()) {
                            LongObjectPair<Term> ar = ae.remove(0);
                            lastA = Math.max(lastA, ar.getOne());
                            ete.add(ar.getTwo());
                        }
                        if (!be.isEmpty()) {
                            LongObjectPair<Term> br = be.remove(0);
                            lastB = Math.max(lastB, br.getOne());
                            ete.add(br.getTwo());
                        }
                    }

                    Term prefix = CONJ.the(DTERNAL, ete);
                    if (prefix == Null) return Null;
                    if (ae.isEmpty() && be.isEmpty()) {
                        return prefix;
                    } else {
                        Term suffix = conjMergeSeqEqualEvents(ae, be, aProp, mergeOrChoose, rng, ae.size(), dither);
                        if (suffix == Null) return Null;


                        int ad = (int) (ae.get(0).getOne() - lastA);
                        int bd = (int) (be.get(0).getOne() - lastB);
                        int gap = gap(ad, bd, aProp, mergeOrChoose, rng);
                        return mergeConjSeq(prefix, suffix, gap, dither);
                    }
                }
            } else {
                //entirely dternalize
                return Op.conjEternalize(shorter, 0, shorter.size());
            }
        }

        return conjMergeSeqEqualEvents(ae, be, aProp, mergeOrChoose, rng, as, dither);


//        Map<Term,LongHashSet> events = new HashMap();
//        a.eventseventsWhile((w,t)->{
//            events.computeIfAbsent(t, (tt)->new LongHashSet()).add(w);
//            return true;
//        }, 0);

//        FasterList<LongObjectPair<Term>> x = new FasterList(events.size());
//        for (Map.Entry<Term, LongHashSet> e : events.entrySet()) {
//            LongHashSet ww = e.getValue();
//            int ws = ww.size();
//            long w;
//            if (ws == 1) {
//                w = ww.longIterator().next();
//            } else {
//                if (mergeOrChoose) {
//                    //average
//                    //TODO more careful calculation here, maybe use BigDecimal in case of large numbers
//                    w = Math.round(((double)ww.sum()) / ws);
//                } else {
//                    w = ww.toArray()[rng.nextInt(ws)];
//                }
//            }
//            x.add(pair(w, e.getKey()));
//        }
//
//        //it may not be valid to choose subsets of the events, in a case like where >1 occurrences of $ must remain parent
//        int max = 1 + x.size() / 2; //HALF
//        int all = x.size();
//        int excess = all - max;
//        if (excess > 0) {
//
//            //decide on some items to remove
//            //must keep the endpoints unless a shift and adjustment are reported
//            //to the callee which decides this for the revised task
//
//            //for now just remove some inner tasks
//            if (all - excess < 2)
//                return null; //retain the endpoints
//            else if (all - excess == 2)
//                x = new FasterList(2).addingAll(x.get(0), x.get(all - 1)); //retain only the endpoints
//            else {
//                for (int i = 0; i < excess; i++) {
//                    x.remove(rng.nextInt(x.size() - 2) + 1);
//                }
//            }
//        }
//        return Op.conjEvents(x);
    }

    private static Term conjMergeSeqEqualEvents(FasterList<LongObjectPair<Term>> ae, FasterList<LongObjectPair<Term>> be, float aProp, boolean mergeOrChoose, Random rng, int n, int dither) {


        int changePoint = n;
        for (int i = 0; i < n; i++) {
            if (!ae.get(i).getTwo().equals(be.get(i).getTwo())) {
                changePoint = i;
                break;
            }
        }
        if (n >= 3 && changePoint < n / 2) {
            //try reverse to match the ends
            int changePointRev = 0;
            for (int i = n - 1; i >= 0; i--) {
                if (!ae.get(i).getTwo().equals(be.get(i).getTwo())) {
                    changePointRev = i;
                    break;
                }
            }
            if (changePointRev != changePoint) {
                //match in reverse mode
                changePoint = (n - 1) - changePointRev;
                ae.reverseThis();
                be.reverseThis();
            }
        }

        Term suffix = null;
        if (changePoint != n) {
            suffix = Op.conjEternalize(ae, changePoint, n);
            if (changePoint == 0)
                return suffix; //no prefix seq
        }


        Term prefix = mergeConjSeq(ae, be, changePoint, aProp, mergeOrChoose, rng, n);
        if (suffix == null) {
            return prefix;
        } else {
            return mergeConjSeq(ae, be, changePoint, aProp, mergeOrChoose, rng, suffix, prefix, dither);
        }
    }

    private static Term mergeConjSeq(FasterList<LongObjectPair<Term>> ae, FasterList<LongObjectPair<Term>> be, int attachPoint, float aProp, boolean mergeOrChoose, Random rng, Term suffix, Term prefix, int dither) {
        int ad = (int) (ae.get(attachPoint).getOne() - ae.get(attachPoint - 1).getOne());
        int bd = (int) (be.get(attachPoint).getOne() - be.get(attachPoint - 1).getOne());
        int gap = gap(ad, bd, aProp, mergeOrChoose, rng);
        return mergeConjSeq(prefix, suffix, gap, dither);
    }

    private static int gap(int ad, int bd, float aProp, boolean mergeOrChoose, Random rng) {
        int gap;
        if (ad == bd)
            gap = ad;
        else {
            if (mergeOrChoose) {
                gap = Util.lerp(aProp, ad, bd);
            } else {
                gap = (rng.nextFloat() <= aProp) ? ad : bd;
            }
        }
        return gap;
    }

    private static Term mergeConjSeq(Term prefix, Term suffix, int gap, int dither) {
        int ditheredGap = Tense.dither(gap, dither);
        return CONJ.the(ditheredGap, prefix, suffix);
        //int ditheredGap = Tense.dither(gap + prefix.dtRange(), dither);
        //return Op.conjMerge(prefix, 0, suffix, ditheredGap);
    }

    private static Term mergeConjSeq(FasterList<LongObjectPair<Term>> ae, FasterList<LongObjectPair<Term>> be, int changePoint, float aProp, boolean mergeOrChoose, Random rng, int n) {
        FasterList<LongObjectPair<Term>> x = new FasterList(n);
        for (int i = 0; i < changePoint; i++) {
            long at = ae.get(i).getOne();
            long bt = be.get(i).getOne();
            if (at == bt)
                x.add(ae.get(i));
            else {
                if (mergeOrChoose) {
                    long abt = Math.round(Util.lerp(aProp, at, bt));
                    if (abt != at)
                        x.add(pair(abt, ae.get(i).getTwo()));
                } else {
                    x.add(((rng.nextFloat() <= aProp) ? ae : be).get(i));
                }
            }
        }
        return Conj.conj(x);
    }


    /*@NotNull*/
    private static Term dtMergeDirect(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, float depth, NAR nar, boolean mergeOrChoose) {

        int adt = a.dt();
        int bdt = b.dt();


//        if (adt!=bdt)
//            System.err.print(adt + " " + bdt);

        depth /= 2f;

        int dt;

        if (adt == bdt) {
            dt = adt;
        } else {
            if ((adt == XTERNAL) || (bdt == XTERNAL)) {
                dt = XTERNAL;
            } else if ((adt == DTERNAL || bdt == DTERNAL) || ((adt >= 0) != (bdt >= 0))) {
                //opposite directions, so settle for DTERNAL
                dt = DTERNAL;
            } else {

                if (mergeOrChoose) {

                    dt = lerp(aProp, bdt, adt);

                } else {
                    dt = (choose(a, b, aProp, nar.random()) == a) ? adt : bdt;
                }
            }
        }

        dt = Tense.dither(dt, nar.dtDitherCycles());

        Term a0 = a.sub(0);
        Term a1 = a.sub(1);

        Term b0 = b.sub(0);
        Term b1 = b.sub(1);

        if (a0.equals(b0) && a1.equals(b1)) {
            return a.dt(dt);
        } else {
            Term na = intermpolate(a0, 0, b0, aProp, depth, nar);
            if (na == Null) return Null;
            Term nb = intermpolate(a1, 0, b1, aProp, depth, nar);
            if (nb == Null) return Null;
            return a.op().the(dt, na, nb);
        }

    }

    static Term choose(Term a, Term b, float aBalance, /*@NotNull*/ Random rng) {
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


//    /*@NotNull*/
//    public static Task chooseByConf(/*@NotNull*/ Task t, @Nullable Task b, /*@NotNull*/ Derivation p) {
//
//        if ((b == null) || !b.isBeliefOrGoal())
//            return t;
//
//        //int dur = p.nar.dur();
//        float tw = t.conf();
//        float bw = b.conf();
//
//        //randomize choice by confidence
//        return p.random.nextFloat() < tw / (tw + bw) ? t : b;
//
//    }

    public static Term intermpolate(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, 0, b, aProp, nar);
    }

    /**
     * a is left aligned, dt is any temporal shift between where the terms exist in the callee's context
     */
    static Term intermpolate(/*@NotNull*/ Term a, long dt, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, dt, b, aProp, 1, nar);
    }


//    @Nullable public static Task mergeTemporal(NAR nar, long start, long end, FasterList<TaskRegion> tt) {
//        //filter the task set:
//        // if there are any exact matches to the interval, remove any others
//        RoaringBitmap oob = new RoaringBitmap();
//        for (int i = 0, ttSize = tt.size(); i < ttSize; i++) {
//            TaskRegion x = tt.get(i);
//            if (x == null || !x.intersects(start, end))
//                oob.add(i);
//        }
//        int numRemoved = oob.getCardinality();
//        if (numRemoved!=0 && numRemoved!=tt.size()) {
//            IntIterator ii = oob.getReverseIntIterator();
//            while (ii.hasNext()) {
//                tt.remove(ii.next());
//            }
//        }
//
//        return mergeTemporal(nar, tt);
//    }


//    /** preprocesses the tasks with respect to the specified time bounds being truthpolated */
//    @Nullable public static Task mergeTemporal(NAR nar, long start, long end, TaskRegion[] tt) {
//
//        if (start == ETERNAL) {
//            //replace the array with eternalized proxy tasks
//            //tt = tt.clone();
//            float factor =
//                    1f;
//            //1/n
//
//            tt = Util.replaceDirect(tt, 0, tt.length,
//                    x -> Task.eternalize((Task) x, factor));
//        } else {
//
//            //   TODO clip tasks to the specified ranges?
//        }
//
//        return mergeTemporal(nar, tt);
//    }

    @Nullable
    public static Task mergeTasks(NAR nar, TaskRegion... tt) {
        long[] u = Tense.union(tt);
        return mergeTasks(nar, u[0], u[1], tt);
    }

    @Nullable
    public static Task mergeTasks(NAR nar, long start, long end, TaskRegion... tt) {
        return mergeTasks(nar, nar.dur(), start, end, false, tt);
    }

    @Nullable
    public static Task mergeTasks(NAR nar, int dur, long start, long end, boolean forceProjection, TaskRegion... tasks) {

        tasks = ArrayUtils.removeNulls(tasks, Task[]::new); //HACK

        /*Truth.EVI_MIN*/
        //c2wSafe(nar.confMin.floatValue()),
        float eviMinInteg = Float.MIN_NORMAL;

        TruthPolation T = Param.truth(start, end, dur).add(tasks);
        LongHashSet stamp = T.filterCyclic();

        Truth baseTruth = T.truth(nar);
        if (baseTruth == null)
            return null; //nothing

        float truthEvi = baseTruth.evi();

        float range = (start != ETERNAL) ? (end - start + 1) : 1;
        if ((truthEvi * range) < eviMinInteg)
            return null;

        Truth cTruth = Truth.theDithered(baseTruth.freq(), truthEvi, nar);
        if (cTruth == null)
            return null;

        if (!forceProjection && T.size() == 1) {
            //can't do better than the only remaining task, unless you want to force reprojection to the specified start,end range (ie. eternalize)
            Task only = T.get(0).task;
            return only;
        }

        byte punc = T.punc();

        Task t = Task.tryTask(T.term, punc, cTruth, (c, tr) ->
                new NALTask(c, punc,
                        tr,
                        nar.time(), start, end,
                        Stamp.sample(Param.STAMP_CAPACITY, stamp /* TODO account for relative evidence contributions */, nar.random())
                )
        );

        if (t == null)
            return null; //failed to create task

        tasks = T.tasks(); //HACK

        t.priSet(Priority.fund(Util.max((TaskRegion p) -> p.task().priElseZero(), tasks),
                true,
                //false,
                Tasked::task, tasks));

        ((NALTask) t).cause(Cause.sample(Param.causeCapacity.intValue(), tasks));

//        for (TaskRegion x : tasks) {
//            x.task().meta("@", (k) -> t); //forward to the revision
//        }

        return t;
    }


    public static float eviAvg(Task x, long start, long end, int dur) {

        if (start == ETERNAL) {
            return x.isEternal() ? x.evi() : x.eviEternalized();
        }

        //integrate, potentialy inside and outside
        float e = eviInteg(x, start, end, dur);
        long range = (start == ETERNAL) ? 1 : (end - start + 1);
        return e / range;

    }

    /**
     * convenience method for selecting evidence integration strategy
     */
    public static float eviInteg(Task x, long start, long end, int dur) {
        assert(start!=ETERNAL);

        if (x.isEternal())
            return x.evi() * (end-start+1); //simple

        if (start == end) {
            return x.evi(start, dur); //point-like
        } else {
            long d = end - start;
            if (d <= dur || d < 2) {
                return x.eviInteg(dur,
                        start,
                        end);
            } else {
                @Nullable Longerval se = Longerval.intersect(start, end, x.start(), x.end());
                if (se != null) {

                    boolean a = (se.a > start && se.a < end);  //add point se.a?

                    boolean b = (se.b > start && se.b < end && se.b!=se.a); //add point se.b?

                    if (a && b) {
                        return x.eviInteg(dur, start, se.a, se.b, end);
                    } else if (a) {
                        return x.eviInteg(dur, start, se.a, end);
                    } else if (b) {
                        return x.eviInteg(dur, start, se.b, end);
                    }

                }

                return x.eviInteg(dur,
                        start,
                        (start + end) / 2L, //midpoint
                        end);
            }
        }
    }

    static boolean equalOrWeaker(Task input, Truth output, long start, long end, Term cc, NAR nar) {
        Truth inTruth = input.truth();
        if ((inTruth.conf() - output.conf() <= nar.confResolution.floatValue())) {
            if (Util.equals(inTruth.freq(), output.freq(), nar.freqResolution.asFloat())) {
                return cc.equals(input.term()) && start >= input.start() && end <= input.end();
            }
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

//        if (!a.isTemporal() || !b.isTemporal()) {
//            return 0f;
//        }

        Op ao = a.op();
        Op bo = b.op();
        if (ao != bo)
            return Float.POSITIVE_INFINITY; //why?


        Subterms aa = a.subterms();
        int len = aa.subs();
        Subterms bb = b.subterms();

        float d = 0;

        //if (len!=blen) {
//        if (!aa.equals(bb)) {
//            return (a.dtRange() + b.dtRange()) / depth; //estimate
//        }
        //int blen = bb.subs();

        boolean aSubsEqualsBSubs = aa.equals(bb);
        if (a.op() == CONJ && !aSubsEqualsBSubs) {
            //HACK :)
            Conj c = new Conj();
            String as = Conj.sequenceString(a, c).toString();
            String bs = Conj.sequenceString(b, c).toString();

            int levDist = Texts.levenshteinDistance(as, bs);
            float seqDiff = (((float) levDist) / (Math.min(as.length(), bs.length())));

            //HACK estimate
            float rangeDiff = Math.max(1f, Math.abs(a.dtRange() - b.dtRange()));

            d += (1f + rangeDiff) * (1f + seqDiff);

        } else {
            if (!aSubsEqualsBSubs) {
                if (aa.subs() != bb.subs())
                    return Float.POSITIVE_INFINITY;

                for (int i = 0; i < len; i++)
                    d += dtDiff(aa.sub(i), bb.sub(i), depth + 1);
            }

            int adt = a.dt();
            int bdt = b.dt();

            //ockham temoral razor - prefer temporally shorter explanations
            if (adt == DTERNAL) adt =
                    //0; //dternal prefer match with immediate dt=0
                    bdt; //any
            if (bdt == DTERNAL) bdt =
                    //bdt = 0; //dternal prefer match with immediate dt=0
                    adt; //any

            if (adt == XTERNAL) adt = bdt;
            if (bdt == XTERNAL) bdt = adt;

            if (adt != bdt/* && adt != DTERNAL && bdt != DTERNAL*/) {

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


        }

        return d / depth;
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
//    @Nullable
//    @Deprecated public static Task merge(/*@NotNull*/ Task a, /*@NotNull*/ Task b, long now, float minEvi, NAR nar) {
//        assert (!a.isEternal() && !b.isEternal()) : "can not merge eternal tasks";
//
////        if (a.op() == CONJ) {
////            //avoid intermpolation of 2 conjunctions with opposite polarities
////            if (!a.term().equals(b.term())
////                    && (a.isPositive() ^ b.isPositive()) && (a.term().dtRange() != 0 || b.term().dtRange() != 0))
////                return null;
////        }
//
//        //ObjectFloatPair<long[]> s = Stamp.zip(new FasterList(a, b), Param.STAMP_CAPACITY);
//        float overlap = Stamp.overlapFraction(a.stamp(), b.stamp());
//        float overlapFactor = Param.overlapFactor(overlap);
//        if (overlapFactor < Float.MIN_NORMAL)
//            return null;
//
//
//        long as, bs;
//        if ((as = a.start()) > (bs = b.start())) {
//            //swap so that 'a' is left aligned
//            Task x = a;
//            a = b;
//            b = x;
//            long xs = as;
//            as = bs;
//            bs = xs;
//        }
//        assert (bs != ETERNAL);
//        assert (as != ETERNAL);
//
//
//        //            float ae = a.evi();
////            float aa = ae * (1 + ai.length());
////            float be = b.evi();
//        //float bb = be * (1 + bi.length());
//        //float p = aa / (aa + bb);
//
//
//        //relate high frequency difference with low confidence
////        float freqDiscount =
////                0.5f + 0.5f * (1f - Math.abs(a.freq() - b.freq()));
////        factor *= freqDiscount;
////        if (factor < Prioritized.EPSILON) return null;
//
//
////            float temporalOverlap = timeOverlap==null || timeOverlap.length()==0 ? 0 : timeOverlap.length()/((float)Math.min(ai.length(), bi.length()));
////            float confMax = Util.lerp(temporalOverlap, Math.max(w2c(ae),w2c(be)),  1f);
////
////
////            float timeDiscount = 1f;
////            if (timeOverlap == null) {
////                long separation = Math.max(a.timeDistance(b.start()), a.timeDistance(b.end()));
////                if (separation > 0) {
////                    long totalLength = ai.length() + bi.length();
////                    timeDiscount =
////                            (totalLength) /
////                                    (separation + totalLength)
////                    ;
////                }
////            }
//
//
//        //width will be the average width
////        long width = (ai.length() + bi.length()) / 2; //TODO weight
////        long mid = (ai.mid() + bi.mid()) / 2;  //TODO weight
//
////            Truth expected = table.truth(mid, now, dur);
////            if (expected == null)
////                return null;
//
//
//        int dur = nar.dur();
////        float intermvalDistance = dtDiff(a.term(), b.term()) /
////                ((1 + Math.max(a.term().dtRange(), b.term().dtRange())) * dur);
////        factor *= (1f / (1f + intermvalDistance));
////        if (factor < Prioritized.EPSILON) return null;
//
//        EviDensity density = new EviDensity(a, b);
//        long start = density.unionStart;
//        long end = density.unionEnd;
//
//
//        Truth an = a.truth(start, end, dur, 0);
//        if (an == null)
//            return null;
//        Truth bn = b.truth(start, end, dur, 0);
//        if (bn == null)
//            return null;
//
//        Truth rawTruth = revise(an, bn,
//                //joint.factor(Math.abs( an.freq() - bn.freq() ))
//                density.factor()
//                , Float.MIN_NORMAL /*Truth.EVI_MIN*/);
//        if (rawTruth == null)
//            return null;
//
//
//        float e2 = rawTruth.evi() * overlapFactor;
//        if (e2 < minEvi)
//            return null;
//        rawTruth = rawTruth.withEvi(e2);
//
//        Truth cTruth = rawTruth.dither(nar);
//        if (cTruth == null)
//            return null;
//        Term cc = null;
//
//////        float maxEviAB = Math.max(an.evi(), bn.evi());
////        float evi = rawTruth.evi();
////        if (maxEviAB < evi) {
////            //more evidence overlap indicates redundant information, so reduce the increase in confWeight (measure of evidence) by this amount
////            //TODO weight the contributed overlap amount by the relative confidence provided by each task
////            //        factor *= overlapDiscount;
////            //        if (factor < Prioritized.EPSILON) return null;
////
////            float eviDiscount = (evi - maxEviAB) * overlapDiscount;
////            float newEvi = evi - eviDiscount;
////            if (!Util.equals(evi, newEvi, Pri.EPSILON)) {
////                rawTruth = rawTruth.withEvi(newEvi);
////            }
////
////        }
////
//
//
//        //TODO maybe delay dithering until after the negation has been determined below
//
////            float conf = w2c(expected.evi() * factor);
////            if (conf >= Param.TRUTH_EPSILON)
////                newTruth = new PreciseTruth(expected.freq(), conf);
////            else
////                newTruth = null;
//
//
//        assert (a.punc() == b.punc());
//
//
//        float aProp = a.isQuestionOrQuest() ? 0.5f : an.evi() / (an.evi() + bn.evi());
//
//
//        Term at = a.term();
//        Term bt = b.term();
//        if (!at.equals(bt)) {
//
//            //Term atConceptual = at.conceptual();
//            //if (Param.DEBUG) assert(bt.conceptual().equals(atConceptual)): at + " and " + bt + " may not belong in the same concept";
//
//            for (int i = 0; i < Param.MAX_TERMPOLATE_RETRIES; i++) {
//                Term t;
//                if (at.equals(bt)) {
//                    t = at;
//                    i = Param.MAX_TERMPOLATE_RETRIES; //no need to retry
//                } else {
//                    long dt = bs - as;
//                    t = intermpolate(at, dt, bt, aProp, nar);
//                    if (t == null || !t.unneg().op().conceptualizable)
//                        continue;
//                }
//
//
//                ObjectBooleanPair<Term> ccp = Task.tryContent(t, a.punc(), Param.DEBUG_EXTRA);
//                /*if (ccp != null) */{
//
//                    cc = ccp.getOne();
//                    //assert (cc.isNormalized());
//
//                    if (ccp.getTwo())
//                        cTruth = cTruth.neg();
//                    break;
//                }
//            }
//
//            if (cc == null)
//                return null;
//
//
//            //        if (cc.op() == CONJ) {
//            //            long mid = Util.lerp(aProp, b.mid(), a.mid());
//            //            long range = cc.op() == CONJ ?
//            //                    cc.dtRange() :
//            //                    (Util.lerp(aProp, b.range(), a.range()));
//            //            start = mid - range / 2;
//            //            end = start + range;
//            //        } else {
//            //            if (u > s) {
//            //                start = end = Util.lerp(aProp, b.mid(), a.mid());
//            //            } else {
//
//            //            }
//            //        }
//        } else {
//            cc = at;
//        }
//
//
//        if (equalOrWeaker(a, cTruth, start, end, cc, nar))
//            return a;
//        if (equalOrWeaker(b, cTruth, start, end, cc, nar))
//            return b;
//
//        NALTask t = new NALTask(cc, a.punc(),
//                cTruth,
//                now, start, end,
//                Stamp.zip(a.stamp(), b.stamp(), aProp) //get a stamp collecting all evidence from the table, since it all contributes to the result
//        );
////        if (overlap > 0 || a.isCyclic() || b.isCyclic())
////            t.setCyclic(true);
//
//        t.priSet(Util.lerp(aProp, b.priElseZero(), a.priElseZero()));
//
//        //t.setPri(a.priElseZero() + b.priElseZero());
//
//        t.cause = Cause.sample(Param.causeCapacity.intValue(), a, b);
//
//        if (Param.DEBUG)
//            t.log("Revection Merge");
//
//
//
//        a.meta("@", (k)->t); //forward to the revision
//        b.meta("@", (k)->t); //forward to the revision
//
//        return t;
//    }
