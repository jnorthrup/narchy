package nars.truth.dynamic;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.util.TaskRegion;
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
    protected static Term stmtDecomposeStructural(Op superOp, boolean subjOrPred, Term subterm, Term common) {
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
    protected static Term stmtCommon(boolean subjOrPred, Term superterm) {
        return subjOrPred ? superterm.sub(1) : superterm.sub(0);
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

    @Nullable
    protected static Term stmtReconstruct(Term superterm, List<Task> components, boolean subjOrPred, boolean union, boolean subjNeg) {
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
                    //new Conj(); //TODO LazyConj
                    new ConjLazy(components.size());

            for (TaskRegion x : components) {
                Term xx = ((Task) x).term();

                Term xxu = xx.unneg();
                int tdt = xxu.dt();
                long tWhen = tdt == DTERNAL ? ETERNAL :
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

            int cs = c.shiftOrDTERNAL();
            if (cs == DTERNAL || cs == ETERNAL) {
                outerDT = DTERNAL; //some temporal information destroyed
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

        if (union || (subjOrPred && subjNeg)) {
            if (op == CONJ || op == IMPL /* but not Sect's */)
                sect = sect.neg();
        }

        return subjOrPred ? op.the(sect, outerDT, common) : op.the(common, outerDT, sect);
    }

    static public boolean decomposeImplConj(Term superterm, long start, long end, AbstractDynamicTruth.ObjectLongLongPredicate<Term> each, Term common, Term decomposed, boolean subjOrPred, boolean negateComponents) {

        int superDT = superterm.dt();
        int decRange = decomposed.eventRange();
        return DynamicConjTruth.ConjIntersection.evalComponents(decomposed, start, end, (what, s, e) -> {
            //TODO fix
//            int innerDT = (s == ETERNAL) ? XTERNAL : Tense.occToDT(
//                    //(e-s)-outerDT
//                    e - s
//            );
            int innerDT;
            if (superDT == DTERNAL || superDT == XTERNAL) {
                if (s!=ETERNAL)
                    innerDT = 0;
                else
                    innerDT = DTERNAL;
            } else {
                if (s == start && e - s>=decRange)
                    innerDT = DTERNAL; //eternal component
                else
                    innerDT = Tense.occToDT(decRange - (s-start));
            }

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

            return each.accept(i, start, end);
        });
    }

    static class DynamicInhSectTruth extends AbstractSectTruth {

        final boolean subjOrPred;

        private DynamicInhSectTruth(boolean union, boolean subjOrPred) {
            super(union);
            this.subjOrPred = subjOrPred;
        }

        @Override
        public boolean acceptComponent(Term superTerm, Term componentTerm, Task componentTask) {
            return componentTask.op() == superTerm.op();
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, subjOrPred, unionOrIntersection);
        }

        protected static Term reconstruct(Term superterm, List<Task> components, boolean subjOrPred, boolean union) {
            return stmtReconstruct(superterm, components, subjOrPred, union, false);
        }

        @Override
        public boolean evalComponents(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {



            Term decomposed = stmtCommon(!subjOrPred, superterm);
            if (!decomposed.op().isAny(Op.Sect)) {
                //try Image normalizing
                superterm = Image.imageNormalize(superterm);
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


    public static final AbstractDynamicTruth UnionSubj = new DynamicInhSectTruth(true, true);
    public static final AbstractDynamicTruth SectSubj = new DynamicInhSectTruth(false, true);


    public static final AbstractDynamicTruth UnionPred = new DynamicInhSectTruth(true, false);
    public static final AbstractDynamicTruth SectPred = new DynamicInhSectTruth(false, false);

    /**
     * according to composition rules, the intersection target is computed with union truth, and vice versa
     */
    public static final AbstractDynamicTruth SectImplSubj = new SectImplSubj();
//        public static final DynamicTruthModel SectImplSubjNeg = new SectImplSubj() {
//            @Override
//            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//                return components(superterm, start, end, each, superterm.sub(0).unneg());
//            }
//
//            @Override
//            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
//                return stmtReconstruct(superterm, components, subjOrPred, union, true);
//
//            }
//        };

    public static final AbstractDynamicTruth UnionImplSubj = new UnionImplSubj();

    public static final AbstractDynamicTruth SectImplPred = new DynamicInhSectTruth(false, false) {
        @Override
        public boolean evalComponents(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
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
//                    t[i] = components.get(i).target();
//                }
//
//
//                return Op.SECTi.the(t);
//            }
//
//        };

    private static class SectImplSubj extends DynamicInhSectTruth {
        public SectImplSubj() {
            super(false, true);
        }

        @Override
        public boolean evalComponents(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            return decomposeImplConj(superterm, start, end, each, superterm.sub(1), superterm.sub(0), true, false);
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, false);
        }
    }

    private static class UnionImplSubj extends DynamicInhSectTruth {
        public UnionImplSubj() {
            super(true, true);
        }

        @Override
        public boolean evalComponents(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            return decomposeImplConj(superterm, start, end, each, superterm.sub(1), superterm.sub(0).unneg(), true, true);
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, true);
        }
    }
}
