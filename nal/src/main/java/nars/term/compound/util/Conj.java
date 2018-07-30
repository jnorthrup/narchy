package nars.term.compound.util;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.task.Revision;
import nars.term.Term;
import nars.term.atom.Bool;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.ImmutableBitmapDataProvider;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.IntPredicate;

import static nars.Op.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * representation of conjoined (eternal, parallel, or sequential) events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing
 */
public class Conj extends ByteAnonMap {


    public static final int ROARING_UPGRADE_THRESH = 8;


    public final LongObjectHashMap event;


    /**
     * state which will be set in a terminal condition, or upon term construction in non-terminal condition
     */
    private Term term = null;

    public Conj() {
        this(4);
    }

    public Conj(int n) {
        super(n);
        event = new LongObjectHashMap<>(n);
    }

    public static int eventCount(Object what) {
        if (what instanceof byte[]) {
            return indexOfZeroTerminated((byte[]) what, (byte) 0);
        } else {
            return ((ImmutableBitmapDataProvider) what).getCardinality();
        }
    }

    /**
     * TODO impl levenshtein via byte-array ops
     */
    public static StringBuilder sequenceString(Term a, Conj x) {
        StringBuilder sb = new StringBuilder(4);
        int range = a.dtRange();
        final float stepResolution = 16f;
        float factor = stepResolution / range;
        a.eventsWhile((when, what) -> {
            int step = Math.round(when * factor);
            sb.append((char) step);

            if (what.op() == NEG)
                sb.append('-'); //since x.add(what) will store the unneg id
            sb.append(((char) x.add(what)));
            return true;
        }, 0, true, true, false, 0);

        return sb;
    }

    /**
     * returns null if wasnt contained, Null if nothing remains after removal
     */
    @Nullable
    public static Term conjDrop(Term conj, Term event, boolean earlyOrLate, boolean filterContradiction) {
        if (conj.op() != CONJ || conj.impossibleSubTerm(event))
            return Null;

        int cdt = conj.dt();

        if (cdt == DTERNAL || cdt == 0) {
            Term[] csDropped = conj.subterms().termsExcept(event);
            if (csDropped != null) {
                if (csDropped.length == 1)
                    return csDropped[0];
                else
                    return CONJ.the(cdt, csDropped);
            }
        } else {

            Conj c = Conj.from(conj);

            /* check that event.neg doesnt occurr in the result.
                for use when deriving goals.
                 since it would be absurd to goal the opposite just to reach the desired later
                 */
            byte id = c.get(event);
            if (id == Byte.MIN_VALUE)
                return conj; //not found


            long targetTime;
            if (c.event.size() == 1) {

                targetTime = c.event.keysView().longIterator().next();
            } else if (earlyOrLate) {
                Object eternalTemporarilyRemoved = c.event.remove(ETERNAL); //HACK
                targetTime = c.event.keysView().min();
                if (eternalTemporarilyRemoved != null) c.event.put(ETERNAL, eternalTemporarilyRemoved); //UNDO HACK
            } else {
                targetTime = c.event.keysView().max();
            }
            assert (targetTime != XTERNAL);

            if (filterContradiction) {


                byte idNeg = (byte) -id;

                final boolean[] contradiction = {false};
                c.event.forEachKeyValue((w, wh) -> {
                    if (w == targetTime || contradiction[0])
                        return; //HACK should return early via predicate method

                    if ((wh instanceof byte[] && ArrayUtils.indexOf((byte[]) wh, idNeg) != -1)
                            ||
                            (wh instanceof RoaringBitmap && ((RoaringBitmap) wh).contains(idNeg)))
                        contradiction[0] = true;
                });
                if (contradiction[0])
                    return Null;
            }


            boolean removed = c.remove(event, targetTime);
            if (!removed) {
                return Null;
            }

            return c.term();
        }

        return conj; //no change

    }

    public static FasterList<LongObjectPair<Term>> eventList(Term t) {
        return t.eventList(t.dt() == DTERNAL ? ETERNAL : 0, 1, true, true);
    }

    public static Conj from(Term t) {
        Conj x = new Conj();
        x.addAuto(t);
        return x;
    }

    public boolean addAuto(Term t) {
        return add(t.dt() == DTERNAL ? ETERNAL : 0, t);
    }

//    private static Conj from(Term t, long rootTime) {
//        Conj x = new Conj();
//        x.add(rootTime, t);
//        return x;
//    }

    public static Term conj(FasterList<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return Null;
            case 1:
                return events.get(0).getTwo();
        }

        Conj ce = new Conj(eventsSize);

        for (LongObjectPair<Term> o : events) {
            if (!ce.add(o.getOne(), o.getTwo())) {
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
            if (!ce.add(o.getOne(), o.getTwo())) {
                break;
            }
        }

        return ce.term();
    }

    public static Term without(Term include, Term exclude, boolean includeNeg) {
        if (include.equals(exclude))
            return True;
        if (includeNeg && include.equalsNeg(exclude))
            return True;
        if (include.op() != CONJ || include.impossibleSubTerm(exclude))
            return include;

        Conj xx = Conj.from(include);
        if (xx.removeEventsByTerm(exclude, true, includeNeg)) {
            return xx.term();
        } else {
            return include;
        }
    }

    public static Term withoutAll(Term include, Term exclude) {
        if (include.op() != CONJ)
            return include;

        if (include.equals(exclude))
            return True;

        Conj x = Conj.from(include);
        int edt = include.dt();
        boolean[] removed = new boolean[]{false};
        exclude.eventsWhile((when, what) -> {
            removed[0] |= x.remove(what, when);
            return true;
        }, edt == DTERNAL ? ETERNAL : 0, true, edt == DTERNAL, false, 0);

        return removed[0] ? x.term() : include;
    }

    public static Term conjMerge(Term a, Term b, int dt) {
        return (dt >= 0) ?
                conjMerge(a, 0, b, +dt + a.dtRange()) :
                conjMerge(b, 0, a, -dt + b.dtRange());
    }

    static public Term conjMerge(Term a, long aStart, Term b, long bStart) {
        Conj c = new Conj();
        if (aStart == bStart) {
            if (c.addAuto(a)) {
                c.addAuto(b);
            }
        } else {
            if (c.add(aStart, a)) {
                c.add(bStart, b);
            }
        }
        return c.term();
    }

    private static int indexOfZeroTerminated(byte[] b, byte val) {
        for (int i = 0; i < b.length; i++) {
            byte bi = b[i];
            if (val == bi)
                return i;
            else if (bi == 0)
                return -1;
        }
        return -1;
    }

    private static Term conjSeq(FasterList<LongObjectPair<Term>> events) {
        return conjSeq(events, 0, events.size());
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * assumes that all of the event terms have distinct occurrence times
     */
    private static Term conjSeq(List<LongObjectPair<Term>> events, int start, int end) {

        LongObjectPair<Term> first = events.get(start);
        int ee = end - start;
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first.getTwo();
            case 2: {
                LongObjectPair<Term> second = events.get(end - 1);
                long a = first.getOne();
                long b = second.getOne();
                return conjSeqFinal(
                        (int) (b - a),
                        /* left */ first.getTwo(), /* right */ second.getTwo());
            }
        }

        int center = start + (end - 1 - start) / 2;


        Term left = conjSeq(events, start, center + 1);
        if (left == Null) return Null;
        if (left == False) return False;

        Term right = conjSeq(events, center + 1, end);
        if (right == Null) return Null;
        if (right == False) return False;

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

//        if (dt == 0 || dt == DTERNAL) {
//            return CONJ.the(left, dt, right);
//        }


        if (left.compareTo(right) > 0) {

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

                    return CONJ.the(dt, new Term[]{left, right});
                }
            }
        }


        return Op.compoundExact(CONJ, dt, left, right);
    }

    /**
     * similar to conjMerge but interpolates events so the resulting
     * intermpolation is not considerably more complex than either of the inputs
     * assumes a and b are both conjunctions
     */
    public static Term conjIntermpolate(Term a, Term b, float aProp, long bOffset, NAR nar) {

        if (bOffset == 0 && a.subterms().equals(b.subterms())) {
            //special case: equal subs
            return a.dt(Revision.chooseDT(a, b, aProp, nar));
        }

        return new Conjterpolate(a, b, bOffset, aProp, nar).term();
    }

    public void clear() {
        super.clear();
        event.clear();
        term = null;
    }

//    static int conflictOrSame(Object e, int id) {
//        if (e == null) {
//
//        } else if (e instanceof RoaringBitmap) {
//            RoaringBitmap r = (RoaringBitmap) e;
//            if (r.contains(-id))
//                return -1;
//            else if (r.contains(id)) {
//                return +1;
//            }
//        } else if (e instanceof byte[]) {
//            byte[] r = (byte[]) e;
//            if (indexOfZeroTerminated(r, (byte) -id) != -1)
//                return -1;
//            else if (indexOfZeroTerminated(r, (byte) id) != -1)
//                return +1;
//        }
//        return 0;
//    }

    /**
     * returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .term() should be considered final
     */
    public boolean add(long at, Term what) {

        if (term != null)
            throw new RuntimeException("already term-inated to: " + term);

        if (what instanceof Bool) {
            //short circuits
            if (what == True)
                return true;
            else if (what == False) {
                this.term = False;
                return false;
            } else if (what == Null) {
                this.term = Null;
                return false;
            }
        }


        Op x = what.op();
        if (x == CONJ) {
            int dt = what.dt();
            if ((dt != XTERNAL) &&
                    (
                            (dt != DTERNAL ^ at == ETERNAL)
//                    (dt!=0 && dt!=DTERNAL) ||
//                    (dt == DTERNAL && at == ETERNAL) ||
//                    (dt == 0 && at != ETERNAL)
                    )
            ) {
                return what.eventsWhile(this::add,
                        at,
                        dt != DTERNAL,
                        dt == DTERNAL,
                        false, 0);
            }
        }

        boolean polarity;
        if (x == NEG) {
            what = what.unneg();
            polarity = false;
        } else {
            polarity = true;
        }

        byte id = add(what);
        if (!addIfValid(at, polarity ? id : (byte) -id)) {
            term = False;
            return false;
        } else {
            return true;
        }
    }

    public boolean add(Term t, long start, long end, int maxSamples, int minSegmentLength) {
        if ((start == end) || start == ETERNAL) {
            return add(start, t);
        } else {
            if (maxSamples == 1) {

                return add((start + end) / 2L, t);
            } else {

                long dt = Math.max(minSegmentLength, (end - start) / maxSamples);
                long x = start;
                while (x < end) {
                    if (!add(x, t))
                        return false;
                    x += dt;
                }
                return true;
            }
        }
    }

    private boolean addIfValid(long at, byte id) {

        Object what = event.getIfAbsentPut(at, () -> new byte[ROARING_UPGRADE_THRESH]);
        if (what instanceof byte[]) {
            byte[] b = (byte[]) what;
            for (int i = 0; i < b.length; i++) {
                byte bi = b[i];
                if (id == -bi)
                    return false; //contradiction
                if (id == bi)
                    return true; //found existing
                else if (bi == 0) {
                    //empty slot, take
                    b[i] = id;
                    return true;
                }
            }

            //no capacity, upgrade to RoaringBitmap

            RoaringBitmap rb = new RoaringBitmap();
            for (byte bb : b)
                rb.add(bb);
            rb.add(id);
            event.put(at, rb);

            return true;
        } else {
            RoaringBitmap r = (RoaringBitmap) what;
            if (!r.contains(-id)) {
                r.add(id);
                return true;
            }
        }
        return false;
    }

    /**
     * @return non-zero byte value
     */
    private byte add(Term t) {
        assert (t != null && !(t instanceof Bool));
        return termToId.getIfAbsentPutWithKey(t.unneg(), tt -> {
            //int s = termToId.size();
            int s = idToTerm.addAndGetSize(tt);
            assert (s < Byte.MAX_VALUE);
            return (byte) s;
        });
    }

    /**
     * returns index of an item if it is present, or -1 if not
     */
    private byte index(Term t) {
        return termToId.getIfAbsent(t.unneg(), (byte) -1);
    }

    private byte get(Term x) {
        boolean neg;
        if (neg = (x.op() == NEG))
            x = x.unneg();
        byte index = index(x);
        if (index == -1)
            return Byte.MIN_VALUE;

        if (neg)
            index = (byte) (-index);

        return index;
    }

    public boolean remove(Term t, long at) {

        Object o = event.get(at);
        if (o == null)
            return false;


        int i = get(t);
        if (i == Byte.MIN_VALUE)
            return false;

        if (removeFromEvent(at, o, true, i) != 0) {
            term = null;
            return true;
        }
        return false;
    }


    /**
     * returns:
     * +2 removed, and now this event time is empty
     * +1 removed
     * +0 not removed
     */
    private int removeFromEvent(long at, Object o, boolean autoRemoveIfEmpty, int... i) {
        if (o instanceof RoaringBitmap) {
            boolean b = false;
            RoaringBitmap oo = (RoaringBitmap) o;
            for (int ii : i)
                b |= oo.checkedRemove(ii);
            if (!b) return 0;
            if (oo.isEmpty()) {
                if (autoRemoveIfEmpty)
                    event.remove(at);
                return 2;
            } else {
                return 1;
            }
        } else {
            byte[] b = (byte[]) o;

            int num = ArrayUtils.indexOf(b, (byte) 0);
            if (num == -1) num = b.length;

            int removals = 0;
            for (int ii : i) {
                int bi = ArrayUtils.indexOf(b, (byte) ii);
                if (bi != -1) {
                    if (b[bi] != 0) {
                        b[bi] = 0;
                        removals++;
                    }
                }
            }

            if (removals == 0)
                return 0;
            else if (removals == num) {
                if (autoRemoveIfEmpty)
                    event.remove(at);
                return 2;
            } else {


                MetalBitSet toRemove = MetalBitSet.bits(b.length);

                for (int zeroIndex = 0; zeroIndex < b.length; zeroIndex++) {
                    if (b[zeroIndex] == 0)
                        toRemove.set(zeroIndex);
                }

                b = ArrayUtils.removeAll(b, toRemove);
                event.put(at, b);
                return 1;
            }
        }
    }

    private boolean removeEventsByTerm(Term t, boolean pos, boolean neg) {

        boolean negateInput;
        if (t.op() == NEG) {
            negateInput = true;
            t = t.unneg();
        } else {
            negateInput = false;
        }

        int i = get(t);
        if (i == Byte.MIN_VALUE)
            return false;

        int[] ii;
        if (pos && neg) {
            ii = new int[]{i, -i};
        } else if (pos) {
            ii = new int[]{negateInput ? -i : i};
        } else if (neg) {
            ii = new int[]{negateInput ? i : -i};
        } else {
            throw new UnsupportedOperationException();
        }


        final boolean[] removed = {false};
        LongArrayList eventsToRemove = new LongArrayList(4);
        event.forEachKeyValue((when, o) -> {
            int result = removeFromEvent(when, o, false, ii);
            if (result == 2) {
                eventsToRemove.add(when);
            }
            removed[0] |= result > 0;
        });

        if (!eventsToRemove.isEmpty()) {
            eventsToRemove.forEach(event::remove);
        }

        if (removed[0]) {
            term = null;
            return true;
        }
        return false;
    }

    public Term term() {
        if (term != null)
            return term;


        int numTimes = event.size();
        if (numTimes == 0)
            return True;


        IntPredicate validator = null;
        Object eternalWhat = event.get(ETERNAL);
        final Term eternal = eternalWhat != null ? term(ETERNAL, eternalWhat) : null;
        if (eternal != null) {

            if (eternal instanceof Bool)
                return this.term = eternal;

            if (numTimes > 1) {


                if (eternal.op() == CONJ) {

                    if (eternalWhat instanceof byte[]) {
                        byte[] b = (byte[]) eternalWhat;
                        validator = (i) -> indexOfZeroTerminated(b, (byte) -i) == -1;
                    } else {
                        RoaringBitmap b = (RoaringBitmap) eternalWhat;
                        validator = (i) -> !b.contains(-i);
                    }
                } else {
                    validator = (t) -> !eternal.equalsNeg(idToTerm.get(Math.abs(t - 1)).negIf(t < 0));
                }
            }
        }

        Term ci;
        if (eternal != null && numTimes == 1) {
            ci = eternal;
        } else {
            FasterList<LongObjectPair<Term>> temporals = new FasterList<>(numTimes - (eternal != null ? 1 : 0));
            for (LongObjectPair<Term> next : (Iterable<LongObjectPair<Term>>) event.keyValuesView()) {
                long when = next.getOne();
                if (when == ETERNAL)
                    continue;

                Term wt = term(when, next.getTwo(), validator);

                if (wt == True) {
                    continue;
                } else if (wt == False) {
                    return this.term = False;
                } else if (wt == Null) {
                    return this.term = Null;
                }

                temporals.add(pair(when, wt));
            }

            Term temporal;
            int ee = temporals.size();
            switch (ee) {
                case 0:
                    temporal = null;
                    break;
                case 1:
                    temporal = temporals.get(0).getTwo();
                    break;
                default:
                    temporals.sortThisBy(LongObjectPair::getOne);
                    temporal = conjSeq(temporals);
                    break;
            }

            if (eternal != null && temporal != null) {
                ci = CONJ.the(temporal, eternal);
            } else if (eternal == null) {
                ci = temporal;
            } else /*if (temporal == null)*/ {
                ci = eternal;
            }
        }

        if (ci == null)
            return True;

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
                            for (int aBb : bb) {
                                Term NC = cci.sub(nccc).unneg();
                                Term NX = cci.sub(aBb).unneg();
                                if (NC.contains(NX)) {
                                    toRemove.set(nccc);
                                }
                            }
                        }
                        if (toRemove.cardinality() > 0) {
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

        final RoaringBitmap rb;
        final byte[] b;
        int n;
        if (what instanceof byte[]) {
            rb = null;
            b = (byte[]) what;
            n = indexOfZeroTerminated(b, (byte) 0); //TODO cardinality(byte[] b)
        } else {
            b = null;
            rb = (RoaringBitmap) what;
            n = rb.getCardinality();
        }

        if (n == 0) {
            return True; //does this happen?
        }
        if (n == 1) {
            //only event at this time
            return sub(b[0], null, validator);
        }

        TreeSet<Term> pt = null, t = null;
//        do {
        pt = t;
        t = new TreeSet<>();
        final boolean[] negatives = {false};
        if (b != null) {
            for (byte x : b) {
                if (x == 0)
                    break;
                Term s = sub(x, negatives, validator);
                if (s == Null || s == False) {
                    return s;
                }
                t.add(s);
            }
        } else {
            TreeSet<Term> tt = t;
            rb.forEach((int termIndex) ->
                    tt.add(sub(termIndex, negatives, validator))
            );
        }

        if (negatives[0]) {
            factorDisj(t, when);
        }

//        } while (!t.equals(pt));


        int ts = t.size();
        switch (ts) {
            case 0:
                return True;
            case 1:
                return t.first();
            default: {
                Term theSequence = null;
                {
                    List<Term> sequences = new FasterList(1);
                    for (Term x : t) {
                        if (x.op() == CONJ) {
                            switch (x.dt()) {
                                case DTERNAL:
                                    if (when == ETERNAL)
                                        sequences.add(x);
                                    break;
                                case 0:
                                case XTERNAL:
                                default:
                                    sequences.add(x);
                                    break;
                            }
                        }
                    }
                    int sn = sequences.size();
                    if (sn > 0 && sn < t.size()) {
                        t.removeAll(sequences);

                        if (sn > 1) {
                            Conj a = new Conj();

                            for (Term s : sequences) {
                                if (!a.add(when, s))
                                    return null;
                            }
                            theSequence = a.term();

                        } else if (sn == 1) {
                            theSequence = sequences.get(0);
                        }
                    } else {
                        theSequence = null; //dont do anything
                    }
                    if (theSequence == Null || theSequence == False)
                        return theSequence;
                    else {
                        if (theSequence == True)
                            theSequence = null;
                    }
                }

                if (t.isEmpty())
                    return theSequence;

                int dt;
                if (when == ETERNAL) {
//                    if (theSequence!=null && theSequence.dt() == 0)
//                        dt = 0; //exception for promoting dternal to parallel
//                    else
                        dt = DTERNAL;
                } else {
                    dt = 0;
                }


                Term z;
                if (t.size() > 1) {
                    Term[] tt = t.toArray(Op.EmptyTermArray); /* sorted iff t is SortedSet */
                    if (theSequence == null) {
//                        if (tt.length == 2) {
//                            assert (dt == 0 || dt == DTERNAL);
//                            return conjSeqFinal(dt, tt[0], tt[1]);
//                        } else {
////                        if (when == ETERNAL && jcog.Util.or((Term x) -> x.op()==CONJ && x.dt()==DTERNAL, tt)) {
////                            z = CONJ.the(dt, tt);
////                        } else {
////                            if (when == 0 && jcog.Util.or((Term x) -> x.op()==CONJ && x.dt()!=DTERNAL))
//                            //return CONJ.the(dt, tt);
////                            else
                        return Op.compoundExact(CONJ, dt, tt);
//                        }
                    } else {
//                        boolean complex = false;
//                        for (Term x : t) {
//                            if (x.hasAny(Op.CONJ)) {
//                                complex = true;
//                                break;
//                            }
//                        }
//                        if (complex) {
                        z = CONJ.the(dt, tt);
//                        } else {
//                            z = Op.compoundExact(CONJ, dt, tt);
//                        }
                    }
                } else {
                    z = t.first();
                }

                if (theSequence != null) {
                    if (theSequence.equals(z))
                        return theSequence;

                    int sdt = theSequence.dt();
                    if ((dt==DTERNAL || dt == XTERNAL || dt == 0) && (sdt == DTERNAL || sdt == XTERNAL || sdt == 0)) {
                        //both commutative

                        if ((dt == DTERNAL || dt == XTERNAL) && (sdt != DTERNAL && sdt !=XTERNAL))
                            dt = sdt; //most specific dt

                        SortedSet<Term> x = theSequence.subterms().toSetSorted();
                        if (z.op()==CONJ)
                            z.subterms().forEach(x::add);
                        else
                            x.add(z);

                        return Op.compoundExact(CONJ, dt, x.toArray(EmptyTermArray)); //concatenate in parallel
                    } else {
                        //Distribute (un-factor) z to each component of the sequence
                        Conj c = new Conj();
                        theSequence.eventsWhile((whn, wht) -> {
                            return c.add(whn, CONJ.the(0, wht, z));
                        }, 0, true /*false*/, false, true, 0);
                        return c.term();
                    }
                }

                return z;
            }
        }

    }

//    private void factorDisj0(TreeSet<Term> t) {
//        Iterator<Term> oo = t.iterator();
//        List<Term> csa = null;
//        while (oo.hasNext()) {
//            Term x = oo.next();
//            if (x.hasAll(NEG.bit | CONJ.bit)) {
//                if (x.op() == NEG) {
//                    Term disj = x.unneg();
//                    if (disj.op() == CONJ && commute(disj.dt(), disj.subs())) {
//                        SortedSet<Term> disjSubs = disj.subterms().toSetSortedExcept(t::contains);
//                        int ds = disjSubs.size();
//                        if (ds == disj.subs())
//                            continue; //no change
//
//                        oo.remove();
//
//                        if (ds > 0) {
//
//                            if (csa == null)
//                                csa = new FasterList<>(1);
//
//                            csa.add(CONJ.the(disj.dt(), disjSubs).neg());
//                        }
//                    }
//                }
//            }
//        }
//        if (csa != null)
//            t.addAll(csa);
//    }

    private static void factorDisj(TreeSet<Term> t, long when) {

        List<Term> d;
        boolean stable, stable2;
        do {
            stable2 = true;
            do {

                d = disjComponents(t);
                if (d == null)
                    return; //no change

                stable = true;

                //1. disj components sharing components that need factored out
                if (d.size() > 1) {
                    UnifiedMap<Term, MetalBitSet> components = new UnifiedMap();
                    final boolean[] hasMultiAppearance = {false};
                    for (int i = 0, dSize = d.size(); i < dSize; i++) {
                        Term dd = d.get(i);
                        int ii = i;
                        dd.unneg().subterms().forEach(ddd -> {
                            MetalBitSet ap = components.computeIfAbsent(ddd, dddd -> MetalBitSet.bits(dSize));
                            ap.set(ii);
                            if (ap.cardinality() > 1) {
                                hasMultiAppearance[0] = true;
                            }
                        });
                    }
                    if (hasMultiAppearance[0]) {
                        //select most commonly appearing, and process first.  then proceed to next until none left
                        Pair<Term, MetalBitSet> factorable = components.keyValuesView().select(p -> p.getTwo().cardinality() > 1).maxBy(p -> p.getTwo().cardinality());
                        Term f = factorable.getOne();
                        MetalBitSet b = factorable.getTwo();
                        SortedSet<Term> ff = new TreeSet<>(/*cardinality*/);
                        for (int i = 0; i < d.size(); i++) {
                            if (b.get(i)) {
                                Term di = d.get(i);
                                ff.add(Conj.without(di.unneg(), f, false));
                                boolean rdi = t.remove(di);
                                assert (rdi);
                            }
                        }

                        stable = false;

                        Term fff = CONJ.the(DTERNAL, f, CONJ.the(DTERNAL, ff).neg()).neg();
                        if (fff != True) {
                            if ((fff == False) || (fff == Null)) {
                                t.clear();
                                t.add(fff);
                                return;
                            } else {
                                t.add(fff);
                            }
                        }
                    }
                }
            } while (!stable);

            //2. disj components contradicting non-disjunctive components
            {

                //test for equal positives
                for (Iterator<Term> iterator = d.iterator(); iterator.hasNext(); ) {
                    Term disj = iterator.next();
                    if (hasEvent(disj.unneg(), t, when, true)) {
//                SortedSet<Term> disjSubs = dd.toSetSorted(ddd -> t.contains(ddd.neg()));
//                int ds = disjSubs.size();
//                if (ds > 0) {
                        //remove the entire disjunctive sub-expression
                        t.remove(disj);
                        iterator.remove();
                        stable2 = false;

                    }
                }

            }

            {
                List<Term> csa = null;

                //test for equal negatives
                for (Term disj : d) {

                    Term du = disj.unneg();


                    Term e = matchingEventsRemoved(du, t, when);
                    if (e != du) {
                        stable2 = false;

                        t.remove(disj);
                        if (csa == null)
                            csa = new FasterList<>(1);

                        Term f = e.neg();
                        if (f == False || f == Null) {
                            //short-circuit
                            t.clear();
                            t.add(f);
                            return;
                        }

                        if (f != True)
                            csa.add(f);
                    }


                }
                if (csa != null) {
                    t.addAll(csa);

                }

            }
        } while (!stable2);

    }

    private static Term matchingEventsRemoved(Term d, TreeSet<Term> t, long at) {
        if (d.dt() == DTERNAL /*commute(d.dt(), d.subs())*/) {
            SortedSet<Term> s = d.subterms().toSetSortedExcept(t::contains);
            if (s.size() < d.subs()) {
                return CONJ.the(d.dt(), s.toArray(EmptyTermArray));
            }
            return d;
        } else {
            Conj remain = new Conj();
            d.eventsWhile((when, what) -> {
                if (at == ETERNAL || when == at) {
                    if (!t.contains(what)) {
                        if (!remain.add(when, what))
                            return false;
                    }
                }
                return true;
            }, 0, true, true, true, 0);
            Term e = remain.term();
            if (e.equals(d)) {
                return d;
            }
            return e;
        }
    }

    private static boolean hasEvent(Term du, TreeSet<Term> t, long at, boolean neg) {
        if (commute(du.dt(), du.subs())) {
            return du.subterms().OR(ddd -> t.contains(ddd.negIf(neg)));
        } else {
            final boolean[] found = new boolean[1];
            du.eventsWhile((when, what) -> {
                if (at == ETERNAL || when == at) {
                    if (t.contains(what.negIf(neg))) {
                        found[0] = true;
                        return false;
                    }
                }
                return true;
            }, 0, false, true, false, 0);
            return found[0];
        }

    }

    @Nullable
    private static List<Term> disjComponents(Set<Term> t) {
        List<Term> d = null;
        for (Term x : t) {
            if (x.hasAll(NEG.bit | CONJ.bit)) {
                if (x.op() == NEG) {
                    Term disj = x.unneg();
                    if (disj.op() == CONJ) {
                        if (d == null)
                            d = new FasterList<>(t.size());
                        d.add(x);
                    }
                }
            }
        }
        return d;
    }


    private Term sub(int termIndex, @Nullable boolean[] negatives, @Nullable IntPredicate validator) {
        assert (termIndex != 0);

        boolean neg = termIndex < 0;
        if (neg) {
            termIndex = -termIndex;
        }

        if (validator != null && !validator.test(termIndex))
            return False;

        Term c = idToTerm.get(termIndex - 1);
        if (neg) {
            c = c.neg();
            if (negatives != null)
                negatives[0] = true;
        }
        return c;
    }

    /**
     * for each of b's events, find the nearest matching event in a while constructing a new Conj consisting of their mergers
     */
    static class Conjterpolate extends Conj {

//        private final Conj aa;
//        private final Term b;
//        private final NAR nar;

        /**
         * proportion of a vs. b, ie: (a/(a+b))
         */
        private final float aProp;

        Conjterpolate(Term a, Term b, long bOffset, float aProp, NAR nar) {

//            this.b = b;
//            this.nar = nar;

//            this.aa = Conj.from(a);
//            this.idToTerm.addAll(aa.idToTerm);
//            this.termToId.putAll(aa.termToId);

            this.aProp = aProp;


            addAuto(a);


            if (bOffset == 0)
                addAuto(b);
            else
                add(bOffset, b);

            compress(Math.max(a.volume(), b.volume()), Math.round(nar.intermpolationDurLimit.floatValue()*nar.dur()));

            //TODO simplify

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
//                        if (!add(when, idToTerm.get(ri-1).negIf(neg))) {
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
//                        if (!add(when, idToTerm.get(ri-1).negIf(neg))) {
//                            err[0] = true;
//                        }
//                    }
//                }
//            });
        }

        protected void compress(int targetVol, int interpolationThresh /* time units */) {
            if (interpolationThresh < 1)
                return;

            //find any two time points that differ by less than the interpolationThresh interval
            long[] times = this.event.keySet().toSortedArray();
            if (times.length < 2) return;
            for (int i = 1; i < times.length; i++) {
                if (times[i-1]==DTERNAL)
                    continue;
                long dt = times[i] - times[i-1];
                if (Math.abs(dt) <= interpolationThresh) {
                    if (combine(times[i-1], times[i])) {
                        i++; //skip past current pair
                    }
                }
            }
        }
        protected boolean combine(long a, long b) {
            assert(a!=b);
            assert(a!=DTERNAL && b!=DTERNAL && a!=XTERNAL && b!=XTERNAL);
            ByteHashSet common = new ByteHashSet();
            addAllTo(common, event.remove(a));
            addAllTo(common, event.remove(b));
            common.remove((byte) 0); //remove any '0' value from a byte[] array

            //detect conflicting combination
            byte[] ca = common.toArray();
            boolean changed = false;
            for (byte cc : ca) {
                if(cc < 0 && common.contains((byte) -cc)) {
                    common.remove(cc);
                    common.remove((byte) -cc);
                    changed = true;
                }
            }
            if (changed) {
                ca = common.toArray();
            }
            if (ca.length > 0) {
                long mid = (a + b) / 2L; //TODO better choice
                event.put(mid, ca);
            }
            return true;
        }

//        @Override
//        public boolean add(long bt, final Term what) {
//            assert (bt != XTERNAL);
//
//            {
//                boolean neg = what.op() == NEG;
//
//
//                byte tid = termToId.getIfAbsent(neg ? what.unneg() : what, (byte) -1);
//                if (tid == (byte) -1)
//                    return super.add(bt, what);
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
//                            whens.add(when);
//                        }
//                    } else {
//                        byte[] ii = (byte[]) wat;
//                        if (ArrayUtils.indexOf(ii, tInA) != -1 && ArrayUtils.indexOf(ii, (byte) -tInA) == -1) {
//                            whens.add(when);
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
//                            return super.add(merged, what);
//                        }
//
//                    } else {
//                        return true; //exact
//                    }
//                }
//                return super.add(bt, what);
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

    private static void addAllTo(ByteHashSet common, Object o) {
        if (o instanceof byte[])
            common.addAll((byte[])o);
        else {
            RoaringBitmap r = (RoaringBitmap)o;
            r.forEach((int x) -> common.add((byte) x));
        }
    }


}
