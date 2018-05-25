package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.TruthFunctions2;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;

import java.lang.reflect.Field;
import java.util.Map;

public enum GoalFunction implements TruthOperator {

    //@AllowOverlap
    Strong() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions2.desireNew(T, B, minConf, true);
            //return desireStrongOriginal(T, B, minConf);
        }
    },

    //@AllowOverlap
    Weak() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions2.desireNew(T, B, minConf, false);
            //return desireWeakOriginal(T, B, minConf);
        }
    },


    //@AllowOverlap
    Deduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return BeliefFunction.Deduction.apply(T, B, m, minConf);
        }
    },

//    //@AllowOverlap
//    DeductionPB() {
//        @Override
//        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
//            return BeliefFunction.DeductionPB.apply(T, B, m, minConf);
//        }
//    },

    //@AllowOverlap
    Induction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.Induction.apply(T, B, m, minConf);
        }
    },

//    /** deduction used for bipolar implication results depending on belief either positive or negative */
//    DeciInduction() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            if (B.isNegative()) {
//                Truth x = Induction.apply(T, B.neg(), m, minConf);
//                if (x!=null) return x.neg();
//                else return null;
//            } else {
//                return Induction.apply(T, B, m, minConf);
//            }
//        }
//    },

    DecomposePositiveNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, false, minConf);
        }
    },
    DecomposeNegativePositiveNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, true, false, minConf);
        }
    },
    DecomposePositivePositiveNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, true, false, minConf);
        }
    },
    DecomposePositiveNegativePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, true, minConf);
        }
    },

    DecomposeNegativeNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, false, false, minConf);
        }
    },
    DecomposePositivePositivePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, true, true, minConf);
        }
    },
    DecomposeNegativePositivePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, true, true, minConf);
        }
    },


    @AllowOverlap
    @SinglePremise
    StructuralReduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.StructuralReduction.apply(T, B, m, minConf);
        }
    },

    @AllowOverlap
    @SinglePremise
    StructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.StructuralDeduction.apply(T, B, m, minConf);
        }
    },
//    @SinglePremise @AllowOverlap StructuralDeductionWeak() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
//            return T != null ? TruthFunctions.deduction1(T, confDefault(m)*0.5f, minConf) : null;
//        }
//    },

    @AllowOverlap
    BeliefStructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.BeliefStructuralDeduction.apply(T, B, m, minConf);
        }
    },

    @AllowOverlap
    BeliefStructuralReduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.StructuralReduction.apply(B, null, m, minConf);
        }
    },

    //@AllowOverlap
    Union() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.Union.apply(T, B, m, minConf);
        }
    },

    //@AllowOverlap
    Intersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.Intersection.apply(T, B, m, minConf);
        }
    },

    //@AllowOverlap
    Difference() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.Difference.apply(T, B, m, minConf);
        }
    },

    DifferenceReverse() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.Difference.apply(B, T, m, minConf);
        }
    },

    @SinglePremise
    Identity() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

//    StructuralIntersection() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return B != null ? TruthFunctions.intersection(B, defaultTruth(m), minConf) : null;
//        }
//    },
    ;


    static final Map<Term, TruthOperator> atomToTruthModifier = $.newHashMap(GoalFunction.values().length);

    static {
        TruthOperator.permuteTruth(GoalFunction.values(), atomToTruthModifier);
    }


    public static TruthOperator get(Term a) {
        return atomToTruthModifier.get(a);
    }


    private final boolean single;
    private final boolean overlap;

    GoalFunction() {

        try {
            Field enumField = getClass().getField(name());
            this.single = enumField.isAnnotationPresent(SinglePremise.class);
            this.overlap = enumField.isAnnotationPresent(AllowOverlap.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean single() {
        return single;
    }

    @Override
    public boolean allowOverlap() {
        return overlap;
    }

    private static float confDefault(NAR m) {
        return m.confDefault(Op.GOAL);
    }
}