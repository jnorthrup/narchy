package nars.truth;

import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.w2cSafe;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed  {

    @Nullable
    Truth truth();


    default float expectation() { return truth().expectation(); }

    /** value between 0 and 1 indicating how distant the frequency is from 0.5 (neutral) */
    default float polarity() {
        return Math.abs(freq() - 0.5f)*2f;
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

//    /**
//     * how expectation polarized (expectation distance from 0.5) a given truth value is:
//     *      expectation=0.5        -> polarization=0
//     *      expectation=0 or 1     -> polarization=1
//     */
//    default float expolarity() {
//        float exp = expectation();
//        if (exp < 0.5f)
//            exp = 1f - exp;
//        return (exp - 0.5f) * 2f;
//    }

//    /** balanced form of expectation, where -1 = no, +1 = yes, and 0 = maybe */
//    default float motivationUnweighted() {
//        return (freq() - 0.5f) * conf() * 2f;
//    }

//    /** balanced form of expectation, where -infinity = no, +infinity = yes, and 0 = maybe */
//    default float motivation() {
//        return (freq() - 0.5f) * evi(dur) * 2f;
//    }

    default float conf() {
        Truth t = truth();
        //return t == null ? Float.NaN : t.conf();
        return t.conf(); //throw NPE if not a belief/goal
    }
    default float freq() {
        Truth t = truth();
        //return t == null ? Float.NaN : t.freq();
        return t.freq(); //throw NPE if not a belief/goal
    }

//    static float confSum(@NotNull Iterable<? extends Truthed> beliefs) {
//        float t = 0;
//        for (Truthed s : beliefs)
//            t += s.truth().conf();
//        return t;
//    }

//    static float confWeightSum(@NotNull Iterable<? extends Truthed> beliefs) {
//        float t = 0;
//        for (Truthed s : beliefs)
//            t += s.truth().evi(dur);
//        return t;
//    }

//    static float freqMean(@NotNull Iterable<? extends Truthed> beliefs) {
//
//        float t = 0;
//        int count = 0;
//        for (Truthed s : beliefs) {
//            t += s.freq();
//            count++;
//        }
//
//        return count == 0 ? 0.5f : t / count;
//    }

    /** weight of evidence ( confidence converted to weight, 'c2w()' )  */
    default float evi() {
        return truth().evi(); //throws NPE if not a belief/goal
    }


    default float eviEternalized() {
//        float c = eternalizedConf();
//        return c2w(c);
        return conf(); //c2w(w2c(conf)) = conf
    }
    default float eviEternalized(float horizon) {
//        float c = eternalizedConf();
//        return c2w(c);
        return w2cSafe(conf(), horizon);
    }


    //void setValue(T v); //move to MutableMetaTruth interface






//    /** TODO move this to a MutableTruth interface to separate a read-only impl */
//    void setConfidence(float c);



}
