package nars.truth;

import nars.truth.func.TruthFunctions;
import org.jetbrains.annotations.Nullable;

import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.w2cSafe;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed  {

    @Nullable
    Truth truth();

    /**
     * Calculate the expectation value of the truth value
     *
     * @return The expectation value
     */
    default float expectation() {
        return TruthFunctions.expectation(freq(), conf());
    }
    
    default float expectation(float factor) {
        return TruthFunctions.expectation(freq(), conf() * factor);
    }

    default float expectationNeg() {
        return TruthFunctions.expectation(1 - freq(), conf());
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


    default float freq() {
        return truth().freq();
    }

    /** the implementation must implement either evi() or conf() */
    default float conf() {
        return w2cSafe(evi());
    }

    /** weight of evidence ( confidence converted to weight, 'c2w()' )
     * the implementation must implement either evi() or conf()
     * */
    default double evi() {
        return c2wSafe((double)conf());
        //return truth().evi();
    }


    default double eviEternalized() {
        return TruthFunctions.eternalize(evi());
    }


}
