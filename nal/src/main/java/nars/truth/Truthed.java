package nars.truth;

import jcog.Skill;
import nars.truth.func.TruthFunctions;

import static nars.truth.func.TruthFunctions.w2cSafe;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed  {

    @Skill("Quantum_spin") float freq();
//    @Skill("Quantum_spin") default float freq() {
//        return truth().freq();
//    }

    /** a dual (reversible) asymptotic normalization of evi(), see: HORIZON */
    default float conf() {
        return (float)confDouble();
    }
    /** provides high-precision confidence value, if implemented */
    default double confDouble() {
        return w2cSafe(evi());
    }
//    /** weight of evidence ( confidence converted to weight, 'c2w()' ) */
//    @Skill({"Epistemology", "Evidence_law"}) default double evi() {
//        //return c2wSafe((double)conf());
//        return truth().evi();
//    }

    @Skill({"Epistemology", "Evidence_law"}) double evi();

//    @Nullable
//    Truth truth();

    /**
     * Calculate the expectation value of the truth value
     *
     * @return The expectation value
     */
    default float expectation() {
        return (float) TruthFunctions.expectation(freq(), (double) conf());
    }

    default float expectationNeg() {
        return (float) TruthFunctions.expectation(1.0F - freq(), (double) conf());
    }

    /** value between 0 and 1 indicating how distant the frequency is from 0.5 (neutral) */
    default float polarity() {
        return Truth.polarity(freq());
    }

    /**
     * Check if the truth value is negative
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is less than (but not equal to) 1/2
     */
    default boolean isNegative() {
        return freq() < 0.5f;
    }

    /**
     * Check if the truth value is negative.
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is greater than or equal to 1/2
     */
    default boolean isPositive() {
        return freq() >= 0.5f;
    }


    default double eviEternalized() {
        return TruthFunctions.eternalize(evi());
    }

}
