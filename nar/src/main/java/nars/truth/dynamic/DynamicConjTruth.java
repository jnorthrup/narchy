package nars.truth.dynamic;

import jcog.WTF;
import jcog.util.ObjectLongLongPredicate;
import nars.NAL;
import nars.Task;
import nars.task.util.Revision;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.IdempotentBool;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjList;
import nars.term.util.conj.ConjSpans;
import nars.truth.proj.TruthProjection;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.*;

public enum DynamicConjTruth {
    ;

    public static final AbstractDynamicTruth ConjIntersection = new AbstractSectTruth() {

        @Override
        public boolean temporal() {
            return true;
        }

        @Override
        protected boolean truthNegComponents() {
            return false;
        }

        @Override
        protected boolean negResult() { return false; }

        /**
            has special support for collapsing the potential sequence to a revision if intersect,
            especially if the separation is less than NAR's dt dithering which will produce invalid dynamic result
		 * @return
		 */
        @Override public @Nullable Task task(final Compound template, final long earliest, final long s, final long e, final DynTaskify d) {

            //TODO generalize beyond n=2
            if (d.size() == 2 && d.get(0).term().equals(d.get(1).term())) {

                final long sep = d.get(0).minTimeTo(d.get(1));
                final int dither = sep == 0L ? 0 : d.nar.dtDither();
                if (sep <= (long) dither) {
                    //collapse to a point smaller than dither time:  same starting time and all terms are the same.
                    //try revision
                    final Pair<Task, TruthProjection> ab = Revision._merge(d.nar, d.ditherTruth, 2, new Task[]{d.get(0), d.get(1)});
                    if (ab != null)
                        return Revision.afterMerge(ab);
                }
                if (sep < (long) dither) {
                    return null; //will yield an invalid result TODO verify if this is always true
                }

            }


            return super.task(template, earliest, s, e, d);
        }

        @Override
        public Term reconstruct(final Compound superterm, final long seqStart, final long seqEnd, final DynTaskify d) {


            final long end;
            boolean aligned = true;
            if (seqStart==ETERNAL) {
                end = ETERNAL;
            } else {
                final long sequenceLatestStart = d.latestStart(); assert(sequenceLatestStart!=ETERNAL);

                //sequenceStart = sequenceStart; //dither now for comparisons in loop

                final long range = seqEnd - seqStart;
                end = sequenceLatestStart + range; //the actual total end of the sequence

                if (d.size() <= 1) {
                    aligned = true;
                } else {
                    //determine what method to use.  if all the non-eternal tasks have similar spans, then reconstructSequence otherwise reconstructInterval
                    long s = Long.MAX_VALUE, e = Long.MAX_VALUE;
                    final int dither = d.nar.dtDither();
                    for (final Task t : d) {
                        final long ts = t.start();
                        if (ts == ETERNAL) continue;
                        if (s == Long.MAX_VALUE) {
                            s = ts;
                            e = t.end();
                        } else {
                            if (Math.abs(s - ts) >= (long) dither || Math.abs(e - t.end()) >= (long) dither) {
                                aligned = false;
                                break;
                            }
                        }
                    }
                }
            }

            //new ConjTree();
            final ConjBuilder c = new ConjList();
            final boolean result = aligned ?
                reconstructSequence(seqStart, end, d, c) :
                reconstructInterval(d, c);

            return result ? c.term() : null;
        }

        @Override
        public boolean evalComponents(final Compound conj, final long start, final long end, final ObjectLongLongPredicate<Term> each) {

            final ConjList c = conj.dt()!=XTERNAL ? ConjList.events(conj, start) : ConjList.eventsXternal(conj, start);

            //special cases:
            //  1. parallel / xternal with co-negated events that need separated in time
            if (start!=ETERNAL && dtSpecial(conj.dt()) && conj.hasAny(NEG)) spreadCoNegations(conj, start, end, c);

            //  2. depvar events that need pre-grouped with other events before evaluating
            if (conj.hasAny(VAR_DEP) && !pairVars(c)) return false; //give up

            //TODO sort the testing order of sub-events to fail fastest


            final long range = end-start;
            return c.AND(new LongObjectPredicate<Term>() {
                @Override
                public boolean accept(long when, Term what) {
                    return each.accept(what, when, when + range);
                }
            });
        }

//        @Override
//        public boolean evalComponents(Compound conj, long start, long end, ObjectLongLongPredicate<Term> each) {
//
//            //try to evaluate the eternal component of factored sequence independently
//            //but this can dilute its truth too much if the sequence is sparse. better to evaluate it
//            //piecewise
//            int superDT = conj.dt();
//            boolean seqFactored = superDT == DTERNAL && ConjSeq.isFactoredSeq(conj);
//            if(seqFactored) {
//                //evaluate the eternal components first.
//                // this is more efficient than sequence distribution,
//                // and avoids the evidence overlap problem
//                Term eternal = ConjSeq.seqEternal(conj);
//                Compound temporal = ConjSeq.seqTemporal(conj);
//                if (each.accept(eternal, start, end + temporal.eventRange())) {
//                    //now concentrate on the temporal component:
//                    conj = temporal;
//                } else {
//                    //attempt distributed sequential evaluation
//                }
//            }
//
//            boolean dternal = !seqFactored && superDT == DTERNAL && !Conj.isSeq(conj);
//            boolean xternal = superDT == XTERNAL;
//
//            if ((xternal || dternal)) {
//                Subterms ss = conj.subterms();
//
//                if (xternal && start!=ETERNAL) {
//                    boolean conegOrEquiv = false;
//                    int sss = ss.subs();
//                    if (sss == 2) {
//                        Term a = ss.sub(0), b = ss.sub(1);
//                        //repeat, must be time separated
//                        if (a.equals(b)) {
//                            conegOrEquiv = true;
//                        }
//                    }
//                    if (!conegOrEquiv && ss.hasAny(NEG)) {
//                        //quick test
//                        if (sss ==2) {
//                            Term a = ss.sub(0), b = ss.sub(1);
//                            conegOrEquiv = a.equalsNeg(b);
//                        } else {
//                            conegOrEquiv = ss.OR(x -> x instanceof Neg && ss.contains(x.unneg())); //conj.dt(DTERNAL).volume() < conj.volume(); //collapses will result in reduced volume
//                        }
//                    }
//                    if (conegOrEquiv) {
//                        //HACK randomly assign each component to a partitioned sub-ranges
//
//                        //TODO refine
//                        int subRange = Tense.occToDT( (long)Math.ceil((end - start) / ((double)sss)) );
//
//                        ThreadLocalRandom rng = ThreadLocalRandom.current();
//                        int[] order = new int[sss];
//                        if (sss == 2)
//                            order = rng.nextBoolean() ? new int[] { 0, 1 } : new int[] { 1, 0 };
//                        else {
//                            for (int i = 0; i < sss; i++) order[i] = i;
//                            ArrayUtil.shuffle(order, rng);
//                        }
//
//
//                        long s = start;
//                        for (int i = 0; i < sss; i++) {
//                            Term x = ss.sub(order[i]);
//                            long e = s + subRange;
//                            if (!each.accept(x, s, e))
//                                return false;
//                            s = e+1;
//                        }
//                        return true;
//                    }
//                }
//
//                //propagate start,end to each subterm.  allowing them to match freely inside
//                for (Term x : ss)
//                    if (!each.accept(x, start, end))
//                        return false;
//
//                return true;
//            } else {
//
//                //??subterm refrences a specific point as a result of event time within the target. so start/end range gets collapsed at this point
//                long range = (end - start);
//
//                //TODO group any depvar events with the nearest event
//                return conj.eventsAND(range != 0 ?
//                        //point-like
//                        (when, event) -> each.accept(event, when, when + range) :
//                        //within range
//                        (when, event) -> each.accept(event, when, when),
//                    start,
//                    false, false);
//            }
//
//
//
//        }


    };

    public static void spreadCoNegations(final Compound conj, final long start, final long end, final ConjList c) {
        final int cc = c.size();
        final long start2 = end!=start ?
                end :
                start+ (long) (Math.max(conj.eventRange(), 1)); //HACK TODO use a dur

        for (int i = 0; i < cc; i++) {
            final Term ci = c.get(i);
            if (ci instanceof Neg) {
                final Term ciu = ci.unneg();
                for (int j = 0; j < cc; j++) {
                    if (j == i) continue;
                    final Term cj = c.get(j);
                    if (!(cj instanceof Neg) && cj.equals(ciu)) {
                        //conegation between i and j
                        //push one randomly to different dt
                        c.when[ ThreadLocalRandom.current().nextBoolean() ? i : j ] = start2;
                    }
                }
            }
        }
    }

    static boolean pairVars(final ConjList c) {
        int cc = c.size();
        final int ccs = cc;
        boolean removed = false;
        nextEvent: for (int i = 0; i < ccs; i++) {
            final Term v = c.get(i);
            if (v == null) continue;
            final Term vu = v.unneg();
            if (vu instanceof Variable && vu.op()==VAR_DEP) {
                c.setFast(i, null);
                removed = true;
                cc--;
                //choose random other event to pair with

                //TODO prefer smaller non-conj, non-disj event

                if (cc == 0) {
                    if (NAL.DEBUG)
                        throw new WTF(); //nothing remains
                    return false;
                }

                final long vWhen = c.when(i);
                final Random rng = ThreadLocalRandom.current();
                for (int r = 0; r < ccs; r++) { //max tries
                    final int pair = rng.nextInt(ccs);
                    if (pair == i) continue;
                    final Term p = c.get(pair);
                    if (p !=null) {
                        final int dt = (int) (vWhen - c.when(pair));
                        final Term paired = CONJ.the(dt, p, v);
                        if (!(paired instanceof IdempotentBool)) {
                            c.setFast(pair, paired);
                            continue nextEvent;
                        }
                    }
                }
                return false;
            }
        }
        if (removed)
            c.removeNulls();
        return true;
    }

    static boolean reconstructSequence(final long sequenceStart, final long end, final DynTaskify d, final ConjBuilder b) {
        final int n = d.size();

        for (int i = 0; i < n; i++) {
            final Task t = d.get(i);
            final long s = t.start();

            //spans entire event
            //Tense.dither(s, dtDither);
            final long when = s == ETERNAL || (sequenceStart != ETERNAL && s <= sequenceStart && t.end() >= end) ? ETERNAL : s;

            final Term _x = t.term();
            final boolean cp = d.componentPolarity.get(i);

            final Term x = _x.negIf(!cp); //HACK

            if (!b.add(when, x))
                return false;
        }
        return true;
    }

    static boolean reconstructInterval(final DynTaskify d, final ConjBuilder b) {
        return ConjSpans.add(d, d.nar.dtDither(), d.componentPolarity, b) != null;
    }

	public static boolean decomposeableConj(final Term conj) {
        return !conj.hasVars() || conj.subterms().count(new Predicate<Term>() {
            @Override
            public boolean test(Term x) {
                return !(x instanceof nars.term.Variable);
            }
        }) > 1;
	}

    //			if (conjSubterms.hasAny(VAR_DEP)) {
//
//				Map<Term, Term> varLocations = new UnifiedMap(conjSubterms.subs());
//
//				return conj.eventsAND((when, event) ->
//								!event.hasAny(VAR_DEP) ||
//										event.recurseTerms(x -> x.hasAny(VAR_DEP),
//												(possiblyVar, parent) ->
//														(possiblyVar.op() != VAR_DEP) ||
//																varLocations.putIfAbsent(possiblyVar, event) == null
//												, null)
//
//						, 0, true, true);
//			}
//			return true;


}
//                if (dternal || xternal || parallel) {
//
//
//                    if (subterms.subs() == 2) {
//
//                        Term a = subterms.sub(0), b = subterms.sub(1);
//                        if (a.equals(b)) {
//                            if (end == start)
//                                //return false; //repeat target sampled at same point, give up
//                                return each.accept(a, start, start); //just one component should work
//
//                            else {
//                                if (start == ETERNAL) //watch out for end==XTERNAL
//                                    return each.accept(a, ETERNAL, ETERNAL) && each.accept(b, ETERNAL, ETERNAL);
//                                else
//                                    return each.accept(a, start, start) && each.accept(b, end, end); //use the difference in time to create two distinct point samples
//                            }
//                        }
//
//                        if (a.equalsNeg(b))
//                            return false; //inversion would collapse. how to decide which subterm sampled where.  ThreadLocalRandom etc
//                    }
////                        if (end - start > 0) {
////                            randomly choose?
////                        }
////                        //a repeat or inverting pair of terms.
////                        // ensure that each component is sampled from different time otherwise collapse occurrs
////                        return each.accept(subterms.sub(0), start0, end0) &&
////                                each.accept(subterms.sub(1), start1, end1);
////                    } else {
////                        /* simple case: */
////                        return subterms.AND(event ->
////                                each.accept(event, start, end)
////                        );
////                    }
//
//                    /* simple case granting freedom when to resolve the components */
//                    return subterms.AND(event -> each.accept(event, start, end));
//                }
//                if (superterm.hasAny(Op.VAR_DEP)) {
//                    //decompose by the target itself, not individual events which will fail when resolving a VAR_DEP sub-event
//                    Subterms ss = superterm.subterms();
//                    if (ss.subs() == 2) {
//                        Term a = ss.sub(0);
//                        Term b = ss.sub(1);
//                        if (superDT > 0) {
//                            return sub.accept(0, a) && sub.accept(superDT, b);
//                        } else {
//                            return sub.accept(0, b) && sub.accept(-superDT, a);
//                        }
//                    } else {
//                        throw new TODO(); //can this happen?
//                    }
//                } else {
//if (event!=superterm)
//else
//return false;
