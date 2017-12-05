package nars.derive.time;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jcog.Util;
import jcog.data.graph.hgraph.Edge;
import jcog.data.graph.hgraph.HashGraph;
import jcog.data.graph.hgraph.Node;
import jcog.data.graph.hgraph.Search;
import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.Task;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.CONJ;
import static nars.derive.time.TimeGraph.TimeSpan.TS_ZERO;
import static nars.time.Tense.*;
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
public class TimeGraph extends HashGraph<TimeGraph.Event, TimeGraph.TimeSpan> {

    private static final boolean allowSelfLoops = true;

    public static class TimeSpan {
        public final long dt;
        //public final float weight;

        public final static TimeSpan TS_ZERO = new TimeSpan(0);
        //        public final static TimeSpan TS_POS_ONE = new TimeSpan(+1);
//        public final static TimeSpan TS_NEG_ONE = new TimeSpan(-1);
        public final static TimeSpan TS_ETERNAL = new TimeSpan(ETERNAL);

        public static TimeSpan the(long dt) {
            assert (dt != TIMELESS);
            assert (dt != XTERNAL);
//            assert (dt != ETERNAL);

            if (dt == 0) {
                return TS_ZERO;
            } else if (dt == ETERNAL) {
                return TS_ETERNAL;
            } else {
                return new TimeSpan(dt);
            }
        }

        private TimeSpan(long dt) {
            this.dt = dt;
            //this.weight = weight;
            //this.hash = Long.hashCode(dt);
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
            //+ (weight != 1 ? "x" + n2(weight) : "");
        }
    }

    /**
     * index by term
     */
    public final Multimap<Term, Event> byTerm = MultimapBuilder
            .linkedHashKeys()
            .linkedHashSetValues() //maybe use TreeSet values and order them by best first
            .build();


    public Event know(Term t) {
        return know(t, TIMELESS);
    }


    public Event know(Term t, long start) {
        return event(t, start, true);
    }

    public void know(Task ta) {
        Term tt = ta.term();

        long start = ta.start();
        long end = ta.end();
        if (end != start && tt.op() != CONJ) {
            //add each endpoint separately
            event(tt, start, true);
            event(tt, end, true);
        } else {
            event(tt, start, true);
        }

    }

    public Event event(Term t, long start) {
        return event(t, start, false);
    }

    public Event event(Term t, long when, boolean add) {
        Event e =
                when == TIMELESS ? new Relative(t) : new Absolute(t, when);
        return add ? add(e).id : event(e);
    }

    public Event event(Event e) {
        Node<Event, TimeSpan> existing = node(e);
        return existing != null ? existing.id : e;
    }


    public boolean link(Event before, TimeSpan e, Event after) {
        return edgeAdd(add(before), e, add(after));
    }

    public void link(Event x, long dt, Event y) {

        if (!allowSelfLoops && (x == y || ((x.id.equals(y.id)) && (x.when() == y.when()))))
            return; //loop

        boolean swap = false;
//        if (dt == ETERNAL || dt == TIMELESS || dt == 0) {
        //lexical order
        int vc = Integer.compare(x.id.volume(), y.id.volume());
        if (vc == 0) {

            if (x.hashCode() > y.hashCode()) { //TODO write real comparator
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
        Event event = x.id;
        Term eventTerm = event.id;

        byTerm.put(eventTerm, event);
        Term tRoot = eventTerm.root();
        if (!tRoot.equals(eventTerm))
            byTerm.put(tRoot, event);

        int eventDT = eventTerm.dt();

        switch (eventTerm.op()) {
            case NEG:
                link(know(eventTerm.unneg()), 0, event); //lower priority?
                break;
            case IMPL:

                Term subj = eventTerm.sub(0);
                Event se = know(subj);
                Term pred = eventTerm.sub(1);
                Event pe = know(pred);
                if (eventDT != XTERNAL && eventDT != DTERNAL) {

                    int st = subj.dtRange();

                    //link(se, (eventDT + st), pe);

                    //if (subj.hasAny(CONJ)) {
                    subj.eventsWhile((w, y) -> {
                        link(know(y), eventDT + st - w, pe);
                        return true;
                    }, 0, true, false, false, 0);

                    //if (pred.hasAny(CONJ)) {
                    pred.eventsWhile((w, y) -> {
                        link(se, eventDT + st + w, know(y));
                        return true;
                    }, 0, true, false, false, 0);

                }

                //link(se, 0, event); //WEAK

                break;
            case CONJ:
                TermContainer tt = eventTerm.subterms();
                long et = event.when();


                int s = tt.subs();
//                if (et == TIMELESS) {
//                    //chain the sibling subevents
//                    if (s == 2) {
//                        Term se0 = tt.sub(0);
//                        Event e0 = know(se0);
//                        Term se1 = tt.sub(1);
//                        Event e1 = know(se1);
//                        int dt;
//                        Event earliest;
//                        if (eventDT == DTERNAL) {
//                            dt = DTERNAL;
//                            earliest = e0; //just use the first by default
//                        } else {
//                            long t0 = eventTerm.subTime(se0);
//                            long t1 = eventTerm.subTime(se1);
//                            long ddt = (int) (t1 - t0);
//                            assert (ddt < Integer.MAX_VALUE);
//                            dt = (int) ddt;
//                            earliest = t0 < t1 ? e0 : e1;
//                        }
//                        link(e0, dt, e1);
//                        link(earliest, 0, event);
//
//                    } else {
//                        throw new TODO();
//                    }
//
//                } else

                //locate the events and sub-events absolutely
                switch (eventDT) {
                    case XTERNAL:
                    case DTERNAL:
                        eventTerm.subterms().forEach(this::know); //TODO can these be absolute if the event is?
                        break;
                    default:

                        eventTerm.eventsWhile((w, y) -> {

                            link(event, w, et != TIMELESS ?
                                    know(y, et == ETERNAL ? ETERNAL : w + et) :
                                    know(y));

                            return true;
                        }, 0, true, false, false, 0);
                        break;
                }

                break;
        }

    }


    boolean solveDT(Term x, Predicate<Event> each) {
        assert (x.dt() == XTERNAL);

        TermContainer xx = x.subterms();
        FasterList<Event> events = new FasterList<>(byTerm.get(x.root()));
        for (int i = 0, eventsSize = events.size(); i < eventsSize; i++) {
            Event r = events.get(i);
            if (r.absolute()) {
                if (r.id.subterms().equals(xx)) {
                    if (!each.test(r))
                        return false; //done
                }
            }

        }


        int subs = x.subs();
        if (subs == 2) {
            Term a = xx.sub(0);
            Term b = xx.sub(1);
            Set<Event>[] exact = new Set[2]; //exact occurrences of each subterm

            boolean aEqB = b.equals(a);


            List<Event> sources = new FasterList<>();
            Consumer<Event> collect = z -> {
                if (z.absolute()) {
                    if (z.id.equals(a)) {
                        if (exact[0] == null) exact[0] = new HashSet();
                        exact[0].add(z);
                    }
                    if (!aEqB && z.id.equals(b)) {
                        if (exact[1] == null) exact[1] = new HashSet();
                        exact[1].add(z);
                    }
//                    return true;
                }
                //return false;
//                return true; //include non-absolutes

                sources.add(z);
            };

            byTerm.get(a).forEach(collect);
//            if (exact[0]==null && a.op()==NEG)
//                byTerm.get(a.unneg()).forEach(collect);

            if (aEqB) {
                exact[1] = exact[0];
            } else {
                byTerm.get(b).forEach(collect);
//                if (exact[1] == null && b.op()==NEG)
//                    byTerm.get(b.unneg()).forEach(collect);
            }

            if (exact[0] != null && exact[1] != null) {
                //known exact occurrences for both subterms
                //iterate all possibilities
                //TODO order in some way
                //simple case:
                if (exact[0].size() == 1 && exact[1].size() == 1) {
                    Event aa = exact[0].iterator().next();
                    Event bb = exact[1].iterator().next();
                    if (!solveDT(x, each, aa, bb))
                        return false;
                } else {
                    if (!exact[0].stream().allMatch(ae ->
                            exact[1].stream().allMatch(be ->
                                    solveDT(x, each, ae, be))))
                        return false;
                }

            }


            int ns = sources.size();
            if (ns > 0) {
                //TODO sort to process smallest terms first
                if (ns > 1) {
                    sources.sort(Comparator.comparingInt(z -> z.id.volume()));
                }

                //            boolean repeat = a.unneg().equals(b.unneg()); //if true, then we must be careful when trying this in a commutive-like result which would collapse the two terms

                return dfs(sources, new CrossTimeSolver() {
                    @Override
                    protected boolean next(BooleanObjectPair<Edge<Event, TimeSpan>> move, Node<Event, TimeSpan> next) {

                        //System.out.println(path);

                        long[] startDT = pathDT(next, a, b, path);
                        if (startDT == null)
                            return true; //nothing at this step

                        long start = startDT[0];
                        long ddt = startDT[1];
                        return TimeGraph.this.solveDT(x, start, ddt, each);
                    }
                });

            }

        } else {
            assert (x.op() == CONJ);
            List<LongObjectPair<Term>> when = $.newArrayList();
            for (int ix = 0; ix < subs; ix++) {
                //assert(!z.hasXternal());
                solveOccurrence(event(xx.sub(ix), TIMELESS), (ze) -> {
                    if (ze.when() == TIMELESS)
                        return true; //keep trying
                    when.add(pair(ze.when(), ze.id));
                    return false; //just one, for now //TODO see if there are any others
                });
            }
            if (when.size() == subs) {
                when.sort(Comparator.comparingLong(LongObjectPair::getOne));
                long base = when.get(0).getOne();
                Term zz = when.get(0).getTwo();
                for (int i = 1; i < subs; i++) {
                    LongObjectPair<Term> wgi = when.get(i);
                    zz = Op.conjMerge(zz, 0, wgi.getTwo(), wgi.getOne() - base);
                    if (zz instanceof Bool)
                        return true; //failure
                }
                return each.test(event(zz, base));
            }
        }


//        if (x.subs() == 2) {
//            Term a = xx.sub(0);
//            Set<Event<Term>> ae = absolutes(a);
//            if (!ae.isEmpty()) {
//                Term b = xx.sub(1);
//                Set<Event<Term>> be = absolutes(b);
//                if (!be.isEmpty()) {
//                    //cartesian product of the two, maybe prioritized by least distance?
//                    LazyIterable<Pair<Event<Term>, Event<Term>>> matrix = Sets.cartesianProduct(ae, be);
//                    matrix.allSatisfy(ab -> {
//
//                        long bt = ab.getTwo().start();
//                        long at = ab.getOne().end();
//
//                        int dt;
//                        if (bt == ETERNAL || at == ETERNAL) {
//                            dt = DTERNAL;
//                        } else {
//                            long ddt = bt - at;
//                            assert (Math.abs(ddt) < Integer.MAX_VALUE);
//                            dt = (int) ddt;
//                        }
//
//                        Term tt = x.dt(dt);
//                        if (tt.op().conceptualizable)
//                            return each.test(tt);
//                        else
//                            return true;
//                    });
//                }
//            }
//        }

        return true; //each.test(event(x, TIMELESS)); //last resort
    }

    private boolean solveDT(Term x, Predicate<Event> each, Event aa, Event bb) {
//        if (x.op() == CONJ && bb.start() < aa.start()) {
//            //earliest first for CONJ, since it will be constructed with conjMerge
//            Event cc = aa;
//            aa = bb;
//            bb = cc;
//        }
        long start;
        if (x.op() == CONJ) {
            //earliest first for CONJ, since it will be constructed with conjMerge
            long astart = aa.when();
            long bstart = bb.when();
            if (astart != TIMELESS && bstart != TIMELESS) {
                start = Math.min(astart, bstart);
            } else {
                start = astart;
            }
        } else {
            start = aa.when();
        }
        return solveDT(x, start, dt(x, aa, bb), each);
    }

    /**
     * since CONJ will be constructed with conjMerge, if x is conj the dt between events must be calculated from start-start. otherwise it is implication and this is measured internally
     */
    static long dt(Term x, Event aa, Event bb) {
        return bb.when() - aa.when();
        //return bb.start() - (x.op() == CONJ ? aa.start() : aa.end());
    }

    private boolean solveDT(Term x, long start, long ddt, Predicate<Event> each) {
        assert (ddt < Integer.MAX_VALUE);
        int dt = (int) ddt;
        Term y = dt(x, dt);

        if (y instanceof Bool)
            return true;

        if (start != ETERNAL && start != TIMELESS && dt != DTERNAL && dt < 0 && y.op() == CONJ) {
            start += dt; //shift to left align
        }

        return start != TIMELESS ?
                each.test(
                        event(y, start, false)
                )
                :
                solveOccurrence(event(y, TIMELESS), each);


    }

    /**
     * preprocess the dt used to construct a new term.
     * ex: dithering
     */
    protected Term dt(Term x, int dt) {

        //CONSTRUCT NEW TERM
        Term y;
        if (x.op() != CONJ) {
            y = x.dt((dt != XTERNAL && dt != DTERNAL) ? dt - x.sub(0).dtRange() : dt); //IMPL
        } else {
            int early = Op.conjEarlyLate(x, true);
            if (early == 1)
                dt = -dt;
            y = Op.conjMerge(
                    x.sub(early), 0,
                    x.sub(1 - early), dt);
        }


        return y;
    }


    protected Random random() {
        return ThreadLocalRandom.current();
    }

//    final static LongSet EMPTY_LONG_SET = LongSets.immutable.empty();

    public void solve(Term x, Predicate<Event> each) {
        solve(x, true, each);
    }

    public void solve(Term x, boolean filterTimeless, Predicate<Event> target) {

        Set<Event> seen = new HashSet<>();

        Predicate<Event> each = y -> {
            if (seen.add(y)) {
                if (y.when() == TIMELESS && (filterTimeless || x.equals(y.id)))
                    return true;
                else
                    return target.test(y);
            } else {
                return true; //filtered
            }
        };

        //test for existing exact solutions to the exact term
        if (!solveExact(x, each))
            return;

        if (solveAll(x, each)) {
            //as a last resort:
            each.test(event(x, TIMELESS));
        }
    }

    private boolean solveExact(Term x, Predicate<Event> each) {
        for (Event e : byTerm.get(x)) {
            if (e.absolute() && !each.test(e))
                return false;
        }
        return true;
    }


    /**
     * each should only receive Event or Unsolved instances, not Relative's
     */
    boolean solveAll(Term x, Predicate<Event> each) {

        //collect XTERNAL terms that will need to be solved
        if (x.hasAny(Op.Temporal)) {
            final TreeSet<Term> xternalsToSolve = new TreeSet<>();
            if (x.dt() == XTERNAL)
                xternalsToSolve.add(x);

            x.subterms().recurseTerms(Term::isTemporal, y -> {
                if (y.dt() == XTERNAL)
                    xternalsToSolve.add(y);
                return true;
            }, null);

            if (!xternalsToSolve.isEmpty()) {

                //solve the XTERNAL from simplest to most complex. the canonical sort order of compounds will naturally descend this way
                if (!Util.andReverse(xternalsToSolve.toArray(new Term[xternalsToSolve.size()]), u ->
                        solveDT(u, v -> {

                            Term y = x.replace(u, v.id);
                            if (y == null || y instanceof Bool)
                                return true; //continue

                            boolean ye = y.equals(x);
                            if (!ye) {

                                if (!solveExact(y, each))
                                    return false;

                                boolean yx = y.hasXternal();
                                if (u.equals(x) && !yx) {
                                    if (v.when() != TIMELESS) {
                                        return each.test(v); //shortcut to finish
                                    } else {
                                        return solveOccurrence(v, each); //only need to solve occurrence
                                    }
                                }

                                return yx ?
                                        solveAll(y, each) : //recurse to solve remaning xternal's
                                        solveOccurrence(event(y, TIMELESS), each); //just need to solve occurrence now
                            } else {
                                //term didnt change...
                                if (v.when() != TIMELESS) {
                                    if (!each.test(v)) //but atleast it somehow solved an occurrence time
                                        return false;
                                }
                            }


                            return true; //keep trying
                        })
                ))
                    return false;
            }
        }

        return solveOccurrence(event(x, TIMELESS), each);
    }

//    protected LinkedHashMap<Term, LongSet> absolutes(Term x) {
//        LinkedHashMap<Term, LongSet> m = new LinkedHashMap<>();
//        absolutes(x, m);
//        return m;
//    }

//    protected void absolutes(Term x, Map<Term, LongSet> absolute) {
//
//        if (absolute.putIfAbsent(x, EMPTY_LONG_SET) != null)
//            return; //already processed
//
//        switch (x.op()) {
//            case CONJ:
//                x.eventsWhile((w, xx) -> {
//                    if (x != xx)
//                        absolutes(xx, absolute);
//                    return true;
//                }, 0, true, false, true, 0);
//                break;
//            case IMPL:
//                absolutes(x.sub(0), absolute);
//                absolutes(x.sub(1), absolute);
//                break;
//            case NEG:
//                absolutes(x.unneg(), absolute);
//                break;
//        }
//
//        Collection<Event> xe = byTerm.get(x);
//        if (xe != null) {
//            LongHashSet l = new LongHashSet();
//            xe.forEach(e -> {
//                if (e.absolute())
//                    l.add(e.start());
//            });
//
//            if (!l.isEmpty()) {
//                absolute.put(x, l.toImmutable());
//            }
//        }
//
//    }


    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    private boolean solveOccurrence(Event x, Predicate<Event> each) {

        return dfs(x, new CrossTimeSolver() {
            @Override
            protected boolean next(BooleanObjectPair<Edge<Event, TimeSpan>> move, Node<Event, TimeSpan> n) {

                if (n.id.absolute()) {

                    long pathEndTime = n.id.when();
                    BooleanObjectPair<Edge<Event, TimeSpan>> pathStart = path.get(0);
                    Term pathStartTerm = pathStart.getTwo().from(pathStart.getOne()).id.id;

                    long startTime = pathEndTime == ETERNAL ?
                            ETERNAL :
                            pathEndTime - pathTime(path, false);

                    return each.test(event(x.id, startTime));
                }

                return true;

            }
        }) && each.test(x) /* last resort */;

    }


    abstract protected class TimeSolver extends Search<Event, TimeSpan> {


        Stream<Edge<Event, TimeSpan>> dynamicLink(Node<Event, TimeSpan> n) {
            return dynamicLink(n, x -> true);
        }

        Stream<Edge<Event, TimeSpan>> dynamicLink(Node<Event, TimeSpan> n, Predicate<Event> preFilter) {
            return byTerm.get(n.id.id).stream()
                    .filter(preFilter)
                    .map(TimeGraph.this::node)
                    .filter(e -> e != n)
                    .map(that ->
                            new Edge<>(n, that, TS_ZERO) //co-occurring
                    );
        }


//        public long startTime(FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {
//            BooleanObjectPair<Edge<Event<Term>, TimeSpan>> firstStep = path.get(0);
//            boolean outOrIn = firstStep.getOne();
//            return firstStep.getTwo().from(outOrIn).id.start();
//        }

        /**
         * computes the length of time spanned from start to the end of the given path
         */
        public long pathTime(FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path, boolean eternalAsZero) {

            long t = 0;
            //compute relative path
            for (int i = 0, pathSize = path.size(); i < pathSize; i++) {
                BooleanObjectPair<Edge<Event, TimeSpan>> r = path.get(i);
                Edge<Event, TimeSpan> event = r.getTwo();

                long spanDT = event.get().dt;

                if (spanDT == ETERNAL) {
                    //no change, crossed a DTERNAL step. this may signal something
                    if (!eternalAsZero)
                        return ETERNAL;
                } else if (spanDT != 0) {
                    t += (spanDT) * (r.getOne() ? +1 : -1);
                }
            }

            return t;
        }

        /**
         * assuming the path starts with one of the end-points (a and b),
         * if the path ends at either one of them
         * this computes the dt to the other one,
         * and (if available) the occurence startTime of the path
         * returns (startTime, dt) if solved, null if dt can not be calculated.
         */
        @Nullable
        protected long[] pathDT(Node<Event, TimeSpan> n, Term a, Term b, FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {
            Term endTerm = n.id.id;
            boolean endA = a.equals(endTerm);
            boolean endB = b.equals(endTerm);

            if (endA || endB) {
                BooleanObjectPair<Edge<Event, TimeSpan>> startStep = path.getFirst();
                BooleanObjectPair<Edge<Event, TimeSpan>> endStep = path.getLast();
                Edge<Event, TimeSpan> startEdge = startStep.getTwo();
                Edge<Event, TimeSpan> endEdge = endStep.getTwo();

                Event startEvent = startEdge.from(startStep.getOne()).id;
                Event endEvent = endEdge.to(endStep.getOne()).id;
                Term startTerm = startEvent.id;

                if ((endA && startTerm.equals(b)) || (endB && startTerm.equals(a))) {


                    long startTime = startEvent.absolute() ? startEvent.when() : TIMELESS;
                    long endTime = endEvent.absolute() ? endEvent.when() : TIMELESS;

                    long dt;
                    if (startTime != TIMELESS && startTime != ETERNAL && endTime != TIMELESS && endTime != ETERNAL) {
                        //use the two endpoints and subtract the dt

                        dt = endTime - startTime;
                    } else {

                        //TODO more rigorous traversal of the dt chain
                        //compute from one end to the other, summing dt in the correct direction along the way
                        //special handling for encountered absolute terms and DTERNAL

                        dt = pathTime(path, true);
                    }
                    if (dt == TIMELESS)
                        return null;

                    if (endA && dt != ETERNAL)
                        dt = -dt; //reverse


                    return new long[]{
                            (startTime != TIMELESS || endTime == TIMELESS) ?
                                    startTime :
                                    (endTime != ETERNAL ? endTime - dt : ETERNAL)
                            , dt};
                }
            }
            return null;
        }

    }


    /**
     * supplies additional virtual edges to other points in time for the given node
     */
    abstract protected class CrossTimeSolver extends TimeSolver {

        @Override
        protected Stream<Edge<Event, TimeSpan>> next(Node<Event, TimeSpan> n) {
            return Stream.concat(n.edges(true, true), dynamicLink(n));
        }

    }

//    /**
//     * TODO not ready yet
//     */
//    protected class DTCommutiveSolver extends TimeSolver {
//        private final Set<Term> targets;
//        private final LongLongPredicate each;
//
//        public DTCommutiveSolver(Set<Term> targets, LongLongPredicate each) {
//            this.targets = targets;
//            this.each = each;
//        }
//
//        @Override
//        protected boolean visit(Node<Event, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {
//            if (path.isEmpty())
//                return true;
//
//
//            //System.out.println(path);
//
//            //long[] dt = pathDT(n, a, b, path);
//
////            if (dt!=null)
////                if (!each.accept(dt[0], dt[1]))
////                    return false;
//
//            return true;
//        }
//    }

    /**
     * absolutely specified event
     */
    public abstract static class Event {

        public final Term id;
        private final int hash;

        protected Event(Term id, long start) {
            this.id = id;
            this.hash = Util.hashCombine(id.hashCode(), Long.hashCode(start));
        }

        abstract public long when();

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            Event e = (Event) obj;
            return hash == e.hash && when() == e.when() && id.equals(e.id);
        }

        //        public float pri() {
//            return 1f;
//        }


        @Override
        public final String toString() {
            long s = when();

            if (s == TIMELESS) {
                return id.toString();
            } else if (s == ETERNAL) {
                return id + "@ETE";
            } else {
                return id + "@" + s;
            }
        }

        public final boolean absolute() {
            return when() != TIMELESS;
        }
    }

    static class Absolute extends Event {
        protected final long when;

        static final long SAFETY_PAD = 32 * 1024;

        public static void validate(long x) {
            if (!((x == ETERNAL || x > 0 || x > ETERNAL + SAFETY_PAD))) //for catching time calculation bugs
                throw new MathArithmeticException();
            if (!((x < 0 || x < TIMELESS - SAFETY_PAD))) //for catching time calculation bugs
                throw new MathArithmeticException();
        }

        private Absolute(Term t, long when) {
            super(t, when);
            validate(when);
            this.when = when;
        }

        @Override
        public final long when() {
            return when;
        }

    }


    /**
     * floating, but potentially related to one or more absolute event
     */
    public static class Relative extends Event {

        public Relative(Term id) {
            super(id, TIMELESS);
        }

        @Override
        public final long when() {
            return TIMELESS;
        }

    }


}
