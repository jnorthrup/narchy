package nars.time;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.edge.LazyMutableDirectedEdge;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.iterator.CartesianIterator;
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
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjList;
import nars.term.var.CommonVariable;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.IdempotentBool.False;
import static nars.term.atom.IdempotentBool.Null;
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
 * <p>
 * TODO
 * subclass of MapNodeGraph which allows tagging edges by an ID.  then index
 * the edges by these keys so they can be efficiently iterated and removed by tag.
 * <p>
 * then use it to store the edges in categories:
 * task time edges
 * belief time edges
 * conclusion time edges
 * autonegations
 * hypothesized (anything created during search)
 * etc.
 * <p>
 * then the premise can clear the conclusion edges while retaining task, belief edges throughout
 * each separate derivation of a premise.
 * also the tags can be used to heuristically bias the search via associated weight.  so autonegation
 * can be weighted less allowing preferential traversal of concrete edges in oscillating pos/neg event pathways.
 */
public class TimeGraph extends MapNodeGraph<TimeGraph.Event, TimeSpan> {


	private static final Event[] EMPTY_EVENT_ARRAY = Event.EmptyArray;

	@Deprecated /* TODO shuffle */ private static final Comparator<FromTo<Node<Event, TimeSpan>, TimeSpan>> temporalProximity = new Comparator<FromTo<Node<Event, TimeSpan>, TimeSpan>>() {
        @Override
        public int compare(FromTo<Node<Event, TimeSpan>, TimeSpan> a, FromTo<Node<Event, TimeSpan>, TimeSpan> b) {
            
            if (a == b) return 0;
            TimeSpan aa = a.id(), bb = b.id();

            long adt = aa.dt, bdt = bb.dt;

            

            if (adt == bdt) {
                int c = a.from().id().compareTo(b.from().id());
                if (c == 0) {
                    c = a.to().id().compareTo(b.to().id());
                    if (c == 0)
                        return Integer.compare(System.identityHashCode(a), System.identityHashCode(b)); 
                }
                return c;
            } else
                return Long.compare(adt, bdt);
        }
    };
	protected final ArrayHashSet<Event> solutions = new ArrayHashSet();
	/**
	 * index by target
	 */

	final Map<Term, Collection<Event>> byTerm = new UnifiedMap<>();
	public boolean autoneg = false;

	




	protected int nodesMax = Integer.MAX_VALUE;
	private transient Term solving;
	private Predicate<Event> target;

	private static Term unknownComponent(Subterms xx, List<Absolute>[] subEvents, int abs) {
        int s = subEvents.length;
		if (abs < s) {
			if (s - abs > 1) {
                Term[] unknowns = new Term[s - abs]; 
                int j = 0;
				for (int i = 0; i < s; i++)
					if (subEvents[i]==null)
						unknowns[j++] = xx.sub(i);
				return CONJ.the(XTERNAL, unknowns);
			} else {
				for (int i = 0; i < s; i++) {
					if (subEvents[i]==null)
						return xx.sub(i);
				}
				throw new UnsupportedOperationException();
			}
		} else
			return null;
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
			return !seq ? event.contains(subEvent) : event.eventFirst().equals(subEvent);

		}
		return false;
	}

	private static long durMerge(Event a, Event b) {
		if (a instanceof Absolute && b instanceof Absolute) {
			long ad = a.dur(), bd = b.dur();
			return (ad!=bd && (a.id.op() == IMPL || b.id.op() == IMPL)) ?
				Math.max(ad, bd) 
				:
				Math.min(ad, bd);
		} else if (a instanceof Absolute) {
			return a.dur();
		} else if (b instanceof Absolute) {
			return b.dur();
		} else {
			return 0L;
		}
	}

	private static boolean termsEvent(Term e) {
		return e != null && e.op().eventable;
	}

	/**
	 * computes dt, the length of time spanned from start to the end of the given path [0],
	 * and the range [1]
	 */
	private static @Nullable long[] pathTime(List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path) {

		long durEventMin = Long.MAX_VALUE, durImplMin = Long.MAX_VALUE;

		
        int i = 0;
		long dt = 0L;

		for (BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>> span : path) {

            FromTo<Node<Event, TimeSpan>, TimeSpan> event = span.getTwo();

			if (i++ == 0) {
				
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


            long spanDT = event.id().dt;

			

			if (spanDT == ETERNAL) {
				
				

				return null; 

			} else if (dt != ETERNAL && spanDT != 0L) {
				dt += (spanDT) * (long) (span.getOne() ? +1 : -1);
			}
		}

        boolean allImpl = durEventMin == Long.MAX_VALUE && durImplMin != Long.MAX_VALUE;
        long dur = allImpl ? durImplMin : durEventMin;
		if (dur == Long.MAX_VALUE)
			dur = 0L; 

		return new long[]{dt, dur};
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
	protected static Event shadow(Term v) {
		
		return new Relative(v);
	}

	public final Event know(Term v) {







		return know(v, TIMELESS);
	}

	public final Event know(Term t, long start) {
		return event(t, start, start, true);
	}

	public final Event know(Term t, long start, long end) {
		return event(t, start, end, true);
	}

	private Event event(Term t, long start, long end, boolean add) {

		if (!t.op().eventable)
			throw new WTF();

		if (add) {
			if (!notExceedingNodes()) {
                Node<Event, TimeSpan> existing = node(t);
				if (existing != null)
					return existing.id();
				else {
					if (NAL.DEBUG)
						throw new IndexOutOfBoundsException("node overflow");
					add = false;
				}
			}
		}


        boolean mayRelink = add && start != TIMELESS &&
			(NAL.derive.TIMEGRAPH_ABSORB_CONTAINED_EVENT ||
				NAL.derive.TIMEGRAPH_MERGE_INTERSECTING_EVENTS);

		FasterList<FromTo<Node<Event, TimeSpan>, TimeSpan>>
			relinkIn = mayRelink ? new FasterList() : null,
			relinkOut = mayRelink ? new FasterList() : null;

		Event event;
		if (start == TIMELESS) {
			event = shadow(t);
		} else {

			if (start != ETERNAL) {
				
				start = eventOcc(start);
				end = eventOcc(end);
			}

			if (add) {

                Collection<Event> te = events(t);
				if (te != null) {
                    int nte = te.size();
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
								
								

								return af; 
							}
						}

						hasNonEternalAbsolutes = true;
					}

					if (hasNonEternalAbsolutes) {
						boolean stable;
						do {

							stable = true;

                            Iterator<Event> ff = te.iterator();
							while (ff.hasNext()) {
                                Event f = ff.next();
								if (!(f instanceof Absolute))
									continue;

                                Absolute af = (Absolute) f;

								if (af.containedIn(start, end)) {
									if (NAL.derive.TIMEGRAPH_ABSORB_CONTAINED_EVENT) {
										
										removeNode(f, relinkIn::add, relinkOut::add);
										ff.remove();
										nte--;
									}
								} else {
									if (start != ETERNAL && NAL.derive.TIMEGRAPH_MERGE_INTERSECTING_EVENTS) {
										long[] merged;
										if ((merged = af.unionIfIntersects(start, end)) != null) {

											
											start = merged[0];
											end = merged[1];

											removeNode(f, relinkIn::add, relinkOut::add);
											ff.remove();
											nte--;
											stable = nte <= 1; 


											break;
										}
									}
								}


							}
						} while (!stable);

					}
				}
			}

			event = event(t, start, end);
		}


		if (add) {
			return mayRelink ?
				addNodeRelinked(event, relinkIn, relinkOut) :
				addNode(event).id;
		} else {
			return event;
		}
	}

	private static TimeGraph.Event event(Term t, long start, long end) {
		return (end != start) ? new AbsoluteRange(t, start, end) : new Absolute(t, start);
	}

	private Event addNodeRelinked(Event e, FasterList<FromTo<Node<Event, TimeSpan>, TimeSpan>> relinkIn, FasterList<FromTo<Node<Event, TimeSpan>, TimeSpan>> relinkOut) {
		AbstractNode<nars.time.TimeGraph.Event, nars.time.TimeSpan> E = addNode(e);

		relinkIn.forEachWith(new Procedure2<FromTo<Node<Event, TimeSpan>, TimeSpan>, AbstractNode<Event, TimeSpan>>() {
            @Override
            public void value(FromTo<Node<Event, TimeSpan>, TimeSpan> i, AbstractNode<Event, TimeSpan> ee) {
                MutableNode src = (MutableNode) i.from();
                if (!TimeGraph.this.containsNode(src.id))
                    return; 

                long d = i.id().dt;
                assert (d != ETERNAL && d != TIMELESS); 

                long stretch = i.to().id().start() - ee.id.start();
                if (stretch < 0L)
                    throw new WTF(); 

                d -= stretch;

                TimeGraph.this.addEdgeByNode(src, TimeSpan.the(d), (MutableNode) ee);

            }
        }, E);

		relinkOut.forEachWith(new Procedure2<FromTo<Node<Event, TimeSpan>, TimeSpan>, AbstractNode<Event, TimeSpan>>() {
            @Override
            public void value(FromTo<Node<Event, TimeSpan>, TimeSpan> o, AbstractNode<Event, TimeSpan> ee) {
                MutableNode tgt = (MutableNode) o.to();
                if (!TimeGraph.this.containsNode(tgt.id))
                    return; 

                long d = o.id().dt;
                assert (d != ETERNAL && d != TIMELESS); 

                long stretch = ee.id.end() - o.from().id().end();
                if (stretch < 0L)
                    throw new WTF(); 

                d += stretch;

                TimeGraph.this.addEdgeByNode((MutableNode) ee, TimeSpan.the(d), tgt);

            }
        }, E);

		return E.id;
	}


	private @Nullable Collection<Event> events(Term t) {
		return byTerm.get(t);
	}

	protected void link(Event before, TimeSpan e, Event after) {
		if (!notExceedingNodes())
			return;

        MutableNode<Event, TimeSpan> x = addNode(before);
        MutableNode<Event, TimeSpan> y = before.equals(after) ? x : addNode(after);

		addEdgeByNode(x, e, y);
	}

	private boolean notExceedingNodes() {
		return nodesMax == Integer.MAX_VALUE || nodes.size() < nodesMax;
	}

	protected void link(Event x, long dt, Event y) {











        boolean parallel = dt == ETERNAL || dt == TIMELESS || dt == 0L;
        int vc = x.compareTo(y);
		if (vc == 0) { 
			if (parallel) return; 
			y = x; 
			if (dt < 0L)
				dt = -dt; 
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















        Collection<Event> ee = byTerm.computeIfAbsent(eventTerm, new java.util.function.Function<Term, Collection<Event>>() {
                    @Override
                    public Collection<Event> apply(Term e) {
                        return new UnifiedSet<>(2, 1f);
                    }
                }
			
		);
        boolean newTerm = ee.isEmpty();
        boolean newEvent = ee.add(event);
		if (newTerm)
			onNewTerm(eventTerm);
		if (!newEvent)
			return;












		if (eventTerm instanceof CommonVariable) {
			
            CommonVariable c = ((CommonVariable) eventTerm);
			for (Term v : c.common())
				link(event, 0L, shadow(v)); 
			return;
		}

		if (decomposeAddedEvent(event)) {
            int edt = eventTerm.dt();

			switch (eventTerm.op()) {









				default: {
					eventTerm.recurseTerms(new Predicate<Term>() {
                        @Override
                        public boolean test(Term s) {
                            return s instanceof Compound && s.hasAny(Op.Temporal);
                        }
                    }, new Predicate<Term>() {
                        @Override
                        public boolean test(Term s) {
                            if (s.op().temporal) {
                                /* absolute context for temporal subterm of non-temporal */
                                TimeGraph.this.know(s);
                            }
                            return true;
                        }
                    }, null);
					break;
				}

				case IMPL:

					Term subj = eventTerm.sub(0), pred = eventTerm.sub(1);
					Event se = know(subj), pe = know(pred);

					if (edt == DTERNAL)
						edt = 0;
					

					

















					if (edt == XTERNAL) {
						/* without any absolute context */
					} else {
						link(se, (long) (edt + subj.eventRange()), pe);
					}


					

					break;


				case CONJ:


					long eventStart = event.start(), eventEnd = event.end();


                    Subterms eventSubs = eventTerm.subterms();
					switch (edt) {

						case XTERNAL:

							if (eventSubs.hasAny(Op.Temporal)) {
								for (Term y : eventSubs) {
									if (y.hasAny(Op.Temporal))
										know(y);
								}
							}
							break;

						case DTERNAL:
						default:
                            boolean absolute = eventStart != ETERNAL && eventStart != TIMELESS;
							if ((edt == 0 || (edt == DTERNAL && !Conj.isSeq(eventTerm)))) {

								
								for (Term y : eventSubs) {
									if (!y.op().eventable)
										continue; 
									if (absolute)
										know(y, eventStart, eventEnd);
									else
										link(event, 0L, know(y));
								}

							} else {

								if (absolute) {

                                    long range = eventEnd - eventStart;
									eventTerm.eventsAND(new LongObjectPredicate<Term>() {
                                        @Override
                                        public boolean accept(long w, Term y) {



                                            Event Y = TimeGraph.this.know(y, w, w + range);

                                            
                                            link(event, w - eventStart, Y);
                                            return true;
                                        }
                                    }, eventStart, false, false);

								} else {
									
									Event[] prev =
										
										{event};
									long[] prevTime = {0L};
									eventTerm.eventsAND(new LongObjectPredicate<Term>() {
                                        @Override
                                        public boolean accept(long w, Term y) {



                                            Event next = TimeGraph.this.know(y);

                                            Event p = prev[0];
                                            if (p != null) {
                                                if (p != event) {
                                                    
                                                    link(p, w - prevTime[0], next);

                                                } else {
                                                    
                                                    link(p, w, next);
                                                }

                                            }
                                            prevTime[0] = w;
                                            prev[0] = next;
                                            return true;
                                        }
                                    }, 0L, false, false);
								}

							}
					}





					break;
			}
		}

	}

	protected boolean decomposeAddedEvent(Event event) {
		return event.id instanceof Compound;
	}

	private void onNewTerm(Term t) {









	}

	private boolean solveDT(Compound x, Predicate<Event> each) {

		if (!termsEvent(x))
			return true;

		assert (x.dt() == XTERNAL);

        Subterms xx = x.subterms();

		

		
		return x.op() == IMPL ? solveDT_2(x, xx, each) : solveDTconj(x, xx, each);

	}

	private boolean solveDTconj(Compound x, Subterms xx, Predicate<Event> each) {

        int s0 = xx.subs();
		xx = xx.commuted();
        int s = xx.subs();

		List<Absolute>[] subEvents = new FasterList[s];
        int abs = solveAbsolutes(xx, subEvents);
		if (abs > 0) {
			if (s == 1) {
				
				
				for (Event e : subEvents[0]) {
					if (e!=null && !each.test(e))
						return false;
				}
			} else {

				if (!solveAbsolutePermutations(xx, subEvents, abs, each))
					return false;

				if (abs == s0)
					return true; 
			}

		}

		if (s0 == 2) {
			if (!solveDT_2(x, xx, each))
				return false;
		}

		if (s0 == 3) {




			Term a = xx.sub(0), b = xx.sub(1), c = xx.sub(2);

			return solveDT((Compound) CONJ.the(XTERNAL, b, c),
                    new Predicate<Event>() {
                        @Override
                        public boolean test(Event bc) {
                            return TimeGraph.this.solveDT((Compound) CONJ.the(XTERNAL, bc.id, a), each
                            );
                        }
                    });

		}

		if (s0 == 4) {

			Term a = xx.sub(0), b = xx.sub(1), c = xx.sub(2), d = xx.sub(3);

			return solveDT((Compound) CONJ.the(XTERNAL, d, c),
                    new Predicate<Event>() {
                        @Override
                        public boolean test(Event cd) {
                            return TimeGraph.this.solveDT((Compound) CONJ.the(XTERNAL, cd.id, b),
                                    new Predicate<Event>() {
                                        @Override
                                        public boolean test(Event bc) {
                                            return TimeGraph.this.solveDT((Compound) CONJ.the(XTERNAL, bc.id, a), each
                                            );
                                        }
                                    }
                            );
                        }
                    }
			);

		}

		


		return true;
	}





	private boolean solveDT_2(Compound x, Subterms xx, Predicate<Event> each) {
		Term a = xx.sub(0), b = (xx.subs() > 1 ? xx.sub(1) : a /* repeat */);
		return solveDT_2(x, a, b, each);
	}

	private boolean solveDT_2(Compound x, Term a, Term b, Predicate<Event> each) {
		if (x.op()==CONJ && a.volume() > b.volume()) {
			
            Term c = a;
			a = b;
			b = c;
		}

		return solveDTAbsolutePair(x, each, a, b) && solveDTpair(x, a, b, each);
	}

	private int solveAbsolutes(Subterms xx, List<Absolute>[] subEvents) {
        int abs = 0;
        int s = subEvents.length;
        FasterList<Absolute> f = new FasterList<Absolute>();
		Predicate<Absolute> adder = new Predicate<Absolute>() {
            @Override
            public boolean test(Absolute se) {
                f.add(se);
                
                return true;
            }
        };
		for (int i = 0; i < s; i++) {
			solveExact(xx.sub(i), adder);
            int fs = f.size();
			if (fs > 0) {
                FasterList<Absolute> ff = f.clone(); 
				subEvents[i] = ff;
				if (fs > 1)
					ff.shuffleThis(random());
				abs++;
				f.clear();
			} else
				subEvents[i] = null;
		}
		return abs;
	}

	private boolean solveAbsolutePermutations(Subterms xx, List<Absolute>[] subEvents, int abs, Predicate<Event> each) {

        Term unknown = unknownComponent(xx, subEvents, abs);

        int s = subEvents.length;
		if (abs > 1) {
			

            List<Absolute>[] subEvents2 = ArrayUtil.removeNulls(subEvents);

            CartesianIterator<Absolute> ci = new CartesianIterator<Absolute>(Absolute[]::new, subEvents2);

			ConjBuilder cc =
				new ConjList(abs);
			

			nextPermute:
			while (ci.hasNext()) {

                Absolute[] ss = ci.next();
				cc.clear();
				long range = 0L;
                long start = Long.MAX_VALUE;
				for (int i = 0; i < abs; i++) {
                    Absolute e = ss[i];


                    long es = e.start();
					if (es!=ETERNAL) {
						
						start = start == ETERNAL ? es : Math.min(es, start);
					} else {
						 if (start == TIMELESS || start == ETERNAL)
						 	start = es;
					}

					if (es != ETERNAL) {
						range = Math.min(e.end() - es, range);
					}
					if (!cc.add(es, e.id))
						continue nextPermute;

				}

                Term nextKnown = cc.term();
				if (termsEvent(nextKnown))
					if (!nextAbsolutePermutation(unknown, start, range, nextKnown, each))
						return false;
			}

		} else {

			int w = -1;
			for (int i = 0; i < s; i++) {
				if (subEvents[i] != null && !subEvents[i].isEmpty()) {
					w = i;
					break;
				}
			}
			
            List<Absolute> ss = subEvents[w];
			for (Absolute e : ss) {
				if (e == null) continue;
                long start = e.start();
                long range = start != ETERNAL ? e.end() - start : 0L;
				if (!nextAbsolutePermutation(unknown, start, range, e.id, each))
					return false;
			}
		}

		return true;
	}

	private boolean nextAbsolutePermutation(Term unknown, long start, long range, Term nextKnown, Predicate<Event> each) {

		if (nextKnown != False && nextKnown != Null) {

			return unknown != null ? solveDT_2((Compound) CONJ.the(XTERNAL, nextKnown, unknown), nextKnown, unknown, new Predicate<Event>() {
                @Override
                public boolean test(Event nu) {
                    return each.test(nu instanceof Absolute ? nu : TimeGraph.this.event(nu.id, start, start + range, false));
                }
            }) : !validPotentialSolution(nextKnown) || each.test(event(nextKnown, start, start + range, false));
		}
		return true; 
	}

	private boolean solveDTpair(Compound x, Term a, Term b, Predicate<Event> each) {

		FasterList<Event> ab = null;

        boolean aEqB = a.equals(b);
        Collection<Event> aa = events(a);
		if (aa != null)
			ab = new FasterList(aa);
		else if (aEqB)
			return true; 

		if (!aEqB) {
            Collection<Event> bb = events(b);
			if (bb != null) {
				if (ab != null)
					ab.addAll(bb);
				else
					ab = new FasterList(bb);
			} else {
				if (ab == null)
					return true; 
			}
		}

        Collection<Event> AB = shuffleAndSort(ab);

		return (AB.isEmpty() || bfsAdd(AB, new DTPairSolver(a, b, x, each, true, false, false)))


			;
	}




































	private boolean solveDTAbsolutePair(Compound x, Predicate<Event> each, Term a, Term b) {
		if (a.hasXternal() || b.hasXternal())
			return true; 

		UnifiedSet<Event> ae = new UnifiedSet(2);
		
		solveOcc(a, false, new Predicate<Event>() {
            @Override
            public boolean test(Event ax) {
                if (ax instanceof Absolute) ae.add(ax);
                return true;
            }
        });
        int aes = ae.size();
		if (aes > 0) {
            Event[] aa = eventArray(ae);

            boolean aEqB = a.equals(b);
			if (aEqB) {


				if (aes > 1) {
					boolean bidi;
					if (x.op() == IMPL)
						bidi = true; 
					else if (x.op() == CONJ)
						bidi = false;
					else
						throw new TODO(); 

					for (int i = 0; i < aa.length; i++) {
                        Event ii = aa[i];
						for (int j = bidi ? 0 : i + 1; j < aa.length; j++) {
							if (i == j) continue;
							if (!solveDTAbsolutePair(x, ii, aa[j], each))
								return false;
						}
					}
				}


			} else {
				ae.clear();

				solveOcc(b, false, new Predicate<Event>() {
                    @Override
                    public boolean test(Event bx) {
                        if ((bx instanceof Absolute) && ae.add(bx)) {
                            for (Event ax : aa) {
                                if (!solveDTAbsolutePair(x, ax, bx, each)) {
                                    return false;
                                }
                            }
                            return true;
                        }
                        return true;
                    }
                });

			}
		}
		return true;
	}

	private Event[] eventArray(UnifiedSet<Event> ae) {
        Event[] aa = ae.toArray(EMPTY_EVENT_ARRAY);
		if (aa.length > 1) ArrayUtil.shuffle(aa, random());
		return aa;
	}

	private boolean solveDTAbsolutePair(Compound x, Event a, Event b, Predicate<Event> each) {

        Op o = x.op();

		if (o == CONJ) {
			
			if (a.start() > b.start()) {
                Event z = a;
				a = b;
				b = z;
			}
		}


		long aWhen = a.start(), bWhen = b.start();
		int dt;
		if (aWhen == ETERNAL || bWhen == ETERNAL)
			dt = 0;
		else {
			assert (aWhen != TIMELESS && bWhen != TIMELESS);
			long d = o == IMPL || aWhen <= bWhen ? (bWhen - aWhen) - (long) a.id.eventRange() : (aWhen - bWhen) - (long) b.id.eventRange();

			dt = occToDT(d);
		}


        long dur = durMerge(a, b);

		
		return o == CONJ ? solveOcc(terms.conjMerge(a.id, dt, b.id), aWhen, dur, each) : solveDT(x, TIMELESS, dt, dur, null, true, each);
	}

	/**
	 * TODO make this for impl only because the ordering of terms is known implicitly from 'x' unlike CONJ
	 */
	private boolean solveDT(Compound x, long start, int dt, long dur,
							@Nullable List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, boolean dir, Predicate<Event> each) {

		return solveOcc(dt(x, dir, dt), start, dur, each);
	}

	private boolean solveOcc(Term y, long start, long dur, Predicate<Event> each) {
		if (!termsEvent(y))
			return true; 

		return start != TIMELESS ?
			each.test(event(y, start, (start != ETERNAL) ? start + dur : start, false))
			:
			solveOcc(y, true, each);
	}

	/**
	 * dt computation for term construction
	 */
	protected int occToDT(long x) {
		assert (x != TIMELESS);
		int idt = x == ETERNAL ? DTERNAL : Tense.occToDT(x);
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
	private static Term dt(Compound x, boolean dir, int dt) {

		assert (dt != XTERNAL && dt!=DTERNAL);
        Op xo = x.op();

        switch (xo) {
            case IMPL:  
                return x.dt(dt);
            case CONJ:

                Subterms xx = x.subterms();
                Term xEarly, xLate;
                if (x.dt() == XTERNAL) {


                    
                    Term x0 = xx.sub(0);
                    Term x1 = xx.sub(1);

                    if (dir) {
                        xEarly = x0;
                        xLate = x1;
                    } else {
                        xEarly = x1;
                        xLate = x0;
                        dt = -dt;
                    }


                } else {

                    int early = Conj.conjEarlyLate(x, true);
                    if (early == 1)
                        dt = -dt;

                    xEarly = xx.sub(early);
                    xLate = xx.sub(1 - early);
                }

                return terms.conjAppend(xEarly, dt, xLate);
        }

		throw new UnsupportedOperationException();
	}

	













	private boolean solution(Event y) {
		if (y instanceof Relative && solving.equals(y.id))
			return true; 

		if (validPotentialSolution(y.id)) {
			if (solutions.add(y)) {
				return target.test(y);
			}
		}
		return true;
	}

	private boolean solveExact(Term x, Predicate<Absolute> each) {
        Event f = shadow(x);




		

        Collection<Event> ee = events(f.id);
		if (ee != null) {
			for (Event e : ee) {
				if (e instanceof Absolute && !each.test((Absolute) e)) {
					return false;
				}
			}
			return true;
		}

		return true;

	}

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
		if (x instanceof Compound && ((Compound)x).subtermsDirect().hasXternal())
			return solveDTAndOccRecursive(x, each); 
		else if (x instanceof Compound && x.dt() == XTERNAL)
			return solveDtAndOccTop(x, each);  
		else
			return solveOcc(x, true, each); 
	}

	private boolean solveRootMatches(Term x, boolean root, Predicate<Event> each) {


		

		
		
		
		

		

		Term y;
		if (!root) {
			y = x;
		} else {
            Term xr = x.root();
			if (x.equals(xr))
				return true; 
			y = xr;
		}

        Collection<Event> s = events(y);
		if (s != null) {
            int ss = s.size();
			if (ss == 1) {
                Event z = s.iterator().next();
				return tryRootMatch(x, z, root, each);
			} else {

				

				FasterList<Event> toTry = null; 

				for (Event z : s) {
					if (z instanceof Absolute) {
						if (toTry == null) toTry = new FasterList(ss);
						toTry.add(z);
					}
					ss--; 
				}

				if (toTry != null) {
					if (toTry.size() > 1)
						toTry.shuffleThis(random());
					return toTry.allSatisfy(new org.eclipse.collections.api.block.predicate.Predicate<Event>() {
                        @Override
                        public boolean accept(Event z) {
                            return tryRootMatch(x, z, root, each);
                        }
                    });
				}
			}
		}
		

		


		return true;
	}

	private static boolean tryRootMatch(Term x, Event z, boolean isRoot, Predicate<Event> each) {
		return !(z instanceof Absolute) || each.test(!isRoot ?
			z 
			:
			
			event(x, z.start(), z.end()));
	}


	private boolean solveDTAndOccRecursive(Term x, Predicate<Event> each) {

		Map<Compound, Set<Term>> subSolved = new UnifiedMap();

		x.recurseTerms(Term::hasXternal, new Predicate<Term>() {
            @Override
            public boolean test(Term y) {
                if (y instanceof Compound && y.dt() == XTERNAL && !y.subterms().hasXternal()) {

                    subSolved.computeIfAbsent((Compound) y, new java.util.function.Function<Compound, java.util.Set<Term>>() {
                        @Override
                        public java.util.Set<Term> apply(Compound yy) {

                            Set<Term> s = new UnifiedSet(2);



                            TimeGraph.this.solveDT(yy, new Predicate<Event>() {
                                @Override
                                public boolean test(Event z) {



                                    
                                    Term zz = z.id;
                                    if (!termsEvent(zz))
                                        return true; 

                                    
                                    s.add(zz);
                                    return true;
                                }
                            });
                            return s.isEmpty() ? java.util.Set.of() : s;
                        }
                    });
                }
                return true;
            }
        }, null);

		subSolved.values().removeIf(java.util.Set::isEmpty);

        int solvedTerms = subSolved.size();
		switch (solvedTerms) {
			case 0:
				
				break;
			case 1:
				
                Map.Entry<Compound, java.util.Set<Term>> xy = subSolved.entrySet().iterator().next();

                java.util.Set<Term> sy = xy.getValue();
				if (!sy.isEmpty()) {
					Term xyx = xy.getKey();
                    Term[] two = sy.toArray(Op.EmptyTermArray);
					if (two.length > 1) ArrayUtil.shuffle(two, random());

					for (Term sssi : two) {
                        if (!xyx.equals(sssi) && !solveIfDifferent(x, x.replace(xyx, sssi), each))
							return false;
					}
				}
				break;
			default:
				List<Pair<Compound, Term[]>> substs = new FasterList();
				int[] permutations = {1};
				for (Map.Entry<Compound, java.util.Set<Term>> entry : subSolved.entrySet()) {
                    Compound h = entry.getKey();
                    java.util.Set<Term> w = entry.getValue();
                    Term[] ww = w.toArray(EmptyTermArray);
					assert (ww.length > 0);
					permutations[0] *= ww.length;
					substs.add(pair(h, ww));
				}
                int ns = substs.size();
				assert (ns > 0);
                Random rng = random();


				Map<Term, Term> m = new UnifiedMap(ns);
				while (permutations[0]-- > 0) {
					for (Pair<Compound, Term[]> si : substs) {
                        Term[] ssi = si.getTwo();
						m.put(si.getOne(), ssi[ssi.length > 1 ? rng.nextInt(ssi.length) : 0]);
					}
                    Term z = x.replace(m);

					if (!solveIfDifferent(x, z, each))
						return false;
					m.clear();
				}
				
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
					

					if (r instanceof Absolute) {
						
                        Event x = a;
						a = r;
						r = x;
					}
					mergeNodes(r, a);
					ee.remove(); 

					
				}
			}
		}

	}























	private boolean solveIfDifferent(Term x, Term y, Predicate<Event> each) {
		if (!termsEvent(y) || y.equals(x))
			return true;

		return solveAll(y, each);
	}

	/* solve xternal occurring at the root of a compound (without any internal xternal remaining) */
	private boolean solveDtAndOccTop(Term x, Predicate<Event> each) {
		return ((x instanceof Compound && x.dt() == XTERNAL) ?
			solveDT((Compound) x, new Predicate<Event>() {
                @Override
                public boolean test(Event y) {
                    return !TimeGraph.this.validPotentialSolution(y.id) ||
                            (y instanceof Absolute ?
                                    each.test(y)
                                    :
                                    solveOcc(y.id, true, each));
                }
            })
			:
			
			solveOcc(x, true, each)
		);
	}

	/**
	 * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
	 */
	private boolean solveOcc(Term x, boolean finish, Predicate<Event> each) {
		return solveRootMatches(x, false, each) &&
			(!x.hasAny(Op.Temporal) || solveRootMatches(x, true, each)) &&

			solveOcc(shadow(x), finish, each);
	}










	private boolean solveOcc(Event x, boolean finish, Predicate<Event> each) {
		assert (!(x instanceof Absolute));

        int solutionsBefore = solutions.size();

		return bfsAdd(x, new OccSolver(true, true, false, each))
			&&
			solveSelfLoop(x, each)
			&&
			(!autoneg ||
				
				solutions.size() > solutionsBefore ||
				bfs(x, new OccSolver(true, false, true, each)))
			&&
			(!finish || solveLastResort(x, each))
			;
	}

	/**
	 * check for any self-loops and propagate forward and/or reverse
	 */
	private boolean solveSelfLoop(Event x, Predicate<Event> each) {
		if (!solutions.isEmpty()) {
            Term t = x.id;
			/** clone the list because modifying solutions while iterating will cause infinite loop */
			List<Event> list = new ArrayList<>();
			for (Event event : solutions.list) {
				if ((event instanceof Absolute && (event.start() != ETERNAL) && event.id.equals(t) && !event.equals(x))) {
					list.add(event);
				}
			}
			return new FasterList<>(list
			).allSatisfy(new org.eclipse.collections.api.block.predicate.Predicate<Event>() {
                @Override
                public boolean accept(Event s) {

                    Collection<Event> eee = TimeGraph.this.events(t);
                    if (eee != null) {
                        Event[] et = eee.toArray(Event.EmptyArray);
                        for (Event e : et) {
                            
                            Node<Event, TimeSpan> ne = TimeGraph.this.node(e);
                            if (ne != null) {
                                for (Iterator<FromTo<Node<Event, TimeSpan>, TimeSpan>> iterator = ne.edgeIterator(false, true); iterator.hasNext(); ) {
                                    FromTo<Node<Event, TimeSpan>, TimeSpan> ee = iterator.next();
                                    long dt = ee.id().dt;
                                    if (dt != 0L && dt != ETERNAL && dt != TIMELESS && ee.loop()) {

                                        if (TimeGraph.this.random().nextBoolean())
                                            dt = -dt; 

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
                    return true;
                }
            });
		}
		return true;
	}

	private static boolean solveLastResort(Event x, Predicate<Event> each) {


		return !(x instanceof Relative) || each.test(x);
		
		
	}

	protected Random random() {
		return ThreadLocalRandom.current();
	}

	Collection<Event> shuffleAndSort(Collection<Event> e) {

		if (e.size() > 1) {
			FasterList<Event> ee = new FasterList(e);
			ee.shuffleThis(this::random);
			ee.sortThisByInt(new IntFunction<Event>() {
                @Override
                public int intValueOf(Event x) {
                    return x instanceof Absolute ? -1 : 0;
                }
            });
			return ee;
		} else
			return e;
	}

	@Nullable Iterable<Event> shuffleAndSort(Iterable<Event> e) {
		FasterList<Event> ee = new FasterList(e);
		switch (ee.size()) {
			case 0:
				return null;
			case 1:
				return List.of(ee.get(0));
			default:
				ee.shuffleThis(this::random);
				ee.sortThisByInt(new IntFunction<Event>() {
                    @Override
                    public int intValueOf(Event x) {
                        return x instanceof Absolute ? -1 : 0;
                    }
                });
				return ee;
		}
	}

	public Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> sortEdges(Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> e) {
		if ((e instanceof Collection && ((Collection) e).size() <= 1))
			return e;

        FasterList ee = new FasterList(e);
		if (ee.size() <= 1)
			return e;

		ee.shuffleThis(this::random);
		return ee;
	}





































	/**
	 * absolutely specified event
	 */

	public static class Absolute extends Event {
		static final long SAFETY_PAD = (long) (32 * 1024);
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
			return 0L;
		}








		final boolean containedIn(long cs, long ce) {
			return (cs <= start && ce >= end());
		}

		final boolean containsOrEquals(long cs, long ce) {
			return (cs >= start && ce <= end());
		}













		private @Nullable long[] unionIfIntersects(long start, long end) {
            long thisStart = this.start;
            long thisEnd = end();
			return LongInterval.intersectsSafe(start, end, thisStart, thisEnd) ?
				Longerval.unionArray(start, end, thisStart, thisEnd) :
				null;
		}

		Event shift(long dt) {
			assert (dt != 0L && dt != ETERNAL && dt != TIMELESS);
			return this instanceof AbsoluteRange ? new AbsoluteRange(id, start + dt, end() + dt) : new Absolute(id, start + dt);
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
			if (end <= start || start == ETERNAL || end == TIMELESS)
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

	public abstract static class Event implements LongObjectPair<Term> {

		public static final Event[] EmptyArray = new Event[0];
		private static final Comparator<Event> cmp = Comparator
			.comparing(new java.util.function.Function<Event, Term>() {
                @Override
                public Term apply(Event e) {
                    return e.id;
                }
            })
			.thenComparingLong(Event::start)
			.thenComparingLong(Event::end);
		public final Term id;
		private final int hash;

		Event(Term id, int hash) {
			this.id = id;
			this.hash = hash;
		}

		public abstract Event neg();

		public abstract long start();

		public abstract long end();

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
				return e == s ? id + "@" + s : id + "@" + s + ".." + e;
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
		public int compareTo(LongObjectPair<Term> e) {
			return this == e ? 0 : cmp.compare(this, (Event) e);
		}

		public abstract long dur(); 

		public long mid() {
			return (start() + end()) / 2L;
		}
	}

	private abstract class CrossTimeSolver extends Search<Event, TimeSpan> {

		/**
		 * enabled layers
		 */
		final boolean existing;
        final boolean tangent;
        final boolean tangentNeg;

		protected CrossTimeSolver(boolean existing, boolean tangent, boolean tangentNeg) {

			assert (existing || tangent || tangentNeg);

			this.existing = existing;
			this.tangent = tangent;
			this.tangentNeg = tangentNeg;
		}

		@Override
		protected Iterable<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> find(Node<Event, TimeSpan> n, List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path) {


			Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> existing = this.existing ? existing(n) : Collections.EMPTY_LIST;


			if (!this.tangent && !tangentNeg)
				return existing;
			else {
				@Deprecated Term nid = n.id().id;
				Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> tangent =
					this.tangent ? tangent(n, nid) : Collections.EMPTY_LIST;


				
				Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> tangentNeg =
					this.tangentNeg ? tangent(n, nid.neg()) : Collections.EMPTY_LIST;

				if (tangent == Collections.EMPTY_LIST && tangentNeg == Collections.EMPTY_LIST)
					return existing;
				else if (tangent == Collections.EMPTY_LIST)
					return Iterables.concat(existing, tangentNeg);
				else if (tangentNeg == Collections.EMPTY_LIST)
					return Iterables.concat(existing, tangent);
				else
					return Iterables.concat(existing, tangent, tangentNeg);
			}
		}

		private Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>> existing(Node<Event, TimeSpan> n) {

			

			return sortEdges(n.edges(true, true, new Predicate<FromTo<Node<Event, TimeSpan>, TimeSpan>>() {
                @Override
                public boolean test(FromTo<Node<Event, TimeSpan>, TimeSpan> x) {
                    return !CrossTimeSolver.this.visited(x.other(n));
                }
            }));
		}

		Iterable<FromTo<Node<Event, nars.time.TimeSpan>, TimeSpan>> tangent(Node<Event, TimeSpan> root, Term t) {

			return new Iterable<FromTo<Node<Event, TimeSpan>, TimeSpan>>() {
                @NotNull
                @Override
                public Iterator<FromTo<Node<Event, TimeSpan>, TimeSpan>> iterator() {
					Iterable<Event> events =  events(t);
					Iterable<Event> ee = events;
                    if (ee == null) {
                        return Util.emptyIterator;
                    } else {






                        Iterable<Event> eee =  shuffleAndSort(ee);
                        if (eee == null)
                            return Util.emptyIterator;


                        return Iterators.transform(
                                Iterators.filter(
                                        Iterators.transform(eee.iterator(), TimeGraph.this::node),
                                        new com.google.common.base.Predicate<Node<Event, TimeSpan>>() {
                                            @Override
                                            public boolean apply(@org.checkerframework.checker.nullness.qual.Nullable Node<Event, TimeSpan> n) {
                                                return n != null && n != root && !CrossTimeSolver.this.visited(n);
                                            }
                                        }
                                ),
                                new Function<Node<Event, TimeSpan>, FromTo<Node<Event, TimeSpan>, TimeSpan>>() {
                                    @org.checkerframework.checker.nullness.qual.Nullable
                                    @Override
                                    public FromTo<Node<Event, TimeSpan>, TimeSpan> apply(@org.checkerframework.checker.nullness.qual.Nullable Node<Event, TimeSpan> n) {
                                        
                                        

                                        return new LazyMutableDirectedEdge<>(root, TS_ZERO, n);
                                    }
                                }
                        );
                    }
                }
            };

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
		protected boolean go(List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, Node<Event, TimeSpan> next) {

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
				return true; 
			}

            @Nullable long[] pt = pathTime(path);
			if (pt == null)
				return true;

            @Nullable long dt = pt[0];

			if (dt == ETERNAL)
				dt = 0L; 

			long start;
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
						if (SS == ETERNAL) {
							SS = EE; 
						} else if (EE == ETERNAL) {
							EE = SS; 
						}

						if (dir) {
							start = ss instanceof Absolute ? SS : EE - dt - (long) a.eventRange();
						} else {
							
							start = ee instanceof Absolute ? EE : SS - dt - (long) b.eventRange();
						}
					}
				}
			}

			if (!dir)
				dt = -dt;

            @Nullable long dur = pt[1];
			if (x.op() == IMPL) {
				start = TIMELESS; 
				dt = dt - (long) a.eventRange();
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
		protected boolean go(List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, Node<Event, TimeSpan> n) {

            Event end = n.id();
			if (!(end instanceof Absolute))
				return true;

            Event start = pathStart(path).id();


            long pathEndTime = end.start();


			long startTime, endTime;
			if (pathEndTime == ETERNAL) {

				if (start instanceof Absolute) {
					startTime = start.start();
					endTime = start.end();
				} else {
					startTime = endTime = ETERNAL;
				}

			} else {

                @Nullable long[] pt = pathTime(path);
				if (pt == null || pt[0] == ETERNAL)
					return true;

				startTime = pathEndTime - (pt[0] /* pathDelta*/);
				assert (startTime != TIMELESS);
				endTime = startTime + pt[1]; /* pathRange */

			}

			return each.test(event(start.id, startTime, endTime));
		}

	}
}
