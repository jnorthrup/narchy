package nars.time;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.WTF;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.math.LongInterval;
import jcog.math.Longerval;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjSeq;
import nars.term.var.CommonVariable;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.Null;
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
 *
 * TODO
 * subclass of MapNodeGraph which allows tagging edges by an ID.  then index
 * the edges by these keys so they can be efficiently iterated and removed by tag.
 *
 * then use it to store the edges in categories:
 *      task time edges
 *      belief time edges
 *      conclusion time edges
 *      autonegations
 *      hypothesized (anything created during search)
 *      etc.
 *
 * then the premise can clear the conclusion edges while retaining task, belief edges throughout
 * each separate derivation of a premise.
 * also the tags can be used to heuristically bias the search via associated weight.  so autonegation
 * can be weighted less allowing preferential traversal of concrete edges in oscillating pos/neg event pathways.
 *
 */
public class TimeGraph extends MapNodeGraph<TimeGraph.Event, TimeSpan> {


    /**
     * index by target
     */

    final Map<Term, Collection<Event>> byTerm = new UnifiedMap<>();
    //protected final MutableSet<Term> autoNeg = new UnifiedSet();

    protected final ArrayHashSet<Event> solutions = new ArrayHashSet();
    private transient Term solving;
    private Predicate<Event> target;

    public boolean autoneg = true;
    protected int nodesMax = Integer.MAX_VALUE;

//    /** temporary unification context */
//    private final UnifyAny u = new UnifyAny();
//    {
//        u.commonVariables = false;
//    }

    /**
     * since CONJ will be constructed with conjMerge, if x is conj the dt between events must be calculated from start-start. otherwise it is implication and this is measured internally
     */
    private int dt(Event aa, Event bb, boolean absolute) {

        assert(!aa.equals(bb));

        long aWhen = aa.start();
        long bWhen;
        if (aWhen == ETERNAL || (bWhen = bb.start()) == ETERNAL)
            return DTERNAL;
        else {
            assert (aWhen != TIMELESS && bWhen != TIMELESS);
            long d;
            if (!absolute || aWhen <= bWhen)
                d = (bWhen - aWhen) - (absolute ? 0 : aa.id.eventRange());
            else
                d = (aWhen - bWhen) - (absolute ? 0 : bb.id.eventRange());

            return occToDT(d);
        }

    }

    @Override
    public void clear() {
        super.clear();
        byTerm.clear();
    }

    /**
     * creates an event for a hypothetical target which may not actually be an event;
     * but if it is there or becomes there, it will connect what it needs to
     */
    private Event shadow(Term v) {
        //return event(v, TIMELESS, false);
        return new Relative(v);
    }

    public final Event know(Term v) {
        //include the temporal information contained in a temporal-containing target;
        // otherwise it contributes no helpful information
        if (v.hasAny(Op.Temporal))
            return event(v, TIMELESS, TIMELESS, true);
        else
            return shadow(v);
    }

    public final Event know(Term t, long start) {
        return event(t, start, start, true);
    }

    public final Event know(Term t, long start, long end) {
        return event(t, start, end, true);
    }




    private Event event(Term t, long start, long end, boolean add) {
        if (!notExceedingNodes())
            throw new IndexOutOfBoundsException("node overflow");

        if (!t.op().eventable)
            throw new WTF();

        boolean mayRelink = add && start != TIMELESS &&
                (NAL.derive.TIMEGRAPH_ABSORB_CONTAINED_EVENT ||
                        NAL.derive.TIMEGRAPH_MERGE_INTERSECTING_EVENTS);

        FasterList<FromTo<Node<Event,TimeSpan>,TimeSpan>>
                relinkIn = mayRelink ? new FasterList() : null,
                relinkOut = mayRelink  ? new FasterList() : null;

        Event event;
        if (start == TIMELESS) {
            assert (add) : "use shadow(t) if not adding";
            event = shadow(t);
        } else {

            if (start!=ETERNAL) {
                //optional dithering
                start = eventOcc(start);
                end = eventOcc(end);
            }

            if (add) {

                Collection<Event> te = events(t);
                int nte = te.size();
                if (nte > 0) {

                    boolean hasNonEternalAbsolutes = false;
                    for (Event f : te) {
                        if (!(f instanceof Absolute))
                            continue;
                        Absolute af = (Absolute) f;

                        long as = af.start();
                        if (start == ETERNAL && as == ETERNAL)
                            return af;
                        if (af.start == start && af.end() == end)
                            return af;
                        if (as == ETERNAL)
                            continue;

                        if (NAL.derive.TIMEGRAPH_ABSORB_CONTAINED_EVENT) {
                            if (af.containsOrEquals(start, end)) {
                                //add = false;
                                //break; //dont affect the stored graph, but return the smaller interval that was input

                                return af; //return the absorbing event
                            }
                        }

                        hasNonEternalAbsolutes = true;
                    }

                    if (add && hasNonEternalAbsolutes) {
                        boolean stable;
                        do {

                            stable = true;

                            Iterator<Event> ff = te.iterator();
                            while (ff.hasNext()) {
                                Event f = ff.next();
                                if (!(f instanceof Absolute))
                                    continue;

                                Absolute af = (Absolute) f;

                                if (add) {
                                    if (af.containedIn(start, end)) {
                                        if (NAL.derive.TIMEGRAPH_ABSORB_CONTAINED_EVENT) {
                                            //absorb existing
                                            removeNode(f, relinkIn::add, relinkOut::add);
                                            ff.remove();
                                            nte--;
                                        }
                                    } else {
                                        if (start != ETERNAL && NAL.derive.TIMEGRAPH_MERGE_INTERSECTING_EVENTS) {
                                            long[] merged;
                                            if ((merged = af.unionIfIntersects(start, end)) != null) {

                                                //stretch
                                                start = merged[0];
                                                end = merged[1];

                                                removeNode(f, relinkIn::add, relinkOut::add);
                                                ff.remove();
                                                nte--;
                                                stable &= nte <= 1; //try again if other nodes, because it may connect with other ranges further in the iteration


                                                break;
                                            }
                                        }
                                    }
                                }


                            }
                        } while (!stable);
                    }
                }
            }

            event = (end != start) ? new AbsoluteRange(t, start, end) : new Absolute(t, start);
        }


        if (add) {
            return mayRelink ?
                    addNodeRelinked(event, relinkIn, relinkOut) :
                    addNode(event).id;
        } else {
            return event;
        }
    }


    private Event addNodeRelinked(Event e, FasterList<FromTo<Node<Event, TimeSpan>, TimeSpan>> relinkIn, FasterList<FromTo<Node<Event, TimeSpan>, TimeSpan>> relinkOut) {
        MutableNode<Event, TimeSpan> E = addNode(e);

        relinkIn.forEachWith((i, ee) -> {
            MutableNode src = (MutableNode) i.from();
            if (!containsNode(src.id))
                return; //was removed

            long d = i.id().dt;
            assert (d != ETERNAL && d != TIMELESS); //HACK TEMPORARY

            long stretch = i.to().id().start() - ee.id.start();
            if (stretch < 0)
                throw new WTF(); //HACK TEMPORARY //assert(stretch >= 0);

            d -= stretch;

            addEdgeByNode(src, TimeSpan.the(d), ee);

        }, E);

        relinkOut.forEachWith((o, ee) -> {
            MutableNode tgt = (MutableNode) o.to();
            if (!containsNode(tgt.id))
                return; //was removed

            long d = o.id().dt;
            assert (d != ETERNAL && d != TIMELESS); //HACK TEMPORARY

            long stretch = ee.id.end() - o.from().id().end();
            if (stretch < 0)
                throw new WTF(); //HACK TEMPORARY //assert(stretch >= 0);

            d += stretch;

            addEdgeByNode(ee, TimeSpan.the(d), tgt);

        }, E);

        return E.id;
    }

    private Collection<Event> events(Term t) {
        Collection<Event> ee = eventsOrNull(t);
        if (ee == null) return List.of(); else return ee;
    }

    @Nullable private Collection<Event> eventsOrNull(Term t) {
        return byTerm.get(t);
    }

//    private int absoluteCount(Term t) {
//        Collection<Event> tt = byTerm.get(t);
//        return absoluteCount(tt);
//    }
//
//    private static int absoluteCount(Collection<Event> tt) {
//        int c = 0;
//        for (Event tx : tt) {
//            if (tx instanceof Absolute)
//                c++;
//        }
//        return c;
//    }

//    public Event event(Event e) {
//        Node<Event, TimeSpan> existing = node(e);
//        return existing != null ? existing.id() : e;
//    }

//    protected void shadow(Term x, Term y) {
//        assert(!(x.equals(y)));
//        shadow(shadow(x), shadow(y));
//    }
//
//    protected void shadow(Event a, Event b) {
//        link(a, TimeSpan.TS_ZERO, b);
//    }

    protected boolean link(Event before, TimeSpan e, Event after) {
        MutableNode<Event, TimeSpan> x = addNode(before);
        MutableNode<Event, TimeSpan> y = before.equals(after) ? x : addNode(after);

        return notExceedingNodes() && addEdgeByNode(x, e, y);
    }

    private boolean notExceedingNodes() {
        return nodesMax == Integer.MAX_VALUE || nodes.size() < nodesMax;
    }

    private void link(Event x, long dt, Event y) {

//        if (dt == ETERNAL)
//            throw new WTF("maybe eliminate this case");

//        if (NAL.DEBUG) {
//            if (dt == DTERNAL)
//                throw new WTF("probably meant to use ETERNAL"); //TEMPORARY
//            if (dt == XTERNAL)
//                throw new WTF("probably meant to use TIMELESS"); //TEMPORARY
//        }

        boolean parallel = dt == ETERNAL || dt == TIMELESS || dt == 0;
        int vc = x.compareTo(y);
        if (vc == 0) { //equal?
            if (parallel) return; //no point
            y = x; //use same instance, they could differ
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

//        final boolean[] newTerm = {false}, newEvent = {false};
//        byTerm.compute(eventTerm, (k, v) -> {
//            if (!(v instanceof Set)) {
//            //if (v == null) {
//                newTerm[0] = newEvent[0] = true;
//                //return new ArrayHashSet<Event>(2).with(event);
//                return new UnifiedSet<Event>(2, 1f).with(event);
//            } else {
//                if (v.add(event)) {
//                    newEvent[0] = true;
//                }
//                return v;
//            }
//        });
        Collection<Event> ee = byTerm.computeIfAbsent(eventTerm, (e) ->
                //!(p instanceof Set) ?
                        new UnifiedSet<>(2, 1f)
                  //      : p
        );
        boolean newTerm = ee.isEmpty();
        boolean newEvent = ee.add(event);
        if (newTerm)
            onNewTerm(eventTerm);
        if (!newEvent)
            return;



//        Collection<Event> ee = byTerm.get(eventTerm);
//        if (ee.isEmpty()) {
//            byTerm.put(eventTerm, new UnifiedSet(2).with(event)); //TODO pool these Set's
//            onNewTerm(eventTerm);
//        } else {
//            if (!ee.addAt(event))
//                //if (!byTerm.put(eventTerm, event))
//                return; //already present
//        }

        if (eventTerm instanceof CommonVariable) {
            CommonVariable c = ((CommonVariable) eventTerm);
            for (Term v : c.common()) {
                link(event, 0, shadow(v)); //equivalence
            }
        }

        if (decomposeAddedEvent(event)) {
            int edt = eventTerm.dt();


            switch (eventTerm.op()) {
                default: {
                    if (eventTerm.hasAny(Op.Temporal)) {
                        eventTerm.recurseTerms(s -> {
                            if (!s.op().temporal)
                                return true;
                            else {
//                                /* absolute context for inner compound is ignored/erased unless the root is temporal compound (handled in cases below) */
//                                know(s);
                                return false;
                            }
                        }, (t) -> true, null);
                    }
                    break;
                }

                case IMPL:

                    Term subj = eventTerm.sub(0), pred = eventTerm.sub(1);
                    Event se = know(subj), pe = know(pred);

                    if (edt == DTERNAL)
                        edt = 0;
                    //if (edt == DTERNAL) {

                        //link(se, ETERNAL, pe);

//                        //link first two events of each
//                        if (subj.hasAny(Op.CONJ)) {
//                        subj.eventsWhile((w, y) -> {
//                            //link(know(y), ETERNAL, pe); //<- is this used?
//                            know(y);
//                            return true;
//                        }, 0, false, true, false);

//
//                        pred.eventsWhile((w, y) -> {
//                            //link(se, ETERNAL, know(y));  //<- is this used?
//                            know(y);
//                            return true;
//                        }, 0, false, true, false);


                    if (edt == XTERNAL) {
                        /* without any absolute context */
                    } else {
                        link(se, edt + subj.eventRange(), pe);
                    }

                    break;


                case CONJ:


                    long eventStart = event.start(), eventEnd = event.end();

                    switch (edt) {

                        case XTERNAL:
                            for (Term y : eventTerm.subterms())
                                know(y);
                            break;

                        case DTERNAL:
                        default:

                            if (edt == DTERNAL && !Conj.isSeq(eventTerm)) {

                                //commutive dternal: inherit event time simultaneously
                                eventTerm.subterms().forEach(y -> know(y, eventStart, eventEnd));

                            } else {

                                if (eventStart != ETERNAL && eventStart != TIMELESS) {

                                    long range = eventEnd - eventStart;
                                    eventTerm.eventsWhile((w, y) -> {
//                                        if (y.equals(eventTerm))
//                                            return true;

                                        Event Y = know(y, w, w + range);

                                        //chain the events to the absolute parent
                                        link(event, w - eventStart, Y);
                                        return true;
                                    }, eventStart, false, false);

                                } else {
                                    //chain the events together relatively.  chain to the parent event if it's absolute
                                    final Event[] prev =
                                            //{ eventStart!=ETERNAL && eventStart!=TIMELESS ? event : null };
                                            {event};
                                    final long[] prevTime = {0};
                                    eventTerm.eventsWhile((w, y) -> {
//                                        if (y.equals(eventTerm))
//                                            return true;

                                        Event next = know(y);

                                        Event p = prev[0];
                                        if (p != null) {
                                            if (p != event) {
                                                //chain to previous, starting with parent
                                                link(p, w - prevTime[0], next);

                                            } else {
                                                //chain to parent
                                                if (eventStart != ETERNAL)
                                                    link(p, w, next);
                                            }

                                        }
                                        prevTime[0] = w;
                                        prev[0] = next;
                                        return true;
                                    }, 0, false, false);
                                }

                            }
                    }
                    break;
            }
        }

    }

    protected boolean decomposeAddedEvent(Event event) {
        return true;
    }

    private void onNewTerm(Term t) {
//        if (autoNeg != null && autoNeg.contains(t.unneg())) {
//            link(shadow(t), 0, shadow(t.neg()));
//        }
    }


    private boolean solveDT(Compound x, Predicate<Event> each) {

        if (!termsEvent(x)) return true;

        assert (x.dt() == XTERNAL);

        Subterms xx = x.subterms();

        //assert(!xx.hasXternal()): "dont solveDTTrace if subterms have XTERNAL";

        int s = xx.subs();
        if (s == 2) {
            Term a = xx.sub(0), b = xx.sub(1);

            return solveDTAbsolutePair(x, each, a, b) && solveDTpair(x, each, a, b);

        } else {
            assert (x.op() == CONJ);

            List<Event>[] subEvents = new FasterList[s];
            int abs = 0;
            for (int i = 0; i < s; i++) {
                List<Event> f = subEvents[i] = new FasterList();
                solveOccurrence(xx.sub(i), false, (se)->{
                    if (se instanceof Absolute) {
                        f.add(se);
                        return false; //one should be enough
                    } else {
                        return true;
                    }
                });
                if(!f.isEmpty())
                    abs++;
            }
            if (abs > 0) {
                //TODO allow solving subset >=2 not just all s

                Term unknown = null;
                if (abs < s) {
                    if (s - abs > 1) {
                        Term[] unknowns = new Term[s - abs]; //assume in correct order
                        int j = 0;
                        for (int i = 0; i < s; i++)
                            if (subEvents[i].isEmpty())
                                unknowns[j++] = xx.sub(i);
                        unknown = CONJ.the(XTERNAL, unknowns);
                    } else {
                        for (int i = 0; i < s; i++) {
                            if (subEvents[i].isEmpty()) {
                                unknown = xx.sub(i);
                                break;
                            }
                        }
                    }
                    assert(unknown!=null);
                }

                long start = Long.MAX_VALUE, range = Long.MAX_VALUE;
                Term nextKnown = null;
                if (abs > 1) {
                    //absolute value for each is known
                    //TODO permute these. this just takes the first of each
                    Conj cc = new Conj(s);
                    for (int i = 0; i < s; i++) {
                        if (!subEvents[i].isEmpty()) {
                            Event e = subEvents[i].get(0);
                            long es = e.start();
                            start = Math.min(es, start);
                            range = range > 0 ? Math.min(e.end() - es, range) : 0;
                            if (!cc.add(es, e.id))
                                break;
                        }
                    }

                    nextKnown = cc.term();

                } else {
                    for (int i = 0; i < s; i++) {
                        if (!subEvents[i].isEmpty()) {
                            Event e = subEvents[i].get(0);
                            nextKnown = e.id;
                            start = e.start();
                            range = e.end() - start;
                            break;
                        }
                    }
                }

                if (nextKnown != False && nextKnown != Null) {
                    assert (nextKnown != null);
                    if (unknown != null) {
                        nextKnown = CONJ.the(XTERNAL, nextKnown, unknown);
                    }

                    if (validPotentialSolution(nextKnown)) {
                        if (!each.test(event(nextKnown, start, start + range, false)))
                            return false;
                    }
                }
            } else {


                if (s == 3) {

                    Term a = xx.sub(0), b = xx.sub(1), c = xx.sub(2);

                    return solveDT((Compound) CONJ.the(XTERNAL, c, b),
                            bc -> solveDT((Compound) CONJ.the(XTERNAL, bc.id, a), each
                            ));

                } else if (s == 4) {

                    Term a = xx.sub(0), b = xx.sub(1), c = xx.sub(2), d = xx.sub(3);

                    return solveDT((Compound) CONJ.the(XTERNAL, d, c),
                            cd -> solveDT((Compound) CONJ.the(XTERNAL, cd.id, b),
                                    bc -> solveDT((Compound) CONJ.the(XTERNAL, bc.id, a), each
                                    )
                            )
                    );

                }
            }

            return true;
        }


    }


    private boolean solveDTpair(Compound x, Predicate<Event> each, Term a, Term b) {

        FasterList<Event> ab = null;

        boolean aEqB = a.equals(b);
        Collection<Event> aa = eventsOrNull(a);
        if (aa!=null)
            ab = new FasterList(aa);
        else if (aEqB)
            return true; //nothing

        if (!aEqB) {
            Collection<Event> bb = eventsOrNull(b);
            if (bb!=null) {
                if (ab != null)
                    ab.addAll(bb);
                else
                    ab = new FasterList(bb);
            } else {
                if (ab == null)
                    return true; //nothing
            }
        }

        Collection<Event> AB = sortEvents(ab);

        return true
            && (AB.isEmpty() || bfsAdd(AB, new DTPairSolver(a, b, x, each, true, false, false)))
//            && bfsNew(AB, new DTPairSolver(a, b, x, each, false, true, false))
//            && bfsNew(AB, new DTPairSolver(a, b, x, each, false, false, true))
        ;
    }

    /**
     * weather subEvent time occurrs at the start of the event
     */
    private static boolean at(Term x, Term y) {
        if (y.equals(x))
            return true;
        int xv = x.volume(), yv = y.volume();
        if (xv > yv)
            return atStart(y, x);
        else if (yv > xv)
            return atStart(x, y);
        else
            return false;
    }

    private static boolean atStart(Term subEvent, Term event) {
        if (event.op() == CONJ && event.dt() != XTERNAL) {
            boolean seq = Conj.isSeq(event);
            if (!seq) {
                return event.contains(subEvent);
            } else if (seq) {
                return event.eventFirst().equals(subEvent);
            }

        }
        return false;
    }

    private boolean solveDTAbsolutePair(Compound x, Predicate<Event> each, Term a, Term b) {
        if (a.hasXternal() || b.hasXternal())
            return true; //N/A

        UnifiedSet<Event> ae = new UnifiedSet(2);
        //solveExact(a, ax -> {
        solveOccurrence(a, false, ax -> {
            if (ax instanceof Absolute) ae.add(ax);
            return true;
        });
        int aes = ae.size();
        if (aes > 0) {
            Event[] aa = eventArray(ae);

            if (a.equals(b) && aes > 1) {


                for (int i = 0; i < aa.length; i++) {
                    Event ii = aa[i];
                    for (int j = i + 1; j < aa.length; j++) {
                        if (!solveDTAbsolutePair(x, ii, aa[j], each))
                            return false;
                    }
                }


            } else {
                solveOccurrence(b, false, bx -> {
                    if ((bx instanceof Absolute) && ae.add(bx)) {
                        for (Event ax : aa) {
                            if (!solveDTAbsolutePair(x, ax, bx, each))
                                return false;
                        }
                    }
                    return true;
                });

            }
        }
        return true;
    }

    private static final Event[] EMPTY_EVENT_ARRAY = new Event[0];

    private Event[] eventArray(UnifiedSet<Event> ae) {
        Event[] aa = ae.toArray(EMPTY_EVENT_ARRAY);
        if (aa.length > 1) ArrayUtil.shuffle(aa, random());
        return aa;
    }

    private boolean solveDTAbsolutePair(Compound x, Event a, Event b, Predicate<Event> each) {
//        assert (!(a.equals(b)));
//        if (a.start() == b.start() && at(a.id, b.id))
//            return true; //same event
//        //TODO additional checking



        if (x.op() == CONJ) {
            int dt = dt(a, b, true);
            return solveConj2DT(each, a, dt, b);
        } else {
            //for impl and other types cant assume occurrence corresponds with subject
            int dt = dt(a, b, false);
            return solveDT(x, TIMELESS, dt, durMerge(a, b), null, true, each);
        }
    }

    /**
     * solution vector for 2-ary CONJ
     */
    private boolean solveConj2DT(Predicate<Event> each, Event a, int dt, Event b) {

        if (dt != DTERNAL && dt != 0) {
            assert (dt != XTERNAL);
            //swap to correct sequence order
            if (a.start() > b.start()) {
                Event z = a;
                a = b;
                b = z;
//                dt = -dt;
            }
        }

        Term c = ConjSeq.sequence(a.id, dt == DTERNAL ? ETERNAL : 0, b.id, dt == DTERNAL ? ETERNAL : dt, terms);


        return solveOccurrence(c, a.start(), durMerge(a, b), each);
    }

    private static long durMerge(Event a, Event b) {
        if (a instanceof Absolute && b instanceof Absolute) {
            long ad = a.dur(), bd = b.dur();
            return (a.id.op() == IMPL || b.id.op() == IMPL) ?
                    Math.max(ad, bd) //implications are like temporal pointers, not events.  so they shouldnt shrink duration
                    :
                    Math.min(ad, bd);
        } else if (a instanceof Absolute && !(b instanceof Absolute)) {
            return a.dur();
        } else if (b instanceof Absolute && !(a instanceof Absolute)) {
            return b.dur();
        } else {
            return 0;
        }
    }

//    /**
//     * tests whether the two terms refer to the same sub-events,
//     * which have known multiple occurrences
//     * which would cause incorrect results if interpreted literally
//     * this prevents separate instances of events from being welded together or arranged in the incorrect temporal order
//     * across time when there should be some non-zero dt
//     * <p>
//     * a volume ideally should be less than b's
//     */
//    private boolean commonSubEventsWithMultipleOccurrences(Term a, Term b) {
//        if (a.op() == CONJ && b.op() == CONJ) {
//            if (a.volume() > b.volume()) {
//                Term c = a; //swap
//                a = b;
//                b = c;
//            }
//
//            UnifiedSet<Term> eventTerms = new UnifiedSet(2);
//            a.eventsWhile((w, aa) -> {
//                if (absoluteCount(aa) > 1) {
//                    eventTerms.add(aa);
//                }
//                return true;
//            }, 0, true, false, true);
//
//            if (eventTerms.isEmpty())
//                return false;
//
//            return !b.eventsWhile((w, bb) -> {
//                return !eventTerms.remove(bb);
//            }, 0, true, false, true);
//        }
//        return false; //TODO test conj -> nonConj common subevent?
//    }

    /**
     * TODO make this for impl only because the ordering of terms is known implicitly from 'x' unlike CONJ
     */
    private boolean solveDT(Compound x, long start, int dt, long dur,
                            @Nullable List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, boolean dir, Predicate<Event> each) {

        return solveOccurrence(dt(x, dir, dt), start, dur, each);
    }

    private static boolean termsEvent(Term e) {
        return e.op().eventable;
    }

    private boolean solveOccurrence(Term y, long start, long dur, Predicate<Event> each) {
        if (!termsEvent(y))
            return true; //keep trying

        return start != TIMELESS ?
                each.test(event(y, start, (start != ETERNAL) ? start + dur : start, false))
                :
                solveOccurrence(y, true, each);
    }



    /** dt computation for term construction */
    protected int occToDT(long x) {
        assert (x != TIMELESS);
        int idt;
        if (x == ETERNAL)
            idt = DTERNAL;
        else
            idt = Tense.occToDT(x);
        return idt;
    }

    /**
     * internal time representation, override to filter occ (ex: dither)
     */
    public long eventOcc(long when) {
        return when;
    }

    /**
     * construct a new target.
     * expects 'dt' to be the final value (already dithered)
     */
    @Deprecated
    private Term dt(Compound x, boolean dir, int dt) {

        assert (dt != XTERNAL);

        if (dt == DTERNAL)
            return x.dt(DTERNAL);

        Subterms xx = x.subterms();
        assert (!xx.hasXternal());

        Op xo = x.op();

        Term x0 = xx.sub(0);
        if (xo == IMPL) {
            return x.dt(dt);
        } else if (xo == CONJ) {
            if (dt == 0) {
                return x.dt(dt);
            } else {

                if (x.dt() == XTERNAL) {

                    //use the provided 'path' and 'dir'ection, if non-null, to correctly order the sequence, which may be length>2 subterms
                    Term x1 = xx.sub(1);

                    if (dir) {
                        return ConjSeq.sequence(x0, 0, x1, dt, terms);
                    } else {
                        return ConjSeq.sequence(x1, 0, x0, -dt, terms);
                    }


                } else {

                    int early = Conj.conjEarlyLate(x, true);
                    if (early == 1)
                        dt = -dt;


                    Term xEarly = xx.sub(early);
                    Term xLate = xx.sub(1 - early);

                    return ConjSeq.sequence(
                            xEarly, 0,
                            xLate, dt, terms);
                }

            }
        }

        throw new UnsupportedOperationException();
    }


    private final boolean solution(Event y) {
        if (!(y.start() == TIMELESS && solving.equals(y.id)) && validPotentialSolution(y.id)) {
            if (solutions.add(y)) {
                return target.test(y);
            }
        }
        return true;
    }


//    private boolean solveExact(Event f, Predicate<Event> each) {
//        if (f instanceof Absolute && !f.id.hasXternal()) {
//            return each.test(f);
//        } else {
//
//            //try exact absolute solutions
//
//            for (Event e : events(f.id)) {
//                if (e instanceof Absolute && ((!(f instanceof Absolute)) || !e.equals(f)) && !each.test(e))
//                    return false;
//            }
//
//            return true;
//        }
//    }

//    private boolean solveExact(Term x, Predicate<Event> each) {
//        return solveExact(null, each, x);
//    }

    //    @Nullable
//    private Event onlyAbsolute(Term x) {
//        Event first = null;
//        for (Event e : byTerm.get(x)) {
//            if (e instanceof Absolute) {
//                if (first == null)
//                    first = e;
//                else
//                    return null; //more than one, ambiguous
//            }
//        }
//        return first;
//    }

    /**
     * main entry point to the solver
     *
     * @seen callee may need to clear the provided seen if it is being re-used
     */
    public boolean solve(Term x, Predicate<Event> target) {


        this.target = target;
        this.solving = x;
        this.solutions.clear();

        return solveAll(x, this::solution);

    }

    protected boolean validPotentialSolution(Term y) {
        return termsEvent(y);
    }

    private boolean solveAll(Term x, Predicate<Event> each) {
        if (!x.hasXternal()) {
            return solveOccurrence(x, true, each);
        } else {
            if (!solveRootMatches(x, each))
                return false;

            if (x.subterms().hasXternal()) {
                return solveDTAndOccRecursive(x, each);
            } else {
                return solveDtAndOccTop(x, each);
            }
        }
    }


    private boolean solveRootMatches(Term x, Predicate<Event> each) {

        Op xop = x.op();
        boolean xVar = xop.var;

        //if (x.equals(x.root())) {
            //try any absolute events which have different target ID but the same target root as these will be readily valid solutions
            FasterList<Event> toTry = null; //buffer to avoid concurrent modification exception
            for (Map.Entry<Term, Collection<Event>> e : byTerm.entrySet()) {
                Term et = e.getKey();

                if (xop == et.op() && (x.equals(et))) { //|| (!xVar && !et.op().var && x.unify(et, u.clear())))) {

                    for (Event z : e.getValue()) {
                        if (z instanceof Absolute || !z.id.equals(x)) {
                            if (toTry == null) toTry = new FasterList();
                            toTry.add(z);
                        }
                    }
                }
            }
            if (toTry != null) {
                if (toTry.size() > 1)
                    toTry.shuffleThis(random());
                return toTry.allSatisfy(each::test);
            }

        //}


        return true;
    }

    private boolean solveDTAndOccRecursive(Term x, Predicate<Event> each) {

        Map<Compound, Set<Compound>> subSolved = new UnifiedMap(4);

        x.recurseTerms(Term::hasXternal, y -> {
            if (y instanceof Compound && y.dt() == XTERNAL && !y.subterms().hasXternal()) {

                subSolved.computeIfAbsent((Compound) y, (yy) -> {

                    Set<Compound> s = new UnifiedSet(2);

//                    int yv = yy.volume();
                    Op yyo = yy.op();
                    solveDT(yy, z -> {
//                        if (z.id.volume()<yv)
//                            return true; //something probably collapsed

                        //TODO there could be multiple solutions for dt
                        Term zz = z.id;
                        if (!(zz instanceof Compound) || zz.op()!=yyo)
                            return true; //skip non-compound result (degenerate?)

                        //assert (zz.dt() != XTERNAL);
                        s.add((Compound) zz);
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
                Map.Entry<Compound, Set<Compound>> xy = subSolved.entrySet().iterator().next();

                Set<Compound> sy = xy.getValue();
                if (!sy.isEmpty()) {
                    Term xyx = xy.getKey();
                    Compound[] two = sy.toArray(Op.EmptyCompoundArray);
                    if (two.length > 1) ArrayUtil.shuffle(two, random());

//                    int xv = x.volume();
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
                List<Pair<Compound, Compound[]>> substs = new FasterList();
                final int[] permutations = {1};
                subSolved.forEach((h, w) -> {
                    Compound[] ww = w.toArray(EmptyCompoundArray);
                    assert (ww.length > 0);
                    permutations[0] *= ww.length;
                    substs.add(pair(h, ww));
                });
                int ns = substs.size();
                assert (ns > 0);
                Random rng = random();


                while (permutations[0]-- > 0) {
                    Map<Term, Term> m = new UnifiedMap(ns);
                    for (Pair<Compound, Compound[]> si : substs) {
                        Compound[] ssi = si.getTwo();
                        Term sssi = ssi[ssi.length > 1 ? rng.nextInt(ssi.length) : 0];
                        m.put(si.getOne(), sssi);
                    }
                    Term z = x.replace(m);

                    if (!solveDtAndOccIfConceptualizable(x, z, each))
                        return false;
                }
                //break;
        }

        return true;
    }

    /**
     * collapse any relative nodes to unique absolute nodes,
     * possibly also map less-specific xternal-containing terms to more-specific unique variations
     */
    public void compact() {
        for (Collection<Event> e : byTerm.values()) {
            if (e.size() == 2) {
                Iterator<Event> ee = e.iterator();
                Event a = ee.next(), r = ee.next();
                if (a instanceof Absolute ^ r instanceof Absolute) {
                    //System.out.println("BEFORE: "); print();

                    if (r instanceof Absolute) {
                        //swap
                        Event x = a;
                        a = r;
                        r = x;
                    }
                    mergeNodes(r, a);
                    ee.remove(); //e.remove(r);

                    //System.out.println("AFTER: "); print(); System.out.println();
                }
            }
        }

    }

    private boolean solveDtAndOccIfConceptualizable(Term x, Term y, Predicate<Event> each) {
        if (y == null || !termsEvent(y) || y.equals(x))
            return true;

        return y.hasXternal() ? solveAll(y, each) : solveDtAndOccTop(y, each);
    }

    /* solve xternal occurring at the root of a compound (without any internal xternal remaining) */
    private boolean solveDtAndOccTop(Term x, Predicate<Event> each) {
        if (!validPotentialSolution(x)) return true;

        return ((x instanceof Compound && x.dt() == XTERNAL)?
            solveDT((Compound) x, y -> !validPotentialSolution(y.id) ||
                    (y instanceof Absolute ?
                            each.test(y)
                            :
                            solveOccurrence(y, true, each)))
        :
            //dont solve if more specific dt solved further in previous solveDT call
            solveOccurrence(x, true, each)
        );
    }


    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    private boolean solveOccurrence(Term x, boolean finish, Predicate<Event> each) {
        if (!validPotentialSolution(x)) return true;

        return solveOccurrence(shadow(x), finish, each);
    }

//    final boolean bfsPush(Event root, Search<Event, TimeSpan> tv) {
//        return bfsPush(List.of(root), tv);
//    }

//    private boolean bfsNew(Iterable<Event> roots, Search<Event, TimeSpan> tv) {
//
//        boolean result = bfs(Iterables.transform(roots, r -> {
//            addNewNode(r);
//            return r;
//        }), tv);
//
////        if (created != null/* && result *//* tail call optimization  - dont bother removing if we're done anyway */) {
////            int m = 0;
////            for (Event x : roots) {
////                if (created.get(m++))
////                    removeNode(x);
////            }
////        }
//
//        return result;
//    }

    private boolean solveOccurrence(Event x, boolean finish, Predicate<Event> each) {
        assert(!(x instanceof Absolute));

        return true
//                solveExact(x, each) &&
               && (x.id.hasXternal() || bfsAdd(x, new OccSolver(true, true, autoneg, each)))
               //&& bfsNew(List.of(x), new OccSolver(false, false, true, each))
               && solveSelfLoop(x, each)
//               && (!autoneg || bfsNew(x.neg(), new OccSolver(true, false, true,
//                    z -> each.test(z.neg()))))
               && (!finish || solveLastResort(x, each))
                ;
    }

    /**
     * check for any self-loops and propagate forward and/or reverse
     */
    private boolean solveSelfLoop(Event x, Predicate<Event> each) {
        if (!solutions.isEmpty()) {
            Term t = x.id;
            /** clone the list because modifying solutions while iterating will cause infinite loop */
            return new FasterList<>(solutions.list).allSatisfy((s) -> {
                if (s instanceof Absolute && (s.start()!=ETERNAL) && !(s.equals(x)) && s.id.equals(t)) {
                    for (Event e : events(t)) {
                        //TODO shuffle found self-loops, there could be sevreal
                        Node<Event, TimeSpan> ne = node(e);
                        if (ne != null) {
                            for (Iterator<FromTo<Node<Event, TimeSpan>, TimeSpan>> iterator = ne.edgeIterator(false, true); iterator.hasNext(); ) {
                                FromTo<Node<Event, TimeSpan>, TimeSpan> ee = iterator.next();
                                long dt = ee.id().dt;
                                if (dt != 0 && dt != ETERNAL && dt != TIMELESS) {
                                    if (ee.loop()) {
//                                        if (random().nextBoolean())
//                                            dt = -dt;
                                        Absolute as = (Absolute) s;
                                        if (!each.test(as.shift(+dt)))
                                            return false;
                                        if (!each.test(as.shift(-dt)))
                                            return false;

                                    }
                                }
                            }
                        }

                    }
                }
                return true;
            });
        }
        return true;
    }

    private boolean solveLastResort(Event x, Predicate<Event> each) {
//        if (!(x instanceof Relative))
//            throw new TODO("should this each.test(x)?");
        //return !(x instanceof Relative) || each.test(x);
        return each.test(x);
    }


    protected Random random() {
        return ThreadLocalRandom.current();
    }


    /**
     * absolutely specified event
     */

    public static class Absolute extends Event {
        static final long SAFETY_PAD = 32 * 1024;
        final long start;

        Absolute(Term t, long start, long end) {
            super(t, Util.hashCombine(t.hashCode(), start, end));

            assert (start != TIMELESS);
            if (!(start == ETERNAL || start > ETERNAL + SAFETY_PAD))
                throw new MathArithmeticException();
            if (!(start < TIMELESS - SAFETY_PAD))
                throw new MathArithmeticException();

            this.start = start;
        }

        Absolute(Term t, long start) {
            this(t, start, start);
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

        final boolean containedIn(long cs, long ce) {
            return (cs <= start && ce >= end());
        }

        final boolean containsOrEquals(long cs, long ce) {
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

        private @Nullable long[] unionIfIntersects(long start, long end) {
            long thisStart = this.start;
            long thisEnd = end();
            return LongInterval.intersectsSafe(start, end, thisStart, thisEnd) ?
                    Longerval.unionArray(start, end, thisStart, thisEnd) :
                    null;
        }

        Event shift(long dt) {
            assert (dt != 0 && dt!=ETERNAL && dt!=TIMELESS);
            if (this instanceof AbsoluteRange) {
                return new AbsoluteRange(id, start + dt, end() + dt);
            } else {
                return new Absolute(id, start + dt);
            }
        }

        @Override
        public Event neg() {
            return new Absolute(id.neg(), start);
        }
    }

    public static final class AbsoluteRange extends Absolute {
        final long end;

        AbsoluteRange(Term t, long start, long end) {
            super(t, start, end);
            if (end <= start || start == ETERNAL || start == TIMELESS)
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

        @Override
        public final Event neg() {
            return new AbsoluteRange(id.neg(), start, end);
        }
    }

    /**
     * TODO RelativeRange?
     */
    public static final class Relative extends Event {

        Relative(Term id) {
            super(id, id.hashCode() /*hashCombine(id.hashCode(), TIMELESS)*/);
        }

        @Override
        public final long start() {
            return TIMELESS;
        }

        @Override
        public final long end() {
            return TIMELESS;
        }

        @Override
        public long dur() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Event neg() {
            return new Relative(id.neg());
        }
    }

    /**
     * floating, but potentially related to one or more absolute event
     */


    private static final Iterable empty = List.of();
    @Deprecated /* TODO shuffle */ private static final Comparator<FromTo<Node<Event, TimeSpan>, TimeSpan>> temporalProximity = (a,b)->{
        //TODO provenance preference: prefer edges tagged by the opposite source (task -> belief, rather than task -> task)
        if (a == b) return 0;
        TimeSpan aa = a.id(), bb = b.id();

        long adt = aa.dt, bdt = bb.dt;

        //assert(adt != ETERNAL && bdt != ETERNAL): "shouldnt happen";

        if (adt == bdt) {
            int c = a.from().id().compareTo(b.from().id());
            if (c == 0) {
                c = a.to().id().compareTo(b.to().id());
                if (c == 0)
                    return Integer.compare(System.identityHashCode(a), System.identityHashCode(b)); //HACK
            }
            return c;
        } else
            return Long.compare(adt, bdt);
    };

//    static final Comparator<? super Event> eventPreference = (a, b)->{
//        if (a == b) return 0;
//        boolean ar = a instanceof Relative, br = b instanceof Relative;
//        if (b instanceof Relative && !(ar)) return -1;
//        if (ar && !(b instanceof Relative)) return +1;
//
//        if (!ar && !br) {
//            //both absolute: prefer longer duration
//            long ad = a.dur(), bd = b.dur();
//            if (ad > bd)
//                return -1;
//            else if (bd > ad) return +1;
//        }
//
//        //TODO shuffle?
//        return a.compareTo(b);
//    };

    public Collection<Event> sortEvents(Collection<Event> e) {

//        int s = ab.size();
//        assert (s > 0);
//
//        if (s > 1) {
////            ab.shuffleThis(random());
////
////            //then sort the Absolute events to be tried first
////            ab.sortThisByBoolean(e -> !(e instanceof Absolute));
//
//        }

        if (e.size() > 1) {
            FasterList<Event> ee = new FasterList(e);
            ee.shuffleThis(this::random);
            ee.sortThisByInt(x -> x instanceof Absolute ? -1 : 0);
            return ee;
        } else
            return e;
    }

    public Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> sortEdges(Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> e) {
        if ((e instanceof Collection && ((Collection)e).size() <= 1))
            return e;

        FasterList ee = new FasterList(e);
        if (ee.size() <= 1)
            return e;

        ee.shuffleThis(this::random);
        return ee;
    }

    private abstract class CrossTimeSolver extends Search<Event, TimeSpan> {

        /** enabled layers */
        final boolean existing, tangent, tangentNeg;

        protected CrossTimeSolver(boolean existing, boolean tangent, boolean tangentNeg) {

            assert(existing || tangent || tangentNeg);

            this.existing = existing;
            this.tangent = tangent;
            this.tangentNeg = tangentNeg;
        }

        @Override
        protected Iterable<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> next(Node<Event, TimeSpan> n) {


            Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> existing = this.existing ? existing(n) : empty;

            Term nid = n.id().id;
            Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> tangent = this.tangent ? tangent(n, nid) : empty;
            Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> tangentNeg = this.tangentNeg ? tangent(n, nid.neg()) : empty;

            //TODO elide concat() if only one selected
            return Iterables.concat(existing, tangent, tangentNeg);
        }

        private Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> existing(Node<Event, TimeSpan> n) {

            //return n.edges(true, true, x -> log.hasNotVisited(x.other(n)));

            return sortEdges(n.edges(true, true, x -> log.hasNotVisited(x.other(n))));
        }

        Iterable<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> tangent(Node<Event, TimeSpan> root, Term t) {

            Collection<Event> ee = eventsOrNull(t);
            if (ee == null) {
                return empty;
            } else {

                Iterable<Event> eee =
                        //ee;
                        sortEvents(ee);

                return Iterables.transform(
                    Iterables.filter(
                        Iterables.transform(eee, TimeGraph.this::node),
                            n -> n != null && n!=root && log.hasNotVisited(n)
                    ),
                    n -> new ImmutableDirectedEdge(root, TS_ZERO, n)
                );
            }

        }



    }

    /**
     * TODO fair sampling
     */
    @Deprecated
    private int choose(int[] subTimes) {
        if (subTimes == null)
            return DTERNAL;
        else {
            return subTimes[subTimes.length == 1 ? 0 : random().nextInt(subTimes.length)];
        }
    }

    /**
     * computes dt, the length of time spanned from start to the end of the given path [0],
     * and the range [1]
     */
    @Nullable
    private static long[] pathTime(List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path) {

        long durEventMin = Long.MAX_VALUE, durImplMin = Long.MAX_VALUE;

        //compute the maximum duration first
        int i = 0;
        for (BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> span : path) {
            FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan> event = span.getTwo();

            if (i++ == 0) {
                //first iter only
                Event e = event.from().id();
                if (e instanceof Absolute) {
                    long d = e.dur();
                    if (e.id.op() == IMPL)
                        durImplMin = Math.min(durImplMin, d);
                    else
                        durEventMin = Math.min(durEventMin, d);
                }
            }

            {
                Event e = event.to().id();
                if (e instanceof Absolute) {
                    long d = e.dur();
                    if (e.id.op() == IMPL)
                        durImplMin = Math.min(durImplMin, d);
                    else
                        durEventMin = Math.min(durEventMin, d);
                }
            }

        }

        boolean allImpl = durEventMin == Long.MAX_VALUE && durImplMin != Long.MAX_VALUE;


        long dt = 0, dur = allImpl ? durImplMin : durEventMin;


        for (BooleanObjectPair<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> span : path) {

            FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan> event = span.getTwo();

            long spanDT = event.id().dt;

            //assert (spanDT != TIMELESS);

            if (spanDT == ETERNAL) {
                //dt = ETERNAL; //lock in eternal mode for the duration of the path, but still accumulate duration
                //no effect but do not set eternable

                if (spanDT!=0)
                    return null; //crossover from eternal to temporal

            } else if (dt != ETERNAL && spanDT != 0) {
                dt += (spanDT) * (span.getOne() ? +1 : -1);
            }
        }


        if (dur == Long.MAX_VALUE)
            dur = 0; //all relative events, shrink to point

        return new long[]{dt, dur};
    }

    public abstract static class Event implements LongObjectPair<Term> {

        public static final Event[] EmptyArray = new Event[0];

        public final Term id;
        private final int hash;

        Event(Term id, int hash) {
            this.id = id;
            this.hash = hash;
        }

        abstract public Event neg();

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

        private final static Comparator<Event> cmp = Comparator
                .comparing((Event e) -> e.id)
                .thenComparingLong(Event::start)
                .thenComparingLong(Event::end);

        @Override
        public int compareTo(LongObjectPair<Term> e) {
            return this==e ? 0 : cmp.compare(this, (Event)e);
        }

        abstract public long dur(); //        return end() - start();

        public long mid() {
            return (start() + end())/2L;
        }
    }

    private class DTPairSolver extends CrossTimeSolver {

        private final Term a;
        private final Term b;
        private final Compound x;
        private final Predicate<Event> each;

        public DTPairSolver(Term a, Term b, Compound x, Predicate<Event> each, boolean existing, boolean tangent, boolean tangentNeg) {
            super(existing, tangent, tangentNeg);
            this.a = a;
            this.b = b;
            this.x = x;
            this.each = each;
        }

        @Override
        protected boolean next(BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>> move, Node<Event, TimeSpan> next) {

            Node<Event, TimeSpan> s = pathStart(path);
            Node<Event, TimeSpan> e = pathEnd(path);
            Event ss = s.id();
            Term st = ss.id;
            Event ee = e.id();
            Term et = ee.id;

            boolean dir;
            if (at(a, st) && at(b, et)) dir = true;
            else if (at(a, et) && at(b, st)) dir = false;
            else {
                return true; //not the target destination
            }

            long start;

            long[] pt = pathTime(path);
            if (pt == null)
                return true;

            long dt = pt[0];

            if (dt == ETERNAL)
                dt = 0; //HACK

            if (!(ss instanceof Absolute) && !(ee instanceof Absolute)) {
                start = TIMELESS;
            } else {
                long SS = ss.start(), EE = ee.start();
                if (SS == TIMELESS || EE == TIMELESS)
                    start = TIMELESS;
                else {
                    if (SS == ETERNAL && EE == ETERNAL) {
                        start = ETERNAL;
                    } else {
                        if (SS == ETERNAL && EE != ETERNAL) {
                            SS = EE; //collapse eternity
                        } else if (SS != ETERNAL && EE == ETERNAL) {
                            EE = SS; //collapse eternity
                        }

                        assert (SS != ETERNAL && EE != ETERNAL);
                        if (dir) {
                            if (ss instanceof Absolute) {
                                start = SS;
                            } else {
                                start = EE - dt - a.eventRange();
                            }
                        } else {
                            if (ee instanceof Absolute) {
                                start = EE;
                            } else {
                                long sss = ss.start();
                                start = SS - dt - b.eventRange();
                            }
                        }
                    }
                }
            }

            if (dt != ETERNAL && !dir)
                dt = -dt;

            long dur = pt[1];
            if (x.op() == IMPL) {
                start = TIMELESS; //the start time does not mean the event occurrence time
                if (dt != ETERNAL)
                    dt -= a.eventRange();
            }

            return solveDT(x, start, occToDT(dt), dur, path, dir, each);
        }
    }

    private class OccSolver extends CrossTimeSolver {

        private final Predicate<Event> each;

        OccSolver(boolean existing, boolean tangent, boolean tangentNeg, Predicate<Event> each) {
            super(existing, tangent, tangentNeg);
            this.each = each;
        }

        @Override
        protected boolean next(BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>> move, Node<Event, TimeSpan> n) {

            Event end = n.id();

            if (!(end instanceof Absolute))
                return true;

            long pathEndTime = end.start();


            long startTime, endTime;
            if (pathEndTime == ETERNAL) {

                startTime = endTime = ETERNAL;

            } else {

                long[] pt = pathTime(path);
                if (pt == null || pt[0] == ETERNAL)
                    return true;

                startTime = pathEndTime - (pt[0] /* pathDelta*/);
                assert (startTime != TIMELESS);
                endTime = startTime + pt[1]; /* pathRange */

            }

            Event solution = event(pathStart(path).id().id, startTime, endTime, false);

            return each.test(solution);
        }

    }
}
