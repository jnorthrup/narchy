package nars.truth.func;

import nars.NAR;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public interface TruthFunc {

    static void permuteTruth(TruthFunc[] values, Map<Term, TruthFunc> table) {
        for (TruthFunc tm : values) {
            table.put(Atomic.the(tm.toString()), tm);

            table.put(Atomic.the(tm + "PP"), tm); //alias

            NegatedTaskTruth np = new NegatedTaskTruth(tm);
            table.put(Atomic.the(tm + "NP"), np);
                /*@Deprecated*/ table.put(Atomic.the(tm.toString() + 'N'), np);
            table.put(Atomic.the(tm + "PN"), new NegatedBeliefTruth(tm));
            table.put(Atomic.the(tm + "NN"), new NegatedTruths(tm));
            //table.put(Atomic.the(tm + "NX"), new NegatedTaskTruth(new SwappedTruth(tm)));
            table.put(Atomic.the(tm + "Depolarized"), new DepolarizedTruth(tm));

            //table.put(Atomic.the(tm.toString() + 'X'), new SwappedTruth(tm));

        }
    }

    /**
     *
     * @param task
     * @param belief
     * @param m
     * @param minConf if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
     * @return
     */
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf);

   


    boolean allowOverlap();
    boolean single();

    abstract class ProxyTruthFunc implements TruthFunc {
        protected final TruthFunc o;
        private final boolean allowOverlap, single;

        protected ProxyTruthFunc(TruthFunc o) {

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
//    final class SwappedTruth extends ProxyTruthFunc {
//
//        public SwappedTruth(TruthFunc o) {
//            super(o);
//        }
//
//        @Override
//        public
//        @Nullable
//        Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
//            return o.apply(belief, task, m, minConf);
//        }
//
//
//        @Override
//        public String toString() {
//            return o.toString() + 'X';
//        }
//
//    }

    /** ____N , although more accurately it would be called: 'NP' */
    final class NegatedTaskTruth extends ProxyTruthFunc {

        public NegatedTaskTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return o.apply(task.neg(), belief, m, minConf);
        }

        @Override public final String toString() {
            return o.toString() + 'N';
        }
    }

    final class NegatedBeliefTruth extends ProxyTruthFunc {

        public NegatedBeliefTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return o.apply(task, belief.neg(), m, minConf);
        }

        @Override public final String toString() {
            return o + "PN";
        }

    }


    /** for when a conclusion's subterms have already been negated accordingly, so that conclusion confidence is positive and maximum
            
            
            
            
            
     */
    final class DepolarizedTruth extends ProxyTruthFunc {

        public DepolarizedTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth T, @Nullable Truth B, NAR m, float minConf) {
            return o.apply(T.negIf(T.isNegative()), B.negIf(B.isNegative()), m, minConf);
        }

        @Override public final String toString() {
            return o + "Depolarized";
        }
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

        public NegatedTruths(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return task == null ? null : o.apply(task.neg(), belief!=null ? belief.neg() : null, m, minConf);
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
