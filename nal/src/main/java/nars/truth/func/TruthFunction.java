package nars.truth.func;

import nars.NAL;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


public interface TruthFunction {

    /**
     * @param minConf if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
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

    /** ____N , although more accurately it would be called: 'NP' */
    final class NegatedTaskTruth extends ProxyTruthFunction {

        NegatedTaskTruth(TruthFunction o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n) {
            return o.apply(task.neg(), belief, minConf, n);
        }

        @Override public final String toString() {
            return o.toString() + 'N';
        }
    }

    final class NegatedBeliefTruth extends ProxyTruthFunction {

        NegatedBeliefTruth(TruthFunction o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n) {
            return o.apply(task, belief.neg(), minConf, n);
        }

        @Override public final String toString() {
            return o + "PN";
        }

    }
    final class NegatedTaskBeliefTruth extends ProxyTruthFunction {

        NegatedTaskBeliefTruth(TruthFunction o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n) {
            return o.apply(task.neg(), belief.neg(), minConf, n);
        }

        @Override public final String toString() {
            return o + "NN";
        }

    }

    /** for when a conclusion's subterms have already been negated accordingly, so that conclusion confidence is positive and maximum
            
            
            
            
            
     */
    final class DepolarizedTruth extends ProxyTruthFunction {

        DepolarizedTruth(TruthFunction o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth T, @Nullable Truth B, float minConf, NAL n) {
            return o.apply(unnegIfNotNull(T), unnegIfNotNull(B), minConf, n);
        }

        @Override public final String toString() {
            return o + "Depolarized";
        }
    }


    @Nullable static Truth unnegIfNotNull(@Nullable Truth x) {
        return x != null ? x.pos() : null;
    }
//    final class DepolarizedTaskTruth extends ProxyTruthOperator {
//
//        public DepolarizedTaskTruth(TruthOperator o) {
//            super(o);
//        }
//
//        @Override @Nullable public Truth apply(@Nullable Truth T, @Nullable Truth B, NAR m, float minConf) {
//            if ((B == null) || (T == null)) return null;
//            else {
//                return o.apply(T.negIf(T.isNegative()), B, m, minConf);
//            }
//        }
//
//        @Override public final String toString() {
//            return o + "DepolarizedTask";
//        }
//    }

    /** negates both task and belief frequency */
    final class NegatedTruths extends ProxyTruthFunction {

        NegatedTruths(TruthFunction o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n) {
            return task == null ? null : o.apply(task.neg(), belief!=null ? belief.neg() : null, minConf, n);
        }

        @Override public final String toString() {
            return o + "NN";
        }

    }

    @Nullable
    static Truth identity(@Nullable Truth t, float minConf) {
        return (t == null || (t.conf() < minConf)) ? null : t;
    }
}
