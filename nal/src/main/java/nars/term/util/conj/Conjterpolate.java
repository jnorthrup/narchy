package nars.term.util.conj;

import jcog.Util;
import nars.NAL;
import nars.term.Term;
import nars.time.Tense;

import java.util.Random;

/**
 * interpolate conjunction sequences
 * for each of b's events, find the nearest matching event in a while constructing a new Conj consisting of their mergers
 * * similar to conjMerge but interpolates events so the resulting
 * * intermpolation is not considerably more complex than either of the inputs
 * * assumes a and b are both conjunctions
 * <p>
 * UNTESTED
 */
public class Conjterpolate extends Conj {
    private final int dither;


    @Override
    public boolean add(long at, Term x) {
        return super.add(Tense.dither(at,dither), x);
    }

    /**
     * proportion of a vs. b, ie: (a/(a+b))
     */
    public Conjterpolate(Term a, Term b, float aProp, NAL nar) {
        super();

        this.dither = nar.dtDither();

        ConjLazy aa = ConjLazy.events(a);
        ConjLazy bb = ConjLazy.events(a);
        int na = aa.size(), nb = bb.size();
        int nabOriginal = Math.min(na,nb);

        int minLen = Math.min(na, nb);
        int prefixMatched = 0;
        for (; aa.when(prefixMatched)==bb.when(prefixMatched) &&
                aa.get(prefixMatched).equals(bb.get(prefixMatched)) && ++prefixMatched < minLen; ) ;

        if (prefixMatched > 0) {
            for (int i = 0; i < prefixMatched; i++) {
                if (!add(aa.when(0), aa.get(0)))
                    return; //conflict or other CONJ failure
                aa.removeFirst();
                bb.removeFirst();
            }
            na -= prefixMatched;
            nb -= prefixMatched;
            minLen = Math.min(na, nb);
        }
//        }

        if (na > 0 && nb > 0) {
            int suffixMatched = 0;
            //TODO compare time relative to end, not beginning
            for (; aa.get(na - 1 - suffixMatched).equals(bb.get(nb - 1 - suffixMatched)) && ++suffixMatched < minLen; )
                ;
//
            if (suffixMatched > 0) {
                //add the suffixed matched segment
                for (int i = 0; i < suffixMatched; i++) {
                    int ni = na - 1 - i;
                    if (!add(aa.when(ni), aa.get(ni)))
                        return; //conflict or other CONJ failure
                    aa.removeLast();
                    bb.removeLast();
                }

                aa.removeAbove(suffixMatched);
                bb.removeAbove(suffixMatched);
                na -= suffixMatched;
                nb -= suffixMatched;
            }
        }

        int remainingEvents = Util.lerp(aProp, na, nb);
        if (remainingEvents > 0) {
            if (nb == 0 ^ na == 0) {
                if (!addAll((aa.isEmpty() ? bb : aa))) //the remaining events
                    return; //conflict or other CONJ failure
            } else {
//                LongObjectPair<Term> a0 = aa.get(0), b0 = bb.get(0);
//                Term a0t = a0.getTwo(), b0t = b0.getTwo();
//                if (a0t instanceof Compound && b0t instanceof Compound) {
//                    if (na == 1 && nb == 1 && nabOriginal > 1) {
//                        //special case: only one event remains, with the same root target
//                        Term ab = Intermpolate.intermpolate((Compound)a0t, (Compound)b0t, aProp, nar);
//
//                        if (ab.op().eventable) {
//                            long when = Intermpolate.chooseDT(
//                                    Tense.occToDT(a0.getOne()),
//                                    Tense.occToDT(b0.getOne()), aProp, nar);
//                            add(when, ab);
//                            return;
//                        }
//                    }
//                }

//                //add common events
//                MutableSet<LongObjectPair<Term>> common = Sets.intersect(ArrayUnenforcedSet.wrap(aa), ArrayUnenforcedSet.wrap(bb));
//                if (!common.isEmpty()) {
//                    for (LongObjectPair<Term> c : common) {
//                        if (!add(c))
//                            break; //try to catch if this happens
//                    }
//                    aa.removeAll(common);
//                    bb.removeAll(common);
//                    remainingEvents -= common.size();
//                }


                if (remainingEvents > 0) {

                    Random rng = nar.random();
                    do {
                        ConjLazy which;
                        if (!aa.isEmpty() && !bb.isEmpty())
                            which = rng.nextFloat() < aProp ? aa : bb;
                        else if (aa.isEmpty() && !bb.isEmpty())
                            which = bb;
                        else if (bb.isEmpty() && !aa.isEmpty())
                            which = aa;
                        else
                            break;  //?

                        int ri = rng.nextInt(which.size());
                        long rw = which.when(ri);
                        Term rt = which.get(ri);
                        which.removeThe(ri);
                        if (!add(rw,rt))
                            return; //conflict or other CONJ failure

                    } while (--remainingEvents > 0);


                }
            }

        }

        //distribute

//        this.rng = rng;
//
//        long dt = Conj.isSeq(a) || Conj.isSeq(b) || a.dt()==0 || b.dt()==0 ? 0 : ETERNAL;
//        addProb = aProp;
//        if (addAt(dt, a)) {
//            addProb = 1-aProp;
//            if (addAt(dt, b)) {
//
//            }
//        }

    }


//
//    @Override
//    public boolean addAt(long at, Term x) {
//        if (rng.nextFloat() < addProb)
//            return super.addAt(at, x);
//        else
//            return true; //ignore
//    }

//    protected void compress(int targetVol, int interpolationThresh /* time units */) {
//
//        if (interpolationThresh < 1)
//            return;
//
//        //factor();
//        distribute();
//
//        //find any two time points that differ by less than the interpolationThresh interval
//        long[] times = this.event.keySet().toSortedArray();
//        if (times.length < 2) return;
//        for (int i = 1; i < times.length; i++) {
//            if (times[i - 1] == ETERNAL)
//                continue;
//            long dt = times[i] - times[i - 1];
//            if (Math.abs(dt) < interpolationThresh) {
//                if (combine(times[i - 1], times[i])) {
//                    i++; //skip past current pair
//                }
//            }
//        }
//    }
//
//    boolean combine(long a, long b) {
//        assert (a != b);
//        assert (a != DTERNAL && b != DTERNAL && a != XTERNAL && b != XTERNAL);
//        ByteHashSet common = new ByteHashSet();
//        events((byte[])event.remove(a), common::addAt);
//        events((byte[])event.remove(b), common::addAt);
//
//        //detect conflicting combination
//        byte[] ca = common.toArray();
//        boolean changed = false;
//        for (byte cc : ca) {
//            if (cc < 0 && common.contains((byte) -cc)) {
//                common.remove(cc);
//                common.remove((byte) -cc);
//                changed = true;
//            }
//        }
//        if (changed) {
//            ca = common.toArray();
//        }
//        if (ca.length > 0) {
//            long mid = (a + b) / 2L; //TODO better choice
//            event.put(mid, ca);
//        }
//        return true;
//    }
//
//        @Override
//        public boolean addAt(long bt, final Term what) {
//            assert (bt != XTERNAL);
//
//            {
//                boolean neg = what.op() == NEG;
//
//
//                byte tid = termToId.getIfAbsent(neg ? what.unneg() : what, (byte) -1);
//                if (tid == (byte) -1)
//                    return super.addAt(bt, what);
//
//                byte tInA = (byte) (tid * (neg ? -1 : +1));
//
//
//                LongArrayList whens = new LongArrayList(2);
//
//                aa.event.forEachKeyValue((long when, Object wat) -> {
//                    if (wat instanceof RoaringBitmap) {
//                        RoaringBitmap r = (RoaringBitmap) wat;
//                        if (r.contains(tInA) && !r.contains(-tInA)) {
//                            whens.addAt(when);
//                        }
//                    } else {
//                        byte[] ii = (byte[]) wat;
//                        if (ArrayUtils.indexOf(ii, tInA) != -1 && ArrayUtils.indexOf(ii, (byte) -tInA) == -1) {
//                            whens.addAt(when);
//                        }
//                    }
//                });
//
//
//                int ws = whens.size();
//                if (ws > 0) {
//
//                    if (whens.contains(bt))
//                        return true;
//
//                    long at;
//                    if (ws > 1) {
//                        LongToLongFunction temporalDistance;
//                        if (bt == ETERNAL) {
//                            temporalDistance = (a) -> a == ETERNAL ? 0 : Long.MAX_VALUE;
//                        } else {
//                            temporalDistance = (a) -> a == ETERNAL ? Long.MAX_VALUE : Math.abs(bt - a);
//                        }
//                        long[] whensArray = whens.toArray();
//                        ArrayUtils.sort(whensArray, temporalDistance);
//
//                        at = whensArray[whensArray.length - 1];
//                    } else {
//                        at = whens.get(0);
//                    }
//
//                    long merged = merge(at, bt);
//                    if (merged != at) {
//
//                        if ((merged == DTERNAL || merged == XTERNAL) && (at != DTERNAL && bt != DTERNAL && at != XTERNAL && bt != XTERNAL)) {
//                            //add as unique event (below)
//                        } else {
////                            boolean r = aa.remove(what, at); //remove original add the new merged
////                            if (!r) {
////                                assert (r);
////                            }
//                            return super.addAt(merged, what);
//                        }
//
//                    } else {
//                        return true; //exact
//                    }
//                }
//                return super.addAt(bt, what);
//
//            }
//
//        }

//        long merge(long a, long b) {
//            if (a == b) return a;
//            if (a == ETERNAL || b == ETERNAL)
//                return ETERNAL;
//            if (a == XTERNAL || b == XTERNAL)
//                throw new RuntimeException("xternal in conjtermpolate");
//
//
//            return Tense.dither(Revision.merge(a, b, aProp, nar), nar);
//
//        }

}
//            this.b = b;
//            this.nar = nar;

//            this.aa = Conj.from(a);
//            this.idToTerm.addAll(aa.idToTerm);
//            this.termToId.putAll(aa.termToId);


//TODO time warping algorithm: find a shift that can be applied ot adding b that allows it to match a longer sub-sequence
//for now this is a naive heuristic
//        int aShift, bShift;
//        if (a.eventFirst().equals(b.eventFirst())) {
//            aShift = bShift = 0;
//        } else if (a.eventLast().equals(b.eventLast())) {
//            bShift = a.eventRange() - b.eventRange();
//            aShift = 0;
//        } else {
//            //align center
//            int ae = a.eventRange();
//            int be = b.eventRange();
//            if (ae!=be) {
//                if (ae < be) {
//                    aShift = be/2 - ae/2;
//                    bShift = 0;
//                } else {
//                    aShift = 0;
//                    bShift = ae/2 - be/2;
//                }
//            } else {
//                aShift = bShift = 0;
//            }
//        }
//
//        if (bShift < 0) {
//            aShift += -bShift;
//            bShift = 0;
//        } else if (aShift < 0) {
//            bShift += -aShift;
//            aShift = 0;
//        }


//            //merge remaining events from 'a'
//            final boolean[] err = {false};
//            aa.event.forEachKeyValue((long when, Object wat) -> {
//                if (err[0])
//                    return; //HACK
//                if (wat instanceof RoaringBitmap) {
//                    RoaringBitmap r = (RoaringBitmap) wat;
//                    r.forEach((int ri) -> {
//                        boolean neg = (ri < 0);
//                        if (neg) ri = -ri;
//                        if (!addAt(when, idToTerm.get(ri-1).negIf(neg))) {
//                            err[0] = true;
//                        }
//                    });
//                } else {
//                    byte[] ii = (byte[]) wat;
//                    for (byte ri : ii) {
//                        if (ri == 0)
//                            break; //eol
//                        boolean neg = (ri < 0);
//                        if (neg) ri = (byte) -ri;
//                        if (!addAt(when, idToTerm.get(ri-1).negIf(neg))) {
//                            err[0] = true;
//                        }
//                    }
//                }
//            });
//add remaining
//                assert (!aa.isEmpty() && !bb.isEmpty());
//
//                ArrayHashSet<LongObjectPair<Term>> ab = new ArrayHashSet(aa.size() + bb.size());
//                ab.addAll(aa);
//                ab.addAll(bb);
//                for (int i = 0; i < remainingEvents; i++) {
//                    if (!addAt(ab.remove(rng))) {
//                        //oops try to prevent if this happens
//                        break;
//                    }
//                }