package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

import static nars.Op.BELIEF;

/**
 * http://aleph.sagemath.org/?q=qwssnn
 * <patham9> only strong rules are allowing overlap
 * <patham9> except union and revision
 * <patham9> if you look at the graph you see why
 * <patham9> its both rules which allow the conclusion to be stronger than the premises
 */
public enum BeliefFunction implements TruthOperator {


//    @AllowOverlap StructuralAbduction() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
//            return abduction($.t(1, m.confDefault(BELIEF)), B, minConf);
//        }
//    },

    Deduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return TruthFunctions.deduction(T, B.freq(), B.conf(), minConf);
            //return TruthFunctions2.deduction(T, B.freq(), B.conf(), minConf);
        }
    },

    @SinglePremise @AllowOverlap StructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            return T != null ? Deduction.apply(T, $.t(1f, confDefault(m)), m, minConf) : null;
        }
    },
//    @SinglePremise @AllowOverlap StructuralDeductionWeak() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
//            return T != null ? TruthFunctions.deduction1(T, confDefault(m)*0.5f, minConf) : null;
//        }
//    },

    /**
     * keeps the same input frequency but reduces confidence
     */
    @AllowOverlap @SinglePremise StructuralReduction() {
        @Override
        public Truth apply(final Truth T, final Truth Bignored, /*@NotNull*/ NAR m, float minConf) {
            float c = T.conf() * BeliefFunction.confDefault(m);
            return c >= minConf ? $.t(T.freq(), c) : null;
        }
    },
//    /** similar to structural deduction but preserves the frequency of the task */
//   @SinglePremise @AllowOverlap StructuralDecompose() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
//            float conf = T.conf() * defaultConfidence(m);
//            if (conf >= minConf)
//                return $.t(T.freq(), conf);
//            else
//                return null;
//        }
//    },

    /**
     * polarizes according to an implication belief and its effective negation reduction
     * TODO rename 'PB' to 'Sym'
     */
    DeductionPB() {
        @Override
        public Truth apply(Truth T, Truth B, NAR n, float minConf) {
            if (B.isNegative()) {
                Truth d = Deduction.apply(T.neg(), B.neg(), n, minConf);
                return d!=null ? d.neg() : null;
            } else {
                return Deduction.apply(T, B, n, minConf);
            }
        }
    },


//    //@AllowOverlap
//    DeductionRecursive() {
//        @Override
//        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
//            return Deduction.apply(T, B, m, minConf);
//        }
//    },

//    @AllowOverlap
//    //* TODO rename 'PB' to 'Sym'
//    DeductionRecursivePB() {
//        @Override
//        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
//            return DeductionPB.apply(T, B, m, minConf);
//        }
//    },

    Induction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.induction(T, B, minConf);
        }
    },
    //* TODO rename 'PB' to 'Sym'
    InductionPB() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (B.isNegative()) {
                return Induction.apply(T.neg(), B.neg(), m, minConf);
            } else {
                return Induction.apply(T, B, m, minConf);
            }
        }
    },

//    /** task frequency negated induction */
//    InductionNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.induction(T.negated(), B, minConf);
//        }
//    },
//    /** belief frequency negated induction */
//    InductionNegB() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.induction(T, B.negated(), minConf);
//        }
//    },

    Abduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR n, float minConf) {
            //Abduction(X,Y)=Induction(Y,X)
            return Induction.apply(B, T, n, minConf);
        }
    },



    /**
     * polarizes according to an implication belief.
     * this is slightly different than DeductionPB.
     * <p>
     * here if the belief is negated, then both task and belief truths are
     * applied to the truth function negated.  but the resulting truth
     * is unaffected as it derives the subject of the implication.
     * * TODO rename 'PB' to 'Sym'
     */
    AbductionPB() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {

            if (B.isNegative()) {
                return Abduction.apply(T.neg(), B.neg(), m, minConf);
            } else {
                return Abduction.apply(T, B, m, minConf);
            }
        }
    },

//    //@AllowOverlap
//    //* TODO rename 'PB' to 'Sym'
//    AbductionRecursivePB() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return AbductionPB.apply(T,B,m,minConf);
//        }
//    },

//    AbductionNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.abduction(T.negated(), B, minConf);
//        }
//    },

    Comparison() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.comparison(T, B, minConf);
        }
    },

//    ComparisonNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.comparison(T, B, true, minConf);
//        }
//    },

    Conversion() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.conversion(B, minConf);
        }
    },

//    @SinglePremise
//    Negation() {
//        
//        @Override public Truth apply( final Truth T, /* nullable */ final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.negation(T, minConf);
//        }
//    },

    @SinglePremise
    Contraposition() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.contraposition(T, minConf);
        }
    },

//    Resemblance() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return TruthFunctions.resemblance(T, B, minConf);
//        }
//    },

    Intersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.intersection(T, B, minConf);
            //return TruthFunctions2.intersectionX(T, B, minConf);
        }
    },
    Union() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            @Nullable Truth z = TruthFunctions.intersection(T.neg(), B.neg(), minConf);
            return z != null ? z.neg() : null;
        }
    },

    IntersectionSym() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (T.isPositive() && B.isPositive()) {
                return Intersection.apply(T, B, m, minConf);
            } else if (T.isNegative() && B.isNegative()) {
                Truth C = Intersection.apply(T.neg(), B.neg(), m, minConf);
                return C!=null ? C.neg() : null;
            } else {
                return null;
            }
        }
    },

    UnionSym() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (T.isPositive() && B.isPositive()) {
                return Union.apply(T, B, m, minConf);
            } else if (T.isNegative() && B.isNegative()) {
                Truth C = Union.apply(T.neg(), B.neg(), m, minConf);
                return C!=null ? C.neg() : null;
            } else {
                return null;
            }
        }
    },

    Difference() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.intersection(T, B.neg(), minConf);
        }
    },

    DifferenceReverse() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.Difference.apply(B, T, m, minConf);
        }
    },

    Analogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.analogy(T, B, minConf);
        }
    },
    ReduceConjunction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.reduceConjunction(T, B, minConf);
        }
    },

//    ReduceDisjunction() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            if (B == null || T == null) return null;
//            return TruthFunctions.reduceDisjunction(T, B,minConf);
//        }
//    },

//    ReduceConjunctionNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            if (B == null || T == null) return null;
//            return TruthFunctions.reduceConjunctionNeg(T, B,minConf);
//        }
//    },

    AnonymousAnalogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.anonymousAnalogy(T, B, minConf);
        }
    },

    Exemplification() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.exemplification(T, B, minConf);
        }
    },


    DecomposePositiveNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, false, minConf);
        }
    },

    DecomposeNegativeNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, false, false, minConf);
        }
    },

    DecomposePositiveNegativePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, true, minConf);
        }
    },

    DecomposeNegativePositivePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, true, true, minConf);
        }
    },

    DecomposePositivePositivePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, true, true, minConf);
        }
    },

    @SinglePremise
    Identity() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

//    /**
//     * same as identity but allows overlap
//     */
//    @SinglePremise
//    @AllowOverlap
//    IdentityTransform() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return TruthOperator.identity(T, minConf);
//        }
//    },

    BeliefIdentity() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(B, minConf);
        }
    },

    @AllowOverlap
    BeliefStructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            if (B == null) return null;
            return StructuralDeduction.apply(B, null, m, minConf);
        }
    },

    @AllowOverlap
    BeliefStructuralAbduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            //if (B == null) return null;
            return Abduction.apply($.t(1f, confDefault(m)), B, m, minConf);
        }
    },

//    BeliefStructuralIntersection() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
//            return B != null ? TruthFunctions.intersection(B, $.t(1, m.confDefault(BELIEF)), minConf) : null;
//        }
//    },


//    BeliefStructuralAnalogy() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
//            if (B == null) return null;
//            return TruthFunctions.analogy(B, 1f, defaultConfidence(m), minConf);
//        }
//    },

    @AllowOverlap
    BeliefStructuralDifference() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            if (B == null) return null;
            Truth res = BeliefStructuralDeduction.apply(T,B, m, minConf);
            return (res != null) ? res.neg() : null;
        }
    },

//    BeliefNegation() {
//        
//        @Override public Truth apply(final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.negation(B, minConf);
//        }
//    }

    ;


    private static float confDefault(/*@NotNull*/ NAR m) {
        return m.confDefault(BELIEF);
    }


//    /**
//     * @param T taskTruth
//     * @param B beliefTruth (possibly null)
//     * @return
//     */
//    @Override
//    abstract public Truth apply(Truth T, Truth B, /*@NotNull*/ Memory m);


    //TODO use an enum map with terms bound to the enum values directly
    static final Map<Term, TruthOperator> atomToTruthModifier = $.newHashMap(BeliefFunction.values().length);


    static {
        TruthOperator.permuteTruth(BeliefFunction.values(), atomToTruthModifier);
    }

    @Nullable
    public static TruthOperator get(Term a) {
        return atomToTruthModifier.get(a);
    }
//    public static TruthOperator get(String a) {
//        return get($.the(a));
//    }

    public final boolean single;
    public final boolean overlap;

    BeliefFunction() {

        try {
            Field enumField = getClass().getField(name());
            this.single = enumField.isAnnotationPresent(SinglePremise.class);
            this.overlap = enumField.isAnnotationPresent(AllowOverlap.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public final boolean single() {
        return single;
    }

    @Override
    public final boolean allowOverlap() {
        return overlap;
    }


}
