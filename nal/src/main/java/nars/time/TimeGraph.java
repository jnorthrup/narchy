package nars.time;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jcog.TODO;
import jcog.Util;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.search.Search;
import jcog.list.Cons;
import jcog.list.FasterList;
import jcog.math.Longerval;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.util.Conj;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
import static nars.time.Tense.*;
import static nars.time.TimeGraph.TimeSpan.TS_ZERO;
import static org.eclipse.collections.impl.tuple.Tuples.twin;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

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
public class TimeGraph extends MapNodeGraph<TimeGraph.Event, TimeGraph.TimeSpan> {


    /**
     * index by term
     */
    protected final Multimap<Term, Event> byTerm = MultimapBuilder
            .linkedHashKeys()
            .linkedHashSetValues() 
            .build();


    protected final MutableSet<Term> autoNeg = new UnifiedSet();



    /**
     * since CONJ will be constructed with conjMerge, if x is conj the dt between events must be calculated from start-start. otherwise it is implication and this is measured internally
     */
    static long dt(Event aa, Event bb) {

        long aWhen = aa.start();
        long bWhen;
        if (aWhen == ETERNAL || (bWhen = bb.start()) == ETERNAL)
            return DTERNAL;
        else {
            assert (aWhen != XTERNAL);
            assert (bWhen != XTERNAL);
            return bWhen - aWhen;
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
        if (t instanceof Bool || t == Op.imExt || t == Op.imInt)
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
        Node<TimeGraph.Event, TimeGraph.TimeSpan> existing = node(e);
        return existing != null ? existing.id : e;
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
    protected void onAdd(Node<TimeGraph.Event, TimeGraph.TimeSpan> x) {
        Event event = x.id;
        Term eventTerm = event.id;

        if (byTerm.put(eventTerm, event)) {
            onNewTerm(eventTerm);
        }





        int edt = eventTerm.dt(), eventDT = edt;


        switch (eventTerm.op()) {
















            case IMPL:

                Term subj = eventTerm.sub(0);



                Event se = know(subj);



                Term pred = eventTerm.sub(1);
                Event pe = know(pred);
                if (eventDT == DTERNAL) {

                    link(se, ETERNAL, pe);

                    subj.eventsWhile((w, y) -> {
                        link(know(y), ETERNAL, pe);
                        return true;
                    }, 0, true, true, false, 0);

                    pred.eventsWhile((w, y) -> {
                        link(se, ETERNAL, know(y));
                        return true;
                    }, 0, true, true, false, 0);

                } else if (eventDT != XTERNAL) {

                    int st = subj.dtRange();


                    link(se, (eventDT + st), pe);

                    subj.eventsWhile((w, y) -> {
                        link(know(y), eventDT + st - w, pe);
                        return true;
                    }, 0, true, true, false, 0);

                    pred.eventsWhile((w, y) -> {
                        link(se, eventDT + st + w, know(y));
                        return true;
                    }, 0, true, true, false, 0);

                }

                

                break;
            case CONJ:
                


                





























                
                long eventStart = event.start();
                long eventEnd = event.end();

                switch (eventDT) {
                    case XTERNAL:
                        break;

                    case DTERNAL:

                        Subterms es = eventTerm.subterms();
                        int esn = es.subs();
                        Term prev = es.sub(0);
                        for (int i = 1; i < esn; i++) { 
                            Term next = es.sub(i);
                            link(knowComponent(eventStart, eventEnd, 0, prev), ETERNAL, knowComponent(eventStart, eventEnd, 0, next));
                            prev = next;
                        }

                        break;
                    case 0:

                        
                        boolean timed = eventStart != ETERNAL;
                        for (Term s : eventTerm.subterms()) {
                            Event t = eventDT == 0 ?
                                    knowComponent(eventStart, eventEnd, 0, s) : 
                                    (timed ? know(s, eventStart, eventEnd) :  
                                            know(s));
                            if (t != null) {
                                link(event, (eventDT == 0 || timed) ? 0 : ETERNAL,
                                        t 
                                );
                            } else {
                                
                            }
                        }
                        break;
                    default:

                        eventTerm.eventsWhile((w, y) -> {

                            link(event, w, knowComponent(eventStart, eventEnd, w, y));

                            return true;
                        }, 0, false, false, false, 0);
                        break;
                }

                break;
        }

    }

    protected void onNewTerm(Term t) {
        if (autoNeg!=null && autoNeg.contains(t)) {
            link(shadow(t), 0, shadow(t.neg()));
        }
    }

    private Event knowComponent(long eventStart, long eventEnd, long w, Term y) {
        return (eventStart == TIMELESS) ?
                know(y)
                : know(y,
                (eventStart == ETERNAL) ? ETERNAL : (w + eventStart),
                (eventStart == ETERNAL) ? ETERNAL : (w + eventEnd)
        );
    }

    boolean solveDT(Term x, Predicate<Event> each) {
        assert (x.dt() == XTERNAL);

        Subterms xx = x.subterms();













        int subs = xx.subs();
        if (subs == 2) {
            Term a = xx.sub(0);
            Term b = xx.sub(1);

            boolean aEqB = a.equals(b);

            if (!a.hasXternal() && !b.hasXternal() && (aEqB || !commonSubEventsWithMultipleOccurrences(a, b))) {
                UnifiedSet<Event> ae = new UnifiedSet(2);
                solveOccurrence(a, ax -> {
                    if (ax instanceof Absolute) ae.add(ax);
                    return true;
                });
                int aes = ae.size();
                if (aes > 0) {
                    if (aEqB) {

                        
                        if (aes > 1) {

                            Event[] ab = ae.toArray(new Event[aes]);
                            
                            Set<LongLongPair> uniqueTry = new UnifiedSet<>(4);
                            for (int i = 0; i < ab.length; i++) {
                                Event abi = ab[i];
                                long from = abi.start();
                                for (int j = 0; j < ab.length; j++) {
                                    if (i == j) continue;
                                    long to = dt(abi, ab[j]);
                                    if (uniqueTry.add(pair(from, to))) {
                                        if (!solveDT(x, from, to,
                                                
                                                Math.min(abi.dur(), ab[j].dur())
                                                , each))
                                            return false;
                                    }
                                }
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
                            
                            
                            Set<Twin<Event>> uniqueTry = new UnifiedSet<>(4);
                            if (!ae.allSatisfy(ax ->
                                    be.allSatisfyWith((bx, axx) -> {
                                        if (uniqueTry.add(twin(axx, bx))) {
                                            return solveDT(x, axx.start(), dt(axx, bx),
                                                    Math.min(axx.dur(), bx.dur()), 
                                                    each);
                                        } else {
                                            return true;
                                        }
                                    }, ax)))
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
                    protected boolean next(BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> move, Node<TimeGraph.Event, TimeGraph.TimeSpan> next) {

                        

                        long[] startDT = pathDT(next, a, b, path);
                        if (startDT == null)
                            return true; 

                        long start = startDT[0];
                        long ddt = startDT[1];
                        return TimeGraph.this.solveDT(x, start, ddt,
                                (start != ETERNAL && start != XTERNAL) ? Math.min(pathStart(path).dur(), pathEnd(path).dur()) : 0 
                                , each);
                    }
                });

            }

























        }



































        return true;
        
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

    private boolean solveDT(Term x, long start, long ddt, long dur, Predicate<Event> each) {
        assert (ddt != TIMELESS && ddt != XTERNAL && ddt!=ETERNAL);
        int dt;



            assert (ddt < Integer.MAX_VALUE) : ddt + " dt calculated";
            dt = (int) ddt;

        Term y = dt(x, dt);

        if (y instanceof Bool)
            return true;


        if (start != ETERNAL && start != TIMELESS && dt != DTERNAL && dt < 0) {
            start += dt; 
        }

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
     * preprocess the dt used to construct a new term.
     * ex: dithering
     */
    protected Term dt(Term x, int dt) {

        assert (dt != XTERNAL);

        if (dt == DTERNAL) {
            return x.dt(DTERNAL);
        }

        
        Op xo = x.op();
        if (xo == IMPL) {
            return x.dt(dt - x.sub(0).dtRange());
        } else if (xo == CONJ) {
            int early = Op.conjEarlyLate(x, true);
            if (early == 1)
                dt = -dt;

            Term xEarly = x.sub(early);
            Term xLate = x.sub(1 - early);

            return Conj.conjMerge(
                    xEarly, 0,
                    xLate, dt);
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

        }

        /* occurrence, with or without any xternal remaining */
        return (x.dt() != XTERNAL || solveDT(x, y -> solveOccurrence(y.id, each))) &&
                solveOccurrence(x, each);

    }






    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    protected boolean solveOccurrence(Term t, Predicate<Event> each) {

        Event x = event(t, TIMELESS, false);

        return solveOccurrence(x, each);

    }

    boolean bfsPush(Event roots, Search<Event, TimeSpan> tv) {
        return bfsPush(List.of(roots), tv);
    }

    boolean bfsPush(Collection<Event> roots, Search<Event, TimeSpan> tv) {
        List<Event> created = new FasterList(roots.size());

        for (Event r : roots) {
            Node<Event, TimeSpan> n;
            if ((n = node(r)) == null) {
                addNode(r);
                created.add(r);
            }



        }

        Queue<Pair<List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>>, Node<Event, TimeSpan>>> q = new ArrayDeque<>(roots.size() /* estimate TODO find good sizing heuristic */);

        boolean result = bfs(roots, tv, q);

        created.forEach(this::removeNode);

        return result;
    }

    private boolean solveOccurrence(Event x, Predicate<Event> each) {
        assert(x instanceof Relative);
        
        return solveExact(x.id, each) && bfsPush(x, new CrossTimeSolver() {

            Set<Event> nexts = null;

            @Override
            protected boolean next(BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> move, Node<Event, TimeSpan> n) {

                if (!(n.id instanceof Absolute))
                    return true;

                long pathEndTime = n.id.start();
                


                long startTime;
                if (pathEndTime == ETERNAL) {
                    startTime = ETERNAL;
                } else {

                    
                    long pathDelta =
                        pathTime(path, false);
                        

                    if (pathDelta == ETERNAL) 
                        return true;
                    else {





                        




                        startTime = pathEndTime - (pathDelta );
                    }
                }

                long endTime;
                if (startTime != ETERNAL && startTime != XTERNAL && x.id.op()!=CONJ) {
                    long startDur = pathStart(path).dur();
                    long endDur = pathEnd(path).dur();
                    long dur = Math.min(startDur, endDur); 
                    endTime = startTime + dur;
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



    static final class TimeSpan {
        public final static TimeSpan TS_ZERO = new TimeSpan(0);
        
        

        public final static TimeSpan TS_ETERNAL = new TimeSpan(ETERNAL);
        public final long dt;

        private TimeSpan(long dt) {
            this.dt = dt;
            
            
        }

        public static TimeSpan the(long dt) {
            assert (dt != TIMELESS);
            assert (dt != XTERNAL) : "probably meant to use TIMELESS";
            assert (dt != DTERNAL) : "probably meant to use ETERNAL";

            if (dt == 0) {
                return TS_ZERO;
            } else if (dt == ETERNAL) {
                return TS_ETERNAL;
            } else {
                return new TimeSpan(dt);
            }
        }

        @Override
        public int hashCode() {
            return Long.hashCode(dt);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || ((obj instanceof TimeSpan && dt == ((TimeSpan) obj).dt));
        }

        @Override
        public String toString() {
            return (dt == ETERNAL ? "~" : (dt >= 0 ? ("+" + dt) : ("-" + (-dt))));
            
        }
    }


    public abstract static class Event implements LongObjectPair<Term> {

        public final Term id;
        private final int hash;

        Event(Term id, int hash) {
            this.id = id;
            this.hash = hash;

        }






        abstract public long start();

        abstract public long end();

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            Event e = (Event) obj;
            return (hash == e.hash) && (start() == e.start()) && (end() == e.end()) && id.equals(e.id);
        }

        




        @Override
        public final String toString() {
            long s = start();

            if (s == TIMELESS) {
                return id.toString();
            } else if (s == ETERNAL) {
                return id + "@ETE";
            } else {
                long e = end();
                if (e == s)
                    return id + "@" + s;
                else
                    return id + "@" + s + ".." + e;
            }
        }

        @Override
        public long getOne() {
            return start();
        }

        @Override
        public Term getTwo() {
            return id;
        }

        @Override
        public int compareTo(LongObjectPair<Term> o) {
            throw new TODO();
        }

        public long dur() {
            return end() - start();
        }
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

        /**
         * contained within but not true if equal
         */
        public boolean containedInButNotEqual(long cs, long ce) {
            return (cs <= start && ce >= end() && start != cs && end() != ce);
        }

        /**
         * contains or is equal to
         */
        public boolean containsOrEqual(long ps, long pe) {
            return (start <= ps && end() >= pe);
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

    }

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
    }

    /**
     * floating, but potentially related to one or more absolute event
     */

    abstract protected class TimeSolver extends Search<Event, TimeSpan> {


        @Nullable Iterator<ImmutableDirectedEdge<Event, TimeSpan>> dynamicLink(Node<TimeGraph.Event, TimeGraph.TimeSpan> n) {
            Iterator<Event> x = byTerm.get(n.id.id).iterator();
            return x.hasNext() ?
                    Iterators.transform(
                        Iterators.filter(Iterators.transform(
                            x,
                            
                            TimeGraph.this::node),
                    e -> e!=null && e != n && !log.hasVisited(e)),
                    that -> new ImmutableDirectedEdge<>(n, that, TS_ZERO) 
            ) : null;
        }






    }

    abstract protected class CrossTimeSolver extends TimeSolver {

        @Override
        protected Iterable<ImmutableDirectedEdge<Event, TimeSpan>> next(Node<TimeGraph.Event, TimeGraph.TimeSpan> n) {
            Iterable<ImmutableDirectedEdge<Event, TimeSpan>> e = n.edges(true, true);

            
            Iterator<ImmutableDirectedEdge<Event, TimeSpan>> d = dynamicLink(n);

            return (d != null && d.hasNext()) ? Iterables.concat(e, new FasterList<>(d)) : e;
        }
    }
    protected static Event pathStart(List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path) {
        BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> step = path.get(0);
        return step.getTwo().from(step.getOne()).id;
    }

    protected static Event pathEnd(List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path) {
        BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> step = path instanceof Cons ?
                ((Cons<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>>) path).tail : path.get(path.size() - 1);
        return step.getTwo().to(step.getOne()).id;
    }

    /**
     * assuming the path starts with one of the end-points (a and b),
     * if the path ends at either one of them
     * this computes the dt to the other one,
     * and (if available) the occurence startTime of the path
     * returns (startTime, dt) if solved, null if dt can not be calculated.
     */
    @Nullable
    protected long[] pathDT(Node<TimeGraph.Event, TimeGraph.TimeSpan> n, Term a, Term b, List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path) {
        Term endTerm = n.id.id;
        int adjEnd;
        boolean endA = ((adjEnd = choose(a.subTimes(endTerm))) == 0);
        boolean endB = !endA &&
                ((adjEnd = choose(b.subTimes(endTerm))) == 0);

        if (adjEnd == DTERNAL)
            return null;

        if (endA || endB) {
            Event startEvent = pathStart(path);

            Term startTerm = startEvent.id;

            boolean fwd = endB && (startTerm.equals(a) || choose(a.subTimes(startTerm)) == 0);
            boolean rev = !fwd && (
                    endA && (startTerm.equals(b) || choose(b.subTimes(startTerm)) == 0));
            if (fwd || rev) {

                long startTime = startEvent.start();

                Event endEvent = pathEnd(path);
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
    long pathTime(List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path, boolean eternalAsZero) {

        long t = 0;
        
        for (BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> r : path) {


            ImmutableDirectedEdge<Event, TimeSpan> event = r.getTwo();

            long spanDT = event.id.dt;

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
