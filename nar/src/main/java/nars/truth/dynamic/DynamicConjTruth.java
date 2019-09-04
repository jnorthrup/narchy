package nars.truth.dynamic;

import jcog.util.ArrayUtil;
import jcog.util.ObjectLongLongPredicate;
import nars.Task;
import nars.subterm.Subterms;
import nars.task.util.Revision;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.util.conj.*;
import nars.time.Tense;
import nars.truth.proj.TruthProjection;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

import static nars.Op.NEG;
import static nars.time.Tense.*;

public class DynamicConjTruth {

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
         */
        @Override public Task task(Compound template, long earliest, long s, long e, DynTaskify d) {

            //TODO generalize beyond n=2
            if (d.size() == 2 && d.get(0).term().equals(d.get(1).term())) {

                long sep = d.get(0).minTimeTo(d.get(1));
                int dither = sep == 0 ? 0 : d.nar.dtDither();
                if (sep <= dither) {
                    //collapse to a point smaller than dither time:  same starting time and all terms are the same.
                    //try revision
                    Pair<Task, TruthProjection> ab = Revision._merge(d.nar, d.ditherTruth, 2, new Task[]{d.get(0), d.get(1)});
                    if (ab != null)
                        return Revision.afterMerge(ab);
                }
                if (sep < dither) {
                    return null; //will yield an invalid result TODO verify if this is always true
                }

            }


            return super.task(template, earliest, s, e, d);
        }

        @Override
        public Term reconstruct(Compound superterm, long sequenceStart, long startEnd, DynTaskify d) {

            ConjBuilder b =
                //new ConjTree();
                new ConjList();


            long end;
            if (sequenceStart==ETERNAL) {
                end = ETERNAL;
            } else {
                long sequenceLatestStart = d.latestStart(); assert(sequenceLatestStart!=ETERNAL);

                //sequenceStart = sequenceStart; //dither now for comparisons in loop

                long range = startEnd - sequenceStart;
                end = sequenceLatestStart + range; //the actual total end of the sequence
            }


            //determine what method to use.  if all the non-eternal tasks have similar spans, then reconstructSequence otherwise reconstructInterval
            boolean aligned = true;
            {
                long s = Long.MAX_VALUE, e = Long.MAX_VALUE;
                int dither = d.nar.dtDither();
                for (Task t : d) {
                    long ts = t.start();
                    if (ts == ETERNAL) continue;
                    if (s == Long.MAX_VALUE) {
                        s = ts; e = t.end();
                    } else {
                        if (Math.abs(s - ts) >= dither || Math.abs(e - t.end()) >= dither) {
                            aligned = false;
                            break;
                        }
                    }
                }
            }

            boolean result = aligned ?
                reconstructSequence(sequenceStart, end, d, b) :
                reconstructInterval(sequenceStart, end, d, b);

            return result ? b.term() : null;
        }

        @Override
        public boolean evalComponents(Compound conj, long start, long end, ObjectLongLongPredicate<Term> each) {

            //try to evaluate the eternal component of factored sequence independently
            //but this can dilute its truth too much if the sequence is sparse. better to evaluate it
            //piecewise
            int superDT = conj.dt();
            boolean seqFactored = superDT == DTERNAL && ConjSeq.isFactoredSeq(conj);
            if(seqFactored) {
                //evaluate the eternal components first.
                // this is more efficient than sequence distribution,
                // and avoids the evidence overlap problem
                Term eternal = ConjSeq.seqEternal(conj);
                Compound temporal = ConjSeq.seqTemporal(conj);
                if (each.accept(eternal, start, end + temporal.eventRange())) {
                    //now concentrate on the temporal component:
                    conj = temporal;
                } else {
                    //attempt distributed sequential evaluation
                }
            }

            boolean dternal = !seqFactored && superDT == DTERNAL && !Conj.isSeq(conj);
            boolean xternal = superDT == XTERNAL;

            if ((xternal || dternal)) {
                Subterms ss = conj.subterms();

                if (xternal && start!=ETERNAL) {
                    boolean conegOrEquiv = false;
                    int sss = ss.subs();
                    if (sss == 2) {
                        Term a = ss.sub(0), b = ss.sub(1);
                        //repeat, must be time separated
                        if (a.equals(b)) {
                            conegOrEquiv = true;
                        }
                    }
                    if (!conegOrEquiv && ss.hasAny(NEG)) {
                        //quick test
                        if (sss ==2) {
                            Term a = ss.sub(0), b = ss.sub(1);
                            conegOrEquiv = a.equalsNeg(b);
                        } else {
                            conegOrEquiv = ss.OR(x -> x instanceof Neg && ss.contains(x.unneg())); //conj.dt(DTERNAL).volume() < conj.volume(); //collapses will result in reduced volume
                        }
                    }
                    if (conegOrEquiv) {
                        //HACK randomly assign each component to a partitioned sub-ranges

                        //TODO refine
                        int subRange = Tense.occToDT( (long)Math.ceil((end - start) / ((double)sss)) );

                        ThreadLocalRandom rng = ThreadLocalRandom.current();
                        int[] order = new int[sss];
                        if (sss == 2)
                            order = rng.nextBoolean() ? new int[] { 0, 1 } : new int[] { 1, 0 };
                        else {
                            for (int i = 0; i < sss; i++) order[i] = i;
                            ArrayUtil.shuffle(order, rng);
                        }


                        long s = start;
                        for (int i = 0; i < sss; i++) {
                            Term x = ss.sub(order[i]);
                            long e = s + subRange;
                            if (!each.accept(x, s, e))
                                return false;
                            s = e+1;
                        }
                        return true;
                    }
                }

                //propagate start,end to each subterm.  allowing them to match freely inside
                for (Term x : ss)
                    if (!each.accept(x, start, end))
                        return false;

                return true;
            } else {

                //??subterm refrences a specific point as a result of event time within the target. so start/end range gets collapsed at this point
                long range = (end - start);

                return conj.eventsAND(range != 0 ?
                        //point-like
                        (when, event) -> each.accept(event, when, when + range) :
                        //within range
                        (when, event) -> each.accept(event, when, when),
                    start,
                    false, false);
            }



        }


    };

    static boolean reconstructSequence(long sequenceStart, long end, DynTaskify d, ConjBuilder b) {
        int n = d.size();


        for (int i = 0; i < n; i++) {
            Task t = d.get(i);
            long s = t.start();
            long when;

            if (s == ETERNAL || (sequenceStart!=ETERNAL && s<=sequenceStart && t.end()>=end))
                when = ETERNAL; //spans entire event
            else
                when = s; //Tense.dither(s, dtDither);

            Term x = t.term().negIf(!d.componentPolarity.get(i));

            if (!b.add(when, x))
                return false;
        }
        return true;
    }
    static boolean reconstructInterval(long sequenceStart, long end, DynTaskify d, ConjBuilder b) {
        @Nullable ConjBuilder bb = ConjSpans.add(d, true, b);
        return (bb != null);
    }

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
