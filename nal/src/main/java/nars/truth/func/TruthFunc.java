package nars.truth.func;

import nars.NAR;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public interface TruthFunc {

    static void permuteTruth(TruthFunc[] values, Map<Term, TruthFunc> table) {
        for (TruthFunc t : values) {

            table.put(Atomic.the(t.toString() /*+ ""*/), t);
            table.put(Atomic.the(t + "PP"), t); //alias

            SwappedTruth swapped = new SwappedTruth(t);
            NegatedTaskTruth negatedTask = new NegatedTaskTruth(t);

            table.put(Atomic.the(t + "X"), swapped);

            table.put(Atomic.the(t + "NP"), negatedTask);
            table.put(Atomic.the(t.toString() + 'N'), negatedTask); //@Deprecated, prefer NP variant

            if (!t.single()) {
                table.put(Atomic.the(t + "PN"), new NegatedBeliefTruth(t));
                table.put(Atomic.the(t + "PNX"), new NegatedBeliefTruth(swapped)); //HACK

                table.put(Atomic.the(t + "NN"), new NegatedTruths(t));
                table.put(Atomic.the(t + "NX"), new NegatedTaskTruth(swapped));
            }

            table.put(Atomic.the(t + "Depolarized"), new DepolarizedTruth(t));



        }
    }

    /**
     *
     * @param task
     * @param belief
     * @param n
     * @param minConf if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
     * @return
     */
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR n, float minConf);

   


    boolean allowOverlap();
    boolean single();

    abstract class ProxyTruthFunc implements TruthFunc {
        final TruthFunc o;
        private final boolean allowOverlap, single;

        ProxyTruthFunc(TruthFunc o) {

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
    final class SwappedTruth extends ProxyTruthFunc {

        SwappedTruth(TruthFunc o) {
            super(o);
        }

        @Override
        public
        @Nullable
        Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR n, float minConf) {
            return o.apply(belief, task, n, minConf);
        }


        @Override
        public String toString() {
            return o.toString() + 'X';
        }

    }

    /** ____N , although more accurately it would be called: 'NP' */
    final class NegatedTaskTruth extends ProxyTruthFunc {

        NegatedTaskTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR n, float minConf) {
            return o.apply(task.neg(), belief, n, minConf);
        }

        @Override public final String toString() {
            return o.toString() + 'N';
        }
    }

    final class NegatedBeliefTruth extends ProxyTruthFunc {

        NegatedBeliefTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR n, float minConf) {
            return o.apply(task, belief.neg(), n, minConf);
        }

        @Override public final String toString() {
            return o + "PN";
        }

    }


    /** for when a conclusion's subterms have already been negated accordingly, so that conclusion confidence is positive and maximum
            
            
            
            
            
     */
    final class DepolarizedTruth extends ProxyTruthFunc {

        DepolarizedTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth T, @Nullable Truth B, NAR n, float minConf) {
            return o.apply(unnegIfNotNull(T), unnegIfNotNull(B), n, minConf);
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
    final class NegatedTruths extends ProxyTruthFunc {

        NegatedTruths(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR n, float minConf) {
            return task == null ? null : o.apply(task.neg(), belief!=null ? belief.neg() : null, n, minConf);
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
