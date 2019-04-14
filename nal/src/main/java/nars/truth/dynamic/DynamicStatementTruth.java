package nars.truth.dynamic;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.util.ObjectLongLongPredicate;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.task.util.TaskRegion;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjLazy;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.*;

public class DynamicStatementTruth {

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
    private static Term stmtCommon(boolean subjOrPred, Compound superterm) {
        return (subjOrPred ? superterm.sub(1) : superterm.sub(0));
    }

    @Nullable
    private static Term[] stmtReconstruct(boolean subjOrPred, List<Task> components) {

        //extract passive target and verify they all match (could differ temporally, for example)
        Term common = components.get(0).term().unneg().sub(subjOrPred ?  1 : 0);
        int n = components.size();
        if (((FasterList<Task>) components).anySatisfy(1, n,
            tr -> !common.equals(tr.term().unneg().sub(subjOrPred ? 1 : 0))
        ))
            return null; //differing passive component; TODO this can be detected earlier, before truth evaluation starts

        return Util.map(0, n, Term[]::new, tr ->
                //components.get(tr).task().target().sub(subjOrPred ? 0 : 1)
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

    private static boolean decomposeImplConj(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term common, Compound decomposed, boolean subjOrPred, boolean negateComponents) {

        int superDT = superterm.dt();
        int decRange = decomposed.eventRange();
        return DynamicConjTruth.ConjIntersection.evalComponents(decomposed, start, end, (what, s, e) -> {

            int innerDT;
            if (superDT == DTERNAL || superDT == XTERNAL) {
                Term cu = common.unneg();
                if (cu.equalsPosOrNeg(what) || cu.containsPosOrNeg(what))
                    innerDT = XTERNAL; //force XTERNAL since 0 or DTERNAL will collapse
                else
                    innerDT = s != ETERNAL ? 0 : DTERNAL;
            } else {
                if (s == start && e - s >= decRange)
                    innerDT = DTERNAL; //eternal component
                else
                    innerDT = Tense.occToDT(decRange - (s-start));
            }

            Term i = subjOrPred ?
                    IMPL.the(what.negIf(negateComponents), innerDT, common)
                    :
                    IMPL.the(common, innerDT, what).negIf(negateComponents);

            return each.accept(i, start, end);
        });
    }

    /** functionality shared between INH and IMPL */
    static class AbstractInhImplSectTruth extends AbstractSectTruth {

        final boolean subjOrPred;

        private AbstractInhImplSectTruth(boolean union, boolean subjOrPred) {
            super(union);
            this.subjOrPred = subjOrPred;
        }

        @Override
        public boolean acceptComponent(Compound superTerm, Task componentTask) {
            return componentTask.op() == superTerm.op();
        }

        @Override
        public Term reconstruct(Compound superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, subjOrPred, unionOrIntersection);
        }

        protected static Term reconstruct(Compound superterm, List<Task> components, boolean subjOrPred, boolean union) {
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
//            if (superSect.subs() == components.size() && ((FasterList<Task>) components).allSatisfy(t -> t != null && !((Task) t).target().sub(subjOrPred ? 0 : 1).isTemporal())) {
//                if (!superSect.isTemporal())
//                    return superterm;
//            }
//        }


            Term sect;
            int outerDT;
            if (op == IMPL) {
                //TODO DynamicConjTruth.ConjIntersection.reconstruct()

                //IMPL: compute innerDT for the conjunction
                ConjBuilder c =
                        //new Conj();
                        new ConjLazy(components.size());

                for (TaskRegion x : components) {
                    Term xx = ((Task) x).term();

                    Term xxu = xx.unneg();
                    int tdt = xxu.dt();
                    long tWhen = (tdt == DTERNAL || tdt == XTERNAL) ? ETERNAL :
                            (subjOrPred ? (-tdt) : (+tdt));

                    boolean forceNegate = false;
                    if (xx.op() == NEG) {

                        if (xxu.op() == IMPL) {
                            xx = xxu;
                            forceNegate = true;
                        } else {
                            if (!subjOrPred) {
                                //assume this is the reduced (true ==> --x)
                                if (!c.add(tWhen, xx.neg()))
                                    break;
                                continue;
                            } else {
                                throw new WTF(); //return null;
                            }
                        }
                    } else if (xx.op() != IMPL) {
                        if (!subjOrPred) {
                            //assume this is the reduced (true ==> x)
                            if (!c.add(tWhen, xx))
                                break;
                            continue;
                        } else {
                            throw new WTF(); //return null;
                        }
                    }

                    if (!c.add(tWhen, xx.sub(subjOrPred ? 0 : 1).negIf(union ^ forceNegate)))
                        break;
                }

                sect = c.term();
                if (sect == Null)
                    return null; //allow non-Null Bool's?

                long cs = c.shift();
                if (cs == ETERNAL) {
                    outerDT = DTERNAL; //temporal information not available or was destroyed
                } else {
                    outerDT = Tense.occToDT(subjOrPred ? -cs - sect.eventRange() : cs);
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

            //TODO check
            if (union || (!subjOrPred)) {
                if (op == CONJ || op == IMPL /* but not Sect's */)
                    sect = sect.neg();
            }

            return subjOrPred ? op.the(sect, outerDT, common) : op.the(common, outerDT, sect);
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {



            Term decomposed = stmtCommon(!subjOrPred, superterm);
            if (!decomposed.op().isAny(Op.Sect)) {
                //try Image normalizing
                superterm = (Compound) Image.imageNormalize(superterm);
                decomposed = stmtCommon(!subjOrPred, superterm);
            }

            if (decomposed.op().isAny(Op.Sect)) {
                Term common = stmtCommon(subjOrPred, superterm);

                Op op = superterm.op();

                return decomposed.subterms().AND(
                        y -> each.accept(stmtDecomposeStructural(op, subjOrPred, y, common), start, end)
                );
            }
            assert (false);
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


    public static final AbstractDynamicTruth SubjUnion = new AbstractInhImplSectTruth(true, true);
    public static final AbstractDynamicTruth SubjInter = new AbstractInhImplSectTruth(false, true);

    public static final AbstractDynamicTruth PredUnion = new AbstractInhImplSectTruth(true, false);
    public static final AbstractDynamicTruth PredInter = new AbstractInhImplSectTruth(false, false);

    public static final AbstractDynamicTruth ImplSubjDisj = new ImplSubjDisj();
    public static final AbstractDynamicTruth ImplSubjConj = new ImplSubjConj();

    public static final AbstractDynamicTruth ImplPred = new AbstractInhImplSectTruth(false, false) {
        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Term common = stmtCommon(subjOrPred, superterm);
            Compound decomposed = (Compound) stmtCommon(!subjOrPred, superterm);
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
//                    t[i] = components.get(i).target();
//                }
//
//
//                return Op.SECTi.the(t);
//            }
//
//        };


    /** &&, truth = union */
    private static class ImplSubjConj extends AbstractInhImplSectTruth {
        ImplSubjConj() {
            super(true, true);
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Subterms ss = superterm.subterms();
            return decomposeImplConj(superterm, start, end, each, ss.sub(1), (Compound) ss.sub(0), true, false /* reconstruct as-is; union only applies to the truth calculation */);
        }

        @Override
        public Term reconstruct(Compound superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, false /* reconstruct as-is; union only applies to the truth calculation */);
        }
    }

    /** ||, truth = intersection */
    private static final class ImplSubjDisj extends AbstractInhImplSectTruth {
        private ImplSubjDisj() {
            super(false, true);
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Subterms ss = superterm.subterms();
            Term subj = ss.sub(0);
            assert(subj.op()==NEG);
            return decomposeImplConj(superterm, start, end, each, ss.sub(1), (Compound)(subj.unneg()), true, true);
        }

        @Override
        public Term reconstruct(Compound superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, true);
        }
    }

}
