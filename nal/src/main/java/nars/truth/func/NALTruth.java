package nars.truth.func;

import jcog.util.Reflect;
import nars.$;
import nars.NAL;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

import static nars.Op.BELIEF;
import static nars.truth.func.TruthFunctions.confCompose;
import static nars.truth.func.TruthFunctions2.weak;

/**
 * NAL Truth Functions
 * the original set of NAL truth functions, preserved as much as possible
 *
 * <patham9> only strong rules are allowing overlap
 * <patham9> except union and revision
 * <patham9> if you look at the graph you see why
 * <patham9> its both rules which allow the conclusion to be stronger than the premises
 */
public enum NALTruth implements TruthFunc {


    /*@AllowOverlap*/ Deduction() {
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


    Induction() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.induction(T, B, minConf);
        }
    },

    /*@AllowOverlap*/ Pre() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.pre(T, B, false, minConf);
        }
    },

    /*@AllowOverlap*/ PreWeak() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.pre(T, B, true, minConf);
        }
    },

    Post() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.post(T, B, true, minConf);
        }
    },

    PostWeak() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.post(T, B, false, minConf);
        }
    },


    /**
     * similar to structural deduction but keeps the same input frequency, only reducing confidence
     */
    @SinglePremise StructuralReduction() {
        @Override
        public Truth apply(final Truth T, final Truth Bignored, float minConf, NAL n) {
            float c = confCompose(T.conf(), NALTruth.confDefault(n));
//            c = weak(c);
            return c >= minConf ? $.t(T.freq(), c) : null;
        }
    },
    @SinglePremise StructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth Bignored, float minConf, NAL n) {
            return T != null ? Deduction.apply(T, $.t(1f, confDefault(n)), minConf, n) : null;
        }
    },
    @SinglePremise StructuralDeductionWeak() {
        @Override
        public Truth apply(final Truth T, final Truth Bignored, float minConf, NAL n) {
            return T != null ? weak(Deduction.apply(T, $.t(1f, confDefault(n)), minConf, n), minConf) : null;
        }
    },


//
//    @SinglePremise @AllowOverlap
//    StructuralStrong() {
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return T != null ? NALTruth.Analogy.apply(T, $.t(1f, confDefault(m)), m, minConf) : null;
//        }
//    },
//    @SinglePremise @AllowOverlap
//    StructuralWeak() {
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return T != null ? weak(NALTruth.Analogy.apply(T, $.t(1f, confDefault(m)), m, minConf)) : null;
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


    Abduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return Induction.apply(B, T, minConf, n);
        }
    },

//    @AllowOverlap AbductionRecursive() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR n, float minConf) {
//            return Abduction.apply(B, T, n, minConf);
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
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return InductionPB.apply(B, T, minConf, n);
//            if (B.isNegative())
//                return Abduction.apply(T.neg(), B.neg(), minConf, n);
//            else
//                return Abduction.apply(T, B, minConf, n);
        }
    },
    AbductionXOR() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            if (B.isNegative())
                return Abduction.apply(T, B.neg(), minConf, n);
            else
                return Abduction.apply(T.neg(), B, minConf, n);
        }
    },

    Comparison() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.comparison(T, B, minConf);
        }
    },

    Conversion() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.conversion(B, minConf);
        }
    },
    Resemblance() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.resemblance(T, B, minConf);
        }
    },

    @SinglePremise
    Contraposition() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.contraposition(T, minConf);
        }
    },


    Intersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.intersection(T, B, minConf);

        }
    },

    Union() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            @Nullable Truth z = TruthFunctions.intersection(T, true, B, true, minConf);
            return z != null ? z.neg() : null;
        }
    },

    IntersectionSym() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return B.isNegative() ?
                    Intersection.apply(T.neg(), B.neg(), minConf, n)
                    :
                    Intersection.apply(T, B, minConf, n);
        }
    },

    @Deprecated IntersectionPB() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return B.isNegative() ?
                    negIfNonNull(Intersection.apply(T.neg(), B.neg(), minConf, n))
                    :
                    Intersection.apply(T, B, minConf, n);
        }
    },
    @Deprecated UnionPB() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
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
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return Intersection.apply(T, B.neg(), minConf, n);
        }
    },

    DifferenceReverse() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return NALTruth.Difference.apply(B, T, minConf, n);
        }
    },

    Analogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.analogy(T, B.freq(), B.conf(), minConf);
        }
    },

    ReduceConjunction() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.reduceConjunction(T, B, minConf);
        }
    },


    AnonymousAnalogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.anonymousAnalogy(T, B, minConf);
        }
    },

    Exemplification() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.exemplification(T, B, minConf);
        }
    },

    DecomposeDiff() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.decomposeDiff(T, B, minConf);
        }
    },

    Decompose() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions.decompose(T, B, true, true, true, minConf);
        }
    },


    Divide() {
        @Override
        public Truth apply(final Truth XY, final Truth X, float minConf, NAL n) {
            return TruthFunctions2.divide(XY, X, minConf);
        }
    },

    @SinglePremise
    Identity() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunc.identity(T, minConf);
        }
    },


    BeliefIdentity() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunc.identity(B, minConf);
        }
    },

    /**
     * maintains input frequency but reduces confidence
     */
    BeliefStructuralReduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            if (B == null) return null;
            return StructuralReduction.apply(B, null, minConf, n);
        }
    },
    BeliefStructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            if (B == null) return null;
            return StructuralDeduction.apply(B, null, minConf, n);
        }
    },

    BeliefStructuralDifference() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            if (B == null) return null;
            Truth res = BeliefStructuralDeduction.apply(T, B, minConf, n);
            return (res != null) ? res.neg() : null;
        }
    },

    @SinglePremise
    StructuralAbduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {

            return Abduction.apply($.t(1f, confDefault(n)), T, minConf, n);
        }
    },

    BeliefStructuralAbduction() {
        @Override
        public Truth apply(@Nullable final Truth T, final Truth B, float minConf, NAL n) {

            return Abduction.apply($.t(1f, confDefault(n)), B, minConf, n);
        }
    },

    //@AllowOverlap
    Desire() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.desire(T, B, minConf, false,true);
            //return TruthFunctions.desire(T, B, minConf, true);
            //return TruthFunctions2.desireSemiBipolar(T, B, minConf, true);
        }
    },

    DesireWeak() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.desire(T, B, minConf, false,false);
            //return TruthFunctions.desire(T, B, minConf, false);
            //return TruthFunctions2.desireSemiBipolar(T, B, minConf, false);
        }
    },

    /** bipolar form of desire - belief truth can invert goal truth */
    Need() {
        @Override
        public Truth apply(final Truth T, final Truth B, float minConf, NAL n) {
            return TruthFunctions2.desire(T, B, minConf, true,true);
        }
    },


//    @SinglePremise @AllowOverlap Curiosity() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return $.t(m.random().nextFloat(), m.confMin.floatValue()*2);
//        }
//    },

    ;

    private static final ImmutableMap<Term, TruthFunc> funcs;

    static {
        MutableMap<Term, TruthFunc> h = new UnifiedMap<>(NALTruth.values().length);
        TruthFunc.permuteTruth(NALTruth.values(), h);
        funcs = h.toImmutable();
    }

    private final boolean single;
    private final boolean overlap;
    NALTruth() {
        Field f = Reflect.on(getClass()).field(name()).get();
        this.single = f.isAnnotationPresent(SinglePremise.class);
        this.overlap = f.isAnnotationPresent(AllowOverlap.class);
    }

    /** polarity symmetry, determined by task */
    private static Truth pt(Truth T, Truth B, NAL n, float minConf, NALTruth which) {
        boolean neg = T.isNegative();
        if (neg) {
            T = T.neg();
            B = B.neg();
        }
        Truth tb = which.apply(T, B, minConf, n);
        return tb != null ? neg ? tb.neg() : tb : null;
    }

    private static float confDefault(NAL m) {
        //TODO choose this according to belief/goal
        return m.confDefault(BELIEF);
    }

    @Nullable
    public static TruthFunc get(Term a) {
        return funcs.get(a);
    }

    @Override
    public final boolean single() {
        return single;
    }

    @Override
    public final boolean allowOverlap() {
        return overlap;
    }


    final public @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, NAL m) {
        return apply(task, belief, NAL.truth.TRUTH_EPSILON, m);
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