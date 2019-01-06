package nars.truth.dynamic;

import nars.NAR;
import nars.Task;
import nars.task.util.TaskRegion;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.jetbrains.annotations.Nullable;

abstract public class AbstractSectTruth extends AbstractDynamicTruth {

    /**
     * true = union, false = intersection
     */
    final boolean unionOrIntersection;

    protected AbstractSectTruth(boolean unionOrIntersection) {
        this.unionOrIntersection = unionOrIntersection;
    }

    @Override
    public final Truth apply(DynEvi l, NAR nar) {
        return apply(l, nar, unionOrIntersection, unionOrIntersection);
    }

    @Nullable
    protected final Truth apply(DynEvi l, NAR nar, boolean negComponents, boolean negResult) {
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