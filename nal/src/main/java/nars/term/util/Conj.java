package nars.term.util;

import jcog.TODO;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtils;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.primitive.ByteSets;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.ImmutableBitmapDataProvider;
import org.roaringbitmap.RoaringBitmap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static java.lang.System.arraycopy;
import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * representation of conjoined (eternal, parallel, or sequential) events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing
 */
public class Conj extends ByteAnonMap {


    public static final int ROARING_UPGRADE_THRESH = 8;

    /**
     * TermBuilder to use internally
     */
    private static final TermBuilder terms =
            //HeapTermBuilder.the;
            Op.terms;


    public final LongObjectHashMap<Object> event;


    /**
     * state which will be set in a terminal condition, or upon term construction in non-terminal condition
     */
    private Term term = null;

    public Conj() {
        this(2);
    }


    /**
     * but events are unique
     */
    public static Conj newConjSharingTermMap(Conj x) {
        return new Conj(x.termToId, x.idToTerm);
    }

    public Conj(ObjectByteHashMap<Term> x, FasterList<Term> y) {
        super(x, y);
        event = new LongObjectHashMap<>(2);
    }

    public Conj(int n) {
        super(n);
        event = new LongObjectHashMap<>(n);
    }

    public Conj(long initialEventAt, Term initialEvent) {
        this(1);
        add(initialEventAt, initialEvent);
    }

    public static Conj from(Term t) {
        Conj c = new Conj();
        c.addAuto(t);
        c.factor();
        return c;
    }

    public static MetalBitSet seqEternalComponents(Subterms x) {
        return x.subsTrue(Conj.isEternalComponent);
    }


    public boolean addAuto(Term t) {
        return add(t.dt() == DTERNAL ? ETERNAL : 0, t);
    }

    public static boolean containsOrEqualsEvent(Term container, Term x) {
        return container.equals(x) || containsEvent(container, x);
    }

    public static boolean containsEvent(Term container, Term x) {
        if (!x.op().eventable)
            return false;
        if (container.op() != CONJ || container.impossibleSubTerm(x))
            return false;

        if (container.contains(x))
            return true;
        if (isSeq(container)) {
            int xdt = x.dt();
            return !container.eventsWhile((when, cc) -> cc==container || !containsOrEqualsEvent(cc, x), //recurse
                    0, xdt!=0, xdt!=DTERNAL, true, 0);
        }

        return false;
    }

    public static boolean isEventFirstOrLast(Term container, Term x, boolean firstOrLast) {
        if (!x.op().eventable)
            return false;
        if (container.op() != CONJ || container.impossibleSubTerm(x))
            return false;

        boolean seq = isSeq(container);
        if (!seq) {
            return ConjCommutive.contains(container, x);
        } else {

            return ConjSeq.contains(container, x, firstOrLast);
        }

    }

    //    private static boolean isEventSequence(Term container, Term subseq, boolean neg, boolean firstOrLast) {
//        if (neg)
//            throw new TODO(); //may not even make sense
//
//        for (Term s : subseq.subterms())
//            if (!container.containsRecursively(s))
//                return false;
//
//        int containerDT = container.dt();
//        if (containerDT ==0 || containerDT ==DTERNAL || containerDT ==XTERNAL)
//            return true; //already met requirements since the container is unordered
//
//
//        //compare the correct order and whether it appears in prefix or suffix as appropriate
////        int range = container.eventRange();
//        long elimStart = Long.MAX_VALUE, elimEnd = Long.MIN_VALUE;
//        FasterList<LongObjectPair<Term>> events = container.eventList();
//        elimNext: for (Term s : subseq.subterms()) {
//            int n = events.size();
//            int start = firstOrLast ? 0 : n-1, inc = firstOrLast ? +1 : -1;
//            int k = start;
//            for (int i = 0; i < n; i++) {
//                LongObjectPair<Term> e = events.get(k);
//                if (e.getTwo().equals(s)) {
//                    long ew = e.getOne();
//                    elimStart = Math.min(elimStart, ew);
//                    elimEnd = Math.max(elimEnd, ew);
//                    events.remove(k);
//                    continue elimNext;
//                }
//                k += inc;
//            }
//            return false; //event not found
//        }
//
//        if (events.isEmpty()) {
//            //fully eliminated
//            return false;
//        }
//
//        for (LongObjectPair<Term> remain : events) {
//            long w = remain.getOne();
//            if (firstOrLast && w < elimStart)
//                return false;//there is a prior event
//            else if (!firstOrLast && w > elimEnd)
//                return false;//there is a later event
//        }
//
//        return true;
//    }


    public int eventCount(long when) {
        Object e = event.get(when);
        return e != null ? Conj.eventCount(e) : 0;
    }

    public static int eventCount(Object what) {
        if (what instanceof byte[]) {
            byte[] b = (byte[]) what;
            int i = indexOfZeroTerminated(b, (byte) 0);
            return i == -1 ? b.length : i;
        } else {
            if (what instanceof RoaringBitmap)
                return ((ImmutableBitmapDataProvider) what).getCardinality();
            else
                return 0;
        }
    }

//    /**
//     * TODO impl levenshtein via byte-array ops
//     */
//    public static StringBuilder sequenceString(Term a, Conj x) {
//        StringBuilder sb = new StringBuilder(4);
//        int range = a.eventRange();
//        final float stepResolution = 16f;
//        float factor = stepResolution / range;
//        a.eventsWhile((when, what) -> {
//            int step = Math.round(when * factor);
//            sb.append((char) step);
//
//            if (what.op() == NEG)
//                sb.append('-'); //since x.add(what) will store the unneg id
//            sb.append(((char) x.add(what)));
//            return true;
//        }, 0, true, true, false, 0);
//
//        return sb;
//    }

    /**
     * returns null if wasnt contained, True if nothing remains after removal
     */
    @Nullable
    public static Term withoutEarlyOrLate(Term conj, Term event, boolean earlyOrLate, boolean filterContradiction) {

        Op o = conj.op();
        if (o == NEG) {
            Term n = withoutEarlyOrLate(conj, event, earlyOrLate, filterContradiction);
            return n!=null ? n.neg() : null;
        }

        if (o == CONJ && !conj.impossibleSubTerm(event)) {
            if (isSeq(conj)) {
                Conj c = Conj.from(conj);
                if (c.dropEvent(event, earlyOrLate, filterContradiction))
                    return c.term();
            } else {
                Term[] csDropped = conj.subterms().subsExcluding(event);
                if (csDropped != null)
                    return (csDropped.length == 1) ? csDropped[0] : terms.conj(conj.dt(), csDropped);
            }
        }

        return null; //no change

    }


    private boolean dropEvent(final Term event, boolean earlyOrLate, boolean filterContradiction) {
    /* check that event.neg doesnt occurr in the result.
        for use when deriving goals.
         since it would be absurd to goal the opposite just to reach the desired later
         */
        byte id = get(event);
        if (id == Byte.MIN_VALUE)
            return false; //not found


        long targetTime;
        if (this.event.size() == 1) {

            targetTime = this.event.keysView().longIterator().next();
        } else if (earlyOrLate) {
            Object eternalTemporarilyRemoved = this.event.remove(ETERNAL); //HACK
            targetTime = this.event.keysView().min();
            if (eternalTemporarilyRemoved != null) this.event.put(ETERNAL, eternalTemporarilyRemoved); //UNDO HACK
        } else {
            targetTime = this.event.keysView().max();
        }
        assert (targetTime != XTERNAL);

        if (filterContradiction) {


            byte idNeg = (byte) -id;

            final boolean[] contradiction = {false};
            this.event.forEachKeyValue((w, wh) -> {
                if (w == targetTime || contradiction[0])
                    return; //HACK should return early via predicate method

                if ((wh instanceof byte[] && ArrayUtils.indexOf((byte[]) wh, idNeg) != -1)
                        ||
                        (wh instanceof RoaringBitmap && ((RoaringBitmap) wh).contains(idNeg)))
                    contradiction[0] = true;
            });
            if (contradiction[0])
                return false;
        }

        return this.remove(targetTime, event);
    }


    /**
     * means that the internal represntation of the term is concurrent
     */
    public static boolean concurrentInternal(int dt) {
        switch (dt) {
            case XTERNAL:
            case DTERNAL:
            case 0:
                return true;
        }
        return false;
    }

    /**
     * this refers to an internal concurrent representation but may not be consistent with all cases
     */
    @Deprecated
    public static boolean concurrent(int dt) {
        if (dt == XTERNAL)
            return true; //TEMPORARY
        switch (dt) {
            case XTERNAL:
            case DTERNAL:
            case 0:
                return true;
        }
        return false;
    }

    public static int conjEarlyLate(Term x, boolean earlyOrLate) {
        assert (x.op() == CONJ);
        int dt = x.dt();
        switch (dt) {
            case XTERNAL:
                throw new UnsupportedOperationException();

            case DTERNAL:
            case 0:
                return earlyOrLate ? 0 : 1;

            default: {


                return (dt < 0) ? (earlyOrLate ? 1 : 0) : (earlyOrLate ? 0 : 1);
            }
        }
    }

    public static Term negateEvents(Term x) {
        switch (x.op()) {
            case NEG:
                return negateEvents(x.unneg()).neg();
            case CONJ: {
                Conj c = Conj.from(x);
                c.negateEvents();
                return c.term();
            }
            default:
                return x.neg();
        }
    }

    public static Term dropAnyEvent(Term x, NAR nar) {
        Op oo = x.op();

        boolean negated = (oo == NEG);
        if (negated) {
            x = x.unneg();
            oo = x.op();
        }

        if (oo == IMPL) {
            boolean sNeg = x.sub(0).op()==NEG;
            Term s = x.sub(0);
            if (sNeg)
                s = s.unneg();
            Term ss = dropAnyEvent(s, nar);
            Term p = x.sub(1);

            if (ss instanceof Bool || s.equals(ss)) {
                Term pp = dropAnyEvent(p, nar);
                if (pp instanceof Bool || p.equals(pp))
                    return x; //no change

                //use Term.transform to shift any internal dt's appropriately
                return x.replace(x.sub(1), pp).negIf(negated);
            } else {
                //use Term.transform to shift any internal dt's appropriately
                return x.replace(x.sub(0), ss.negIf(sNeg)).negIf(negated);
            }
        }

        if (oo != CONJ)
            return Null;



        Random rng = nar.random();

        Term y;
        int dt = x.dt();
        if (!Conj.isSeq(x)) {
            Subterms xx = x.subterms();
            int ns = xx.subs();
            if (ns == 2) {
                //choose which one will remain
                y = xx.sub(rng.nextInt(ns));
            } else {
                y = terms.conj(dt, xx.subsExcluding(rng.nextInt(ns)));
            }
        } else {
            Conj c = from(x);
            c.distribute();
            long eventAt;
            if (c.event.size() == 1) {
                eventAt = c.event.keysView().longIterator().next();
            } else {
                //choose random event
                long[] events = c.event.keySet().toArray();
                if (events.length == 0)
                    throw new WTF();
                eventAt = events.length == 1 ? events[0] : events[rng.nextInt(events.length)];
            }
            Object event = c.event.get(eventAt);
            if (event instanceof byte[]) {
                int events = eventCount(event);
                byte b = ((byte[]) event)[rng.nextInt(events)];
                boolean removed = c.remove(eventAt, b);
                assert (removed);
            } else {
                throw new TODO();
            }
            y = c.term();
//                FasterList<LongObjectPair<Term>> ee = Conj.eventList(x);
//                ee.remove(nar.random().nextInt(ee.size()));
//                y = Conj.conj(ee);
//                if (y.equals(x))
//                    return Null;
//                assert (y != null);
        }
        return y.negIf(negated);
    }

    /**
     * whether the conjunction is a sequence (includes check for factored inner sequence)
     */
    public static boolean isSeq(Term conj) {
        if (conj.op() != CONJ)
            return false;

        int dt = conj.dt();

        if (!dtSpecial(dt))
            return true; //basic sequence

        if (dt == DTERNAL) {
            Subterms x = conj.subterms();
            return x.hasAny(CONJ) && x.subs(xx -> xx.op() == CONJ && xx.dt() != DTERNAL) == 1;
        }

        return false;
    }

    public static boolean isFactoredSeq(Term conj) {
        return conj.dt()==DTERNAL && isSeq(conj);
    }

    static final Predicate<Term> isTemporalComponent = x->x.op()==CONJ && x.dt()!=DTERNAL;
    public static final Predicate<Term> isEternalComponent = isTemporalComponent.negate();

    /** extracts the eternal components of a seq. assumes the conj actually has been determined to be a sequence */
    public static Term seqEternal(Term seq) {
        assert(seq.op()==CONJ && seq.dt()==DTERNAL);
        return seqEternal(seq.subterms());
    }

    public static Term seqEternal(Subterms ss) {
        return seqEternal(ss, ss.subsTrue(isEternalComponent));
    }

    public static Term seqEternal(Subterms ss, MetalBitSet m) {
        switch (m.cardinality()) {
            case 0: throw new WTF();
            case 1: return ss.sub(m.next(true, 0, Integer.MAX_VALUE));
            default:
                Term e = CONJ.the(ss.subsIncluding(m));
                assert(!(e instanceof Bool));
                return e;
        }
    }

    public static Term seqTemporal(Term seq) {
        assert(seq.op()==CONJ && seq.dt()==DTERNAL);
        return seqTemporal(seq.subterms());
    }

    public static Term seqTemporal(Subterms s) {
        Term t = s.subFirst(isTemporalComponent);
        assert(t.op()==CONJ);
        return t;
    }
    public static Term seqTemporal(Subterms s, MetalBitSet eternalComponents) {
        return s.sub(eternalComponents.next(false, 0, Integer.MAX_VALUE));
    }

    private void negateEvents() {
        event.forEachValue(x -> {
            if (!(x instanceof byte[]))
                throw new TODO();
            byte[] bx = (byte[]) x;
            for (int i = 0; i < bx.length; i++) {
                byte b = bx[i];
                if (b == 0) break; //null terminator
                bx[i] = (byte) -b;
            }
        });
    }


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

    public static Term without(Term include, Term exclude, boolean excludeNeg) {
        if (excludeNeg) {
            if (include.unneg().equals(exclude.unneg()))
                return True;
        } else {
            if (include.equals(exclude))
                return True;
        }

        if (include.op() != CONJ || include.impossibleSubTerm(excludeNeg ? exclude.unneg() : exclude))
            return include;


        int dt = include.dt();

        if (Conj.isSeq(include)) {

            Conj xx = Conj.from(include);
            if (xx.removeEventsByTerm(exclude, true, excludeNeg)) {
                return xx.term();
            } else {
                return include;
            }

        } else {
            Subterms s = include.subterms();
            //try positive first
            Term[] ss = s.subsExcluding(exclude);
            if (ss != null) {
                return ss.length > 1 ? terms.conj(dt, ss) : ss[0];
            } else {
                //try negative next
                if (excludeNeg) {
                    ss = s.subsExcluding(exclude.neg());
                    if (ss != null) {
                        return terms.conj(dt, ss);
                    }
                }

                return include; //not found
            }

        }

    }

    public static Term withoutAll(Term include, Term exclude) {
        return withoutAll(include, exclude, false);
    }

    /**
     * if same timing, does not allow relative shift
     */
    public static Term withoutAll(Term include, Term exclude, boolean atExactTime) {
        if (include.op() != CONJ)
            return include;

        if (include.equals(exclude))
            return True;

        if (exclude.op() != CONJ)
            return without(include, exclude, false);


        if (concurrent(include.dt()) && concurrent(exclude.dt())) {

            Subterms es = exclude.subterms();
            @Nullable MutableSet<Term> ii = include.subterms().toSet(i -> !es.contains(i));
            if (ii != null && (ii.size() < include.subs())) {
                return terms.conj(include.dt(), ii.toArray(EmptyTermArray));
            } else {
                return include; //no change
            }

        } else {


            Conj x = Conj.from(include);
            int idt = include.dt();
            boolean[] removed = new boolean[]{false};
            exclude.eventsWhile((when, what) -> {
                //removed[0] |= x.remove(what, when);
                removed[0] |= atExactTime ? x.remove(when, what) : x.removeAll(what);
                return true;
            }, idt == DTERNAL ? ETERNAL : 0, true, exclude.dt() == DTERNAL, false, 0);

            return removed[0] ? x.term() : include;
        }
    }

    /** note this doesnt remove the terms which only appeared in the target time being removed */
    public boolean removeAll(long when) {
        return event.remove(when)!=null;
    }

    public boolean removeAll(Term what) {
        byte id = get(what);
        if (id != Byte.MIN_VALUE) {
            long[] events = event.keySet().toArray(); //create an array because removal will interrupt direct iteration of the keySet
            boolean removed = false;
            for (long e : events) {
                removed |= remove(e, what);
            }
            return removed;
        }
        return false;
    }

    static public Term sequence(Term a, long aStart, Term b, long bStart) {

        if (bStart == ETERNAL && aStart!=ETERNAL)
            throw new WTF();
        if (aStart == TIMELESS || bStart == TIMELESS)
            throw new WTF();
//        if (aStart == DTERNAL || bStart == DTERNAL || aStart == XTERNAL || bStart == XTERNAL)
//            throw new WTF("probably meant ETERNAL"); //TEMPORARY

        boolean simple = (a.unneg().op() != CONJ) && (b.unneg().op() != CONJ);

        if (simple) {
            int dt = aStart == ETERNAL ? DTERNAL : occToDT(bStart - aStart);
            return conjSeqFinal(dt, a, b);
        } else {
            Conj c = new Conj();
            if (c.add(aStart, a))
                c.add(bStart, b);
            return c.term();
        }

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

        int dt = (int) (events.get(center + 1).getOne() - first.getOne() - left.eventRange());

        return conjSeqFinal(dt, left, right);
    }

    public static Term conjSeqFinal(int dt, Term left, Term right) {
        assert (dt != XTERNAL);

        if (left == Null) return Null;
        if (right == Null) return Null;

        if (left == False) return False;
        if (right == False) return False;

        if (left == True) return right;
        if (right == True) return left;

        if (!left.op().eventable || !right.op().eventable)
            return Null;

        if (dt == 0 || dt == DTERNAL) {
            if (left.equals(right))
                return left;
            else if (left.equalsNeg(right))
                return False;
        }

        if (left.compareTo(right) > 0) {
            if (dt!=DTERNAL)
                dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }

//        if (left.op() == CONJ && right.op() == CONJ) {
//            int ldt = left.dt(), rdt = right.dt();
//            if (ldt != XTERNAL && !concurrent(ldt) && rdt != XTERNAL && !concurrent(rdt)) {
//                int ls = left.subs(), rs = right.subs();
//                if ((ls > 1 + rs) || (rs > ls)) {
//
//                    return terms.conj(dt, left, right);
//                }
//            }
//        }

        Term t = terms.theCompound(CONJ, dt, left, right);

//        if (t.op()==CONJ && t.OR(tt -> tt.op()==NEG && tt.unneg().op()==CONJ)) {
//            //HACK verify
//            Term u = t.anon();
//            if (u!=t && (u.op()!=CONJ || u.volume()!=t.volume())) {
//                t = terms.conj(dt, left, right);
//            }
//        }

        //HACK sometimes this seems to happen
        if (t.hasAny(BOOL)) {
            if (t.contains(False))
                return False;
            if (t.contains(Null))
                return Null;
        }
        return t;
    }


    public void clear() {
        super.clear();
        event.clear();
        term = null;
    }

    protected int conflictOrSame(long at, byte what) {
        return conflictOrSame(event.get(at), what);
    }

    protected static int conflictOrSame(Object e, byte id) {
        if (e == null) {

        } else if (e instanceof RoaringBitmap) {
            RoaringBitmap r = (RoaringBitmap) e;
            if (r.contains(-id))
                return -1;
            else if (r.contains(id)) {
                return +1;
            }
        } else if (e instanceof byte[]) {
            byte[] b = (byte[]) e;
            for (int i = 0; i < b.length; i++) {
                int bi = b[i];
                if (bi == -id)
                    return -1;
                else if (bi == id)
                    return +1;
                else if (bi == 0)
                    break; //null terminator
            }
        }
        return 0;
    }

    public final Conj with(long at, Term x) {
        add(at, x);
        return this;
    }

    /**
     * returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .term() should be considered final
     */
    public boolean add(long at, Term x) {
        if (term != null)
            throw new RuntimeException("already concluded: " + term);
        if (at == DTERNAL || at == XTERNAL)//TEMPORARY
            throw new WTF("probably meant ETERNAL or TIMELESS");

        return added(
                (x instanceof Compound && x.op() == CONJ) ?
                        addConjEvent(at, x)
                        :
                        addEvent(at, x)
        );
    }

    protected boolean addConjEvent(long at, Term x) {

        int xdt = x.dt();
        if (xdt == DTERNAL) {
            if (at == ETERNAL) {
                if (addConjEventFactored()) {
                    Subterms tt = x.subterms();
                    if (tt.hasAny(CONJ)) {
                        //add any contained sequences first
                        return tt.AND(ttt ->
                                (ttt.op() != CONJ || Tense.dtSpecial(ttt.dt())) || add(0, ttt)
                        ) && tt.AND(ttt ->
                                (ttt.op() == CONJ && !Tense.dtSpecial(ttt.dt())) || add(ETERNAL, ttt)
                        );
                    }
                }
            }
        }

//        if (seq && at == ETERNAL && addConjEventFactored()) {
//            Subterms tt = x.subterms();
//            //add any contained sequences first
//            return tt.AND(ttt ->
//                    (ttt.op() != CONJ || Tense.dtSpecial(ttt.dt())) || add(0, ttt)
//            ) && tt.AND(ttt ->
//                    (ttt.op() == CONJ && !Tense.dtSpecial(ttt.dt())) || add(ETERNAL, ttt)
//            );
//        }

        if (at != ETERNAL || (xdt == 0) || (xdt == DTERNAL)) {

            if (at == ETERNAL && Conj.isSeq(x))
                at = 0;
            return x.eventsWhile(this::addEvent, at,
                    at != ETERNAL, //unpack parallel except in DTERNAL root, allowing: ((a&|b) && (c&|d))
                    true,
                    false, 0);
        } else {
            return addEvent(at, x);
        }
    }

    /** return false to force distribute sequence add */
    protected boolean addConjEventFactored() {
        return true;
    }

    private boolean added(boolean success) {
        if (success)
            return true;
        else {
            if (term == null)
                term = False;
            return false;
        }
    }

//    public boolean add(Term t, long start, long end, int maxSamples, int minSegmentLength) {
//        if ((start == end) || start == ETERNAL) {
//            return add(start, t);
//        } else {
//            if (maxSamples == 1) {
//
//                return add((start + end) / 2L, t);
//            } else {
//
//                long dt = Math.max(minSegmentLength, (end - start) / maxSamples);
//                long x = start;
//                while (x < end) {
//                    if (!add(x, t))
//                        return false;
//                    x += dt;
//                }
//                return true;
//            }
//        }
//    }

    private boolean addEvent(long at, Term x) {
//        if (Param.DEBUG) {
//            if (at == DTERNAL) //HACK
//                throw new WTF("probably meant at=ETERNAL not DTERNAL");
//        }

        if (x instanceof Bool) {
            //short circuits
            if (x == True)
                return true;
            else if (x == False) {
                this.term = False;
                return false;
            } else if (x == Null) {
                this.term = Null;
                return false;
            }
        }


        //quick test for conflict with existing ETERNALs
        Object eternalEvents = event.get(ETERNAL);
        if (eventCount(eternalEvents) > 0) {
            if (!eventsAND(eternalEvents, b -> !unindex(b).equalsNeg(x))) {
                this.term = False;
                return false;
            }
            if (eventsOR(eternalEvents, b->unindex(b).equals(x))) {
                return true; //absorbed into existing eternal
            }
        }

        //test this first
        boolean polarity = x.op() != NEG;
        byte id = add(polarity ? x : x.unneg());

        if (!polarity) id = (byte) -id;

        switch (addFilter(at, x, id)) {
            case +1:
                return true; //ignore and continue
            case 0:
                return addEvent(at, id, x); //continue
            case -1:
                return false; //reject and fail
            default:
                throw new UnsupportedOperationException();
        }

    }

    private boolean addEvent(long at, byte id) {
        Term xx = unindex(id);
        return addEvent(at, id, xx);
    }


    private boolean addEvent(long at, byte id, Term x) {

        Object events = event.getIfAbsentPut(at, () -> new byte[ROARING_UPGRADE_THRESH]);
        if (events instanceof byte[]) {
            byte[] b = (byte[]) events;

            //quick test for exact absorb/contradict
            for (int i = 0; i < b.length; i++) {
                byte bi = b[i];
                if (bi == 0)
                    break;
                if (id == -bi)
                    return false; //contradiction
                if (id == bi)
                    return true; //found existing
            }

            for (int i = 0; i < b.length; i++) {
                byte bi = b[i];
                if (bi == 0) {
                    //empty slot, take
                    b[i] = id;
                    return true;
                } else {

                    Term result = merge(bi, x, at == ETERNAL);

                    if (result != null) {
                        if (result == True)
                            return true; //absorbed input
                        if (result == False || result == Null) {
                            this.term = result; //failure
                            return false;
                        } else {
                            if (i < b.length - 1) {
                                arraycopy(b, i + 1, b, i, b.length - 1 - i);
                                i--; //compactify
                            } else
                                b[i] = 0; //erase, continue comparing. the result remains eligible for add

                            return add(at, result);
                        }
                    }
                }
            }

            //no remaining capacity, upgrade to RoaringBitmap

            RoaringBitmap rb = new RoaringBitmap();
            for (byte bb : b)
                rb.add(bb);
            rb.add(id);
            event.put(at, rb);


            return true;
        } else {
            return todoOrFalse();
        }
    }


    /**
     * allows subclass implement different behavior.
     * <p>
     * return:
     * -1: ignore and fail the conjunction
     * 0: default, continue
     * +1: ignore and continue
     */
    protected int addFilter(long at, Term x, byte id) {
        return 0;
    }


    /**
     * merge an incoming term with a disjunctive sub-expression (occurring at same event time) reductions applied:
     * ...
     */
    private static Term disjunctify(Term existing, Term incoming, boolean eternal) {
        Term existingUnneg = existing.unneg();
        Term incomingUnneg = incoming.unneg();
        if (incoming.op() == NEG && incomingUnneg.op() == CONJ) {
            return disjunctionVsDisjunction(existingUnneg, incomingUnneg, eternal);
        } else {
            return disjunctionVsNonDisjunction(existingUnneg, incoming, eternal);
        }
    }

    private static Term disjunctionVsNonDisjunction(Term conjUnneg, Term incoming, boolean eternal) {
//        if (incoming.op()==CONJ)
//            throw new WTF(incoming + " should have been decomposed further");

        final Term[] result = new Term[1];
        conjUnneg.eventsWhile((when, what) -> {
            if (eternal || when == 0) {
                if (incoming.equalsNeg(what)) {
                    //overlap with the option so annihilate the entire disj
                    result[0] = True;
                    return false; //stop iterating
                } else if (incoming.equals(what)) {
                    //contradiction
                    result[0] = False;
                    //keep iterating, because possible total annihilation may follow.
                }
            }
            return eternal || when == 0;
        }, 0, true, true, eternal, 0);


        if (result[0] == True) {
            return incoming; //disjunction totally annihilated by the incoming condition
        }

        int dt = eternal ? DTERNAL : 0;

        if (result[0] == False) {
            //removing the matching subterm from the disjunction and reconstruct it
            //then merge the incoming term


            if (!isSeq(conjUnneg)) {
                Term newConj = Conj.without(conjUnneg, incoming, false);
                if (newConj.equals(conjUnneg))
                    return True; //no change

                if (newConj.equals(incoming)) //quick test
                    return False;

                return terms.conj(dt, newConj.neg(), incoming);

            } else {
                Conj c = new Conj();
                if (eternal) c.addAuto(conjUnneg); else c.add(0, conjUnneg);
                c.factor();
                if (!eternal) {
                    boolean removed;
                    if (!(removed = c.remove(dt, incoming))) {
                        //possibly absorbed in a factored eternal component TODO check if this is always the case
                        if (c.eventOccurrences() > 1 && c.eventCount(ETERNAL) > 0) {
                            //try again after distributing to be sure:
                            c.distribute();
                            if (!(removed = c.remove(dt, incoming))) {
                                return Null; //return True;
                            }
                        } else {
                            return Null; //return True;
                        }

                    }
                } else {
                    c.removeAll(incoming);
                }
                Term newConjUnneg = c.term();
                int shift = Tense.occToDT(c.shift());

                Term newConj = newConjUnneg.neg();
                if(shift!=0) {
                    if (dt == DTERNAL)
                        dt = 0;

                    Conj d = new Conj();
                    d.add(dt, incoming);
                    d.add(shift, newConj);
                    return d.term();

                } else {
                    return conjoin(incoming, newConj, eternal);
                }

            }


        }

        return null; //no interaction

    }

    private static Term disjunctionVsDisjunction(Term a, Term b, boolean eternal) {
        Conj aa = new Conj();
        if (eternal) aa.addAuto(a); else aa.add(0, a);
        aa.factor();
        //aa.distribute();

        Conj bb = newConjSharingTermMap(aa);
        if (eternal) bb.addAuto(b); else bb.add(0, b);
        bb.factor();
        //bb.distribute();

        Conj cc = intersect(aa, bb);
        if (cc == null) {
            //perfectly disjoint; OK
            return null;
        } else {
            Term A = aa.term();
            if (a == Null)
                return Null;
            Term B = bb.term();
            if (b == Null)
                return Null;
            long as = aa.shift();
            long bs = bb.shift();
            long abShift = Math.min(as, bs);
            Conj dd = newConjSharingTermMap(cc);
            if (eternal && as==0 && bs==0) {
                as = bs = ETERNAL;
            }
            if (dd.add(as, A.neg()))
                dd.add(bs, B.neg());

            Term D = dd.term();
            if (D == Null)
                return Null;

            if (cc.eventOccurrences()==0)
                return D.neg();
            else {
                cc.add((eternal && (cc.eventOccurrences()==1 && cc.eventCount(ETERNAL)>0 && !Conj.isSeq(A) && !Conj.isSeq(B))) ?
                        ETERNAL : (abShift), D.neg());
                return cc.term().neg();
            }
        }
    }

    /** # of unique event occurrence times */
    public int eventOccurrences() {
        return event.size();
    }

    /** produces a Conj instance containing the intersecting events.
     * null if no events in common and x and y were not modified.
     *  erases contradiction (both cases) between both so may modify X and Y.  probably should .distribute() x and y first  */
    @Nullable public static Conj intersect(Conj x, Conj y) {

        assert(x.termToId == y.termToId): "x and y should share term map";

        MutableLongSet commonEvents = x.event.keySet().select(y.event::containsKey);
        if (commonEvents.isEmpty())
            return null;

        Conj c = newConjSharingTermMap(x);
        final boolean[] modified = {false};
        commonEvents.forEach(e -> {
            ByteSet xx = x.eventSet(e);
            ByteSet yy = y.eventSet(e);
            ByteSet common = xx.select(xxx -> yy.contains(xxx));
            ByteSet contra = xx.select(xxx -> yy.contains((byte) -xxx));
            if (!common.isEmpty()) {
                common.forEach(cc -> {
                    c.add(e, x.unindex(cc));
                    boolean xr = x.remove(e, cc);
                    boolean yr = y.remove(e, cc);
                    assert(xr && yr);
                    modified[0] = true;
                });
            }
            if (!contra.isEmpty()) {
                contra.forEach(cc -> {
                    boolean xr = x.remove(e, cc, (byte) -cc);
                    boolean yr = y.remove(e, cc, (byte) -cc);
                    assert(xr && yr);
                    modified[0] = true;
                });
            }
        });
        return (!modified[0] && c.event.isEmpty()) ? null : c;
    }

    public ByteSet eventSet(long e) {
        Object ee = event.get(e);
        if (ee == null)
            return ByteSets.immutable.empty();
        if (!(ee instanceof byte[]))
            throw new TODO();
        byte[] eee = (byte[])ee;
        int ec = eventCount(eee);
        assert(ec > 0);
        if (ec == 1)
            return ByteSets.immutable.of(eee[0]);
        else if (ec == 2)
            return ByteSets.immutable.of(eee[0], eee[1]);
        else {
            ByteHashSet b = new ByteHashSet(ec);
            events(eee, b::add);
            return b;
        }
    }

    /**
     * stage 2: a and b are both the inner conjunctions of disjunction terms being compared for contradiction or factorable commonalities.
     * a is the existing disjunction which can be rewritten.  b is the incoming
     */
    private static Term OLD_disjunctionVsDisjunction(Term a, Term b, boolean eternal) {
        int adt = a.dt(), bdt = b.dt();
        boolean bothCommute = (adt == 0 || adt == DTERNAL) && (bdt == 0 || bdt == DTERNAL);
        if (bothCommute) {
            if ((adt == bdt || adt == DTERNAL || bdt == DTERNAL)) {
                //factor out contradicting subterms
                MutableSet<Term> aa = a.subterms().toSet();
                MutableSet<Term> bb = b.subterms().toSet();
                Iterator<Term> bbb = bb.iterator();
                boolean change = false;
                while (bbb.hasNext()) {
                    Term bn = bbb.next();
                    if (aa.remove(bn.neg())) {
                        bbb.remove(); //both cases allowed; annihilate both
                        change = true;
                    }
                }
                if (change) {
                    //reconstitute the two terms, glue them together as a new super-disjunction to replace the existing (and interrupt adding the incoming)

                    Term A = terms.conj(adt, aa.toArray(EmptyTermArray)).neg();
                    if (A == False || A == Null || (adt==bdt && aa.equals(bb)))
                        return A;

                    Term B = terms.conj(bdt, bb.toArray(EmptyTermArray)).neg();
                    if (B == False || B == Null || A.equals(B))
                        return B;

                    return terms.conj(eternal ? DTERNAL : 0, A, B);
                }
            }
            //return null; //no change
            return Null;
        } else {
//            //TODO factor out the common events, and rewrite the disjunction as an extra CONJ term with the two options (if they are compatible)
//            //HACK simple case
//            if (a.subs()==2 && b.subs()==2 && !a.subterms().hasAny(CONJ) && !b.subterms().hasAny(CONJ)){
//                Term common = a.sub(0);
//                if (common.equals(b.sub(0))) {
//                    Term ar = Conj.without(a, common, false);
//                    if (ar!=null) {
//                        int ac = a.subTimeFirst(common);
//                        if (ac!=DTERNAL) {
//                            int ao = a.subTimeFirst(ar);
//                            if (ao != DTERNAL) {
//                                Term br = Conj.without(b, common, false);
//                                if (br != null) {
//                                    int bc = b.subTimeFirst(common);
//                                    if (bc!=DTERNAL) {
//                                        int bo = b.subTimeFirst(br);
//                                        if (bo != DTERNAL) {
//                                            Term arOrBr = terms.conj((bo-bc) - (ao-ac), ar.neg(), br.neg()).neg();
//                                            Term y = terms.conj(-ac, common, arOrBr).neg(); //why neg
//                                            return y;
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }

//            //HACK if the set of components is disjoint, allow
//            if (!a.subterms().hasAny(CONJ) && !b.subterms().hasAny(CONJ)) {
//                Subterms bs = b.subterms();
//                if (!a.subterms().OR(aa -> bs.contains(aa) || bs.containsNeg(aa)))
//                    return null;//allow
//            }

            //TODO finer grained disjoint event comparison

            //TODO sequence conditions
            //throw new TODO(a + " vs " + b);
            return Null; //disallow for now. the complexity might be excessive
            //return null; //OK
        }
    }


    private static Term conjoinify(Term conj, Term incoming, boolean eternal) {

        int dtOuter = eternal ? DTERNAL : 0;

        if (incoming.op() == CONJ) {
            int incomingDT = incoming.dt();
            if (incomingDT == dtOuter || conj.dt() == dtOuter) {
                return terms.conj(dtOuter, conj, incoming);
            } else if (incomingDT == conj.dt()) {
                if (incomingDT == XTERNAL) {

                    return null; //two XTERNAL conjoined in DTetrnity, OK

                    //return terms.conj(incomingDT, conj, incoming); //promote two xternal in DTernity to one xternal

                } else if (!eternal && ((incomingDT == 0) || (incomingDT == DTERNAL))) {
                    //promote two parallel to one parallel
                    return terms.conj(incomingDT, conj, incoming);
                }
            }

            //two sequences, probly. maybe some preprocessing that can be applied here
            //otherwise just add the new event

            //return terms.theSortedCompound(CONJ, dtOuter, conj, incoming);
            return null; //OK

        } else {

            int dtInner = conj.dt();

//            Subterms cs = conj.subterms();
//            if (dtSpecial(dtInner)) {
//                if (cs.containsNeg(incoming))
//                    return False; //contradiction
//                else if (cs.contains(incoming))
//                    return True; //present, ignore
//
//                if (!cs.hasAny(Op.CONJ)) {
//                    return terms.conj(dtOuter, subAppend(cs, incoming));
//
////                    if (dtInner == dtOuter) {
////                        //commutive merge
////                        return terms.theSortedCompound(CONJ, dtOuter, subAppend(cs, incoming));
////                    } else {
////                        return terms.theSortedCompound(CONJ, dtOuter, conj, incoming);
////                    }
//                }
//            }
            if ((dtInner == 0 || dtInner == DTERNAL) && conj.contains(incoming))
                return True; //quick test for absorption

            boolean innerCommute = Tense.dtSpecial(dtInner);// && !conj.subterms().hasAny(Op.CONJ);

            Conj c = !innerCommute ? new Conj() : null;
            FasterList<Term> cx = innerCommute ? new FasterList() : null;
//            boolean incomingHasConj = incoming.hasAny(CONJ);
            boolean ok = conj.eventsWhile((whn, wht) -> {
                Term ww;

                ww = terms.conj(dtOuter, wht, incoming);

                if (ww == False || ww == Null)
                    return false;
                if (ww == True)
                    return true;
                if (ww != wht && ww.equals(wht))
                    ww = wht; //use original if possible


                if (innerCommute) {
                    cx.add(ww);
                    return true;
                } else {
                    //return c.add(whn, ww);
                    return c.addEvent(whn, ww);//direct
                }

            }, 0, dtInner == 0, dtInner == DTERNAL, dtInner == XTERNAL, 0);
            if (!ok)
                return False;

            Term d = innerCommute ? terms.conj(dtInner, cx.toArrayRecycled(Term[]::new))
                    :
                    c.term();

            if (d == False || d == Null)
                return d; //fail

            if (d == conj || (d != conj && d.equals(conj)))
                return True;  //no change since the incoming has been absorbed

            if (d.op() != CONJ)
                return d; //simplified/reduced event

            //all original subterms remain intact, return simplified factored version
//            //return terms.theSortedCompound(CONJ, dtOuter, conj, incoming);
            return null; //OK

        }

    }

    protected Term merge(byte existingId, Term incoming, boolean eternalOrParallel) {
        return merge(unindex(existingId), incoming, eternalOrParallel);
    }

    /**
     * @param existing
     * @param incoming
     * @param eternalOrParallel
     *      * True - absorb and ignore the incoming
     *      * Null/False - short-circuit annihilation due to contradiction
     *      * non-null - merged value.  if merged equals current item, as-if True returned. otherwise remove the current item, recurse and add the new merged one
     *      * null - do nothing, no conflict.  add x to the event time
     */
    protected static Term merge(Term existing, Term incoming, boolean eternalOrParallel) {

        if (existing.equals(incoming))
            return True; //exact same
        if (existing.equalsNeg(incoming))
            return False; //contradiction

        boolean incomingPolarity = incoming.op() != NEG;
        Term incomingUnneg = incomingPolarity ? incoming : incoming.unneg();
        Term existingUnneg = existing.unneg();
        boolean xConj = incomingUnneg.op() == CONJ;
        boolean eConj = existingUnneg.op() == CONJ;

        if (!eConj && !xConj)
            return null;  //OK neither a conj/disj

        if (!Term.commonStructure(existingUnneg, incomingUnneg))
            return null; //OK no potential for interaction

        boolean existingPolarity = existing.op() != NEG;
        Term result;
        if (eConj && xConj) {
            //decide which is larger, swap for efficiency
            boolean swap = ((existingPolarity == incomingPolarity) && incoming.volume() > existing.volume());
            if (swap) {
                Term x = incoming;
                incoming = existing;
                existing = x;
            }
            result = merge(existing, existingPolarity, incoming, eternalOrParallel);
        } else if (eConj && !xConj) {
            result = merge(existing, existingPolarity, incoming, eternalOrParallel);
        } else /*if (xConj && !eConj)*/ {
            result = merge(incoming, incomingPolarity, existing, eternalOrParallel);
        }

        if (result != null && !(result instanceof Bool) && result.equals(existing))
            return True; //absorbed

        return result;
    }

    private static Term merge(Term conj, boolean conjPolarity, Term x, boolean eternal) {
        return conjPolarity ? conjoinify(conj, x, eternal) : disjunctify(conj, x, eternal);
    }

    /** stateless/fast 2-ary conjunction in either eternity (dt=DTERNAL) or parallel(dt=0) modes */
    public static Term conjoin(Term x, Term y, boolean eternalOrParallel) {
        if (x == Null) return Null;
        if (y == Null) return Null;

        if (x == False) return False;
        if (y == False) return False;

        if (x == True) return y;
        if (y == True) return x;

        Term xy = merge(x, y, eternalOrParallel);

        //decode result term
        if (xy == True) {
            return x; //x absorbs y
        } else if (xy == null) {
            int dt = eternalOrParallel ? DTERNAL : 0;
            return terms.theSortedCompound(CONJ, dt, x, y);
        } else {
            //failure or some particular merge result
            return xy;
        }
    }

    /**
     * @return non-zero byte value
     */
    private byte add(Term t) {
//        if (!(t != null && eventable(t))) //eventable
//            throw new WTF(t + " is not valid event in " + Conj.class);

        return termToId.getIfAbsentPutWithKey(t, tt -> {
            int s = idToTerm.addAndGetSize(tt);
            assert (s < Byte.MAX_VALUE);
            return (byte) s;
        });
    }

//    static boolean eventable(Term t) {
//        return !t.op().isAny(BOOL.bit | INT.bit | IMG.bit | NEG.bit);
//    }

    /**
     * returns index of an item if it is present, or -1 if not
     */
    private byte index(Term t) {
        return termToId.getIfAbsent(t.unneg(), (byte) -1);
    }

    private Term unindex(byte id) {
        Term x = idToTerm.get(Math.abs(id) - 1);
        if (x == null)
            throw new NullPointerException();
        return x.negIf(id < 0);
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

    public boolean remove(long at, Term t) {

        byte i = get(t);
        return remove(at, i);
    }

    public boolean remove(long at, byte... what) {

        Object o = event.get(at);
        if (o == null)
            return false;


        if (removeFromEvent(at, o, true, what) != 0) {
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
    private int removeFromEvent(long at, Object o, boolean autoRemoveIfEmpty, byte... i) {
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
                assert (ii != 0);
                int bi = ArrayUtils.indexOf(b, (byte) ii, 0, num);
                if (bi != -1) {
                    //if (b[bi] != 0) {
                    b[bi] = 0;
                    removals++;
                    //}
                }
            }

            if (removals == 0)
                return 0;
            else if (removals == num) {
                if (autoRemoveIfEmpty)
                    event.remove(at);
                return 2;
            } else {


                //sort all zeros to the end
                ArrayUtils.sort(b, 0, num - 1, (byte x) -> x == 0 ? Byte.MIN_VALUE : x);

//                MetalBitSet toRemove = MetalBitSet.bits(b.length);
//
//                for (int zeroIndex = 0; zeroIndex < b.length; zeroIndex++) {
//                    if (b[zeroIndex] == 0)
//                        toRemove.set(zeroIndex);
//                }
//
//                b = ArrayUtils.removeAll(b, toRemove);
//                event.put(at, b);
                return 1;
            }
        }
    }

    public boolean removeEventsByTerm(Term t, boolean pos, boolean neg) {

        boolean negateInput;
        if (t.op() == NEG) {
            negateInput = true;
            t = t.unneg();
        } else {
            negateInput = false;
        }

        byte i = get(t);
        if (i == Byte.MIN_VALUE)
            return false;

        byte[] ii;
        if (pos && neg) {
            ii = new byte[]{i, (byte) -i};
        } else if (pos) {
            ii = new byte[]{negateInput ? (byte) -i : i};
        } else if (neg) {
            ii = new byte[]{negateInput ? i : (byte) -i};
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

        if (event.isEmpty())
            return True;

        factor();

        int numOcc = eventOccurrences();
        int numTmpOcc = numOcc;


        List<Term> tmp = new FasterList(2);
        Term eternal;
        if (event.containsKey(ETERNAL)) {
            numTmpOcc--;
            eternal = term(ETERNAL, tmp);
            if (eternal != null) {
                if (eternal == False || eternal == Null)
                    return this.term = eternal;
            }
        } else {
            eternal = null;
        }

        Term temporal;
        Term ci;
        switch (numTmpOcc) {
            case 0:
                temporal = null;
                break;
            case 1:
                //one other non-eternal time
                LongObjectPair<Object> e = event.keyValuesView().select(x -> x.getOne() != ETERNAL).getFirst();
                Term t = term(e.getOne(), e.getTwo(), tmp);
                if (t == null || t == True) {
                    t = null;
                } else if (t == False) {
                    return this.term = False;
                } else if (t == Null) {
                    return this.term = Null;
                }
                if (t!=null) {
                    if (eternal != null && e.getOne() == 0) {
                        boolean econj = eternal.op() == CONJ;
                        if (!econj || eternal.dt() == DTERNAL) {
                            //promote eternal to parallel
                            if (!econj) {
                                return ConjCommutive.the(0, eternal, t);
                            } else {
                                List<Term> ecl = new FasterList(eternal.subs() + 1);
                                eternal.subterms().addTo(ecl);
                                ecl.add(t);
                                return ConjCommutive.the(0, ecl);
                            }
                        }
                    }
                }
                temporal = t;
                break;
            default:
                FasterList<LongObjectPair<Term>> temporals = null;

                for (LongObjectPair next : event.keyValuesView()) {
                    long when = next.getOne();
                    if (when == ETERNAL)
                        continue;

                    Term wt = term(when, next.getTwo(), tmp);
                    if (wt == null || wt == True) {
                        continue;
                    } else if (wt == False) {
                        return this.term = False;
                    } else if (wt == Null) {
                        return this.term = Null;
                    }

                    if (temporals == null) temporals = new FasterList<>(numOcc + 1);

                    temporals.add(pair(when, wt));
                }
                if (temporals != null) {

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
                } else
                    temporal = null;
                break;
        }



        if (eternal != null && temporal != null) {
            if (eternal == True)
                ci = temporal;
            else
                ci = ConjCommutive.the(DTERNAL, temporal, eternal);
        } else if (eternal == null) {
            ci = temporal;
        } else /*if (temporal == null)*/ {
            ci = eternal;
        }

        if (ci == null)
            return True;

        return ci;
    }

    /**
     * factor common temporal event components to an ETERNAL component
     * returns true if possibly changed
     */
    public boolean factor() {

        if (eventOccurrences() <= 1) {
            return false;
        }

        RichIterable<LongObjectPair<Object>> events = event.keyValuesView();
        int numTemporalCompoundEvents = events.count(l -> l.getOne() != ETERNAL && eventCount(l.getTwo()) > 1);
        if (numTemporalCompoundEvents <= 1)
            return false;
        int numTemporalEvents = events.count(l -> l.getOne() != ETERNAL);
        if (numTemporalCompoundEvents != numTemporalEvents)
            return false;

        ByteHashSet common = new ByteHashSet();
        //TODO if this is iterated in order of least # of events at each time first, it is optimal
        if (!events.allSatisfy((whenWhat) -> {
            long when = whenWhat.getOne();
            if (when == ETERNAL)
                return true;
            Object what = whenWhat.getTwo();
            if (what instanceof byte[]) {
                byte[] bWhat = (byte[]) what;
                if (common.isEmpty()) {
                    //add the first set of events
                    events(bWhat, common::add);
                } else {
                    if (common.removeIf(c -> !eventsContains(bWhat, c))) {
                        //done //need to keep iterator
                        return !common.isEmpty();
                    }
                }

                return true;
            } else {
                throw new TODO();
            }
        }))
            return false;

        assert (!common.isEmpty());

        long[] eventTimes = new long[numTemporalCompoundEvents];
        final int[] e = {0};
        final int[] maxSlotEvents = {0};
        if (events.anySatisfy((whenWhat) -> {
            long when = whenWhat.getOne();
            if (when == ETERNAL)
                return false; //skip the eternal event
            Object what = whenWhat.getTwo();
            eventTimes[e[0]++] = when;

            if (what instanceof byte[]) {
                //all would be eliminated at this time slot
                return eventsAND(((byte[]) what), common::contains);
            }
            return false;
        }))
            return false;

        int commonCount = common.size();
        //nothing in common
        if (commonCount != 0) {//verify that no event time is completely eliminated by the common terms
            if (maxSlotEvents[0] == commonCount)
                return false;

            MutableByteIterator ii = common.byteIterator();
            while (ii.hasNext()) {
                byte f = ii.next();
                if (addEvent(ETERNAL, f)) {
                    for (long ee : eventTimes) {
                        if (!remove(ee, f))
                            throw new WTF();
                    }
                } else
                    throw new WTF();
            }
        }
        return true;
    }

    private static boolean eventsContains(byte[] events, byte b) {
        return ArrayUtils.contains(events, b);
    }

    protected static void events(byte[] events, ByteProcedure each) {
        for (byte e : events) {
            if (e != 0) {
                each.value(e);
            } else
                break; //null-terminator
        }
    }

    private static boolean eventsAND(Object events, BytePredicate each) {
        if (events instanceof byte[])
            return eventsAND((byte[]) events, each);
        else
            throw new TODO();
    }
    private static boolean eventsOR(Object events, BytePredicate each) {
        if (events instanceof byte[])
            return eventsOR((byte[]) events, each);
        else
            throw new TODO();
    }
    private static boolean eventsAND(byte[] events, BytePredicate each) {
        for (byte e : events) {
            if (e != 0) {
                if (!each.accept(e))
                    return false;
            } else
                break; //null-terminator
        }
        return true;
    }
    private static boolean eventsOR(byte[] events, BytePredicate each) {
        for (byte e : events) {
            if (e != 0) {
                if (each.accept(e))
                    return true;
            } else
                break; //null-terminator
        }
        return false;
    }

    private static void flattenInto(Collection<Term> ee, Term ex, int dt) {
        if (ex.op() == CONJ && ex.dt() == dt)
            ex.subterms().forEach(eee -> flattenInto(ee, eee, dt));
        else
            ee.add(ex);
    }

    public long shift() {
        long min = Long.MAX_VALUE;
        LongIterator ii = event.keysView().longIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != ETERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min==Long.MAX_VALUE ? 0 : min;
    }

    public Term term(long when) {
        return term(when, new FasterList(1));
    }

    public Term term(long when, List<Term> tmp) {
        return term(when, event.get(when), tmp);
    }
    public Term term(long when, Object what, List<Term> tmp) {
        return term(when, what, null, tmp);
    }

//    private Term term(long when, Object what) {
//        return term(when, what, null);
//    }

    @Nullable private int term(Object what, IntPredicate validator, Consumer<Term> each) {
        if (what == null)
            return 0;

        final byte[] b;
        int n = eventCount(what);
        if (n == 0)
            return 0; //does this happen?

        if (what instanceof byte[]) {
            //rb = null;
            b = (byte[]) what;
        } else {
            throw new TODO();
        }

        if (n == 1) {
            //only event at this time
            each.accept( sub(b[0], null, validator) );
            return 1;
        }

        final boolean[] negatives = {false};

        int k = 0;
        for (byte x : b) {
            if (x == 0)
                break; //null-terminator reached
            Term s = sub(x, negatives, validator);
            each.accept(s);
            k++;
//            if (s instanceof Bool) {
//                if (s == False || s == Null)
//                    return s;
//                else {
//                    //ignore True case
//                }
//            } else {
//
//                if (t == null)
//                    t = new TreeSet();
//
//                if (n>1 && s.op() == CONJ && (when==ETERNAL && s.dt()==DTERNAL) || (when!=ETERNAL && s.dt()==0)) {
//                    //flatten contained eternal/parallel conjunction, if appropriate for the target time
//                    for (Term ss : s.subterms())
//                        t.add(ss);
//                } else {
//                    t.add(s);
//                }
//            }
        }
        return k;
    }

    private Term term(long when, Object what, IntPredicate validator, List<Term> tmp) {

        tmp.clear();

        int n = term(what, validator, tmp::add);

        if (n == 0)
            return null;

        int ts = tmp.size();
        switch (ts) {
            case 0:
                return null;
            case 1:
                return tmp.get(0);
            default: {
                return terms.theSortedCompound(CONJ, when == ETERNAL ? DTERNAL : 0, tmp);
            }

        }
    }


    private static boolean todoOrFalse() {
        if (Param.DEBUG)
            throw new TODO();
        else
            return false;
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

//    public boolean addDithered(Term term, long start, long end, int maxSamples, int minSegmentLength, NAR nar) {
//        if (start != ETERNAL) {
//            int d = nar.timeResolution.intValue();
//            if (d != 1) {
//                start = Tense.dither(start, d);
//                end = Tense.dither(end, d);
//            }
//        }
//        return add(term, start, end, maxSamples, minSegmentLength);
//    }

    /** opposite of factor; 'multiplies' all temporal components with any eternal components */
    public void distribute() {

        int occ = eventOccurrences();
        if (occ <= 1)
            return;

        if (eventCount(ETERNAL) == 0)
            return;

        Term ete = term(ETERNAL);

        removeAll(ETERNAL);

        //replace all terms (except the eternal components removed) with the distributed forms

        this.idToTerm.replaceAll((x)->{
            if (!x.equals(ete)) {
                Term y = CONJ.the(x, ete);
                if (!y.equals(x)) {
                    byte id = termToId.removeKeyIfAbsent(x, Byte.MIN_VALUE);
                    assert(id!=Byte.MIN_VALUE);
                    termToId.put(y, id);
                    return y;
                }
            }
            return x;
        });
    }

    //    private static void addAllTo(ByteHashSet common, Object o) {
//        if (o instanceof byte[])
//            common.addAll((byte[]) o);
//        else {
//            RoaringBitmap r = (RoaringBitmap) o;
//            r.forEach((int x) -> common.add((byte) x));
//        }
//    }


}
