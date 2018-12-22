package nars.time;

import com.google.common.collect.Iterables;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.graph.FromTo;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.math.Longerval;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.Conj;
import nars.term.var.Img;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static jcog.Util.hashCombine;
import static jcog.data.graph.search.Search.pathStart;
import static nars.Op.*;
import static nars.time.Tense.*;
import static nars.time.TimeSpan.TS_ZERO;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

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
//    public final Multimap<Term, Event> byTerm = MultimapBuilder
//            .hashKeys()
//            .linkedHashSetValues()
//            .build();

    public final Map<Term, Collection<Event>> byTerm = new HashMap<>() {
        @Override
        public Collection<Event> get(Object key) {
            Collection<Event> x = super.get(key);
            return x == null ? List.of() : x;
        }
    };

//    public final UnifiedSetMultimap<Term, Event> byTerm = new UnifiedSetMultimap<>();


    public final MutableSet<Term> autoNeg = new UnifiedSet() {
        @Override
        public boolean add(Object key) {
            return super.add(((Term) key).unneg());
        }
    };

    protected final ArrayHashSet<Event> solutions = new ArrayHashSet();
    private transient boolean filterTimeless;
    private transient Predicate<Event> target;
    private transient Term solving;


    /**
     * since CONJ will be constructed with conjMerge, if x is conj the dt between events must be calculated from start-start. otherwise it is implication and this is measured internally
     */
    static long dt(Event aa, Event bb) {

        long aWhen = aa.start();
        long bWhen;
        if (aWhen == ETERNAL || (bWhen = bb.start()) == ETERNAL)
            return ETERNAL;
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
    public final Event shadow(Term v) {
        //return event(v, TIMELESS, false);
        return new Relative(v);
    }

    public final Event know(Term v) {
        return event(v, TIMELESS, true);
    }

    public final Event know(Term t, long start) {
        return event(t, start, start, true);
    }

    public final Event know(Term t, long start, long end) {
        return event(t, start, end, true);
    }

    public final Event event(Term t, long when, boolean add) {
        return event(t, when, when, add);
    }

    public Event event(Term t, long start, long end, boolean add) {
        if (t instanceof Int || t instanceof Bool || t instanceof Img)
            throw new WTF();

        Event event;
        if (start == TIMELESS) {
            assert(add): "use shadow(t) if not adding";
            event = new Relative(t);
        } else {

            //if (add) {

            Collection<Event> te = byTerm.get(t);
            int nte = te.size();
            if (nte > 0) {

                boolean stable;
                do {

                    stable = true;

                    Iterator<Event> ff = te.iterator();
                    while (ff.hasNext()) {
                        Event f = ff.next();
                        if (!(f instanceof Absolute))
                            continue;
                        Absolute af = (Absolute) f;
                        long as = af.start();
                        if (start == ETERNAL && as == ETERNAL)
                            return af;
                        if (as == ETERNAL)
                            continue;

                        if (af.containsOrEquals(start, end)) {
                            //add = false;
                            //break; //dont affect the stored graph, but return the smaller interval that was input

                            return af; //return the absorbing event
                        }


                        if (add && af.containedIn(start, end)) {
                            //absorb existing
                            removeNode(f);
                            ff.remove();
                            nte--;
                        } else {
                            long[] merged;
                            if ((merged = af.unionIfIntersects(start, end)) != null) {

                                //stretch
                                start = merged[0];
                                end = merged[1];

                                if (add) {
                                    removeNode(f);
                                    ff.remove();
                                    nte--;
                                    stable &= nte <= 1; //try again if other nodes, because it may connect with other ranges further in the iteration
                                }

                                break;
                            }
                        }


                    }
                } while (!stable);
                //}
            }

            event = (end != start) ? new AbsoluteRange(t, start, end) : new Absolute(t, start);
        }


        if (add) {
            return addNode(event).id;
        } else {
            //return event(event);
            return event;
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

    private boolean link(Event before, TimeSpan e, Event after) {
        MutableNode<Event, TimeSpan> x = addNode(before);
        MutableNode<Event, TimeSpan> y = addNode(after);

        if (e==TimeSpan.TS_ETERNAL)
            return false; //skip eternal links

        return addEdge(x, e, y);
    }

    protected void link(Event x, long dt, Event y) {

        boolean parallel = dt == ETERNAL || dt == TIMELESS || dt == 0;
        int vc = x.compareTo(y);
        if (vc == 0) { //equal?
            if (parallel)
                return; //no point
            if (dt < 0)
                dt = -dt; //use only positive dt values for self loops
        } else {
            if (vc > 0) {
                if (!parallel) {
                    dt = -dt;
                }
                Event z = x;
                x = y;
                y = z;
            }
        }

        link(x, TimeSpan.the(dt), y);
    }

    @Override
    protected void onAdd(Node<Event, TimeSpan> x) {
        Event event = x.id();
        Term eventTerm = event.id;


        Collection<Event> ee = byTerm.get(eventTerm);
        if (ee.isEmpty()) {
            byTerm.put(eventTerm, new UnifiedSet(2).with(event));
            onNewTerm(eventTerm);
        } else {
            if (!ee.add(event))
                //if (!byTerm.put(eventTerm, event))
                return; //already present
        }

        if (decomposeAddedEvent(event)) {
            int edt = eventTerm.dt();


            switch (eventTerm.op()) {

                case SECTe:
                    //TODO n-ary
//                    Event a = onlyAbsolute(eventTerm.sub(0));
//                    if (a != null) {
//                        Event b = onlyAbsolute(eventTerm.sub(1));
//                        if (b != null) {
//                            long as = a.start();
//                            long bs = b.start();
//                            if (as == ETERNAL) {
//                                know(eventTerm, bs, b.end());
//                            } else if (bs == ETERNAL) {
//                                know(eventTerm, as, a.end());
//                            } else {
////                                Longerval u = Longerval.union(as, a.end(), bs, b.end());
////                                know(eventTerm, u.a, u.b);
//                                long[] u = Longerval.intersectionArray(as, a.end(), bs, b.end());
//                                if (u != null)
//                                    know(eventTerm, u[0], u[1]);
//                            }
//                        }
//                    }
                    break;

//                case DIFFi:
//                case SECTi:
//                case SECTe:
//                case SIM:
//                case INH:
//                case SETe:
//                case SETi: {
//                    if (!(event instanceof Absolute)) {
//                        //naive estimate method 1:
//                        //if all components of the compound are known, assign the average of their occurrence for the compound
//                        Subterms ess = eventTerm.subterms();
//                        int essn = ess.subs();
//                        LongArrayList subOcc = new LongArrayList(essn);
//
//                        nextTerm:
//                        for (Term s : eventTerm.subterms()) {
//                            for (Event es : events().get(s)) {
//                                if (es instanceof Absolute) {
//                                    //TODO select more carefully
//                                    subOcc.add(es.mid());
//                                    continue nextTerm;
//                                }
//                            }
//                            //occurrence not found
//                            subOcc = null;
//                            break;
//                        }
//                        if (subOcc != null) {
//                            know(eventTerm, Math.round(subOcc.average())); //TODO estimate start/end range
//                        }
//                    }
//                }


                case IMPL:

                    Term subj = eventTerm.sub(0), pred = eventTerm.sub(1);
                    Event se = know(subj), pe = know(pred);

                    if (edt == DTERNAL) {

                        //link(se, ETERNAL, pe);

//                        //link first two events of each
//                        if (subj.hasAny(Op.CONJ)) {
//                        subj.eventsWhile((w, y) -> {
//                            link(know(y), ETERNAL, pe);
//                            return true;
//                        }, 0, false, true, true, 0);
//                          }
//
//                        pred.eventsWhile((w, y) -> {
//                            link(se, ETERNAL, know(y));
//                            return true;
//                        }, 0, false, true, true, 0);

                    } else if (edt == XTERNAL) {

                    } else {
                        link(se, edt + subj.eventRange(), pe);

//                        if (!(subj.op()==CONJ && subj.dt()==XTERNAL)) {
//                            int st = subj.eventRange();
//                            link(se, (edt + st), pe);
//
//
//                            if (subj.op() == Op.CONJ && subj.dt() == DTERNAL) { //HACK to decompose ordinarily non-decomposed &&
//                                subj.eventsWhile((w, y) -> {
//                                    link(know(y), edt + st - w, pe);
//                                    return true;
//                                }, 0, false, true, false, 0);
//                            }
//
//                            if (pred.op() == Op.CONJ && pred.dt() == DTERNAL) { //HACK to decompose ordinarily non-decomposed &&
//                                pred.eventsWhile((w, y) -> {
//                                    link(se, edt + st + w, know(y));
//                                    return true;
//                                }, 0, false, true, false, 0);
//                            }
//                        }
                    }

                    break;


                case CONJ:


                    long eventStart = event.start(), eventEnd = event.end();

                    switch (edt) {

                        case XTERNAL:
                            for (Term sub : eventTerm.subterms())
                                know(sub);
                            break;


                        case 0:
                            Subterms es = eventTerm.subterms();
                            int esn = es.subs();
                            for (int i = 0; i < esn; i++)
                                link(event, 0, know(es.sub(i), eventStart, eventEnd));

                            break;

                        case DTERNAL:
                        default:

                            if (eventStart != ETERNAL && eventStart != TIMELESS) {
                                //chain the events to the absolute parent
                                long range = eventEnd - eventStart;
                                eventTerm.eventsWhile((w, y) -> {

                                    link(event, w - eventStart, know(y, w, w + range));
                                    return true;
                                }, eventStart, false, false, false, 0);
                            } else {
                                //chain the events together relatively
                                final Event[] prev = {event};
                                final long[] prevDT = {0};
                                eventTerm.eventsWhile((w, y) -> {
                                    if (!y.equals(eventTerm)) {
                                        Event next = know(y);

                                        link(prev[0], w - prevDT[0], next);
                                        prevDT[0] = w;
                                        prev[0] = next;
                                    }
                                    return true;
                                }, 0, false, false, false, 0);
                            }

                            break;
                    }

                    break;
            }
        }

    }

    protected boolean decomposeAddedEvent(Event event) {
        return true;
    }

    protected void onNewTerm(Term t) {
        if (autoNeg != null && autoNeg.contains(t.unneg())) {
            link(shadow(t), 0, shadow(t.neg()));
        }
    }


    boolean solveDT(Term x, Predicate<Event> each) {
        assert (x.dt() == XTERNAL);

        Subterms xx = x.subterms();

        int subs = xx.subs();
        if (subs == 2) {
            Term a = xx.sub(0), b = xx.sub(1);

            boolean aEqB = a.equals(b);
            //TODO case if aEqNegB

            if (!solveDTAbsolutePair(x, each, a, b, aEqB))
                return false;

            return solveDTTrace(x, each, a, b, aEqB);

        } else {
            //TODO
        }


        return true;

    }


    private boolean solveDTTrace(Term x, Predicate<Event> each, Term a, Term b, boolean aEqB) {


        FasterList<Event> rels = new FasterList<>(4);
        rels.addAll(byTerm.get(a));
        if (!aEqB)
            rels.addAll(byTerm.get(b));

        int relCount = rels.size();
        if (relCount > 0) {
            if (relCount > 1)
                rels.shuffleThis(random());

            return bfsPush(rels, new CrossTimeSolver() {
                @Override
                protected boolean next(BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>> move, Node<Event, TimeSpan> next) {

                    /**
                     * assuming the path starts with one of the end-points (a and b),
                     * if the path ends at either one of them
                     * this computes the dt to the other one,
                     * and (if available) the occurence startTime of the path
                     * returns (startTime, dt) if solved, null if dt can not be calculated.
                     */

                    long[] startDT;
                    Event startEvent, endEvent;
                    Term startTerm;
                    Term endTerm = next.id().id;
                    int adjEnd;
                    boolean endA = ((adjEnd = choose(a.subTimes(endTerm))) == 0);
                    boolean endB = !endA &&
                            ((adjEnd = choose(b.subTimes(endTerm))) == 0);

                    if (adjEnd != DTERNAL && (endA || endB)) {

                        startEvent = pathStart(path).id();

                        startTerm = startEvent.id;

                        boolean fwd = endB && (startTerm.equals(a) || choose(a.subTimes(startTerm)) == 0);
                        boolean rev = !fwd && (
                                endA && (startTerm.equals(b) || choose(b.subTimes(startTerm)) == 0));
                        if (fwd || rev) {

                            long startTime = startEvent.start();

                            endEvent = Search.pathEnd(path).id();
                            long endTime = endEvent.start();


                            long dt;
                            if (startTime != TIMELESS && startTime != ETERNAL && endTime != TIMELESS && endTime != ETERNAL) {
                                dt = endTime - startTime;
                            } else {
                                dt = pathTime(path);
                                if (dt == TIMELESS)
                                    return true;
                            }

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

                                startDT = new long[]{w, ETERNAL};
                            } else {

                                if (a.equals(b))
                                    rev = random().nextBoolean();

                                if (rev) {
                                    dt = -dt;
                                    long s = startTime;
                                    startTime = endTime;
                                    endTime = s;
                                }


                                startDT = new long[]{
                                        (startTime != TIMELESS || endTime == TIMELESS) ?
                                                startTime :
                                                (endTime != ETERNAL ? endTime - dt : ETERNAL)
                                        , dt};
                            }

                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }


                    long start = startDT[0];
                    if (x.op() != CONJ)
                        start = TIMELESS; //cant assume this start time corresponds to the impl term being solved
                    long ddt = startDT[1];
                    return TimeGraph.this.solveDT(x, start, ddt,
                            (start != ETERNAL && start != XTERNAL) ?
                                    durMerge(startEvent, endEvent) : 0
                            , path, each);
                }
            });

        }
        return true;
    }

    private boolean solveDTAbsolutePair(Term x, Predicate<Event> each, Term a, Term b, boolean aEqB) {
        if (!a.hasXternal() && !b.hasXternal() && (aEqB || !commonSubEventsWithMultipleOccurrences(a, b))) {
            UnifiedSet<Event> ae = new UnifiedSet(2);
            //solveExact(a, ax -> {
            solveOccurrence(a, ax -> {
                if (ax instanceof Absolute) ae.add(ax);
                return true;
            });
            int aes = ae.size();
            if (aes > 0) {

                if (aEqB && aes > 1) {

                    Event[] aa = eventArray(ae);

                    for (int i = 0; i < aa.length; i++) {
                        for (int j = i + 1; j < aa.length; j++) {
                            if (!solvePairDT(x, aa[i], aa[j], each))
                                return false;
                        }
                    }


                } else {
                    UnifiedSet<Event> be = new UnifiedSet(2);
                    //solveExact(b, bx -> { //less exhaustive
                    solveOccurrence(b, bx -> {
                        if (bx instanceof Absolute) be.add(bx);
                        return true;
                    });


                    if (!be.isEmpty()) {
                        for (Event ax : eventArray(ae)) {
                            for (Event bx : eventArray(be)) {
                                if (!solvePairDT(x, ax, bx, each))
                                    return false;
                            }
                        }

                        return true; //continue
                    }
                }
            }
        }
        return true;
    }

    private static final Event[] EMPTY_EVENT_ARRAY = new Event[0];

    private Event[] eventArray(UnifiedSet<Event> ae) {
        Event[] aa = ae.toArray(EMPTY_EVENT_ARRAY);
        if (aa.length > 1) ArrayUtils.shuffle(aa, random());
        return aa;
    }

    private boolean solvePairDT(Term x, Event a, Event b, Predicate<Event> each) {
        long dt = dt(a, b);

        if (x.op() == CONJ) {
            return solveConj2DT(each, a, Tense.occToDT(dt), b);
        } else {
            //for impl and other types cant assume occurrence corresponds with subject
            return solveDT(x, TIMELESS, dt, durMerge(a, b), null, each);
        }
    }

    /**
     * solution vector for 2-ary CONJ
     */
    private boolean solveConj2DT(Predicate<Event> each, Event a, int dt, Event b) {

        int ddt = dt(dt);

        if (ddt != DTERNAL && ddt != 0) {
            assert (ddt != XTERNAL);
            //swap to correct sequence order
            if (a.start() > b.start()) {
                Event z = a;
                a = b;
                b = z;
                ddt = -ddt;
            }
        }

        Term c =
                (ddt==DTERNAL || ddt == 0) ?
                    CONJ.the(ddt, a.id, b.id) :
                    Conj.sequence(a.id, 0, b.id, (ddt == DTERNAL ? ETERNAL /* HACK */ : ddt));

//        if (c.op() != CONJ && ((ddt == 0) && (dt != 0))) { //undo parallel-ization if the collapse caused an invalid term
//            c = Conj.the(a.id, 0, b.id, (dt == DTERNAL ? ETERNAL /* HACK */ : dt));
//        }
        if (termsEvent(c)) {
            return solveOccurrence(c, a.start(), durMerge(a, b), each);
        }
        return true; //keep trying
    }

    private static long durMerge(Event a, Event b) {
        if (a instanceof Absolute && b instanceof Absolute) {
            return Math.min(a.dur(), b.dur());
//        else if (a instanceof Absolute && !(b instanceof Absolute)) {
//            return a.dur();
//        } else if (b instanceof Absolute && !(a instanceof Absolute)) {
//            return b.dur();
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
    private boolean solveDT(Term x, long start, long ddt, long dur,
                            @Nullable List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, Predicate<Event> each) {
        assert (ddt != TIMELESS && ddt != XTERNAL);

        int dt;
        if (ddt == ETERNAL)
            dt = DTERNAL;
        else if (ddt == TIMELESS)
            dt = XTERNAL;
        else
            dt = Tense.occToDT(ddt);

        Term y = dt(x, path, dt);

        if (!termsEvent(y))
            return true;  //invalid term


        if (start != ETERNAL && start != TIMELESS && dt != DTERNAL && dt < 0) {
            start += dt;
        }

        return solveOccurrence(y, start, dur, each);


    }

    public static boolean termsEvent(Term e) {
        Op eo = e.op();
        return eo.conceptualizable || eo.var;
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
    protected Term dt(Term x, @Nullable List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, int dt) {

        assert (dt != XTERNAL);

        if (dt == DTERNAL) {
            return x.dt(DTERNAL);
        }


        Op xo = x.op();
        Term x0 = x.sub(0);
        if (xo == IMPL) {
            return x.dt(dt - x0.eventRange());
        } else if (xo == CONJ) {
            if (dt == 0) {
                return x.dt(dt);
            } else {

                if (x.dt() == XTERNAL) {
                    if (path != null) {
                        //use the provided 'path', if non-null, to correctly order the sequence, which may be length>2 subterms
                        Term x1 = x.sub(1);

                        if (x0.equals(x1)) {
                            //order doesnt matter
                            return Conj.sequence(x0, 0, x0, dt);
                        }

                        Term pStart = pathStart(path).id().id;
                        //TODO verify pathEnd?
                        if (pStart.equals(x0)) {
                            return Conj.sequence(x0, 0, x1, dt);
                        } else if (pStart.equals(x1)) {
                            return Conj.sequence(x1, 0, x0, -dt);
                        } else {
                            return Bool.Null; //TODO
                        }
                    } else {
                        return Bool.Null;
                    }
                } else {

                    int early = Conj.conjEarlyLate(x, true);
                    if (early == 1)
                        dt = -dt;


                    Term xEarly = x.sub(early);
                    Term xLate = x.sub(1 - early);

                    return Conj.sequence(
                            xEarly, 0,
                            xLate, dt);
                }

            }
        }

        throw new UnsupportedOperationException();
    }


    private boolean solution(Event y) {
        if (solutions.add(y)) {

            if (y.start() == TIMELESS && (filterTimeless || solving.equals(y.id)))
                return true;

            return target.test(y);
        } else {
            return true;
        }
    }


    private boolean solveExact(Event f, Predicate<Event> each) {
        if (f instanceof Absolute) {
            if (!each.test(f))
                return false;
        }
        Term x = f.id;
        return solveExact(f, each, x);
    }

//    private boolean solveExact(Term x, Predicate<Event> each) {
//        return solveExact(null, each, x);
//    }

    private boolean solveExact(@Nullable Event f, Predicate<Event> each, Term x) {
        //try other absolute solutions
        for (Event e : byTerm.get(x)) {
            if (e instanceof Absolute && ((!(f instanceof Absolute)) || !e.equals(f)) && !each.test(e))
                return false;
        }
        return true;
    }

    @Nullable
    private Event onlyAbsolute(Term x) {
        Event first = null;
        for (Event e : byTerm.get(x)) {
            if (e instanceof Absolute) {
                if (first == null)
                    first = e;
                else
                    return null; //more than one, ambiguous
            }
        }
        return first;
    }

    /**
     * main entry point to the solver
     *
     * @seen callee may need to clear the provided seen if it is being re-used
     */
    public boolean solve(Term x, boolean filterTimeless, Predicate<Event> target) {

        this.filterTimeless = filterTimeless;
        this.target = target;
        this.solving = x;
        solutions.clear();

        Predicate<Event> each = this::solution;

        if (!x.hasXternal()) {
            //if (!x.hasAny(Op.Temporal)) {
            return solveOccurrence(x, each);
        }

        Subterms xx = x.subterms();
        if (xx.hasXternal()) {

            Map<Term, Set<Term>> subSolved = new HashMap(1);

            xx.recurseTerms(Term::hasXternal, y -> {
                if (y.dt() == XTERNAL) {
                    subSolved.computeIfAbsent(y, (yy) -> {
                        Set<Term> s = new UnifiedSet(1);
                        solveDT(yy, z -> {
                            //TODO there could be multiple solutions for dt
                            assert (z.id.dt() != XTERNAL);
                            s.add(z.id);
                            return true;
                        });
                        return s.isEmpty() ? java.util.Set.of() : s;
                    });
                }
                return true;
            }, null);

            subSolved.values().removeIf(java.util.Set::isEmpty);

            int solvedTerms = subSolved.size();
            switch (solvedTerms) {
                case 0:
                    //continue below
                    break;
                case 1:
                    //randomize the entries
                    Map.Entry<Term, Set<Term>> xy = subSolved.entrySet().iterator().next();

                    Set<Term> sy = xy.getValue();
                    if (!sy.isEmpty()) {
                        Term xyx = xy.getKey();
                        Term[] two = sy.toArray(EmptyTermArray);
                        if (two.length > 1) ArrayUtils.shuffle(two, random());
                        for (Term sssi : two) {
                            Term y = x.replace(xyx, sssi);
                            if (!solveDtAndOccIfConceptualizable(x, y, each))
                                return false;
                        }
                    }
                    break;
                default:
                    //TODO cartesian product of terms. could be expensive
                    //for now randomize and start with first entry
                    List<Pair<Term, Term[]>> substs = new FasterList();
                    final int[] permutations = {1};
                    subSolved.forEach((h, w) -> {
                        Term[] ww = w.toArray(EmptyTermArray);
                        assert (ww.length > 0);
                        permutations[0] *= ww.length;
                        substs.add(pair(h, ww));
                    });
                    int ns = substs.size();
                    assert (ns > 0);
                    Random rng = random();


                    while (permutations[0]-- > 0) {
                        Map<Term, Term> m = new UnifiedMap(ns);
                        for (Pair<Term, Term[]> si : substs) {
                            Term[] ssi = si.getTwo();
                            Term sssi = ssi[ssi.length > 1 ? rng.nextInt(ssi.length) : 0];
                            m.put(si.getOne(), sssi);
                        }
                        Term z = x.replace(m);
                        if (!solveDtAndOccIfConceptualizable(x, z, each))
                            return false;
                    }
                    //break;
            }


        }

        return solveDtAndOcc(x, each);

    }

    private boolean solveDtAndOccIfConceptualizable(Term x, Term y, Predicate<Event> each) {
        return y == null || !termsEvent(y) || y.equals(x) || solveDtAndOcc(y, each);
    }

    private boolean solveDtAndOcc(Term x, Predicate<Event> each) {
        /* occurrence, with or without any xternal remaining */

        final boolean[] dtSolved = {false};
        if ((x.dt() != XTERNAL || solveDT(x, y -> {
            dtSolved[0] = true;
            return solveOccurrence(y, each);
        }))) {
            if (!dtSolved[0]) //dont solve if more specific dt solved further in previous solveDT call
                return solveOccurrence(x, each);
            else
                return true;
        } else
            return false;


    }


    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    protected final boolean solveOccurrence(Term t, Predicate<Event> each) {

        return solveOccurrence(shadow(t), each);

    }

    final boolean bfsPush(Event root, Search<Event, TimeSpan> tv) {
        return bfsPush(List.of(root), tv);
    }

    boolean bfsPush(Collection<Event> roots, Search<Event, TimeSpan> tv) {


        MetalBitSet created = null;
        {
            int n = 0;
            for (Event r : roots) {
                if (addNewNode(r)) {
                    if (created == null) {
                        created = MetalBitSet.bits(roots.size());
                    }
                    created.set(n);
                }
                n++;
            }
        }

        Queue<Pair<List<BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>>>, Node<Event, nars.time.TimeSpan>>> q = new ArrayDeque<>(roots.size() /* estimate TODO find good sizing heuristic */);

//        Iterable<Node<Event,TimeSpan>> rr = Iterables.transform(roots, r -> {
//            Node<Event, TimeSpan> n = node(r);
//            if (n == null) {
//                //virtual node
//                n = new AbstractNode<>(r) {
//                    @Override
//                    public Iterable edges(boolean in, boolean out) {
//                        if (out) {
//                            return ()->((TimeSolver)tv).dynamicLink(this);
//                        } else {
//                            return List.of();
//                        }
//                    }
//                };
//
//            }
//            return n;
//        });
//        boolean result = bfs(q, rr, tv);

        boolean result = bfs(roots, q, tv);

        if (created != null && result /* tail call optimization  - dont bother removing if we're done anyway */) {
            int m = 0;
            for (Event x : roots) {
                if (created.get(m++))
                    removeNode(x);
            }
        }

        return result;
    }

    private boolean solveOccurrence(Event x, Predicate<Event> each) {

        return solveExact(x, each) && bfsPush(x, new CrossTimeSolver() {

            Set<Event> nexts = null;

            @Override
            protected boolean next(BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> move, Node<Event, TimeSpan> n) {

                Event nn = n.id();
                if (!(nn instanceof Absolute))
                    return true;

                long pathEndTime = nn.start();


                long startTime;
                if (pathEndTime == ETERNAL) {
                    startTime = ETERNAL;
                } else {

                    long pathDelta = pathTime(path);
                    if (pathDelta == TIMELESS)
                        return true;

                    startTime = pathEndTime - (pathDelta);

                }

                long endTime;
                if (startTime != ETERNAL && startTime != XTERNAL/* && x.id.op() != CONJ*/) {
                    endTime = startTime + durMerge(pathStart(path).id(), pathEnd(path).id());
                } else {
                    endTime = startTime;
                }

                Event next = event(x.id, startTime, endTime, false);

                if (nexts == null || nexts.add(next)) {
                    if (nexts == null) {
                        nexts = new UnifiedSet<>(1);
                        nexts.add(next);
                    }
                    return each.test(next);
                } else {
                    return true;
                }
            }

        }) && (!(x instanceof Relative) || each.test(x) /* last resort */); //absolute has already been tried first
    }


    protected Random random() {
        return ThreadLocalRandom.current();
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
            if (!(start == ETERNAL || start > ETERNAL + SAFETY_PAD))
                throw new MathArithmeticException();
            if (!(start < TIMELESS - SAFETY_PAD))
                throw new MathArithmeticException();

            this.start = start;
        }

        protected Absolute(Term t, long start) {
            this(t, start, hashCombine(t.hashCode(), start));
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

//        /**
//         * contained within but not true if equal
//         */
//        public final boolean containedInButNotEqual(long cs, long ce) {
//            return containedIn(cs, ce) && (start != cs || end() != ce);
//        }

        public final boolean containedIn(long cs, long ce) {
            return (cs <= start && ce >= end());
        }

        public final boolean containsOrEquals(long cs, long ce) {
            return (cs >= start && ce <= end());
        }

//        public boolean intersectsWith(long cs, long ce) {
//            return Longerval.intersects(start, end(), cs, ce);
//        }


//        /**
//         * contains or is equal to
//         */
//        public boolean containsOrEqual(long cs, long ce) {
//            return (start <= cs && end() >= ce);
//        }

        @Nullable
        public long[] unionIfIntersects(long start, long end) {
            long thisStart = this.start;

            long thisEnd = end();

            return Longerval.intersectLength(start, end, thisStart, thisEnd) >= 0 ?
                    Longerval.unionArray(start, end, thisStart, thisEnd) :
                    null;
        }
    }

    public static class AbsoluteRange extends Absolute {
        protected final long end;

        protected AbsoluteRange(Term t, long start, long end) {
            super(t, start, hashCombine(hashCombine(t.hashCode(), start), end));
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
            super(id, hashCombine(id.hashCode(), TIMELESS));
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

    abstract protected static class TimeSolver extends Search<Event, TimeSpan> {


    }

    abstract protected class CrossTimeSolver extends TimeSolver {

        @Override
        protected Iterable<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> next(Node<Event, TimeSpan> n) {

            Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> exist = n.edges(true, true);

            return Iterables.concat(exist, () -> {
                Collection<Event> ee = byTerm.get(n.id().id);
                if (!ee.isEmpty()) {

                    List<FromTo<Node<Event, TimeSpan>, TimeSpan>> dyn = null;

                    for (Event x : ee) {
                        Node<Event, TimeSpan> xx = node(x);
                        if (xx != null && xx != n && !log.hasVisited(xx)) {
                            if (dyn == null)
                                dyn = new FasterList<>(1);
                            dyn.add(new ImmutableDirectedEdge<>(n, TS_ZERO, xx));
                        }
                    }
                    if (dyn != null)
                        return dyn.iterator();
                }
                return Collections.emptyIterator();
            });


//            Iterator<Event> x = ee.iterator();
//            return
//                    Iterators.transform(
//                            Iterators.filter(Iterators.transform(
//                                    x,
//                                    TimeGraph.this::node),
//                                    e -> e != null && e != n && !log.hasVisited(e)),
//                            that -> new ImmutableDirectedEdge<>(n, TS_ZERO, that));

        }
    }

    private int choose(int[] subTimes) {
        if (subTimes == null)
            return DTERNAL;
        else {
            return subTimes[subTimes.length == 1 ? 0 : random().nextInt(subTimes.length)];
        }
    }

    /**
     * computes the length of time spanned from start to the end of the given path
     */
    static long pathTime(List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path) {

        long t = 0;

        for (BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> r : path) {

            FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan> event = r.getTwo();

            long spanDT = event.id().dt;

            if (spanDT == ETERNAL || spanDT == TIMELESS) {
                return TIMELESS;

            } else if (spanDT != 0) {
                t += (spanDT) * (r.getOne() ? +1 : -1);
            }

        }

        return t;
    }

}
