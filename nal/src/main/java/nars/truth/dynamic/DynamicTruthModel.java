package nars.truth.dynamic;

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
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel implements BiFunction<DynTruth, NAR, Truth> {
    @Nullable
    public final DynTruth eval(final Term superterm, boolean beliefOrGoal, long start, long end, boolean timeFlexible, NAR n) {

        assert (superterm.op() != NEG);

        DynTruth d = new DynTruth(4);

        return components(superterm, start, end, (Term concept, long subStart, long subEnd) -> {
            boolean negated = concept.op() == Op.NEG;
            if (negated)
                concept = concept.unneg();

            Concept subConcept =
                    n.conceptualizeDynamic(concept);


            if (!(subConcept instanceof TaskConcept)) {
                return false;
            }


            BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);


            /* x.intersects(subStart, subEnd) && */
            Task bt = table.match(subStart, subEnd, concept, d.evi != null ? d::doesntOverlap : null, n);
            if (bt == null)
                return false;

            /** project to a specific time, and apply negation if necessary */
            bt = Task.project(timeFlexible, bt, subStart, subEnd, n, negated);
            if (bt == null)
                return false;

            return d.add(bt);

        }) ? d : null;
    }

    protected abstract boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic term from some or all components
     */
    abstract public Term reconstruct(Term superterm, List<TaskRegion> c);

    @FunctionalInterface
    interface ObjectLongLongPredicate<T> {
        boolean accept(T object, long start, long end);
    }


    public static class DynamicSectTruth {

        final static class SectIntersection extends Intersection {

            final boolean union, subjOrPred;

            private SectIntersection(boolean union, boolean subjOrPred) {
                this.union = union;
                this.subjOrPred = subjOrPred;
            }

            @Override
            protected boolean negateFreq() {
                return union;
            }

            @Override
            public Term reconstruct(Term superterm, List<TaskRegion> components) {
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
                            y -> each.accept(stmtDecompose(op, subjOrPred, y, common), start, end)
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

                if (Param.DEBUG)
                    throw new UnsupportedOperationException();

                return false;
            }

            public boolean decomposeImplConj(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term common, Term decomposed, Op op) {
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
                        decRange = decomposed.dtRange();
                        break;
                }
                return decomposed.eventsWhile((offset, y) -> {
                            boolean ixTernal = offset == ETERNAL || offset == XTERNAL;

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
                                    ixTernal ? DTERNAL : occ, union);

                            if (x == Null)
                                return false;

                            return each.accept(x, subStart, subEnd);
                        }
                        , outerDT == DTERNAL ? ETERNAL : 0, innerDT == 0,
                        innerDT == DTERNAL,
                        innerDT == XTERNAL, 0);
            }


        }

        public static final DynamicTruthModel UnionSubj = new SectIntersection(true, true);
        public static final DynamicTruthModel IsectSubj = new SectIntersection(false, true);
        public static final DynamicTruthModel UnionPred = new SectIntersection(true, false);
        public static final DynamicTruthModel IsectPred = new SectIntersection(false, false);
    }

    public static class DynamicDiffTruth {
        abstract static class Difference extends DynamicTruthModel {


            @Override
            public Truth apply(DynTruth d, NAR n) {
                assert (d.size() == 2);
                Truth a = ((Task) d.get(0)).truth();
                if (a == null)
                    return null;
                Truth b = ((Task) d.get(1)).truth();
                if (b == null)
                    return null;

                return NALTruth.Difference.apply(a, b, n, Float.MIN_NORMAL);
            }


            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                return each.accept(superterm.sub(0), start, end) && each.accept(superterm.sub(1), start, end);
            }
        }


        static class DiffInh extends Difference {
            final boolean subjOrPred;

            DiffInh(boolean subjOrPred) {
                this.subjOrPred = subjOrPred;
            }

            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                Term common = stmtCommon(subjOrPred, superterm);
                Term decomposed = stmtCommon(!subjOrPred, superterm);
                Op supOp = superterm.op();
                return each.accept(stmtDecompose(supOp, subjOrPred, decomposed.sub(0), common), start, end) &&
                        each.accept(stmtDecompose(supOp, subjOrPred, decomposed.sub(1), common), start, end);
            }

            @Override
            public Term reconstruct(Term superterm, List<TaskRegion> c) {
                return stmtReconstruct(superterm, c, subjOrPred, false);
            }
        }

        public static final Difference DiffRoot = new Difference() {

            @Override
            public Term reconstruct(Term superterm, List<TaskRegion> components) {
                return Op.DIFFe.the(
                        components.get(0).task().term(),
                        components.get(1).task().term());
            }

        };
        public static final Difference DiffSubj = new DiffInh(true);
        public static final Difference DiffPred = new DiffInh(false);
    }

    public static final DynamicTruthModel ImageIdentity = new DynamicTruthModel() {

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            return each.accept(Image.imageNormalize(superterm), start, end);
        }

        @Override
        public Term reconstruct(Term superterm, List<TaskRegion> c) {
            return superterm; //exact
        }

        @Override
        public Truth apply(DynTruth taskRegions, NAR nar) {
            return ((Task) taskRegions.get(0)).truth();
        }
    };

    public static class DynamicConjTruth {

        public static final DynamicTruthModel ConjIntersection = new Intersection() {

            @Override
            public Term reconstruct(Term superterm, List<TaskRegion> components) {

                Conj c = new Conj(components.size());

                for (TaskRegion t : components)
                    if (!c.add(((Task) t).term(), t.start(), t.end(), 1, 1))
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

                        Term a = subterms.sub(0);
                        Term b = subterms.sub(1);
                        if (a.equals(b)) {
                            if (end == start)
                                //return false; //repeat term sampled at same point, give up
                                return each.accept(a, start, start); //just one component should work
                            else
                                return each.accept(a, start, start) && each.accept(b, end, end); //use the difference in time to create two distinct point samples
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
    private static Term stmtDecompose(Op superOp, boolean subjOrPred, Term subterm, Term common) {
        return stmtDecompose(superOp, subjOrPred, subterm, common, DTERNAL);
    }


    private static Term stmtDecompose(Op superOp, boolean subjOrPred, Term subterm, Term common, int dt) {
        return stmtDecompose(superOp, subjOrPred, subterm, common, dt, false);
    }

    /**
     * statement component (temporal)
     */
    private static Term stmtDecompose(Op superOp, boolean subjOrPred, Term subterm, Term common, int dt, boolean negate) {
        Term s, p;
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
            return Null; //throw new WTF();

        return y;

    }

    /**
     * statement common component
     */
    private static Term stmtCommon(boolean subjOrPred, Term superterm) {
        return subjOrPred ? superterm.sub(1) : superterm.sub(0);
    }

    @Nullable
    private static Term[] stmtReconstruct(boolean subjOrPred, List<TaskRegion> components) {

        //extract passive term and verify they all match (could differ temporally, for example)
        Term[] common = new Term[1];
        if (!((FasterList<TaskRegion>) components).allSatisfy(tr -> {
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
    private static Term stmtReconstruct(Term superterm, List<TaskRegion> components, boolean subjOrPred, boolean union) {
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
//            if (superSect.subs() == components.size() && ((FasterList<TaskRegion>) components).allSatisfy(t -> t != null && !((Task) t).term().sub(subjOrPred ? 0 : 1).isTemporal())) {
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
            if (sect == Null)
                return null; //but allow other Bool's

            long cs = c.shift();
            if (cs == DTERNAL || cs == ETERNAL) {
                outerDT = DTERNAL; //some temporal information destroyed
            } else {
                long shift = -cs - sect.dtRange();
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


        @Override
        public Truth apply(DynTruth l, NAR nar) {

            Truth y = null;
            for (TaskRegion li : l) {
                Truth x = (((Task) li)).truth();
                if (x == null)
                    return null;

                if (negateFreq())
                    x = x.neg();

                if (y == null) {
                    y = x;
                } else {
                    y = NALTruth.Intersection.apply(y, x, nar, Float.MIN_NORMAL);
                    if (y == null)
                        return null;
                }
            }


            return y.negIf(negateFreq());
        }

        boolean negateFreq() {
            return false;
        }

    }


}