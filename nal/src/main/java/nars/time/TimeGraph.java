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
            .linkedHashSetValues() //maybe use TreeSet values and order them by best first
            .build();


    private boolean autoNeg = true;


    /**
     * since CONJ will be constructed with conjMerge, if x is conj the dt between events must be calculated from start-start. otherwise it is implication and this is measured internally
     */
    static long dt(Term x, Event aa, Event bb) {

        long aWhen = aa.start();
        long bWhen;
        if (aWhen == ETERNAL || (bWhen = bb.start()) == ETERNAL)
            return DTERNAL;
        else {
            assert (aWhen != XTERNAL);
            assert (bWhen != XTERNAL);
            return bWhen - aWhen;
        }
        //return bb.start() - (x.op() == CONJ ? aa.start() : aa.end());
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

                //merge with any shorter or adjacent events that this range fully contains
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

//        if (!add && !byTerm.containsKey(e.id))
//            add = true; //add if non-existent in byTerms

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
            return; //loop

        boolean swap = false;
//        if (dt == ETERNAL || dt == TIMELESS || dt == 0) {
        //lexical order

//        if (dt == 0 && x.id.unneg().equals(y.id/*.unneg()*/))
//            return; //throw new RuntimeException("instantaneous self loop");

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
    protected void onAdd(Node<TimeGraph.Event, TimeGraph.TimeSpan> x) {
        Event event = x.id;
        Term eventTerm = event.id;

        if (byTerm.put(eventTerm, event)) {
            onNewTerm(eventTerm);
        }

//        Term tRoot = eventTerm.root();
//        if (!tRoot.equals(eventTerm))
//            byTerm.put(tRoot, event);

        int edt = eventTerm.dt(), eventDT = edt;


        switch (eventTerm.op()) {
//            case INH:
//                @Nullable DynamicTruthModel dmt = DefaultConceptBuilder.unroll(eventTerm); //TODO optimize
//                if (dmt != null) {
//                    Term[] c = dmt.components(eventTerm);
//                    if (c != null && c.length > 1) {
//                        for (Term cc : c) {
//                            link(know(cc), 0, event);
//                        }
//                    }
//                }
//                break;

//            case NEG:
//                if (autoUnneg)
//                    link(know(eventTerm.unneg()), 0, event); //lower priority?
//                break;
            case IMPL:

                Term subj = eventTerm.sub(0);
//                if (event instanceof Absolute && event.start()!=ETERNAL) {
//                    se = know(subj, event.start(), event.end()); //subject at the event time
//                } else {
                Event se = know(subj);
//
//                }
//                link(se, 0, event);
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

                //link(se, 0, event); //WEAK

                break;
            case CONJ:
                //Subterms tt = eventTerm.subterms();


                //int s = tt.subs();
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
                long eventStart = event.start();
                long eventEnd = event.end();

                switch (eventDT) {
                    case XTERNAL:
                        break;

                    case DTERNAL:

                        Subterms es = eventTerm.subterms();
                        int esn = es.subs();
                        Term prev = es.sub(0);
                        for (int i = 1; i < esn; i++) { //dternal chain
                            Term next = es.sub(i);
                            link(knowComponent(eventStart, eventEnd, 0, prev), ETERNAL, knowComponent(eventStart, eventEnd, 0, next));
                            prev = next;
                        }

                        break;
                    case 0:

                        //  eventTerm.subterms().forEach(this::know); //TODO can these be absolute if the event is?
                        boolean timed = eventStart != ETERNAL;
                        for (Term s : eventTerm.subterms()) {
                            Event t = eventDT == 0 ?
                                    knowComponent(eventStart, eventEnd, 0, s) : //0
                                    (timed ? know(s, eventStart, eventEnd) :  //DTERNAL and TIMED
                                            know(s));
                            if (t != null) {
                                link(event, (eventDT == 0 || timed) ? 0 : ETERNAL,
                                        t //DTERNAL and TIMELESS
                                );
                            } else {
                                //WHY?
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
        if (autoNeg) {
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
//        FasterList<Event> events = new FasterList<>(byTerm.get(x.root()));
//        for (int i = 0, eventsSize = events.size(); i < eventsSize; i++) {
//            Event r = events.get(i);
//            if (r instanceof Absolute) {
//                if (r.id.subterms().equals(xx)) {
//                    if (!each.test(r))
//                        return false; //done
//                }
//            }
//
//        }


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

                        //same term, must have >1 absolute timepoints
                        if (aes > 1) {

                            Event[] ab = ae.toArray(new Event[aes]);
                            //Arrays.sort(ab, Comparator.comparingLong(Event::when));
                            Set<LongLongPair> uniqueTry = new UnifiedSet<>(4);
                            for (int i = 0; i < ab.length; i++) {
                                Event abi = ab[i];
                                long from = abi.start();
                                for (int j = 0; j < ab.length; j++) {
                                    if (i == j) continue;
                                    long to = dt(x, abi, ab[j]);
                                    if (uniqueTry.add(pair(from, to))) {
                                        if (!solveDT(x, from, to,
                                                Math.min(abi.dur(), ab[j].dur()) //dur=intersection
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
                            //search only if one to N; there may be incorrect possibilities among N to N comparisons
                            //if (aes == 1 || bes == 1) {
                            Set<Twin<Event>> uniqueTry = new UnifiedSet<>(4);
                            if (!ae.allSatisfy(ax ->
                                    be.allSatisfyWith((bx, axx) -> {
                                        if (uniqueTry.add(twin(axx, bx))) {
                                            return solveDT(x, axx.start(), dt(x, axx, bx),
                                                    Math.min(axx.dur(), bx.dur()), //dur=intersection
                                                    each);
                                        } else {
                                            return true;
                                        }
                                    }, ax)))
                                return false;
                            //}
                        }
                    }
                }
            }


//            UnifiedSet<Event>[] abs = new UnifiedSet[2]; //exact occurrences of each subterm


            FasterList<Event> rels = new FasterList<>(4);
//            int[] phase = new int[]{0};
            //                int p = phase[0];
//                if (z instanceof Absolute) {
//                    if (abs[p] == null) abs[p] = new UnifiedSet(2);
//                    abs[p].add(z);
//                    //}
//                }
            Consumer<Event> collect = rels::add;

            byTerm.get(a).forEach(collect);
//            if (abs[0] == null)
//                byTerm.get(a.neg()).forEach(collect); //if nothing, look for negations

            if (aEqB) {
//                abs[1] = abs[0];
            } else {
//                phase[0] = 1;
                byTerm.get(b).forEach(collect);
//                if (abs[1] == null)
//                    byTerm.get(b.neg()).forEach(collect);  //if nothing, look for negations
            }

//            if (abs[0] != null && abs[1] != null) {
            //known exact occurrences for both subterms
            //iterate all possibilities
            //TODO order in some way
            //TODO other simple cases: 1 -> N
//                if (abs[0].size() == 1 && abs[1].size() == 1) {
//                    //simple case:
//                    Event aa = abs[0].iterator().next();
//                    Event bb = abs[1].iterator().next();
//                    if (!solveDT(x, each, aa, bb))
//                        return false;
//                } else {
//                    if (!abs[0].allSatisfy(ae ->
//                            abs[1].allSatisfyWith((be, aaee) ->
//                                    solveDT(x, each, aaee, be), ae)))
//                        return false;
//                }

//            }


            int relCount = rels.size();
            if (relCount > 0) {

//                if (ns > 1) {
//                    //sort by volume
//                    rels.sortThisByInt(s -> s.id.volume());
//
//                }

                //            boolean repeat = a.unneg().equals(b.unneg()); //if true, then we must be careful when trying this in a commutive-like result which would collapse the two terms

                if (relCount > 1)
                    rels.shuffleThis(random());

                return bfsPush(rels, new CrossTimeSolver() {
                    @Override
                    protected boolean next(BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> move, Node<TimeGraph.Event, TimeGraph.TimeSpan> next) {

                        //System.out.println(path);

                        long[] startDT = pathDT(next, a, b, path);
                        if (startDT == null)
                            return true; //nothing at this step

                        long start = startDT[0];
                        long ddt = startDT[1];
                        return TimeGraph.this.solveDT(x, start, ddt,
                                (start != ETERNAL && start != XTERNAL) ? Math.min(pathStart(path).dur(), pathEnd(path).dur()) : 0 //dur=intersection
                                , each);
                    }
                });

            }

//        } else {
//            assert (x.op() == CONJ);
//            List<LongObjectPair<Term>> when = $.newArrayList();
//            for (int ix = 0; ix < subs; ix++) {
//                //assert(!z.hasXternal());
//                solveOccurrence(event(xx.sub(ix), TIMELESS), (ze) -> {
//                    if (ze.when() == TIMELESS)
//                        return true; //keep trying
//                    when.add(pair(ze.when(), ze.id));
//                    return false; //just one, for now //TODO see if there are any others
//                });
//            }
//            if (when.size() == subs) {
//                when.sort(Comparator.comparingLong(LongObjectPair::getOne));
//                long base = when.get(0).getOne();
//                Term zz = when.get(0).getTwo();
//                for (int i = 1; i < subs; i++) {
//                    LongObjectPair<Term> wgi = when.get(i);
//                    zz = Op.conjMerge(zz, 0, wgi.getTwo(), wgi.getOne() - base);
//                    if (zz instanceof Bool)
//                        return true; //failure
//                }
//                return each.test(event(zz, base));
//            }
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

        return true;
        //return each.test(event(x, TIMELESS)); //last resort
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
//        if (ddt == ETERNAL) {
//            dt = DTERNAL;
//        } else {
            assert (ddt < Integer.MAX_VALUE) : ddt + " dt calculated";
            dt = (int) ddt;
//        }
        Term y = dt(x, dt);

        if (y instanceof Bool)
            return true;


        if (start != ETERNAL && start != TIMELESS && dt != DTERNAL && dt < 0 && y.op() == CONJ) {
            start += dt; //shift to left align
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

        //CONSTRUCT NEW TERM
        Op xo = x.op();
        if (xo == IMPL) {
            return x.dt(dt - x.sub(0).dtRange());
        } else if (xo == CONJ) {
//            int early = Op.conjEarlyLate(x, true);
//            if (early == 1)
//                dt = -dt;
//
//            Term xEarly = x.sub(early);
//            Term xLate = x.sub(1 - early);
            Term xEarly = x.sub(0);
            Term xLate = x.sub(1);

            return Conj.conjMerge(
                    xEarly, 0,
                    xLate, dt);
        } else {
            //?
        }


        throw new UnsupportedOperationException();
    }


//    final static LongSet EMPTY_LONG_SET = LongSets.immutable.empty();

//    public void solve(Term x, Predicate<Event> each) {
//        solve(x, true, each);
//    }

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
//                if (!x.equalsRoot(y.id))
//                    return true; //potentially degenerate solution
                if (y.start() == TIMELESS && (filterTimeless || x.equals(y.id)))
                    return true;

                return target.test(y);
            } else {
                return true; //filtered
            }
        };


        //test for existing exact solutions to the exact term
        return //solveExact(x, each);
                solveAll(x, each);
        //each.test(event(x, TIMELESS)); //as a last resort: does this help?


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

            //this is limited to one solution for each XTERNAL subterm
            x.subterms().recurseTerms(Term::isTemporal, y -> {
                if (y.dt() == XTERNAL) {
                    solveDT(y, (z) -> {
                        subSolved.put(y, z.id);
                        //event(z); //know it
                        return false; //for now, only one please
                    });
                }
                return true;
            }, null);

            //transform
            if (!subSolved.isEmpty()) {
                //apply the transforms
                Term y = x.replace(subSolved);
                if (y != null && !(y instanceof Bool)) {
                    x = y;
                }
            }

        }

        if (x.dt() == XTERNAL) {

            if (!solveDT(x, y -> {
                return solveOccurrence(y.id, each); //solve the occurence for the term, not 'y' as an event even if it's absolute
            }))
                return false;
        }

        return solveOccurrence(x, each); /* occurrence, with or without any xternal remaining */

    }

    protected boolean solveOccurrence(Term x, Set<Event> filter, Predicate<Event> each) {
        Event ex = event(x, TIMELESS, false);
        return filter.add(ex) ? solveOccurrence(ex, each) : true;
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
            if (node(r) == null) {
                addNode(r);
                created.add(r);
            }
        }

        boolean result = bfs(roots, tv);

        created.forEach(this::removeNode);

        return result;
    }

    private boolean solveOccurrence(Event x, Predicate<Event> each) {
        assert(x instanceof Relative);
        //int startMargin = x.id.dtRange();
        return solveExact(x.id, each) && bfsPush(x, new CrossTimeSolver() {

            Set<Event> nexts = null;

            @Override
            protected boolean next(BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> move, Node<Event, TimeSpan> n) {

                if (!(n.id instanceof Absolute))
                    return true;

                long pathEndTime = n.id.start();
                //BooleanObjectPair<Edge<Event, TimeSpan>> pathStart = path.get(0);
//                    Term pathStartTerm = pathStart.getTwo().from(pathStart.getOne()).id.id;

                long startTime;
                if (pathEndTime == ETERNAL) {
                    startTime = ETERNAL;
                } else {

                    //long[] pathDelta =
                    long pathDelta =
                        pathTime(path, false);
                        //pathDT(n, x.id, n.id.id, path);

                    if (pathDelta == ETERNAL) //no concrete path thru eternity
                        return true;
                    else {
//                        boolean dir = (path.get(0).getTwo().);
//                        if (dir) {
//                            pathDelta *= -1;
//                            pathDelta -= startMargin;
//                        }
                        //int endMargin = n.id.id.dtRange();
//                        if(pathDelta < 0)
//                            pathDelta += startMargin;
//                        else if (pathDelta > 0)
//                            pathDelta -= startMargin;
                        startTime = pathEndTime - (pathDelta );
                    }
                }

                long endTime;
                if (startTime != ETERNAL && startTime != XTERNAL) {
                    long startDur = pathStart(path).dur();
                    long endDur = pathEnd(path).dur();
                    long dur = Math.min(startDur, endDur); //dur=intersection
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

    protected Random random() {
        return ThreadLocalRandom.current();
    }

    public Multimap<Term, Event> events() {
        return byTerm;
    }

    public void autoNeg(boolean b) {
        autoNeg = b;
    }

    static class TimeSpan {
        public final static TimeSpan TS_ZERO = new TimeSpan(0);
        //public final float weight;
        //        public final static TimeSpan TS_POS_ONE = new TimeSpan(+1);
//        public final static TimeSpan TS_NEG_ONE = new TimeSpan(-1);
        public final static TimeSpan TS_ETERNAL = new TimeSpan(ETERNAL);
        public final long dt;

        private TimeSpan(long dt) {
            this.dt = dt;
            //this.weight = weight;
            //this.hash = Long.hashCode(dt);
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
            //+ (weight != 1 ? "x" + n2(weight) : "");
        }
    }


    public abstract static class Event implements LongObjectPair<Term> {

        public final Term id;
        private final int hash;

        Event(Term id, int hash) {
            this.id = id;
            this.hash = hash;
//            this.hash = Util.hashCombine(id.hashCode(), Long.hashCode(start));
        }
//
//        Event(Term id, long start, long end) {
//            this.id = id;
//            this.hash = Util.hashCombine(id.hashCode(), Long.hashCode(start), Long.hashCode(end));
//        }

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

        //        public float pri() {
//            return 1f;
//        }


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

    public static class Absolute extends Event {
        static final long SAFETY_PAD = 32 * 1024;
        protected final long start;

        protected Absolute(Term t, long start, int hashCode) {
            super(t, hashCode);

            //validation
            assert (start != TIMELESS);
            if (!((start == ETERNAL || start > 0 || start > ETERNAL + SAFETY_PAD))) //for catching time calculation bugs
                throw new MathArithmeticException();
            if (!((start < 0 || start < TIMELESS - SAFETY_PAD))) //for catching time calculation bugs
                throw new MathArithmeticException();

            this.start = start;
        }

        protected Absolute(Term t, long start) {
            this(t, start, Util.hashCombine(t.hashCode(), start));
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
            if ((end <= start || start == ETERNAL || end == ETERNAL || start == XTERNAL || end == XTERNAL))
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
                            //Iterators.filter(x, preFilter::test),
                            TimeGraph.this::node),
                    e -> e!=null && e != n && !log.hasVisited(e)),
                    that -> new ImmutableDirectedEdge<>(n, that, TS_ZERO) //co-occurring
            ) : null;
        }






    }

    abstract protected class CrossTimeSolver extends TimeSolver {

        @Override
        protected Iterable<ImmutableDirectedEdge<Event, TimeSpan>> next(Node<TimeGraph.Event, TimeGraph.TimeSpan> n) {
            Iterable<ImmutableDirectedEdge<Event, TimeSpan>> e = n.edges(true, true);

            //must be cached to avoid concurrent modification exception
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
        boolean endA = ((adjEnd = a.subTimeSafe(endTerm)) == 0); //TODO use offset for the endTerm if endTermRelB!=0 and !=DTERNAL (not sub event)
        boolean endB = !endA &&
                ((adjEnd = b.subTimeSafe(endTerm)) == 0); //TODO use offset for the endTerm if endTermRelB!=0 and !=DTERNAL (not sub event)

        if (adjEnd == DTERNAL)
            return null;

        if (endA || endB) {
            Event startEvent = pathStart(path);

            Term startTerm = startEvent.id;

            boolean fwd = endB && (startTerm.equals(a) || a.subTimeSafe(startTerm) == 0);
            boolean rev = !fwd && (
                    endA && (startTerm.equals(b) || b.subTimeSafe(startTerm) == 0));//TODO use offset for the endTerm if endTermRelB!=0 and !=DTERNAL (not sub event)
            if (fwd || rev) {

                long startTime = startEvent.start();

                Event endEvent = pathEnd(path);
                long endTime = endEvent.start();


                long dt;
                if (startTime != TIMELESS && startTime != ETERNAL && endTime != TIMELESS && endTime != ETERNAL) {

                    dt = endTime - startTime;
                } else {

                    //TODO more rigorous traversal of the dt chain
                    //compute from one end to the other, summing dt in the correct direction along the way
                    //special handling for encountered absolute terms and DTERNAL

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
                        rev = random().nextBoolean(); //equal chance for each direction

                    if (rev) {
                        dt = -dt; //reverse
                        long s = startTime;
                        startTime = endTime;
                        endTime = s;
                    }

                    //TODO may need to subtract from dt any inner events with dtRange


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

    /**
     * computes the length of time spanned from start to the end of the given path
     */
    long pathTime(List<BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>>> path, boolean eternalAsZero) {

        long t = 0;
        //compute relative path
        for (BooleanObjectPair<ImmutableDirectedEdge<Event, TimeSpan>> r : path) {
//            for (int i = 0, pathSize = path.size(); i < pathSize; i++) {
//                BooleanObjectPair<Edge<Event, TimeSpan>> r = path.get(i);
            ImmutableDirectedEdge<Event, TimeSpan> event = r.getTwo();

            long spanDT = event.id.dt;

            if (spanDT == ETERNAL) {
                //no change, crossed a DTERNAL step. this may signal something
                if (!eternalAsZero)
                    return ETERNAL; //short-circuit to eternity
                //else: t+=0

            } else if (spanDT != 0) {
                t += (spanDT) * (r.getOne() ? +1 : -1);
            }

        }

        return t;
    }

}
