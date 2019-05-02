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
            ConjLazy l = new ConjLazy(n);
            for (int i = 0, dSize = d.size(); i < dSize; i++) {
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

            return l.term();
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

            if (xternal || dternal) {
                Subterms ss = conj.subterms();
                if (xternal) {
                    boolean coneg = false;
                    if (ss.hasAny(Op.NEG)) {
                        //quick test
                        if (ss.subs()==2) {
                            coneg = ss.sub(0).equalsNeg(ss.sub(1));
                        } else {
                            coneg = conj.dt(DTERNAL).volume() < conj.volume(); //collapses will result in reduced volume
                        }
                    }
                    if (coneg) {
                        int n = ss.subs();
                        int subRange = Tense.occToDT((end - start) / (n));
                        if (subRange >= 1) {
                            //HACK randomly assign each component to a partitioned sub-ranges

                            int[] order = new int[n];
                            for (int i = 0; i < n; i++) order[i] = i;
                            ArrayUtils.shuffle(order, ThreadLocalRandom.current());

                            int o = 0;
                            for (Term x : ss) {
                                int offset = order[o++] * subRange;
                                long subStart = start + offset;
                                long subEnd = subStart + subRange;
                                if (!each.accept(x, subStart, subEnd))
                                    return false;
                            }
                            return true;
                        } else {
                            //TODO try to determine some interval relative to the first found task which could separate start..end to allow differentiation
                        }
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
