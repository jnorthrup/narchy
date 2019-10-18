package nars.truth.func;

import jcog.Skill;
import jcog.util.Reflect;
import nars.$;
import nars.NAL;
import nars.truth.Truth;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

import static nars.Op.BELIEF;
import static nars.truth.func.TruthFunctions.confCompose;
import static nars.truth.func.TruthFunctions2.weak;

/**
 * NAL Truth Model
 *      derivative of the original set of NAL truth functions, preserved as much as possible
 */
public enum NALTruth implements TruthFunction {

	Deduction() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.deduction(T, B, true, minConf);
        }
    },

    @AllowOverlap DeductionRecursive() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.deduction(T, B, true, minConf);
        }
    },

    DeductionWeak() {
		@Override
		public Truth apply(Truth T, Truth B, float minConf, NAL n) {
			return TruthFunctions.deduction(T, B, false, minConf);
		}
	},
    @AllowOverlap DeductionRecursiveWeak() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.deduction(T, B, false, minConf);
        }
    },

    /**
     * similar to structural deduction but keeps the same input frequency, only reducing confidence
     */
    @AllowOverlap @SinglePremise StructuralReduction() {
        @Override
        public Truth apply(Truth T, Truth Bignored, float minConf, NAL n) {
            float c = confCompose(T, NALTruth.confDefault(n));
            return c >= minConf ? $.tt(T.freq(), c) : null;
        }
    },
    @AllowOverlap @SinglePremise StructuralReductionWeak() {
        @Override
        public Truth apply(Truth T, Truth Bignored, float minConf, NAL n) {
            float c = confCompose(T, NALTruth.confDefault(n));
            return c >= minConf ? weak($.tt(T.freq(), c),minConf) : null;
        }
    },
    @AllowOverlap @SinglePremise StructuralDeduction() {
        @Override
        public Truth apply(Truth T, Truth Bignored, float minConf, NAL n) {
            return T != null ? Deduction.apply(T, $.tt(1f, confDefault(n)), minConf, n) : null;
        }
    },
    @AllowOverlap @SinglePremise StructuralDeductionWeak() {
        @Override
        public Truth apply(Truth T, Truth Bignored, float minConf, NAL n) {
            return T != null ? weak(Deduction.apply(T, $.tt(1f, confDefault(n)), minConf, n), minConf) : null;
        }
    },

    BeliefStructuralIntersection() {
        @Override
        public Truth apply(Truth Tignored, Truth B, float minConf, NAL n) {
            return Intersection.apply(B, $.tt(1f, confDefault(n)), minConf, n);
        }
    },


//
//    @SinglePremise @AllowOverlap
//    StructuralStrong() {
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return T != null ? NALTruth.Analogy.apply(T, $.tt(1f, confDefault(m)), m, minConf) : null;
//        }
//    },
//    @SinglePremise @AllowOverlap
//    StructuralWeak() {
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return T != null ? weak(NALTruth.Analogy.apply(T, $.tt(1f, confDefault(m)), m, minConf)) : null;
//        }
//    },


//    /**
//     * polarizes according to an implication belief and its effective negation reduction
//     * TODO rename 'PB' to 'Sym'
//     */
//    DeductionPB() {
//        @Override
//        public Truth apply(Truth T, Truth B, NAR n, float minConf) {
//            if (B.isNegative()) {
//                Truth d = Deduction.apply(T.neg(), B.neg(), n, minConf);
//                return d != null ? d.neg() : null;
//            } else {
//                return Deduction.apply(T, B, n, minConf);
//            }
//        }
//    },


    InductionPB() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            if (B.isNegative()) {
                B = B.neg();
                T = T.neg();
            }
            return Induction.apply(T, B, minConf, n);
        }
    },

    Induction() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.induction(T, B, minConf);
        }
    },

	@Skill("Sherlock_Holmes#Holmesian_deduction") Abduction() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return Induction.apply(B, T, minConf, n);
        }
    },

//    @AllowOverlap AbductionRecursive() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
//            return Abduction.apply(T, B, minConf, n);
//        }
//    },


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
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return InductionPB.apply(B, T, minConf, n);
//            if (B.isNegative())
//                return Abduction.apply(T.neg(), B.neg(), minConf, n);
//            else
//                return Abduction.apply(T, B, minConf, n);
        }
    },
    AbductionXOR() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            if (B.isNegative())
                return Abduction.apply(T, B.neg(), minConf, n);
            else
                return Abduction.apply(T.neg(), B, minConf, n);
        }
    },

    Comparison() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.comparison(T, B, minConf);
        }
    },
    ComparisonSymmetric() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.comparisonSymmetric(T, B, minConf);
        }
    },

    Conversion() {
        @Override
        public Truth apply(@Nullable Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.conversion(B, minConf);
        }
    },
    Resemblance() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.resemblance(T, B, minConf);
        }
    },

    @SinglePremise
    Contraposition() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.contraposition(T, minConf);
        }
    },


    Intersection() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.intersection(T, B, minConf);

        }
    },

    Union() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.union(T, B, minConf);
            //return TruthFunctions2.union(T, B, minConf);
        }
    },

    IntersectionSym() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return B.isNegative() ?
                    Intersection.apply(T.neg(), B.neg(), minConf, n)
                    :
                    Intersection.apply(T, B, minConf, n);
        }
    },
    @AllowOverlap Pre() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.pre(T, B, false, minConf);
        }
    },
    @AllowOverlap PreWeak() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.pre(T, B, true, minConf);
        }
    },
    @AllowOverlap Post() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.post(T, B, true, minConf);
        }
    },
    @AllowOverlap PostWeak() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.post(T, B, false, minConf);
        }
    },
    @Deprecated IntersectionPB() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return B.isNegative() ?
                    negIfNonNull(Intersection.apply(T.neg(), B.neg(), minConf, n))
                    :
                    Intersection.apply(T, B, minConf, n);
        }
    },
    @Deprecated UnionPB() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return B.isNegative() ?
                    negIfNonNull(Union.apply(T.neg(), B.neg(), minConf, n))
                    :
                    Union.apply(T, B, minConf, n);
        }
    },

//    UnionPT() {
//        @Override
//        public Truth apply(Truth T, Truth B, NAL n, float minConf) {
//            return NALTruth.pt(T, B, n, minConf, T.isNegative() ?  Intersection /* yes i know this is opposite but it works */ : com.sun.jna.Union);
//        }
//    },
//
//    IntersectionPT() {
//        @Override
//        public Truth apply(Truth T, Truth B, NAL n, float minConf) {
//            return NALTruth.pt(T, B, n, minConf, T.isNegative() ? Union /* yes i know this is opposite but it works */ : Intersection);
//        }
//    },


    Difference() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return Intersection.apply(T, B.neg(), minConf, n);
        }
    },

    DifferenceReverse() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return NALTruth.Difference.apply(B, T, minConf, n);
        }
    },

    Analogy() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.analogy(T, B.freq(), B.confDouble(), minConf);
        }
    },

    ReduceConjunction() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.reduceConjunction(T, B, minConf);
        }
    },


    AnonymousAnalogy() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.anonymousAnalogy(T, B, minConf);
        }
    },

    Exemplification() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions.exemplification(T, B, minConf);
        }
    },

//    DecomposeDiff() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
//            return TruthFunctions2.decomposeDiff(T, B, minConf);
//        }
//    },

    Divide() {
        @Override
        public Truth apply(Truth XY, Truth X, float minConf, NAL n) {
            return TruthFunctions2.divide(XY, X, minConf);
        }
    },

    @SinglePremise
    Identity() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunction.identity(T, minConf);
        }
    },


    BeliefIdentity() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunction.identity(B, minConf);
        }
    },

    /**
     * maintains input frequency but reduces confidence
     */
    BeliefStructuralReduction() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            if (B == null) return null;
            return StructuralReduction.apply(B, null, minConf, n);
        }
    },
    BeliefStructuralDeduction() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            if (B == null) return null;
            return StructuralDeduction.apply(B, null, minConf, n);
        }
    },

    BeliefStructuralDifference() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            if (B == null) return null;
            Truth res = BeliefStructuralDeduction.apply(T, B, minConf, n);
            return (res != null) ? res.neg() : null;
        }
    },

    @SinglePremise
    StructuralAbduction() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return Abduction.apply($.tt(1f, confDefault(n)), T, minConf, n);
        }
    },

    BeliefStructuralAbduction() {
        @Override
        public Truth apply(@Nullable Truth T, Truth B, float minConf, NAL n) {
            return Abduction.apply($.tt(1f, confDefault(n)), B, minConf, n);
        }
    },

    @AllowOverlap Desire() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.desire(T, B, minConf, false,true);
            //return TruthFunctions.desire(T, B, minConf, true);
            //return TruthFunctions2.desireSemiBipolar(T, B, minConf, true);
        }
    },

    @AllowOverlap DesireWeak() {
        @Override
        public Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return TruthFunctions2.desire(T, B, minConf, false,false);
            //return TruthFunctions.desire(T, B, minConf, false);
            //return TruthFunctions2.desireSemiBipolar(T, B, minConf, false);
        }
    },

    /** wrapper for Desire that inverts the belief truth according to the polarity of the task  truth */
    Undesire() {
        @Override
        public @Nullable Truth apply(Truth T, Truth B, float minConf, NAL n) {
            return NALTruth.Desire.apply(T, B.negIf(T.isPositive()), minConf, n);
        }
    },


//    /** bipolar form of desire - belief truth can invert goal truth */
//    Need() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
//            return TruthFunctions2.desire(T, B, minConf, true,true);
//        }
//    },


//    @SinglePremise @AllowOverlap Curiosity() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return $.tt(m.random().nextFloat(), m.confMin.floatValue()*2);
//        }
//    },

    ;

    public static final TruthModel the = new TruthModel(NALTruth.values());




    private final boolean single;
    private final boolean overlap;

    NALTruth() {
        Field f = Reflect.on(getClass()).field(name()).get();
        this.single = f.isAnnotationPresent(SinglePremise.class);
        this.overlap = f.isAnnotationPresent(AllowOverlap.class);
    }

    private static float confDefault(NAL m) {
        //TODO choose this according to belief/goal
        return m.confDefault(BELIEF);
    }

    @Override
    public final boolean single() {
        return single;
    }

    @Override
    public final boolean allowOverlap() {
        return overlap;
    }


    public final @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, NAL m) {
        return apply(task, belief, Float.MIN_NORMAL, m);
    }

    private static @Nullable Truth negIfNonNull(@Nullable Truth t) {
        return t == null ? null : t.neg();
    }

}

//    IntersectionSym() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return TruthFunctions2.intersectionSym(T,B,minConf);
//        }
//    },
//
//    UnionSym() {
//        @Override
//        public @Nullable Truth apply(@Nullable Truth T, @Nullable Truth B, NAR m, float minConf) {
//            return TruthFunctions2.unionSym(T,B,minConf);
//
////            if (T.isPositive() && B.isPositive()) {
////                return Union.apply(T, B, m, minConf);
////            } else if (T.isNegative() && B.isNegative()) {
////                Truth C = Union.apply(T.neg(), B.neg(), m, minConf);
////                return C != null ? C.neg() : null;
////            } else {
////                return null;
////            }
//        }
//    },