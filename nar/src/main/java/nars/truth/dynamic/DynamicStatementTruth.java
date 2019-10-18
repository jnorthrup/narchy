package nars.truth.dynamic;

import jcog.Util;
import jcog.util.ObjectLongLongPredicate;
import nars.Op;
import nars.subterm.Subterms;
import nars.task.util.TaskRegion;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.util.TermException;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjList;
import nars.time.Tense;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;
import static nars.time.Tense.*;

public enum DynamicStatementTruth { ;

    public static final AbstractDynamicTruth Impl = new DynamicImplTruth();

    public static final AbstractDynamicTruth SubjUnion = new AbstractInhImplSectTruth(true, true);
    public static final AbstractDynamicTruth SubjInter = new AbstractInhImplSectTruth(true, false);
    public static final AbstractDynamicTruth PredUnion = new AbstractInhImplSectTruth(false, true);
    public static final AbstractDynamicTruth PredInter = new AbstractInhImplSectTruth(false, false);


    /**
     * statement common component
     */
    static Term stmtCommon(boolean subjOrPred, Subterms superterm) {
        return superterm.sub(subjOrPred ? 1 : 0);
    }

    private static Term subSubjPredWithNegRewrap(boolean subjOrPred, TaskRegion tr, boolean polarity) {
        return tr.task().term().sub(subjOrPred ? 1 : 0 /* reverse */).negIf(!polarity);
    }


    /**
     * functionality shared between INH and IMPL
     */
    static class AbstractInhImplSectTruth extends AbstractSectTruth {

        final boolean subjOrPred;

        /**
         * true = union, false = intersection
         */
        final boolean unionOrIntersection;

        AbstractInhImplSectTruth(boolean subjOrPred, boolean union) {
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
                if (superSect instanceof Neg) {
                        negOuter = true;
                        superSect = superSect.unneg();
                }


            Term sect;
            int outerDT;
            if (op == IMPL) {
                //TODO DynamicConjTruth.ConjIntersection.reconstruct()

                //IMPL: compute innerDT for the conjunction
                ConjBuilder c =
                        //new ConjTree();
                        new ConjList();

                Term constantCondition = null;
                for (int i = 0, componentsSize = d.size(); i < componentsSize; i++) {
                    Term xx = d.term(i);
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


                        constantCondition = constantCondition != null ? CONJ.the(constantCondition, xx) : xx;

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
                //temporal information not available or was destroyed
                outerDT = cs == ETERNAL ? DTERNAL : Tense.occToDT(subjOrPred ? -cs - sect.eventRange() : cs);

            } else {

                Term[] result;

                //extract passive target and verify they all match (could differ temporally, for example)
                Term common = d.get(0).term().unneg().sub(subjOrPred ? 1 : 0);
                int n = d.size();
                if (d.anySatisfy(1, n,
                    i -> !common.equals(i.term().unneg().sub(subjOrPred ? 1 : 0))
                )) {
                    //HACK
                    //this seems to happen with Images and conjunction subterms that collapse
                    //ex: $.07 wonder("-DéøËìáÁØÕ",((tetris-->left) &&+130 (--,(tetris-->left)))). 131970⋈132110 %1.0;.27%
                    //    $0.0 ((tetris-->left)-->(wonder,"-DéøËìáÁØÕ",/)). 234800⋈234830 %1.0;.01%
                    return Null;
                    //throw new TermException("can not dynamically reconstruct", d.get(0)); //return null; //differing passive component; TODO this can be detected earlier, before truth evaluation starts?
                }

                sect = superSect.op().the(Util.map(0, n, Term[]::new, i ->
                        subSubjPredWithNegRewrap(!subjOrPred, d.get(i), d.componentPolarity.get(i))
                ));
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
        public Term reconstruct(Compound superterm, long start, long end, DynTaskify components) {
            return reconstruct(superterm, components, subjOrPred, false);
        }

        @Override
        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {


            Term decomposed = stmtCommon(!subjOrPred, superterm);
            if (decomposed.unneg().op()!= CONJ) {
                //superterm = (Compound) Image.imageNormalize(superterm);
                decomposed = stmtCommon(!subjOrPred, superterm);
            }

            //if (unionOrIntersection) {
            if (decomposed instanceof Neg) {

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
            if (outerNegate = (subterm instanceof Neg)) {
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
