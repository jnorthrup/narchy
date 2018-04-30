package nars.term.compound.util;

import jcog.data.bit.MetalBitSet;
import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.IntPredicate;

import static nars.Op.*;
import static nars.term.Terms.sorted;
import static nars.util.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * representation of conjoined (eternal, parallel, or sequential) events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing
 */
public class Conj {

    //capacity of the initial array before upgrading to RoaringBitmap
    private static final int ROARING_UPGRADE_THRESH = 4;

//    private static final byte EMPTY = 0;
//    private static final byte POSITIVE = 1;
//    private static final byte NEGATIVE = 2;
    public final LongObjectHashMap event = new LongObjectHashMap<>(2);
    /**
     * unnegated events
     */
    //final Map<Term,Byte> terms = new TermHashMap();
    final ObjectByteHashMap<Term> terms = new ObjectByteHashMap<>(4);

//    /**
//     * keys are encoded 8-bits + 8-bits vector of the time,term index
//     *
//     * values are:
//     *      0 00 - not present
//     *      1 01 - positive     (x)
//     *      2 10 - negated  (--, x)
//     *      3 11 - unused
//     *
//     * TODO use a 'ShortCrumbHashMap'
//     *     for compact 4-bit values http://mathworld.wolfram.com/Crumb.html
//     *
//     */
//    public final ShortByteHashMap event = new ShortByteHashMap(2);
//
//    /** occurrences */
//    public final LongArrayList times = new LongArrayList(1);
    final List<Term> termsIndex = new FasterList(4);
    /**
     * state which will be set in a terminal condition, or upon term construction in non-terminal condition
     */
    Term term = null;

    public Conj() {

    }

    public static int eventCount(Object what) {
        if (what instanceof byte[]) {
            return indexOfZeroTerminated((byte[]) what, (byte) 0);
        } else {
            return ((RoaringBitmap) what).getCardinality();
        }
    }

    /**
     * TODO impl levenshtein via byte-array ops
     */
    public static StringBuilder sequenceString(Term a, Conj x) {
        StringBuilder sb = new StringBuilder(4);
        int range = a.dtRange();
        final float stepResolution = 16f; //how finely to fractionalize time range
        float factor = stepResolution / range;
        a.eventsWhile((when, what) -> {
            int step = Math.round(when * factor);
            sb.append((char) step); //character representing the relative offset of the event
            sb.append(((char) x.add(what)));
            return true;
        }, 0, true, true, false, 0);

        return sb;
    }

    /**
     * returns null if wasnt contained, Null if nothing remains after removal
     */
    @Nullable
    public static Term conjDrop(Term conj, Term event, boolean earlyOrLate) {
        if (conj.op() != CONJ || conj.impossibleSubTerm(event))
            return Null;

        int cdt = conj.dt();
        //quick test for first layer subterm of a commutive conjunction
        if (cdt == DTERNAL || cdt == 0) {
            Term[] csDropped = conj.subterms().termsExcept(event);
            if (csDropped != null) {
                if (csDropped.length == 1)
                    return csDropped[0];
                else
                    return CONJ.the(cdt, csDropped);
            }
        }

        Conj c = Conj.from(conj);
        long targetTime;
        if (c.event.size() == 1) {
            //TODO maybe prefer the subevent with the shortest range()
            targetTime = c.event.keysView().longIterator().next();
        } else if (earlyOrLate) {
            Object eternalTemporarilyRemoved = c.event.remove(ETERNAL);
            targetTime = c.event.keysView().min();
            if (eternalTemporarilyRemoved != null)
                c.event.put(ETERNAL, eternalTemporarilyRemoved);
        } else {
            targetTime = c.event.keysView().max();
        }
        assert (targetTime != XTERNAL);
        boolean removed = c.remove(event, targetTime);
        if (!removed) {
            return Null; //the event was not at the target time
        }

        return c.term();

//        FasterList<LongObjectPair<Term>> events = Conj.decompose(conj);
//        Comparator<LongObjectPair<Term>> c = Comparator.comparingLong(LongObjectPair::getOne);
//        int eMax = events.maxIndex(earlyOrLate ? c.reversed() : c);
//
//        LongObjectPair<Term> ev = events.get(eMax);
//        if (ev.getTwo().equals(event)) {
//            events.removeFast(eMax);
//            return Op.conj(events);
//        } else {
//            return Null;
//        }
    }

    public static FasterList<LongObjectPair<Term>> eventList(Term t) {
        return t.eventList(t.dt() == DTERNAL ? ETERNAL : 0, 1, true, true);
    }

    public static Conj from(Term t) {
        return from(t, t.dt() == DTERNAL ? ETERNAL : 0);
    }

    public static Conj from(Term t, long rootTime) {
        Conj x = new Conj();
        x.add(t, rootTime);
        return x;
    }

    public static Term conj(FasterList<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return Null;
            case 1:
                return events.get(0).getTwo();
        }

        Conj ce = new Conj();

        for (int i = 0; i < eventsSize; i++) {
            LongObjectPair<Term> o = events.get(i);
            if (!ce.add(o.getTwo(), o.getOne())) {
                break;
            }
        }

        return ce.term();
    }

    public static Term conj(Collection<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return Null;
            case 1:
                return events.iterator().next().getTwo();
        }

        Conj ce = new Conj();

        for (LongObjectPair<Term> o : events) {
            if (!ce.add(o.getTwo(), o.getOne())) {
                break;
            }
        }

        return ce.term();
    }

    public static Term without(Term conj, Term event, boolean includeNeg, NAR nar) {
        if (conj.op() != CONJ || conj.impossibleSubTerm(event))
            return Null;

        Conj c = Conj.from(conj);
        if (c.removeAll(event, true, includeNeg)) {
            return c.term(); //return the changed conj
        } else {
            return Null; //same
        }
    }

    public static Term conjMerge(Term a, Term b, int dt) {
        return (dt >= 0) ?
                conjMerge(a, 0, b, +dt + a.dtRange()) :
                conjMerge(b, 0, a, -dt + b.dtRange());
    }

    static public Term conjMerge(Term a, long aStart, Term b, long bStart) {
        Conj c = new Conj();
        if (c.add(a, aStart)) {
            c.add(b, bStart);
        }
        return c.term(); //Null, True, or False
    }

    static int indexOfZeroTerminated(byte[] b, byte val) {
        for (int i = 0; i < b.length; i++) {
            byte bi = b[i];
            if (val == bi) {
                return i;
            } else if (bi == 0) {
                return -1; //terminate early
            }
        }
        return -1;
    }

    public static Term conjSeq(FasterList<LongObjectPair<Term>> events) {
        return conjSeq(events, 0, events.size());
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * assumes that all of the event terms have distinct occurrence times
     */
    public static Term conjSeq(List<LongObjectPair<Term>> events, int start, int end) {

        LongObjectPair<Term> first = events.get(start);
        int ee = end - start;
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first.getTwo();
            case 2:
                LongObjectPair<Term> second = events.get(end - 1);
                return conjSeqFinal(
                        (int) (second.getOne() - first.getOne()),
                        /* left */ first.getTwo(), /* right */ second.getTwo());
        }

        int center = start + (end - 1 - start) / 2;


        Term left = conjSeq(events, start, center + 1);
        if (left == Null) return Null;
        if (left == False) return False; //early fail shortcut

        Term right = conjSeq(events, center + 1, end);
        if (right == Null) return Null;
        if (right == False) return False; //early fail shortcut

        int dt = (int) (events.get(center + 1).getOne() - first.getOne() - left.dtRange());

        return conjSeqFinal(dt, left, right);
    }

    private static Term conjSeqFinal(int dt, Term left, Term right) {
        assert (dt != XTERNAL);

        if (left == False) return False;
        if (left == Null) return Null;
        if (left == True) return right;

        if (right == False) return False;
        if (right == Null) return Null;
        if (right == True) return left;

        if (dt == 0 || dt == DTERNAL) {
            if (left.equals(right)) return left;
            if (left.equalsNeg(right)) return False;

            //return CONJ.the(dt, left, right); //send through again
        }


        //System.out.println(left + " " + right + " " + left.compareTo(right));
        //return CONJ.the(dt, left, right);
        if (left.compareTo(right) > 0) {
            //larger on left
            dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }

        if (left.op() == CONJ && right.op() == CONJ) {
            int ldt = left.dt(), rdt = right.dt();
            if (ldt != XTERNAL && !concurrent(ldt) && rdt != XTERNAL && !concurrent(rdt)) {
                int ls = left.subs(), rs = right.subs();
                if ((ls > 1 + rs) || (rs > ls)) {
                    //seq imbalance; send through again
                    return CONJ.the(dt, left, right);
                }
            }
        }


        return instance(CONJ, dt, left, right);
    }

    /**
     * returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .term() should be considered final
     */
    public boolean add(Term t, long at) {

        if (term != null)
            throw new RuntimeException("already terminated");

        if (t == True)
            return true; //ignore
        else if (t == False) {
            this.term = False;
            return false;
        } else if (t == Null) {
            this.term = Null;
            return false;
        }


        Op x = t.op();
        boolean polarity;
        if (x == NEG) {
            t = t.unneg();
            polarity = false;
        } else {
            polarity = true;
        }

        int dt;
        boolean atEternal = at == ETERNAL;
        if (x == CONJ && (dt = t.dt()) != XTERNAL
                && (dt != DTERNAL || atEternal)
                && (dt != 0 || !atEternal)
                && (!atEternal || (dt == DTERNAL))
                ) {

//            try {
            return t.eventsWhile((w, e) -> add(e, w),
                    at,
                    !atEternal,
                    atEternal, //only decompose DTERNAL if in the ETERNAL context, otherwise they are embedded as events
                    false, 0);
//            } catch (StackOverflowError e) {
//                System.err.println(t + " " + at + " " + dt);
//                throw new RuntimeException(t + " should not have recursed");
//            }
        } else {

            int id = add(t);
            if (!polarity)
                id = -id;


            if (!addIfValid(at, id)) {
                //contradiction
                term = False;
                return false;
            }

            return true;
        }
    }

    public boolean add(Term t, long start, long end, int maxSamples, int minSegmentLength) {
        if ((start == end) || start == ETERNAL) {
            return add(t, start);
        } else {
            if (maxSamples == 1) {
                //add at the midpoint
                return add(t, (start + end) / 2L);
            } else {
                //compute segment length
                long dt = Math.max(minSegmentLength, (end - start) / maxSamples);
                long x = start;
                while (x < end) {
                    if (!add(t, x))
                        return false;
                    x += dt;
                }
                return true;
            }
        }
    }

    protected boolean addIfValid(long at, int id) {
        Object what = event.getIfAbsentPut(at, () -> new byte[ROARING_UPGRADE_THRESH]);
        if (what instanceof RoaringBitmap) {
            RoaringBitmap r = (RoaringBitmap) what;
            if (!r.contains(-id)) {
                r.add(id);
                return true;
            }
        } else {
            byte[] ii = (byte[]) what;
            if (indexOfZeroTerminated(ii, ((byte) -id)) == -1) {
                int nextSlot = indexOfZeroTerminated(ii, (byte) 0);
                if (nextSlot != -1) {
                    ii[nextSlot] = (byte) id;
                } else {
                    //upgrade to roaring and add
                    RoaringBitmap rb = new RoaringBitmap();
                    for (byte b : ii)
                        rb.add(b);
                    rb.add(id);
                    event.put(at, rb);
                }
                return true;
            }
        }
        return false;
    }

    public int add(Term t) {
        assert (t != null && !(t instanceof Bool));
        return terms.getIfAbsentPutWithKey(t.unneg(), tt -> {
            int s = terms.size();
            termsIndex.add(tt);
            assert (s < Byte.MAX_VALUE);
            return (byte) s;
        }) + 1;
    }

//    private byte id(long w) {
//
//        int i = times.indexOf(w);
//        if (i!=-1) {
//            return (byte) i;
//        } else {
//            int s = times.size();
//            assert(s < Byte.MAX_VALUE);
//            times.add(w);
//            return (byte)s;
//        }
//    }
//
//    short id(Term t, long w) {
//        byte tb = id(t);
//        byte wb = id(w);
//        return (short) ((tb << 8) | wb);
//    }
//
//    byte termIndex(short s) {
//        return (byte) ((s >> 8) & 0xff);
//    }
//    byte timeIndex(short s) {
//        return (byte) (s & 0xff);
//    }

    public boolean remove(Term t, long at) {


        Object o = event.get(at);
        if (o == null)
            return false; //nothing at that time

        boolean neg = t.op() == NEG;
        int i = add(t); //should be get(), add doesnt apply
        if (neg)
            i = -i;
        return removeFromEvent(at, o, i);
    }

    private boolean removeFromEvent(long at, Object o, int... i) {
        if (o instanceof RoaringBitmap) {
            boolean b = false;
            for (int ii : i)
                b |= ((RoaringBitmap) o).checkedRemove(ii);
            return b;
        } else {
            byte[] b = (byte[]) o;
            boolean removed = false;
            for (int ii : i) {
                int bi = ArrayUtils.indexOf(b, (byte) ii);
                if (bi != -1) {
                    b = ArrayUtils.remove(b, bi);
                    removed = true;
                }
            }
            if (removed) {
                event.put(at, b);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean removeAll(Term t, boolean pos, boolean neg) {

        boolean negateInput;
        if (t.op() == NEG) {
            negateInput = true;
            t = t.unneg();
        } else {
            negateInput = false;
        }

        int i = add(t); //should be get(), add doesnt apply
        int[] ii;
        if (pos && neg) {
            ii = new int[] { i, -i };
        } else if (pos) {
            ii = new int[] { negateInput ? -i : i };
        } else if (neg) {
            ii = new int[] { negateInput ? i : -i };
        } else {
            throw new UnsupportedOperationException();
        }

        final boolean[] removed = {false};
        event.forEachKeyValue((when, o) -> {
            removed[0] |= removeFromEvent(when, o, ii);
        });
        return removed[0];
    }

    public Term term() {
        if (term != null)
            return term;


        int numTimes = event.size();
        switch (numTimes) {
            case 0:
                return Null;
            case 1:
                break;
            default:
                break;
        }

        //event.compact();


        IntPredicate validator = null;
        Object eternalWhat = event.get(ETERNAL);
        Term eternal = term(ETERNAL, eternalWhat);
        if (eternal != null) {

            if (eternal instanceof Bool)
                return this.term = eternal; //override and terminates

            if (numTimes > 1) {
                //temporal components follow, so build the verifier:

                if (eternal.op() == CONJ) {
                    //Subterms eteSub = eternal.subterms();
                    if (eternalWhat instanceof byte[]) {
                        byte[] b = (byte[]) eternalWhat;
                        validator = (i) -> indexOfZeroTerminated(b, (byte) -i) == -1;
                    } else {
                        RoaringBitmap b = (RoaringBitmap) eternalWhat;
                        validator = (i) -> !b.contains(-i);
                    }
                } else {
                    Term finalEternal = eternal;
                    validator = (t) -> !finalEternal.equalsNeg(termsIndex.get(Math.abs(t - 1)).negIf(t < 0));
                }
            }
        }

        Term ci;
        if (eternal != null && numTimes == 1) {
            ci = eternal;
        } else {
            FasterList<LongObjectPair<Term>> e = new FasterList<>(numTimes - (eternal != null ? 1 : 0));
            Iterator<LongObjectPair> ii = event.keyValuesView().iterator();
            while (ii.hasNext()) {
                LongObjectPair next = ii.next();
                long when = next.getOne();
                if (when == ETERNAL)
                    continue; //already handled above

                Term wt = term(when, next.getTwo(), validator);

                if (wt == True) {
                    continue; //canceled out
                } else if (wt == False) {
                    return this.term = False; //short-circuit false
                } else if (wt == Null) {
                    return this.term = Null; //short-circuit null
                }

                e.add(pair(when, wt));
            }

            int ee = e.size();
            Term temporal;
            switch (ee) {
                case 0:
                    return True;
                case 1:
                    temporal = e.get(0).getTwo();
                    break;
                default:
                    e.sortThisBy(LongObjectPair::getOne);
                    temporal = conjSeq(e);
                    break;
            }

            ci = eternal != null ?
                    //CONJ.the(DTERNAL, sorted(temporal, eternal))
                    CONJ.instance(DTERNAL, sorted(temporal, eternal))
                    //instance(CONJ, DTERNAL, sorted(temporal, eternal))
                    :
                    temporal;
        }


        //End Stage Reduction(s)

        //(NOT (x AND y)) AND (NOT x) == NOT X
        //http://www.wolframalpha.com/input/?i=(NOT+(x+AND+y))+AND+(NOT+x)
        if (ci.op() == CONJ && ci.hasAny(NEG)) {
            Subterms cci;
            if ((cci = ci.subterms()).hasAny(CONJ)) {
                int ciDT = ci.dt();
                if (ciDT == 0 || ciDT == DTERNAL) {
                    int s = cci.subs();
                    RoaringBitmap ni = null, nc = null;
                    for (int i = 0; i < s; i++) {
                        Term cii = cci.sub(i);
                        if (cii.op() == NEG) {
                            Term cInner = cii.unneg();
                            if (cInner.op() == CONJ && cInner.dt() == ciDT /* same DT */) {
                                if (nc == null) nc = new RoaringBitmap();
                                nc.add(i);
                            } else {
                                if (ni == null) ni = new RoaringBitmap();
                                ni.add(i);
                            }
                        }
                    }
                    if (nc != null && ni != null) {
                        int[] bb = ni.toArray();
                        MetalBitSet toRemove = MetalBitSet.bits(bb.length);
                        PeekableIntIterator ncc = nc.getIntIterator();
                        while (ncc.hasNext()) {
                            int nccc = ncc.next();
                            for (int j = 0; j < bb.length; j++) {
                                Term NC = cci.sub(nccc).unneg();
                                Term NX = cci.sub(bb[j]).unneg();
                                if (NC.contains(NX)) {
                                    toRemove.set(nccc);
                                }
                            }
                        }
                        if (toRemove.getCardinality() > 0) {
                            return CONJ.the(ciDT, cci.termsExcept(toRemove));
                        }
                    }


                }
            }
        }
        return ci;
    }

    public long shift() {
        long min = Long.MAX_VALUE;
        LongIterator ii = event.keysView().longIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != DTERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min;
    }

    public Term term(long when) {
        return term(when, event.get(when), null);
    }

    private Term term(long when, Object what) {
        return term(when, what, null);
    }

    private Term term(long when, Object what, IntPredicate validator) {

        if (what == null) return null;

        final RoaringBitmap rb;
        final byte[] b;
        int n;
        if (what instanceof byte[]) {
            b = (byte[]) what;
            rb = null;
            n = indexOfZeroTerminated(b, (byte) 0);
            if (n == 1) {
                //simplest case
                return sub(b[0], null, validator);
            }
        } else {
            rb = (RoaringBitmap) what;
            b = null;
            n = rb.getCardinality();
        }


        final boolean[] negatives = {false};
        TreeSet<Term> t = new TreeSet();
        if (b != null) {
            for (byte x : b) {
                if (x == 0)
                    break; //done
                t.add(sub(x, negatives, validator));
            }
        } else {
            rb.forEach((int termIndex) -> {
                t.add(sub(termIndex, negatives, validator));
            });
        }

        if (negatives[0] && n > 1) {
            //annihilate common terms inside and outside of disjunction
            //      ex:
            //          -X &&  ( X ||  Y)
            //          -X && -(-X && -Y)  |-   -X && Y
            Iterator<Term> oo = t.iterator();
            List<Term> csa = null;
            while (oo.hasNext()) {
                Term x = oo.next();
                if (x.hasAll(NEG.bit | CONJ.bit)) {
                    if (x.op() == NEG) {
                        Term x0 = x.sub(0);
                        if (x0.op() == CONJ && CONJ.commute(x0.dt(), x0.subs())) { //DISJUNCTION
                            Term disj = x.unneg();
                            SortedSet<Term> disjSubs = disj.subterms().toSetSorted();
                            //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                            if (disjSubs.removeAll(t)) {
                                //reconstruct disj if changed
                                oo.remove();

                                if (!disjSubs.isEmpty()) {
                                    if (csa == null)
                                        csa = new FasterList(1);
                                    csa.add(
                                            CONJ.the(disj.dt(), sorted(disjSubs)).neg()
                                    );
                                }
                            }
                        }
                    }
                }
            }
            if (csa != null)
                t.addAll(csa);
        }

        int ts = t.size();
        switch (ts) {
            case 0:
                //throw new RuntimeException("fault");
                return True;
            case 1:
                return t.first();
            default: {
                int dt;
                if (when == ETERNAL) {
                    dt = DTERNAL;
                } else {
//                    if (when == XTERNAL)
//                        dt = XTERNAL;
//                    else
                    dt = 0; //same time
                }
                return
                        //CONJ.the(
                        Op.instance(CONJ,
                                dt,
                                sorted(t));
                //t);
            }
        }
    }

    public Term sub(int termIndex) {
        return sub(termIndex, null, null);
    }

    public Term sub(int termIndex, @Nullable boolean[] negatives, @Nullable IntPredicate validator) {
        assert (termIndex != 0);

        boolean neg = false;
        if (termIndex < 0) {
            termIndex = -termIndex;
            neg = true;
        }

        if (validator != null && !validator.test(termIndex))
            return False;

        Term c = termsIndex.get(termIndex - 1);
        if (neg) {
            c = c.neg();
            if (negatives != null)
                negatives[0] = true;
        }
        return c;
    }

//    public void forEachTerm(Object what, Consumer<Term> each) {
//        if (what instanceof byte[]) {
//            byte[] b = (byte[])what;
//            for (byte termIndex : b) {
//                if (termIndex == 0)
//                    break; //done
//                each.accept(sub(termIndex));
//            }
//        } else {
//            ((RoaringBitmap)what).forEach((int termIndex) -> {
//                each.accept(sub(termIndex));
//            });
//        }
//    }
}
