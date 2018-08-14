package nars.time;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jcog.Util;
import jcog.data.graph.FromTo;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;
import jcog.math.Longerval;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Conj;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static jcog.data.graph.search.Search.pathStart;
import static nars.Op.*;
import static nars.time.Tense.*;
import static nars.time.TimeSpan.TS_ZERO;

/**
 * represents a multigraph of events and their relationships
 * calculates unknown times by choosing from the possible
 * pathfinding results.
 * <p>
 * it can be used in various contexts:
 * a) the tasks involved in a derivation
 * b) as a general purpose temporal index, ie. as a meta-layer
 * attached to one or more concept belief tables
 * <p>
 * DTERNAL relationships can be maintained separate
 * from +0.
 */
public class TimeGraph extends MapNodeGraph<Event, TimeSpan> {


    /**
     * index by term
     */
    protected final Multimap<Term, Event> byTerm = MultimapBuilder
            .linkedHashKeys()
            .linkedHashSetValues()
            .build();


    public final MutableSet<Term> autoNeg = new UnifiedSet();


    /**
     * since CONJ will be constructed with conjMerge, if x is conj the dt between events must be calculated from start-start. otherwise it is implication and this is measured internally
     */
    static int dt(Event aa, Event bb) {

        long aWhen = aa.start();
        long bWhen;
        if (aWhen == ETERNAL || (bWhen = bb.start()) == ETERNAL)
            return DTERNAL;
        else {
            assert (aWhen != XTERNAL);
            assert (bWhen != XTERNAL);
            return Tense.occToDT(bWhen - aWhen);
        }

    }

    @Override
    public void clear() {
        super.clear();
        byTerm.clear();
    }

    /**
     * creates an event for a hypothetical term which may not actually be an event;
     * but if it is there or becomes there, it will connect what it needs to
     */
    public Event shadow(Term v) {
        return event(v, TIMELESS, false);
    }

    public Event know(Term v) {
        return event(v, TIMELESS, true);
    }

    public final Event know(Term t, long start) {
        return event(t, start, start, true);
    }

    public Event know(Term t, long start, long end) {
        return event(t, start, end, true);
    }

    public Event event(Term t, long when, boolean add) {
        return event(t, when, when, add);
    }

    public Event event(Term t, long start, long end, boolean add) {
        if (t instanceof Bool || t == Op.ImgExt || t == Op.ImgInt)
            return null;

        Event e;
        if (start != TIMELESS) {

            Collection<Event> te = byTerm.get(t);
            for (Event f : te) {
                if (f instanceof Absolute && ((Absolute) f).containsOrEqual(start, end))
                    return f;
            }

            if (add) {


                Iterator<Event> ff = te.iterator();
                while (ff.hasNext()) {
                    Event f = ff.next();
                    if (!(f instanceof Absolute))
                        continue;
                    Absolute af = (Absolute) f;
                    if (af.start() == ETERNAL)
                        continue;
                    Longerval merged = null;
                    if (af.containedInButNotEqual(start, end)) {
                        removeNode(f);
                        ff.remove();
                    } else if ((merged = af.unionIfIntersectsButNotEqual(start, end)) != null) {
                        if (merged.a < start)
                            start = merged.a;
                        if (merged.b > end)
                            end = merged.b;
                        removeNode(f);
                        ff.remove();
                    }


                }
            }

            if (end != start)
                e = new AbsoluteRange(t, start, end);
            else
                e = new Absolute(t, start);
        } else {
            e = new Relative(t);
        }


        if (add) {
            return addNode(e).id;
        } else {
            return event(e);
        }
    }

    protected int absoluteCount(Term t) {
        int c = 0;
        for (Event tx : byTerm.get(t)) {
            if (tx instanceof Absolute)
                c++;
        }
        return c;
    }

    public Event event(Event e) {
        Node<Event, TimeSpan> existing = node(e);
        return existing != null ? existing.id() : e;
    }

    public boolean link(Event before, TimeSpan e, Event after) {
        return addEdge(addNode(before), e, addNode(after));
    }

    public void link(Event x, long dt, Event y) {

        if ((x == y || ((x.id.equals(y.id)) && (x.start() == y.start()))))
            return;

        boolean swap = false;


        int vc = Integer.compare(x.id.volume(), y.id.volume());
        if (vc == 0) {

            if (x.hashCode() > y.hashCode()) {
                swap = true;
            }
        } else if (vc > 0) {
            swap = true;
        }

        if (swap) {
            if (dt != ETERNAL && dt != TIMELESS && dt != 0) {
                dt = -dt;
            }
            Event z = x;
            x = y;
            y = z;
        }

        link(x, TimeSpan.the(dt), y);
    }

    @Override
    protected void onAdd(Node<Event, TimeSpan> x) {
        Event event = x.id();
        Term eventTerm = event.id;

        if (byTerm.put(eventTerm, event)) {
            onNewTerm(eventTerm);
        }


        int edt = eventTerm.dt();


        switch (eventTerm.op()) {


            case IMPL:

                Term subj = eventTerm.sub(0);
                Term pred = eventTerm.sub(1);

                Event se = know(subj);
                Event pe = know(pred);

                if (edt == DTERNAL) {

                    link(se, ETERNAL, pe);

                    subj.eventsWhile((w, y) -> {
                        link(know(y), ETERNAL, pe);
                        return true;
                    }, 0, true, false, false, 0);

                    pred.eventsWhile((w, y) -> {
                        link(se, ETERNAL, know(y));
                        return true;
                    }, 0, true, false, false, 0);

                } else if (edt != XTERNAL) {

                    int st = subj.dtRange();


                    link(se, (edt + st), pe);

                    subj.eventsWhile((w, y) -> {
                        link(know(y), edt + st - w, pe);
                        return true;
                    }, 0, true, false, false, 0);

                    pred.eventsWhile((w, y) -> {
                        link(se, edt + st + w, know(y));
                        return true;
                    }, 0, true, false, false, 0);

                }


                break;
            case CONJ:


                long eventStart = event.start();
                long eventEnd = event.end();

                switch (edt) {
                    case XTERNAL:
                        for (Term sub : eventTerm.subterms()) {
                            know(sub);
                            //link(event, TimeSpan.TS_ETERNAL, know(sub));
                        }
                        break;

                    case 0:
                    case DTERNAL:

                        Subterms es = eventTerm.subterms();
                        int esn = es.subs();
                        //Event prevEvent = null;

                        long superSubDT = edt == 0 ? 0 /* left aligned parallel */ : ETERNAL;
                        for (int i = 0; i < esn; i++) {
                            Term next = es.sub(i);
                            Event nextEvent = knowComponent(next, eventStart, eventEnd);
//                            if (i == 0) {
//                                prevEvent = nextEvent;
//                                continue;
//                            }

                            //link(prevEvent, ETERNAL, nextEvent);
                            //prevEvent = nextEvent;
                            link(event, superSubDT, nextEvent);
                        }

                        break;
//                    case 0:
//
//
//                        boolean timed = eventStart != ETERNAL;
//                        for (Term s : eventTerm.subterms()) {
//                            Event t = edt == 0 ?
//                                    knowComponent(s, 0, eventStart, eventEnd) :
//                                    (timed ? know(s, eventStart, eventEnd) :
//                                            know(s));
//                            if (t != null) {
//                                link(event, (edt == 0 || timed) ? 0 : ETERNAL,
//                                        t
//                                );
//                            } else {
//
//                            }
//                        }
//                        break;
                    default:

                        if (eventStart != ETERNAL && eventStart != TIMELESS) {
                            //chain the events to the absolute parent
                            long range = eventEnd - eventStart;
                            eventTerm.eventsWhile((w, y) -> {

                                link(event, w - eventStart, know(y, w, w + range));
                                return true;
                            }, eventStart, true, false, false, 0);
                        } else {
                            //chain the events together relatively
                            final Event[] prev = {null};
                            final long[] prevDT = {0};
                            eventTerm.eventsWhile((w, y) -> {
                                Event next = know(y);
                                if (prev[0] == null) {
                                    link(event, 0, next); //chain the starting event to the beginning of the compound superterm
                                    assert (w == 0);
                                } else {
                                    link(prev[0], w - prevDT[0], next);
                                    prevDT[0] += w;
                                }
                                prev[0] = next;
                                return true;
                            }, 0, false, false, false, 0);
                        }

                        break;
                }

                break;
        }

    }

    protected void onNewTerm(Term t) {
        if (autoNeg != null && autoNeg.contains(t.unneg())) {
            link(shadow(t), 0, shadow(t.neg()));
        }
    }

    @Deprecated
    private Event knowComponent(Term y, long start, long end) {
        assert (start != DTERNAL && end != DTERNAL && start != XTERNAL && end != XTERNAL); //mismatch type

        if (start == TIMELESS) {
            assert (end == TIMELESS);
            return know(y);
        } else if (end == ETERNAL) {
            assert (end == ETERNAL);
            return know(y, ETERNAL);
        } else {
            return know(y, start, end);
        }
    }

    boolean solveDT(Term x, Predicate<Event> each) {
        assert (x.dt() == XTERNAL);

        Subterms xx = x.subterms();


        int subs = xx.subs();
        if (subs == 2) {
            Term a = xx.sub(0);
            Term b = xx.sub(1);

            boolean aEqB = a.equals(b);
            //TODO case if aEqNegB

            if (!a.hasXternal() && !b.hasXternal() && (aEqB || !commonSubEventsWithMultipleOccurrences(a, b))) {
                UnifiedSet<Event> ae = new UnifiedSet(2);
                solveOccurrence(a, ax -> {
                    if (ax instanceof Absolute) ae.add(ax);
                    return true;
                });
                int aes = ae.size();
                if (aes > 0) {
                    if (aEqB && aes > 1) {


                        Event[] ab = ae.toArray(new Event[aes]);
                        ArrayUtils.shuffle(ab, random());

                        for (int i = 0; i < ab.length; i++) {
                            for (int j = i + 1; j < ab.length; j++) {
                                if (!solvePairDT(x, ab[i], ab[j], each))
                                    return false;
                            }
                        }


                    } else {
                        UnifiedSet<Event> be = new UnifiedSet(2);
                        solveOccurrence(b, bx -> {
                            if (bx instanceof Absolute) be.add(bx);
                            return true;
                        });
                        int bes = be.size();
                        if (bes > 0) {


                            //Set<Twin<Event>> uniqueTry = new UnifiedSet<>(4);
                            if (!ae.allSatisfy(ax -> {
                                if (!be.allSatisfyWith((bxx, axx) -> {
                                    /*if (uniqueTry.add(twin(axx, bxx)))*/
                                    if (!solvePairDT(x, axx, bxx, each))
                                        return false;

                                    return true; //keep on trying

                                }, ax))
                                    return false;
                                return true;
                            }))
                                return false;

                        }
                    }
                }
            }


            FasterList<Event> rels = new FasterList<>(4);


            Consumer<Event> collect = rels::add;

            byTerm.get(a).forEach(collect);


            if (aEqB) {

            } else {

                byTerm.get(b).forEach(collect);


            }


            int relCount = rels.size();
            if (relCount > 0) {


                if (relCount > 1)
                    rels.shuffleThis(random());

                return bfsPush(rels, new CrossTimeSolver() {
                    @Override
                    protected boolean next(BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> move, Node<Event, TimeSpan> next) {


                        long[] startDT = pathDT(next, a, b, path);
                        if (startDT == null)
                            return true;

                        long start = startDT[0];
                        if (x.op() != CONJ)
                            start = TIMELESS; //cant assume this start time corresponds to the impl term being solved
                        long ddt = startDT[1];
                        return TimeGraph.this.solveDT(x, start, ddt,
                                (start != ETERNAL && start != XTERNAL) ?
                                        durMerge(pathStart(path).id(), pathEnd(path).id()) : 0
                                , each);
                    }
                });

            }


        }


        return true;

    }

    private boolean solvePairDT(Term x, Event a, Event b, Predicate<Event> each) {
        int dt = dt(a, b);

        if (x.op() == CONJ) {

            return solveConj2DT(each, a, dt, b);
        } else {
            //for impl and other types cant assume occurrence corresponds with subject
            return solveDT(x, TIMELESS, dt, durMerge(a, b), each);
        }
    }

    /**
     * solution vector for 2-ary CONJ
     */
    private boolean solveConj2DT(Predicate<Event> each, Event a, int dt, Event b) {

        long ddt = dt(dt);

        if (ddt!=DTERNAL && ddt!=0) {
            assert(ddt!=XTERNAL);
            //swap to correct sequence order
            if (a.start() > b.start()){
                Event z = a;
                a = b;
                b = z;
            }
        }

        Term c = Conj.conjMerge(a.id, 0, b.id, (ddt == DTERNAL ? ETERNAL /* HACK */ : ddt) );
        if (c.op() != CONJ && ((ddt == 0) && (dt!=0))) { //undo parallel-ization if the collapse caused an invalid term
            c = Conj.conjMerge(a.id, 0, b.id, (dt == DTERNAL ? ETERNAL /* HACK */ : dt) );
        }
        if (c.op().conceptualizable) {
            return solveOccurrence(c, a.start(), durMerge(a, b), each);
        }
        return true; //keep trying
    }

    private long durMerge(Event a, Event b) {
        if (a instanceof Absolute && b instanceof Absolute)
            return Math.min(a.dur(), b.dur());
        else if (a instanceof Absolute && !(b instanceof Absolute)) {
            return a.dur();
        } else if (b instanceof Absolute && !(a instanceof Absolute)) {
            return b.dur();
        } else {
            return 0;
        }
    }

    /**
     * tests whether the two terms refer to the same sub-events,
     * which have known multiple occurrences
     * which would cause incorrect results if interpreted literally
     * this prevents separate instances of events from being welded together or arranged in the incorrect temporal order
     * across time when there should be some non-zero dt
     */
    boolean commonSubEventsWithMultipleOccurrences(Term a, Term b) {
        UnifiedSet<Term> eventTerms = new UnifiedSet(2);
        a.eventsWhile((w, aa) -> {
            eventTerms.add(aa);
            return true;
        }, 0, true, true, true, 0);

        final boolean[] ambiguous = {false};
        b.eventsWhile((w, bb) -> {
            if (eventTerms.remove(bb)) {
                if (absoluteCount(bb) > 1) {
                    ambiguous[0] = true;
                    return false;
                }
            }
            return true;
        }, 0, true, true, true, 0);

        return ambiguous[0];
    }

    /**
     * TODO make this for impl only because the ordering of terms is known implicitly from 'x' unlike CONJ
     */
    @Deprecated
    private boolean solveDT(Term x, long start, long ddt, long dur, Predicate<Event> each) {
        assert (ddt != TIMELESS && ddt != XTERNAL && ddt != ETERNAL);
        int dt;


        assert (ddt < Integer.MAX_VALUE) : ddt + " dt calculated";
        dt = (int) ddt;

        Term y = dt(x, dt);

        if (!(y.op().conceptualizable))
            return true;


        if (start != ETERNAL && start != TIMELESS && dt != DTERNAL && dt < 0) {
            start += dt;
        }

        return solveOccurrence(y, start, dur, each);


    }

    private boolean solveOccurrence(Term y, long start, long dur, Predicate<Event> each) {
        return start != TIMELESS ?
                each.test(
                        event(y,
                                start,
                                start != ETERNAL && start != XTERNAL ? (start + dur) : start
                                , false)
                )
                :
                solveOccurrence(y, each);
    }

    /**
     * override to filter dt
     */
    public int dt(int dt) {
        return dt;
    }

    /**
     * preprocess the dt used to construct a new term.
     * ex: dithering
     */
    @Deprecated
    protected Term dt(Term x, int dt) {

        assert (dt != XTERNAL);

        if (dt == DTERNAL) {
            return x.dt(DTERNAL);
        }


        Op xo = x.op();
        if (xo == IMPL) {
            return x.dt(dt - x.sub(0).dtRange());
        } else if (xo == CONJ) {
            if (dt == 0) {
                return x.dt(dt);
            } else {
                //TODO use the provided 'path', if non-null, to order the sequence, which may be length>2 subterms
                if (x.dt() == XTERNAL)
                    return Null;

                int early = Op.conjEarlyLate(x, true);
                if (early == 1)
                    dt = -dt;

                Term xEarly = x.sub(early);
                Term xLate = x.sub(1 - early);

                return Conj.conjMerge(
                        xEarly, 0,
                        xLate, dt);
            }
        } else {

        }


        throw new UnsupportedOperationException();
    }


    /**
     * main entry point to the solver
     */
    public final void solve(Term x, boolean filterTimeless, Predicate<Event> target) {
        solve(x, filterTimeless, new HashSet<>(), target);
    }

    /**
     * main entry point to the solver
     *
     * @seen callee may need to clear the provided seen if it is being re-used
     */
    public boolean solve(Term x, boolean filterTimeless, Set<Event> seen, Predicate<Event> target) {

        Predicate<Event> each = y -> {
            if (seen.add(y)) {


                if (y.start() == TIMELESS && (filterTimeless || x.equals(y.id)))
                    return true;

                return target.test(y);
            } else {
                return true;
            }
        };


        return
                solveAll(x, each);


    }

    private boolean solveExact(Term x, Predicate<Event> each) {
        for (Event e : byTerm.get(x)) {
            if (e instanceof Absolute && !each.test(e))
                return false;
        }
        return true;
    }

    /**
     * each should only receive Event or Unsolved instances, not Relative's
     */
    boolean solveAll(Term x, Predicate<Event> each) {

        if (!x.isTemporal())
            return solveOccurrence(x, each);

        if (x.subterms().hasXternal()) {

            Map<Term, Term> subSolved = new UnifiedMap(2);


            x.subterms().recurseTerms(Term::isTemporal, y -> {
                if (y.dt() == XTERNAL) {
                    solveDT(y, (z) -> {
                        subSolved.put(y, z.id);

                        return false;
                    });
                }
                return true;
            }, null);


            if (!subSolved.isEmpty()) {

                Term y = x.replace(subSolved);
                if (y != null && !(y instanceof Bool)) {
                    x = y;
                }
            }

        } else {
            //learn it as a unique result
            //know(x);
        }

        /* occurrence, with or without any xternal remaining */
        return (x.dt() != XTERNAL || solveDT(x, y -> solveOccurrence(y, each))) &&
                solveOccurrence(x, each);

    }


    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    protected boolean solveOccurrence(Term t, Predicate<Event> each) {

        return solveOccurrence(event(t, TIMELESS, false), each);

    }

    boolean bfsPush(Event roots, Search<Event, TimeSpan> tv) {
        return bfsPush(List.of(roots), tv);
    }

    boolean bfsPush(Collection<Event> roots, Search<Event, TimeSpan> tv) {
        List<Event> created = new FasterList(roots.size());

        for (Event r : roots) {
            Node<Event, nars.time.TimeSpan> n;
            if ((n = node(r)) == null) {
                addNode(r);
                created.add(r);
            }


        }

        Queue<Pair<List<BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>>>, Node<Event, nars.time.TimeSpan>>> q = new ArrayDeque<>(roots.size() /* estimate TODO find good sizing heuristic */);

        boolean result = bfs(roots, tv, q);

        created.forEach(this::removeNode);

        return result;
    }

    private boolean solveOccurrence(Event x, Predicate<Event> each) {
        if (x instanceof Absolute)
            return each.test(x);

        return solveExact(x.id, each) && bfsPush(x, new CrossTimeSolver() {

            Set<Event> nexts = null;

            @Override
            protected boolean next(BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> move, Node<Event, TimeSpan> n) {

                if (!(n.id() instanceof Absolute))
                    return true;

                long pathEndTime = n.id().start();


                long startTime;
                if (pathEndTime == ETERNAL) {
                    startTime = ETERNAL;
                } else {


                    long pathDelta =
                            pathTime(path, false);


                    if (pathDelta == ETERNAL)
                        return true;
                    else {


                        startTime = pathEndTime - (pathDelta);
                    }
                }

                long endTime;
                if (startTime != ETERNAL && startTime != XTERNAL/* && x.id.op() != CONJ*/) {
                    Event s = pathStart(path).id();
                    Event e = pathEnd(path).id();

                    endTime = startTime + durMerge(s, e);
                } else {
                    endTime = startTime;
                }

                Event next = event(x.id, startTime, endTime, false);

                if (nexts == null || nexts.add(next)) {
                    if (nexts == null) {
                        nexts = new UnifiedSet<>(4);
                        nexts.add(next);
                    }
                    return each.test(next);
                } else {
                    return true;
                }
            }

        }) && each.test(x) /* last resort */;
    }


    protected Random random() {
        return ThreadLocalRandom.current();
    }

    public Multimap<Term, Event> events() {
        return byTerm;
    }


    /**
     * absolutely specified event
     */

    public static class Absolute extends Event {
        static final long SAFETY_PAD = 32 * 1024;
        protected final long start;

        protected Absolute(Term t, long start, int hashCode) {
            super(t, hashCode);


            assert (start != TIMELESS);
            if (!((start == ETERNAL || start > 0 || start > ETERNAL + SAFETY_PAD)))
                throw new MathArithmeticException();
            if (!((start < 0 || start < TIMELESS - SAFETY_PAD)))
                throw new MathArithmeticException();

            this.start = start;
        }

        protected Absolute(Term t, long start) {
            this(t, start, Util.hashCombine(t.hashCode(), Long.hashCode(start)));
        }

        @Override
        public final long start() {
            return start;
        }

        public long end() {
            return start;
        }

        @Override
        public long dur() {
            return 0;
        }

        /**
         * contained within but not true if equal
         */
        public boolean containedInButNotEqual(long cs, long ce) {
            return containedIn(cs, ce) && (start != cs || end() != ce);
        }

        public boolean containedIn(long cs, long ce) {
            return (cs <= start && ce >= end());
        }

        public boolean intersectsWith(long cs, long ce) {
            return Longerval.intersects(start, end(), cs, ce);
        }


        /**
         * contains or is equal to
         */
        public boolean containsOrEqual(long cs, long ce) {
            return (start <= cs && end() >= ce);
        }

        @Nullable
        public Longerval unionIfIntersectsButNotEqual(long start, long end) {
            long thisEnd = end();
            long thisStart = this.start;
            if (start != thisStart && end != thisEnd) {
                if (Longerval.intersectLength(start, end, thisStart, thisEnd) >= 0)
                    return Longerval.union(start, end, thisStart, thisEnd);
            }
            return null;
        }
    }

    public static class AbsoluteRange extends Absolute {
        protected final long end;

        protected AbsoluteRange(Term t, long start, long end) {
            super(t, start, Util.hashCombine(t.hashCode(), Long.hashCode(start), Long.hashCode(end)));
            if (end <= start || start == ETERNAL || start == XTERNAL || end == XTERNAL)
                throw new RuntimeException("invalid AbsoluteRange start/end times: " + start + ".." + end);
            this.end = end;
        }

        public long end() {
            return end;
        }

        @Override
        public long dur() {
            return end - start;
        }
    }

    /**
     * TODO RelativeRange?
     */
    public static class Relative extends Event {

        Relative(Term id) {
            super(id, Util.hashCombine(id.hashCode(), Long.hashCode(TIMELESS)));
        }

        @Override
        public final long start() {
            return TIMELESS;
        }

        @Override
        public long end() {
            return TIMELESS;
        }

        @Override
        public long dur() {
            throw new UnsupportedOperationException();
            //return 0;
        }
    }

    /**
     * floating, but potentially related to one or more absolute event
     */

    abstract protected class TimeSolver extends Search<Event, TimeSpan> {


        @Nullable Iterator<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> dynamicLink(Node<Event, TimeSpan> n) {
            Iterator<Event> x = byTerm.get(n.id().id).iterator();
            return x.hasNext() ?
                    Iterators.transform(
                            Iterators.filter(Iterators.transform(
                                    x,

                                    TimeGraph.this::node),
                                    e -> e != null && e != n && !log.hasVisited(e)),
                            that -> new ImmutableDirectedEdge<>(n, TS_ZERO, that)
                    ) : null;
        }


    }

    abstract protected class CrossTimeSolver extends TimeSolver {

        @Override
        protected Iterable<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> next(Node<Event, TimeSpan> n) {
            Iterable<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> e = n.edges(true, true);


            Iterator<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> d = dynamicLink(n);

            return (d != null && d.hasNext()) ? Iterables.concat(e, new FasterList<>(d)) : e;
        }
    }

    /**
     * assuming the path starts with one of the end-points (a and b),
     * if the path ends at either one of them
     * this computes the dt to the other one,
     * and (if available) the occurence startTime of the path
     * returns (startTime, dt) if solved, null if dt can not be calculated.
     */
    @Nullable
    protected long[] pathDT(Node<Event, TimeSpan> n, Term a, Term b, List<BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>>> path) {
        Term endTerm = n.id().id;
        int adjEnd;
        boolean endA = ((adjEnd = choose(a.subTimes(endTerm))) == 0);
        boolean endB = !endA &&
                ((adjEnd = choose(b.subTimes(endTerm))) == 0);

        if (adjEnd == DTERNAL)
            return null;

        if (endA || endB) {
            Event startEvent = pathStart(path).id();

            Term startTerm = startEvent.id;

            boolean fwd = endB && (startTerm.equals(a) || choose(a.subTimes(startTerm)) == 0);
            boolean rev = !fwd && (
                    endA && (startTerm.equals(b) || choose(b.subTimes(startTerm)) == 0));
            if (fwd || rev) {

                long startTime = startEvent.start();

                Event endEvent = Search.pathEnd(path).id();
                long endTime = endEvent.start();


                long dt;
                if (startTime != TIMELESS && startTime != ETERNAL && endTime != TIMELESS && endTime != ETERNAL) {

                    dt = endTime - startTime;
                } else {


                    dt = pathTime(path, true);
                }
                if (dt == TIMELESS)
                    return null;

                if (dt == ETERNAL) {
                    long w;
                    if (startTime == TIMELESS) {
                        w = endTime;
                    } else {
                        if (startTime == ETERNAL)
                            w = endTime;
                        else {
                            w = startTime;
                        }
                    }

                    return new long[]{w, ETERNAL};
                } else {

                    if (a.equals(b))
                        rev = random().nextBoolean();

                    if (rev) {
                        dt = -dt;
                        long s = startTime;
                        startTime = endTime;
                        endTime = s;
                    }


                    return new long[]{
                            (startTime != TIMELESS || endTime == TIMELESS) ?
                                    startTime :
                                    (endTime != ETERNAL ? endTime - dt : ETERNAL)
                            , dt};
                }
            }
        }
        return null;
    }

    private int choose(int[] subTimes) {
        if (subTimes == null)
            return DTERNAL;
        else {
            if (subTimes.length == 1)
                return subTimes[0];
            else
                return subTimes[random().nextInt(subTimes.length)];
        }
    }

    /**
     * computes the length of time spanned from start to the end of the given path
     */
    static long pathTime(List<BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>>> path, boolean eternalAsZero) {

        long t = 0;

        for (BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> r : path) {


            FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan> event = r.getTwo();

            long spanDT = event.id().dt;

            if (spanDT == ETERNAL) {

                if (!eternalAsZero)
                    return ETERNAL;


            } else if (spanDT != 0) {
                t += (spanDT) * (r.getOne() ? +1 : -1);
            }

        }

        return t;
    }

}
