package nars.truth.func;

import nars.NAL;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


public interface TruthFunction {

    /**
     * @param minConf if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
     *                very important for minConf >= Float.MIN_NORMAL and not zero.
     */
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n);

    boolean allowOverlap();

    boolean single();

    abstract class ProxyTruthFunction implements TruthFunction {
        final TruthFunction o;
        private final boolean allowOverlap, single;

        ProxyTruthFunction(TruthFunction o) {
            this.o = o;
            this.allowOverlap = o.allowOverlap();
            this.single = o.single();
        }

        public abstract String toString();

        @Override public final boolean allowOverlap() {  return allowOverlap;         }

        @Override public final boolean single() {
            return single;
        }

    }

    /** swaps the task truth and belief truth */
    final class SwappedTruth extends ProxyTruthFunction {

        SwappedTruth(TruthFunction o) {
            super(o);
        }

        @Nullable @Override
        public Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n) {
            return o.apply(belief, task, minConf, n);
        }

        @Override
        public String toString() {
            return o.toString() + 'X';
        }

    }


    /** polarity specified for each component:
     *      -1 = negated, 0 = depolarized, +1 = positive
     * */
    final class RepolarizedTruth extends ProxyTruthFunction {

        final int taskPolarity, beliefPolarity;
        private final String suffix;
        final boolean swap;

        RepolarizedTruth(TruthFunction o, int taskPolarity, int beliefPolarty, String suffix) {
            this(o, taskPolarity, beliefPolarty, false, suffix);
        }

        RepolarizedTruth(TruthFunction o, int taskPolarity, int beliefPolarty, boolean swap, String suffix) {
            super(o);
            this.taskPolarity = taskPolarity; this.beliefPolarity = beliefPolarty;
            this.suffix = suffix;
            this.swap = swap;
        }

        /** special handling for applying the polarization to the original inputs before swapping */
        RepolarizedTruth swapped() {
            assert(!swap);
            return new RepolarizedTruth(o, beliefPolarity, taskPolarity,true,suffix);
        }

        @Override @Nullable public Truth apply(@Nullable Truth T, @Nullable Truth B, float minConf, NAL n) {
            if (swap) {
                @Nullable Truth x = T;
                T = B;
                B = x;
            }
            return o.apply(repolarize(T, taskPolarity), repolarize(B, beliefPolarity), minConf, n);
        }

        @Nullable private static Truth repolarize(@Nullable Truth t, int polarity) {
            if (t == null || polarity==1)
                return t;
            else if (polarity==-1 || (polarity==0 && t.isNegative()))
                return t.neg();
            else
                return t;
        }

        @Override public final String toString() {
            return o + suffix + (swap ? "X" : "");
        }
    }


    @Nullable static Truth unnegIfNotNull(@Nullable Truth x) {
        return x != null ? x.pos() : null;
    }




    @Nullable
    static Truth identity(@Nullable Truth t, float minConf) {
        return (t == null || (t.conf() < minConf)) ? null : t;
    }
}
