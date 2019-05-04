package nars.truth.dynamic;

import jcog.util.ArrayUtils;
import jcog.util.ObjectLongLongPredicate;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.task.util.TaskRegion;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjLazy;
import nars.time.Tense;
import nars.truth.Stamp;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;

import java.util.concurrent.ThreadLocalRandom;

import static nars.time.Tense.*;

public class DynamicConjTruth {

    public static final AbstractDynamicTruth ConjIntersection = new AbstractSectTruth(false) {

        @Override
        protected boolean truthNegComponents() {
            return false;
        }

        @Override
        public Term reconstruct(Compound superterm, DynTaskify d, NAR nar, long start, long end) {

            long range;
            if (start!=ETERNAL) {
                //adjust end for the internal sequence range
                end = d.maxValue(Stamp::end);
                range = end - start;
            } else {
                range = ETERNAL;
            }

            boolean factored = Conj.isFactoredSeq(superterm);


            int n = d.size();
            ConjBuilder l =
                    new ConjLazy(n);
                    //new Conj(n);
            for (int i = 0; i < n; i++) {
                TaskRegion t = d.get(i);
                long s = t.start();
                long when;
                if ((s == ETERNAL) || (range == ETERNAL))
                    when = ETERNAL;
                else if (factored && (range > 0 && s <= start && t.end() >= end))
                    when = ETERNAL; //the component spans the entire range, so consider it an eternal factor
                else
                    when = s;


                if (!l.add(when, ((Task) t).term().negIf(!d.componentPolarity.get(i))))
                    break;
            }

            Term y = l.term();

//            //TEMPORARY
//            if (!(y instanceof Bool) && y.anon().op()!=y.op()) {
//                l.term();
//                throw new WTF("conj fault");
//            }

            return y;
        }

        @Override
        public boolean evalComponents(Compound conj, long start, long end, ObjectLongLongPredicate<Term> each) {

            //try to evaluate the eternal component of factored sequence independently
            //but this can dilute its truth too much if the sequence is sparse. better to evaluate it
            //piecewise
            boolean seqFactored = Conj.isFactoredSeq(conj);
            if(seqFactored) {
                //evaluate the eternal components first.
                // this is more efficient than sequence distribution,
                // and avoids the evidence overlap problem
                Term eternal = Conj.seqEternal(conj);
                Compound temporal = Conj.seqTemporal(conj);
                if (each.accept(eternal, start, end + temporal.eventRange())) {
                    //now concentrate on the temporal component:
                    conj = temporal;
                } else {
                    //attempt distributed sequential evaluation
                }
            }

            int superDT = conj.dt();
            boolean dternal = !Conj.isSeq(conj) && superDT == DTERNAL;
            boolean xternal = superDT == XTERNAL;
            boolean parallel = superDT == 0;

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
                    if (!conegOrEquiv && ss.hasAny(Op.NEG)) {
                        //quick test
                        if (sss ==2) {
                            Term a = ss.sub(0), b = ss.sub(1);
                            conegOrEquiv = a.equalsNeg(b);
                        } else {
                            conegOrEquiv = !conj.equals(conj.root()) && conj.dt(DTERNAL).volume() < conj.volume(); //collapses will result in reduced volume
                        }
                    }
                    if (conegOrEquiv) {
                        //HACK randomly assign each component to a partitioned sub-ranges

                        //TODO refine
                        int subRange = Tense.occToDT( (long)Math.ceil((end - start) / ((float)(sss))) );

                        ThreadLocalRandom rng = ThreadLocalRandom.current();
                        int[] order = new int[sss];
                        if (sss == 2)
                            order = rng.nextBoolean() ? new int[] { 0, 1 } : new int[] { 1, 0 };
                        else {
                            for (int i = 0; i < sss; i++) order[i] = i;
                            ArrayUtils.shuffle(order, rng);
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

                LongObjectPredicate<Term> sub;

                //??subterm refrences a specific point as a result of event time within the target. so start/end range gets collapsed at this point
                long range = (end - start);
                if (range != 0)
                    sub = (when, event) -> each.accept(event, when, when + range); //within range
                else
                    sub = (when, event) -> each.accept(event, when, when); //point-like

                return conj.eventsWhile(sub, start,
                        parallel,
                        dternal,
                        xternal);
            }



        }


    };
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