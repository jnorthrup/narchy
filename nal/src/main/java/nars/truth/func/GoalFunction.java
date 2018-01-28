package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;

import java.lang.reflect.Field;
import java.util.Map;

import static nars.truth.TruthFunctions.*;

public enum GoalFunction implements TruthOperator {

    //@AllowOverlap
    Strong() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return desireStrongNew(T, B, minConf);
            //return desireStrongOriginal(T, B, minConf);
            //return TruthFunctions.desire(T, B, minConf, false);
        }
    },

    //@AllowOverlap
    Weak() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return desireWeakNew(T, B, minConf);
            //return desireWeakOriginal(T, B, minConf);
            //return TruthFunctions.desire(T, B, minConf, true);
        }
    },


    Deduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return BeliefFunction.Deduction.apply(T, B, m, minConf);
        }
    },

   /** deduction used for bipolar implication results. symmetric for positive or negative T unlike ordinary deduction */
//    //@AllowOverlap
    DeciDeduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            if (!B.isNegative()) {
                return BeliefFunction.Deduction.apply(T, B, m, minConf);
            } else {
                return BeliefFunction.Deduction.apply(T.neg(), B.neg(), m, minConf);
            }
        }

    },

    Induction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.Induction.apply(T, B, m, minConf);
        }
    },

    /** deduction used for bipolar implication results */
    DeciInduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.InductionPB.apply(T, B, m, minConf);
        }
    },

    DecomposePositiveNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return decompose(T, B, true, false, false, minConf);
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
            return decompose(T, B, false, false, false, minConf);
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
            return deduction1(T, defaultConf(m), minConf);
        }
    },

    @AllowOverlap
    BeliefStructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return deduction1(B, defaultConf(m), minConf);
        }
    },


    //@AllowOverlap
    Union() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return union(T, B, minConf);
        }
    },

    //@AllowOverlap
    Intersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return intersection(T, B, minConf);
        }
    },

    //@AllowOverlap
    Difference() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return difference(T, B, minConf);
        }
    },
    DifferenceReverse() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return difference(B, T, minConf);
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

    private static float defaultConf(NAR m) {
        return m.confDefault(Op.GOAL);
    }
}