package nars.truth.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.task.util.Answer;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {

    abstract public Truth apply(DynTruth var1, NAR nar);

    public final DynStampTruth eval(final Term superTerm, boolean beliefOrGoal, long start, long end, Predicate<Task> superFilter, boolean forceProjection, NAR n) {

        assert (superTerm.op() != NEG);

        DynStampTruth d = new DynStampTruth(0); //TODO pool?


        Predicate<Task> filter =
                Param.DYNAMIC_TRUTH_STAMP_OVERLAP_FILTER ?
                        Answer.filter(superFilter, d::doesntOverlap) :
                        superFilter;


        //TODO expand the callback interface allowing models more specific control over matching/answering/sampling subtasks

        return components(superTerm, start, end, (Term subTerm, long subStart, long subEnd) -> {

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

            if (!acceptComponent(superTerm, subTerm, bt))
                return false;

            /* project to a specific time, and apply negation if necessary */
            bt = Task.project(forceProjection, true, bt, subStart, subEnd, n, negated);
            if (bt == null)
                return false;

            return d.add(bt);

        }) ? d : null;
    }

    public abstract boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    /**
     * used to reconstruct a dynamic term from some or all components
     */
    abstract public Term reconstruct(Term superterm, List<Task> c, NAR nar);

    /** allow filtering of resolved Tasks */
    public boolean acceptComponent(Term superTerm, Term componentTerm, Task componentTask) {
        return true;
    }

    public BeliefTable newTable(Term t, boolean beliefOrGoal, ConceptBuilder cb) {
        return new BeliefTables(
                new DynamicTruthTable(t, this, beliefOrGoal),
                cb.newTemporalTable(t, beliefOrGoal),
                cb.newEternalTable(t)
        );
    }

    public Bag newTaskLinkBag(Term t, ConceptBuilder b) {
        return b.newLinkBag(t);
    }

    @FunctionalInterface
    public interface ObjectLongLongPredicate<T> {
        boolean accept(T object, long start, long end);
    }


    public static class DynamicSectTruth {

        static class SectIntersection extends Intersection {

            final boolean subjOrPred;

            private SectIntersection(boolean union, boolean subjOrPred) {
                super(union);
                this.subjOrPred = subjOrPred;
            }

            @Override
            public boolean acceptComponent(Term superTerm, Term componentTerm, Task componentTask) {
                return componentTask.op()==superTerm.op();
            }

            @Override
            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
                return reconstruct(superterm, components, subjOrPred, union);
            }

            protected static Term reconstruct(Term superterm, List<Task> components, boolean subjOrPred, boolean union) {
                return stmtReconstruct(superterm, components, subjOrPred, union);
            }

            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {

                superterm = Image.imageNormalize(superterm);

                Term common = stmtCommon(subjOrPred, superterm);
                Term decomposed = stmtCommon(!subjOrPred, superterm);
                Op op = superterm.op();

                if (decomposed.op().isAny(Op.Sect)) {
                    return decomposed.subterms().AND(
                            y -> each.accept(stmtDecomposeStructural(op, subjOrPred, y, common), start, end)
                    );
                }

//                if (union) {
//                    if (decomposed.op() == NEG) {
//                        if (superterm.op() == IMPL) {
//                            decomposed = decomposed.unneg();
//                        } else {
//                            //leave as-is
//                            // assert (decomposed.op() == CONJ /* and not Sect/Union */) : "unneg'd decomposed " + decomposed + " in superterm " + superterm;
//                        }
//                    }
//                }


                return false;
            }


        }


        public static final DynamicTruthModel UnionSubj = new SectIntersection(true, true);
        public static final DynamicTruthModel SectSubj = new SectIntersection(false, true);


        public static final DynamicTruthModel UnionPred = new SectIntersection(true, false);
        public static final DynamicTruthModel SectPred = new SectIntersection(false, false);

        /**
         * according to composition rules, the intersection term is computed with union truth, and vice versa
         */
        public static final DynamicTruthModel SectImplSubj = new SectImplSubj();
        public static final DynamicTruthModel UnionImplSubj = new UnionImplSubj();

        public static final DynamicTruthModel SectImplPred = new SectIntersection(false, false) {
            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                Term common = stmtCommon(subjOrPred, superterm);
                Term decomposed = stmtCommon(!subjOrPred, superterm);
                return decomposeImplConj(superterm, start, end, each, common, decomposed, false, false);
            }
        };


//        public static final DynamicTruthModel SectRoot = new Intersection(false) {
//
//            @Override
//            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//                assert (superterm.op() == SECTe);
//                return superterm.subterms().AND(s ->
//                        each.accept(s, start, end)
//                );
//            }
//
//            @Override
//            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
//
//                //TODO test if the superterm will be equivalent to the component terms before reconstructing
//                Term[] t = new Term[components.size()];
//                for (int i = 0, componentsSize = components.size(); i < componentsSize; i++) {
//                    t[i] = components.get(i).term();
//                }
//
//
//                return Op.SECTi.the(t);
//            }
//
//        };

        private static class SectImplSubj extends SectIntersection {
            public SectImplSubj() {
                super(true, true);
            }

            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                return components(superterm, start, end, each, superterm.sub(0));
            }

            private static boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term subj) {
                return decomposeImplConj(superterm, start, end, each, superterm.sub(1), subj, true, false);
            }

            @Override
            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
                return reconstruct(superterm, components, true, false);
            }
        }
        private static class UnionImplSubj extends SectIntersection {
            public UnionImplSubj() {
                super(false, true);
            }

            @Override
            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
                return components(superterm, start, end, each, superterm.sub(0).unneg());
            }

            protected boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term subj) {
                return decomposeImplConj(superterm, start, end, each, superterm.sub(1), subj, true, true);
            }

            @Override
            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
                return reconstruct(superterm, components, true, true);
            }
        }
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

                Subterms subterms = superterm.subterms();

                boolean dternal = superDT == DTERNAL;
                boolean xternal = superDT == XTERNAL;
                if ((dternal || xternal) && subterms.subs() == 2 && subterms.subs(x->x.eventRange() > 0)==1) {
                    //distribute factored conjunction
                    Term factor = subterms.subFirst(x->x.eventRange()==0);
                    Term sequence = subterms.subFirst(x->x.eventRange()>0);
                    return components(sequence, start, end, (event, whenStart, whenEnd)->{
                        Term eventDistributed = CONJ.the(superDT, event, factor);
                        if (eventDistributed == False || eventDistributed == Null)
                            return false;
                        if (eventDistributed == True)
                            return true;
                        return each.accept(eventDistributed, whenStart, whenEnd);
                    });
                }

                boolean parallel = superDT == 0;
                if (dternal || xternal || parallel) {


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

                if (superterm.hasAny(Op.VAR_DEP)) {
                    //decompose by the term itself, not individual events which will fail when resolving a VAR_DEP sub-event
                    Subterms ss = superterm.subterms();
                    if (ss.subs() == 2) {
                        Term a = ss.sub(0);
                        Term b = ss.sub(1);
                        if (superDT > 0) {
                            return sub.accept(0, a) && sub.accept(superDT, b);
                        } else {
                            return sub.accept(0, b) && sub.accept(-superDT, a);
                        }
                    } else {
                        throw new TODO(); //can this happen?
                    }
                } else {
                    //if (event!=superterm)
                    //else
                    //return false;
                    return superterm.eventsWhile(sub, start,
                            parallel,
                            dternal,
                            xternal, 0);
                }

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
        if (structural && subterm.op() == NEG) {
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
            return Null; //throw new WTF();

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
            Term uu = tr.term().unneg();
            Term tt = subjOrPred ? uu.sub(1) : uu.sub(0);
            Term p = common[0];
            if (p == null) {
                common[0] = tt;
                return true;
            } else {
                return p.equals(tt);
            }
        }) || (common[0] == null))
            return null; //differing passive component; TODO this can be detected earlier, before truth evaluation starts

        return Util.map(0, components.size(), Term[]::new, tr ->
                        //components.get(tr).task().term().sub(subjOrPred ? 0 : 1)
                        subSubjPredWithNegRewrap(!subjOrPred, components.get(tr))
        );
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
            if (sect == Null)
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


    abstract public static class Intersection extends DynamicTruthModel {

        /**
         * true = union, false = intersection
         */
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


    static public boolean decomposeImplConj(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term common, Term decomposed, boolean subjOrPred, boolean negateComponents) {

//        int outerDT = superterm.dt();
        long is, ie;
        if (start == ETERNAL) {
            is = ie = ETERNAL;
        } else {
            is = start;
            ie = end + decomposed.eventRange();// + outerDT;
        }

        return DynamicConjTruth.ConjIntersection.components(decomposed, is, ie, (what,s,e)->{
            //TODO fix
            int innerDT = (s == ETERNAL) ?  DTERNAL : Tense.occToDT(
                    //(e-s)-outerDT
                    e-s
            );
            Term i;
            if (subjOrPred) {
                if (negateComponents)
                    what = what.neg();
                i = IMPL.the(what, innerDT, common);
            } else {
                i = IMPL.the(common, innerDT, what);
                if (negateComponents)
                    i = i.neg();
            }
            return each.accept(i, s, e);
        });
//        int innerDT = decomposed.dt();
//        int decRange;
//        switch (outerDT) {
//            case DTERNAL:
//                decRange = 0;
//                break;
//            case XTERNAL:
//                decRange = XTERNAL;
//                break;
//            default:
//                decRange = decomposed.eventRange();
//                break;
//        }
//        boolean startSpecial = (start == ETERNAL || start == XTERNAL);
//        //TODO use dynamic conjunction decompose which provides factoring
//        Op superOp = superterm.op();
//        return decomposed.eventsWhile((offset, y) -> {
//                    boolean ixTernal = startSpecial || offset == ETERNAL || offset == XTERNAL;
//
//                    long subStart = ixTernal ? start : start + offset;
//                    long subEnd = end;
//                    if (subEnd < subStart) {
//                        //swap
//                        long x = subStart;
//                        subStart = subEnd;
//                        subEnd = x;
//                    }
//
//                    int occ = (outerDT != DTERNAL && decRange != XTERNAL) ? occToDT(decRange - offset + outerDT) : XTERNAL;
//                    Term x = stmtDecompose(op, subjOrPred, y, common,
//                            ixTernal ? DTERNAL : occ, negateComponents, false);
//
//                    if (x == Null || x.unneg().op()!=superOp)
//                        return false;
//
//                    return each.accept(x, subStart, subEnd);
//                }
//                , outerDT == DTERNAL ? ETERNAL : 0, innerDT == 0,
//                innerDT == DTERNAL,
//                innerDT == XTERNAL, 0);
    }

    public static final DynamicTruthModel ImageDynamicTruthModel = new DynamicTruthModel() {

        @Override
        public BeliefTable newTable(Term t, boolean beliefOrGoal, ConceptBuilder cb) {
            return new Image.ImageBeliefTable(t, Image.imageNormalize(t), beliefOrGoal);
        }

        @Override
        public Truth apply(DynTruth var1, NAR nar) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> c, NAR nar) {
            throw new UnsupportedOperationException();
        }

//        @Override
//        public Bag newTaskLinkBag(Term t, ConceptBuilder b) {
//
//            return new ProxyBag(b);
//        }
    };
}