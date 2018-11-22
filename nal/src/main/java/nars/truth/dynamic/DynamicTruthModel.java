package nars.truth.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.task.util.Answer;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Conj;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {

    abstract public Truth apply(DynTruth var1, NAR nar);

    public final DynStampTruth eval(final Term superterm, boolean beliefOrGoal, long start, long end, Predicate<Task> superFilter, boolean forceProjection, NAR n) {

        assert (superterm.op() != NEG);

        DynStampTruth d = new DynStampTruth(0);


        Predicate<Task> filter = Answer.filter(superFilter, d::doesntOverlap);

        //TODO expand the callback interface allowing models more specific control over matching/answering/sampling subtasks
        
        return components(superterm, start, end, (Term subTerm, long subStart, long subEnd) -> {

            if (subTerm instanceof Bool)
                return false;

            boolean negated = subTerm.op() == Op.NEG;
            if (negated)
                subTerm = subTerm.unneg();

            Concept subConcept = n.conceptualizeDynamic(subTerm);
            if (!(subConcept instanceof TaskConcept))
                return false;

            BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);
            Task bt = forceProjection ? table.answer(subStart, subEnd, subTerm, filter, n) :
                                table.match(subStart, subEnd, subTerm, filter, n);
            if (bt == null)
                return false;

            /* project to a specific time, and apply negation if necessary */
            bt = Task.project(forceProjection, bt, subStart, subEnd, n, negated);
            if (bt == null)
                return false;

            return d.add(bt);

        }) ? d : null;
    }

    protected abstract boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic term from some or all components
     */
    abstract public Term reconstruct(Term superterm, List<Task> c, NAR nar);

    @FunctionalInterface
    interface ObjectLongLongPredicate<T> {
        boolean accept(T object, long start, long end);
    }


    public static class DynamicSectTruth {

        static class SectIntersection extends Intersection  {

            final boolean subjOrPred;

            private SectIntersection(boolean union, boolean subjOrPred) {
                super(union);
                this.subjOrPred = subjOrPred;
            }


            @Override
            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
                if (!superterm.hasAny(Op.Temporal))
                    return superterm; //shortcut

                return stmtReconstruct(superterm, components, subjOrPred, union);
            }

            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                Term common = stmtCommon(subjOrPred, superterm);
                Term decomposed = stmtCommon(!subjOrPred, superterm);
                Op op = superterm.op();

                if (decomposed.op().isAny(Op.Sect)) {
                    return decomposed.subterms().AND(
                            y -> each.accept(stmtDecomposeStructural(op, subjOrPred, y, common), start, end)
                    );
                }

                if (union) {
                    if (decomposed.op() == NEG) {
                        if (superterm.op() == IMPL) {
                            decomposed = decomposed.unneg();
                        } else {
                            //leave as-is
                            // assert (decomposed.op() == CONJ /* and not Sect/Union */) : "unneg'd decomposed " + decomposed + " in superterm " + superterm;
                        }
                    }
                }


                if (op == IMPL && decomposed.op() == CONJ /* NEG , union ?  */) {
                    return decomposeImplConj(superterm, start, end, each, common, decomposed, op);

                }

                if (Param.DEBUG_EXTRA)
                    throw new TODO();

                return false;
            }

            private boolean decomposeImplConj(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term common, Term decomposed, Op op) {
                int outerDT = superterm.dt();
                int innerDT = decomposed.dt();
                int decRange;
                switch (outerDT) {
                    case DTERNAL:
                        decRange = 0;
                        break;
                    case XTERNAL:
                        decRange = XTERNAL;
                        break;
                    default:
                        decRange = decomposed.eventRange();
                        break;
                }
                boolean startSpecial = (start==ETERNAL || start == XTERNAL);
                return decomposed.eventsWhile((offset, y) -> {
                            boolean ixTernal = startSpecial || offset == ETERNAL || offset == XTERNAL;

                            long subStart = ixTernal ? start : start + offset;
                            long subEnd = end;
                            if (subEnd < subStart) {
                                //swap
                                long x = subStart;
                                subStart = subEnd;
                                subEnd = x;
                            }

                            int occ = (outerDT != DTERNAL && decRange != XTERNAL) ? occToDT(decRange - offset + outerDT) : XTERNAL;
                            Term x = stmtDecompose(op, subjOrPred, y, common,
                                    ixTernal ? DTERNAL : occ, union, false);

                            if (x == Bool.Null)
                                return false;

                            return each.accept(x, subStart, subEnd);
                        }
                        , outerDT == DTERNAL ? ETERNAL : 0, innerDT == 0,
                        innerDT == DTERNAL,
                        innerDT == XTERNAL, 0);
            }


        }

        /** polarity of calculated truth is chosen dynamically majority of component polarities */
        final static class SectIntersectionBipolar extends SectIntersection  {

            private SectIntersectionBipolar(boolean union, boolean subjOrPred) {
                super(union, subjOrPred);
            }

            @Override
            public Truth apply(DynTruth l, NAR nar) {
                boolean negComponents = !decidePolarity(l, nar);
                Truth r = super.apply(l, nar, union ? !negComponents : negComponents, union);
                if (negComponents&&r!=null)
                    r = r.neg();
                return r;
            }

            private boolean decidePolarity(DynTruth l, NAR nar) {
                int posCount = l.count(Truthed::isPositive);

                if (posCount == 0) return false;

                int s = l.size();
                if (posCount == s) return true;


                return nar.random().nextFloat() <= ((float)posCount)/ s;
            }
        }

        public static final DynamicTruthModel UnionSubj = new SectIntersection(true, true);
        public static final DynamicTruthModel SectSubj = new SectIntersection(false, true);

        public static final DynamicTruthModel UnionSubjBipolar = new SectIntersectionBipolar(true, true);
        public static final DynamicTruthModel SectSubjBipolar = new SectIntersectionBipolar(false, true);

        public static final DynamicTruthModel UnionPred = new SectIntersection(true, false);
        public static final DynamicTruthModel SectPred = new SectIntersection(false, false);

        public static final DynamicTruthModel SectRoot = new Intersection(false) {

            @Override
            protected boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                assert(superterm.op()==SECTe);
                return superterm.subterms().AND(s ->
                   each.accept(s, start, end)
                );
            }

            @Override
            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {

                //TODO test if the superterm will be equivalent to the component terms before reconstructing
                Term[] t = new Term[components.size()];
                for (int i = 0, componentsSize = components.size(); i < componentsSize; i++) {
                    t[i] = components.get(i).term();
                }


                return Op.SECTi.the(t);
            }

        };
    }

//    public static class DynamicDiffTruth {
//        abstract static class Difference extends DynamicTruthModel {
//
//
//            @Override
//            public Truth apply(DynTruth d, NAR n) {
//                assert (d.size() == 2);
//                Truth a = d.get(0).truth();
//                if (a == null)
//                    return null;
//                Truth b = d.get(1).truth();
//                if (b == null)
//                    return null;
//
//                return NALTruth.Difference.apply(a, b, n);
//            }
//
//
//            @Override
//            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//                return each.accept(superterm.sub(0), start, end) && each.accept(superterm.sub(1), start, end);
//            }
//        }
//
//
//        static class DiffInh extends Difference {
//            final boolean subjOrPred;
//
//            DiffInh(boolean subjOrPred) {
//                this.subjOrPred = subjOrPred;
//            }
//
//            @Override
//            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//                Term common = stmtCommon(subjOrPred, superterm);
//                Term decomposed = stmtCommon(!subjOrPred, superterm);
//                Op supOp = superterm.op();
//                return each.accept(stmtDecompose(supOp, subjOrPred, decomposed.sub(0), common), start, end) &&
//                        each.accept(stmtDecompose(supOp, subjOrPred, decomposed.sub(1), common), start, end);
//            }
//
//            @Override
//            public Term reconstruct(Term superterm, List<Task> c, NAR nar) {
//                return stmtReconstruct(superterm, c, subjOrPred, false);
//            }
//        }
//
//        public static final Difference DiffRoot = new Difference() {
//
//            @Override
//            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
//                return Op.DIFFe.the(
//                        components.get(0).task().term(),
//                        components.get(1).task().term());
//            }
//
//        };
//        public static final Difference DiffSubj = new DiffInh(true);
//        public static final Difference DiffPred = new DiffInh(false);
//    }

//    public static final DynamicTruthModel ImageIdentity = new DynamicTruthModel() {
//
//        @Override
//        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//            return each.accept(Image.imageNormalize(superterm), start, end);
//        }
//
//        @Override
//        public Term reconstruct(Term superterm, List<Task> c, NAR nar) {
//            return superterm; //exact
//        }
//
//        @Override
//        public Truth apply(DynTruth taskRegions, NAR nar) {
//            return taskRegions.get(0).truth();
//        }
//    };

    public static class DynamicConjTruth {

        public static final DynamicTruthModel ConjIntersection = new Intersection(false) {

            @Override
            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {

                Conj c = new Conj(components.size());

                for (TaskRegion t : components)
                    if (!c.addDithered(((Task) t).term(), t.start(), t.end(), 1, 1, nar))
                        break;

                return c.term();
            }

            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                int superDT = superterm.dt();


                boolean xternal = superDT == XTERNAL;
                boolean dternal = superDT == DTERNAL;
                boolean parallel = superDT == 0;
                if (dternal || xternal || parallel) {

                    Subterms subterms = superterm.subterms();
                    if (subterms.subs() == 2) {

                        Term a = subterms.sub(0), b = subterms.sub(1);
                        if (a.equals(b)) {
                            if (end == start)
                                //return false; //repeat term sampled at same point, give up
                                return each.accept(a, start, start); //just one component should work

                            else {
                                if (start == ETERNAL) //watch out for end==XTERNAL
                                    return each.accept(a, ETERNAL, ETERNAL) && each.accept(b, ETERNAL, ETERNAL);
                                else
                                    return each.accept(a, start, start) && each.accept(b, end, end); //use the difference in time to create two distinct point samples
                            }
                        }

                        if (a.equalsNeg(b))
                            return false; //inversion would collapse. how to decide which subterm sampled where.  ThreadLocalRandom etc
                    }
//                        if (end - start > 0) {
//                            randomly choose?
//                        }
//                        //a repeat or inverting pair of terms.
//                        // ensure that each component is sampled from different time otherwise collapse occurrs
//                        return each.accept(subterms.sub(0), start0, end0) &&
//                                each.accept(subterms.sub(1), start1, end1);
//                    } else {
//                        /* simple case: */
//                        return subterms.AND(event ->
//                                each.accept(event, start, end)
//                        );
//                    }

                    /* simple case granting freedom when to resolve the components */
                    return subterms.AND(event -> each.accept(event, start, end));
                }

                LongObjectPredicate<Term> sub;
                if (xternal || dternal) {
                    //propagate start,end to each subterm.  allowing them to match freely inside
                    sub = (whenIgnored, event) -> each.accept(event, start, end);
                } else {
                    //??subterm refrences a specific point as a result of event time within the term. so start/end range gets collapsed at this point
                    long range = (end - start);
                    sub = (when, event) -> each.accept(event, when, when + range);
                }

                //if (event!=superterm)
                //else
                //return false;
                return superterm.eventsWhile(sub, start,
                        parallel,
                        dternal,
                        xternal, 0);

            }
        };
    }

    /**
     * statement component
     */
    private static Term stmtDecomposeStructural(Op superOp, boolean subjOrPred, Term subterm, Term common) {
        return stmtDecompose(superOp, subjOrPred, subterm, common, DTERNAL, false, true);
    }


    /**
     * statement component (temporal)
     */
    private static Term stmtDecompose(Op superOp, boolean subjOrPred, Term subterm, Term common, int dt, boolean negate, boolean structural) {
        Term s, p;

        boolean outerNegate = false;
        if (structural && subterm.op()==NEG) {
            outerNegate = true;
            subterm = subterm.unneg();
        }

        if (subjOrPred) {
            s = subterm.negIf(negate);
            p = common;
        } else {
            s = common;
            p = subterm.negIf(negate);
        }
        assert (!(s == null || p == null));

        Term y;
        if (dt == DTERNAL) {
            y = superOp.the(s, p);
        } else {
            assert (superOp == IMPL);
            y = superOp.the(s, dt, p);
        }

        if (!y.op().conceptualizable)
            return Bool.Null; //throw new WTF();

        return y.negIf(outerNegate);

    }

    /**
     * statement common component
     */
    private static Term stmtCommon(boolean subjOrPred, Term superterm) {
        return subjOrPred ? superterm.sub(1) : superterm.sub(0);
    }

    @Nullable
    private static Term[] stmtReconstruct(boolean subjOrPred, List<Task> components) {

        //extract passive term and verify they all match (could differ temporally, for example)
        Term[] common = new Term[1];
        if (!((FasterList<Task>) components).allSatisfy(tr -> {
            Term tt = subSubjPredWithNegRewrap(subjOrPred, tr);
            Term p = common[0];
            if (p == null) {
                common[0] = tt;
                return true;
            } else {
                return p.equals(tt);
            }
        }) || (common[0] == null))
            return null; //differing passive component; TODO this can be detected earlier, before truth evaluation starts

        return Util.map(0, components.size(), tr ->
                        //components.get(tr).task().term().sub(subjOrPred ? 0 : 1)
                        subSubjPredWithNegRewrap(!subjOrPred, components.get(tr))
                , Term[]::new);
    }

    private static Term subSubjPredWithNegRewrap(boolean subjOrPred, TaskRegion tr) {
        Term t = tr.task().term();
        boolean neg = t.op() == NEG;
        if (neg) {
            t = t.unneg();
        }

        Term tt = t.sub(subjOrPred ? 1 : 0 /* reverse */);
        if (neg) {

            //assert(!subjOrPred && t.op()==IMPL); //impl predicate

            tt = tt.neg();
        }
        return tt;
    }

    @Nullable
    private static Term stmtReconstruct(Term superterm, List<Task> components, boolean subjOrPred, boolean union) {
        Term superSect = superterm.sub(subjOrPred ? 0 : 1);
        Op op = superterm.op();
        if (union) {
            if (superSect.op() == NEG) {
                if (op == CONJ || op == IMPL /* will be subj only, pred is auto unnegated */)
                    superSect = superSect.unneg();
                else {
                    throw new WTF();
                }

            }
        }

        //may not be correct TODO
//        if (!Param.DEBUG) {
//            //elide reconstruction when superterm will not differ by temporal terms
//            //TODO improve
//            if (superSect.subs() == components.size() && ((FasterList<Task>) components).allSatisfy(t -> t != null && !((Task) t).term().sub(subjOrPred ? 0 : 1).isTemporal())) {
//                if (!superSect.isTemporal())
//                    return superterm;
//            }
//        }


        Term sect;
        int outerDT;
        if (op == IMPL) {
            //IMPL: compute innerDT for the conjunction
            Conj c = new Conj();
            for (TaskRegion x : components) {
                Term xx = ((Task) x).term();

                boolean forceNegate = false;
                if (xx.op() == NEG) {

//                    if (op == IMPL) {
                    if (xx.unneg().op() == IMPL) {
                        xx = xx.unneg();
                        forceNegate = true;
                    } else {
                        if (!subjOrPred) {
                            //assume this is the reduced (true ==> --x)
                            c.add(ETERNAL, xx.neg());
                            continue;
                        } else {
                            throw new WTF();
                        }
                    }
                } else if (xx.op() != IMPL) {
                    if (!subjOrPred) {
                        //assume this is the reduced (true ==> x)
                        c.add(ETERNAL, xx);
                        continue;
                    } else {
                        //throw new WTF();
                        return null;
                    }
                }

                int tdt = xx.dt();
                if (!c.add(tdt == DTERNAL ? ETERNAL : -tdt, xx.sub(subjOrPred ? 0 : 1).negIf(union ^ forceNegate)))
                    break;
            }

            sect = c.term();
            if (sect == Bool.Null)
                return null; //but allow other Bool's

            long cs = c.shift();
            if (cs == DTERNAL || cs == ETERNAL) {
                outerDT = DTERNAL; //some temporal information destroyed
            } else {
                long shift = -cs - sect.eventRange();
                outerDT = Tense.occToDT(shift);
            }

        } else {

            Term[] subs = stmtReconstruct(subjOrPred, components);
            if (subs == null)
                return null;

            if (union && superSect.op() == CONJ) {
                for (int i = 0, subsLength = subs.length; i < subsLength; i++) {
                    subs[i] = subs[i].neg();
                }
            }

            sect = superSect.op().the(subs);
            outerDT = DTERNAL;
        }

        Term common = superterm.sub(subjOrPred ? 1 : 0);

        if (union) {
            if (op == CONJ || op == IMPL /* but not Sect's */)
                sect = sect.neg();
        }

        return subjOrPred ? op.the(sect, outerDT, common) : op.the(common, outerDT, sect);
    }


    abstract static class Intersection extends DynamicTruthModel {

        /** true = union, false = intersection */
        final boolean union;

        Intersection(boolean union) {
            this.union = union;
        }

        protected final boolean negateFreq() {
            return union;
        }

        @Override
        public Truth apply(DynTruth l, NAR nar) {
            boolean n = negateFreq();
            return apply(l, nar, n, n);
        }

        @Nullable
        protected Truth apply(DynTruth l, NAR nar, boolean negComponents, boolean negResult) {
            Truth y = null;
            for (TaskRegion li : l) {
                Truth x = (((Task) li)).truth();
                if (x == null)
                    return null;

                if (negComponents)
                    x = x.neg();

                if (y == null) {
                    y = x;
                } else {
                    y = NALTruth.Intersection.apply(y, x, nar);
                    if (y == null)
                        return null;
                }
            }


            return y.negIf(negResult);
        }

    }


}