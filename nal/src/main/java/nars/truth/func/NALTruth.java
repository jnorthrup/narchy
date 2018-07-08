package nars.truth.func;

import jcog.Util;
import jcog.util.Reflect;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.TruthFunctions2;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

import static nars.Op.BELIEF;

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


    Deduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return TruthFunctions2.deduction(T, B.freq(), B.conf(), minConf);

        }
    },

    @AllowOverlap DeductionRecursive() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return Deduction.apply(T, B, m, minConf);
        }
    },

    @SinglePremise @AllowOverlap StructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return T != null ? Deduction.apply(T, $.t(1f, confDefault(m)), m, minConf) : null;
        }
    },

    /**
     * similar to structural deduction but keeps the same input frequency, only reducing confidence
     */
    @AllowOverlap @SinglePremise StructuralReduction() {
        @Override
        public Truth apply(final Truth T, final Truth Bignored, NAR m, float minConf) {
            float c = T.conf() * NALTruth.confDefault(m);
            return c >= minConf ? $.t(T.freq(), c) : null;
        }
    },

    @SinglePremise @AllowOverlap
    StructuralStrong() {
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return T != null ? NALTruth.Analogy.apply(T, $.t(1f, confDefault(m)), m, minConf) : null;
        }
    },




    /**
     * maintains input frequency but reduces confidence
     */
    @AllowOverlap @SinglePremise BeliefStructuralReduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (B == null) return null;
            return BeliefStructuralDeduction.apply(B, null, m, minConf);
        }
    },


    /**
     * polarizes according to an implication belief and its effective negation reduction
     * TODO rename 'PB' to 'Sym'
     */
    DeductionPB() {
        @Override
        public Truth apply(Truth T, Truth B, NAR n, float minConf) {
            if (B.isNegative()) {
                Truth d = Deduction.apply(T.neg(), B.neg(), n, minConf);
                return d != null ? d.neg() : null;
            } else {
                return Deduction.apply(T, B, n, minConf);
            }
        }
    },


    Induction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.induction(T, B, minConf);
        }
    },

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


    Abduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR n, float minConf) {

            return Induction.apply(B, T, n, minConf);
        }
    },

    @AllowOverlap AbductionRecursive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR n, float minConf) {
            return Abduction.apply(B, T, n, minConf);
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


    Comparison() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.comparison(T, B, minConf);
        }
    },
    ComparisonSymmetric() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions2.comparisonSymmetric(T, B, minConf);
        }
    },

    Conversion() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.conversion(B, minConf);
        }
    },


    @SinglePremise
    Contraposition() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions2.contraposition(T, minConf);
        }
    },


    Intersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.intersection(T, B, minConf);

        }
    },
    Union() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            @Nullable Truth z = Intersection.apply(T.neg(), B.neg(), m, minConf);
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
                return C != null ? C.neg() : null;
            } else {
                return null;
            }
        }
    },

    /**
     * special truth function for implication composition
     */
    Implsition() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            float tc = T.conf();
            float bc = B.conf();
            float c = tc * bc;
            if (c < minConf) return null;
            float f = Util.lerp(bc / (tc + bc), T.freq(), B.freq());
            return $.t(f, c);

        }
    },
    UnionSym() {
        @Override
        public @Nullable Truth apply(@Nullable Truth T, @Nullable Truth B, NAR m, float minConf) {

            if (T.isPositive() && B.isPositive()) {
                return Union.apply(T, B, m, minConf);
            } else if (T.isNegative() && B.isNegative()) {
                Truth C = Union.apply(T.neg(), B.neg(), m, minConf);
                return C != null ? C.neg() : null;
            } else {
                return null;
            }
        }
    },
    Difference() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return Intersection.apply(T, B.neg(), m, minConf);
        }
    },

    DifferenceReverse() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return NALTruth.Difference.apply(B, T, m, minConf);
        }
    },

    Analogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions2.analogy(T, B, minConf);
        }
    },
//    ReduceConjunction() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return TruthFunctions.reduceConjunction(T, B, minConf);
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

    DecomposePositivePositiveNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, true, false, minConf);
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
            return TruthFunc.identity(T, minConf);
        }
    },


    BeliefIdentity() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunc.identity(B, minConf);
        }
    },

    @AllowOverlap
    BeliefStructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (B == null) return null;
            return StructuralDeduction.apply(B, null, m, minConf);
        }
    },

    @AllowOverlap
    BeliefStructuralAbduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {

            return Abduction.apply($.t(1f, confDefault(m)), B, m, minConf);
        }
    },


    @AllowOverlap
    BeliefStructuralDifference() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (B == null) return null;
            Truth res = BeliefStructuralDeduction.apply(T, B, m, minConf);
            return (res != null) ? res.neg() : null;
        }
    },

    @Deprecated Strong() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return Desire.apply(T, B, m, minConf);
        }
    },

    Desire() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions2.desire(T, B, minConf, true);
        }
    },
    DesireWeak() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions2.desire(T, B, minConf, false);
        }
    },

//    @Deprecated Weak() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return DesireWeak.apply(T, B, m, minConf);
//        }
//    },

    @SinglePremise @AllowOverlap Curiosity() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            float conf = m.confMin.floatValue();// + m.confResolution.floatValue();
            if (T==null)
                return $.t(m.random().nextFloat(), conf);
            else
                return $.t(T.freq(), conf);
        }
    },

    ;


    static final ImmutableMap<Term, TruthFunc> funcs;

    static {
        MutableMap<Term, TruthFunc> h = new UnifiedMap<>(NALTruth.values().length);
        TruthFunc.permuteTruth(NALTruth.values(), h);
        funcs = h.toImmutable();
    }

    public final boolean single;
    public final boolean overlap;


    NALTruth() {
        Field f = Reflect.on(getClass()).field(name()).get();
        this.single = f.isAnnotationPresent(SinglePremise.class);
        this.overlap = f.isAnnotationPresent(AllowOverlap.class);
    }

    private static float confDefault(NAR m) {
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


}
