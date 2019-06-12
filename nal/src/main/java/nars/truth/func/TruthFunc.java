package nars.truth.func;

import nars.NAL;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public interface TruthFunc {

    static void permuteTruth(TruthFunc[] values, Map<Term, TruthFunc> table) {
        for (TruthFunc t : values) {

            NegatedTaskTruth negatedTask = new NegatedTaskTruth(t);

            add(table, t, "");
            add(table, t, "PP");

            add(table, negatedTask, /*N*/ "");
            add(table, negatedTask, /**/"P");

            if (!t.single()) {
                add(table, new NegatedBeliefTruth(t),"" /*PN*/);
                add(table, new NegatedTaskBeliefTruth(t), "" /*NN*/);
            }

            DepolarizedTruth dpt = new DepolarizedTruth(t);
            table.put(Atomic.the(t + "Depolarized"), dpt);
//            table.put(Atomic.the(t + "DepolarizedX"), new SwappedTruth(dpt));



        }
    }

    /** addss it and the swapped */
    static void add(Map<Term, TruthFunc> table, TruthFunc t, String postfix) {
        String name = t + postfix;
        table.put(Atomic.the(name), t);
//        table.put(Atomic.the(name + "X"), new SwappedTruth(t));
    }

    /**
     *
     * @param task
     * @param belief
     * @param minConf if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
     * @param n
     * @return
     */
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n);

   


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

//    /** swaps the task truth and belief truth */
//    final class SwappedTruth extends ProxyTruthFunc {
//
//        public SwappedTruth(TruthFunc o) {
//            super(o);
//        }
//
//        @Override
//        public
//        @Nullable
//        Truth apply(@Nullable Truth task, @Nullable Truth belief, NAL n, float minConf) {
//            return o.apply(belief, task, n, minConf);
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

        NegatedTaskTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n) {
            return o.apply(task.neg(), belief, minConf, n);
        }

        @Override public final String toString() {
            return o.toString() + 'N';
        }
    }

    final class NegatedBeliefTruth extends ProxyTruthFunc {

        NegatedBeliefTruth(TruthFunc o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, float minConf, NAL n) {
            return o.apply(task, belief.neg(), minConf, n);
        }

        @Override public final String toString() {
            return o + "PN";
        }

    }
    final class NegatedTaskBeliefTruth extends ProxyTruthFunc {

        NegatedTaskBeliefTruth(TruthFunc o) {
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
    final class DepolarizedTruth extends ProxyTruthFunc {

        DepolarizedTruth(TruthFunc o) {
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
    final class NegatedTruths extends ProxyTruthFunc {

        NegatedTruths(TruthFunc o) {
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
