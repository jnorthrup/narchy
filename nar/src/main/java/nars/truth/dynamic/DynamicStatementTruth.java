package nars.truth.dynamic;

import jcog.Util;
import jcog.util.ObjectLongLongPredicate;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.task.util.TaskRegion;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;
import nars.term.util.TermException;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjLazy;
import nars.time.Tense;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;
import static nars.time.Tense.*;

public class DynamicStatementTruth {

    public static final AbstractDynamicTruth SubjUnion = new AbstractInhImplSectTruth(true, true);
    public static final AbstractDynamicTruth SubjInter = new AbstractInhImplSectTruth(true, false);
    public static final AbstractDynamicTruth PredUnion = new AbstractInhImplSectTruth(false, true);
    public static final AbstractDynamicTruth PredInter = new AbstractInhImplSectTruth(false, false);

    public static final AbstractDynamicTruth ImplSubjDisj = new AbstractInhImplSectTruth(true, false) {

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Subterms ss = superterm.subterms();
            Term subj = ss.sub(0);
            assert (subj.op() == NEG);
            return decomposeImplConj(superterm, start, end, each, ss.sub(1),
                    (Compound) (subj.unneg()), true, false);
        }

        @Override
        public Term reconstruct(Compound superterm, DynTaskify components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, false).neg();
        }
    };

    public static final AbstractDynamicTruth ImplSubjConj = new AbstractInhImplSectTruth(true, true) {
        @Override
        protected boolean truthNegComponents() {
            return true;
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Subterms ss = superterm.subterms();
            return decomposeImplConj(superterm, start, end, each, ss.sub(1), (Compound) ss.sub(0), true,
                    false /* reconstruct as-is; union only applies to the truth calculation */);
        }

        @Override
        public Term reconstruct(Compound superterm, DynTaskify components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, false
                    /* reconstruct as-is; union only applies to the truth calculation */);
        }
    };

    public static final AbstractDynamicTruth ImplPred = new AbstractInhImplSectTruth(false, false) {
        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Term common = stmtCommon(subjOrPred, superterm);
            Compound decomposed = (Compound) stmtCommon(!subjOrPred, superterm);
            return decomposeImplConj(superterm, start, end, each, common, decomposed, false, false);
        }
    };


    /**
     * statement common component
     */
    private static Term stmtCommon(boolean subjOrPred, Compound superterm) {
        return superterm.sub(subjOrPred ? 1 : 0);
    }

    private static Term subSubjPredWithNegRewrap(boolean subjOrPred, TaskRegion tr, boolean polarity) {
        return tr.task().term().sub(subjOrPred ? 1 : 0 /* reverse */).negIf(!polarity);
    }

    private static boolean decomposeImplConj(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term common, Compound decomposed, boolean subjOrPred, boolean negateComponents) {

        int superDT = superterm.dt() == DTERNAL ? 0 : superterm.dt();
        int decRange = decomposed.eventRange();
        return DynamicConjTruth.ConjIntersection.evalComponents(decomposed, start, end, (what, s, e) -> {

            int innerDT;
            if (superDT == XTERNAL) {

                innerDT = XTERNAL; //force XTERNAL since 0 or DTERNAL will collapse

            } else {
//                if (s == start && e - s >= decRange) {
//                    innerDT = 0; //eternal/immediate component
//                } else
                    innerDT = Tense.occToDT(decRange - (s - start)) + superDT;
            }

            Term i = subjOrPred ?
                    IMPL.the(what.negIf(negateComponents), innerDT, common)
                    :
                    IMPL.the(common, innerDT, what).negIf(negateComponents);

            return each.accept(i, start, end);
        });
    }


    /**
     * functionality shared between INH and IMPL
     */
    private static class AbstractInhImplSectTruth extends AbstractSectTruth {

        final boolean subjOrPred;

        /**
         * true = union, false = intersection
         */
        final boolean unionOrIntersection;

        private AbstractInhImplSectTruth(boolean subjOrPred, boolean union) {
            this.unionOrIntersection = union;
            this.subjOrPred = subjOrPred;
        }

        @Override
        protected boolean truthNegComponents() {
            return false;
        }

        @Override
        protected boolean negResult() {
            return unionOrIntersection;
        }

        static Term reconstruct(Compound superterm, DynTaskify d, boolean subjOrPred, boolean union) {
            Term superSect = superterm.sub(subjOrPred ? 0 : 1);
            Op op = superterm.op();
            boolean negOuter = false;
                if (superSect.op() == NEG) {
                        negOuter = true;
                        superSect = superSect.unneg();
                }


            Term sect;
            int outerDT;
            if (op == IMPL) {
                //TODO DynamicConjTruth.ConjIntersection.reconstruct()

                //IMPL: compute innerDT for the conjunction
                ConjBuilder c =
                        //new Conj(d.size());
                        new ConjLazy(d.size());

                Term constantCondition = null;
                for (int i = 0, componentsSize = d.size(); i < componentsSize; i++) {
                    TaskRegion x = d.get(i);
                    Term xx = ((Task) x).term();
                    if (xx.op()==IMPL) {


                        int tdt = xx.dt();

                        if (tdt == XTERNAL)
                            throw new TermException("xternal in dynamic impl reconstruction", xx);

                        if (tdt == DTERNAL)
                            tdt = 0;
                        long tWhen = (subjOrPred ? (-tdt) : (+tdt));

                        if (xx.subs() != 2)
                            return Null; //something invalid happened

                        if (!c.add(tWhen, xx.sub(subjOrPred ? 0 : 1).negIf(!d.componentPolarity.get(i) ^ union)))
                            return Null;
                    } else {
                        //conjoin any constant conditions (which may precipitate from reductions)


                        if (constantCondition != null)
                            constantCondition = CONJ.the(constantCondition, xx);
                        else
                            constantCondition = xx;

                        if (constantCondition == True)
                            constantCondition = null;

                        if (!xx.op().eventable || constantCondition == Null)
                            return Null;
                    }
                }

                sect = c.term();

                if (constantCondition!=null)
                    sect = CONJ.the(constantCondition, sect);

                if (sect == Null)
                    return Null; //allow non-Null Bool's?

                long cs = c.shift();
                if (cs == ETERNAL) {
                    outerDT = DTERNAL; //temporal information not available or was destroyed
                } else {
                    outerDT = Tense.occToDT(subjOrPred ? -cs - sect.eventRange() : cs);
                }

            } else {

                Term[] result;

                //extract passive target and verify they all match (could differ temporally, for example)
                Term common = d.get(0).term().unneg().sub(subjOrPred ? 1 : 0);
                int n = d.size();
                if (d.anySatisfy(1, n,
                    i -> !common.equals(i.term().unneg().sub(subjOrPred ? 1 : 0))
                ))
                    throw new TermException("can not dynamically reconstruct", d.get(0)); //return null; //differing passive component; TODO this can be detected earlier, before truth evaluation starts?

                Term[] subs = Util.map(0, n, Term[]::new, i ->
                    subSubjPredWithNegRewrap(!subjOrPred, d.get(i), d.componentPolarity.get(i))
                );

                sect = superSect.op().the(subs);
                outerDT = DTERNAL;
            }

            Term common = superterm.sub(subjOrPred ? 1 : 0);

            if (negOuter)
                sect = sect.neg();

            return subjOrPred ? op.the(sect, outerDT, common) : op.the(common, outerDT, sect);
        }

//        @Override
//        public boolean acceptComponent(Compound superTerm, Task componentTask) {
//            return componentTask.op() == superTerm.op();
//        }

        @Override
        public Term reconstruct(Compound superterm, DynTaskify components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, subjOrPred, false);
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {


            Term decomposed = stmtCommon(!subjOrPred, superterm);
            if (decomposed.unneg().op()!= CONJ) {
                //Image normalize
                superterm = (Compound) Image.imageNormalize(superterm);
                decomposed = stmtCommon(!subjOrPred, superterm);
            }

            //if (unionOrIntersection) {
            if (decomposed.op() == NEG) {

                decomposed = decomposed.unneg();
            }


            //assert (decomposed.op() == CONJ);

            //if (decomposed.op() == Op.CONJ) {
            Term common = stmtCommon(subjOrPred, superterm);

            Op op = superterm.op();

            return decomposed.subterms().AND(
                y -> each.accept(stmtDecomposeStructural(op, subjOrPred, y, common), start, end)
            );

        }

        /**
         * statement component
         */
        private static Term stmtDecomposeStructural(Op superOp, boolean subjOrPred, Term subterm, Term common) {
            boolean outerNegate;
            if (outerNegate = (subterm.op() == NEG)) {
                subterm = subterm.unneg();
            }

            Term s, p;
            if (subjOrPred) {
                s = subterm;
                p = common;
            } else {
                s = common;
                p = subterm;
            }

            return superOp.the(s, p).negIf(outerNegate);

        }

    }

}
