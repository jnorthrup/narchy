package nars.truth.func;

import nars.NAR;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public interface TruthOperator {

    Atomic NONE = Atomic.the("None");

    static void permuteTruth(@NotNull TruthOperator[] values, @NotNull Map<Term, TruthOperator> table) {
        for (TruthOperator tm : values) {
            table.put(Atomic.the(tm.toString()), tm);
            table.put(Atomic.the(tm.toString() + 'X'), new SwappedTruth(tm));
            table.put(Atomic.the(tm.toString() + 'N'), new NegatedTaskTruth(tm)); //ie. NP
            table.put(Atomic.the(tm + "PN"), new NegatedBeliefTruth(tm));
            table.put(Atomic.the(tm + "NN"), new NegatedTruths(tm));
            table.put(Atomic.the(tm + "NX"), new NegatedTaskTruth(new SwappedTruth(tm)));
            table.put(Atomic.the(tm + "Depolarized"), new DepolarizedTruth(tm));
            table.put(Atomic.the(tm + "DepolarizedTask"), new DepolarizedTaskTruth(tm));
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

    default Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m) {
        return apply(task, belief, m, m.confMin.floatValue());
    }


    boolean allowOverlap();
    boolean single();

    abstract class ProxyTruthOperator implements TruthOperator {
        @NotNull protected final TruthOperator o;
        private final boolean allowOverlap, single;

        protected ProxyTruthOperator(TruthOperator o) {

            this.o = o;
            this.allowOverlap = o.allowOverlap();
            this.single = o.single();
        }

        @Override public final boolean allowOverlap() {  return allowOverlap;         }

        @Override public final boolean single() {
            return single;
        }

    }
    final class SwappedTruth extends ProxyTruthOperator {

        public SwappedTruth(TruthOperator o) {
            super(o);
        }

        @Override
        public
        @Nullable
        Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return o.apply(belief, task, m, minConf);
        }

        @NotNull
        @Override
        public String toString() {
            return o.toString() + 'X';
        }

    }

    /** ____N , although more accurately it would be called: 'NP' */
    final class NegatedTaskTruth extends ProxyTruthOperator {

        public NegatedTaskTruth(@NotNull TruthOperator o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return task == null ? null : o.apply(task.neg(), belief, m, minConf);
        }

        @NotNull @Override public final String toString() {
            return o.toString() + 'N';
        }
    }

    final class NegatedBeliefTruth extends ProxyTruthOperator {

        public NegatedBeliefTruth(@NotNull TruthOperator o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return o.apply(task, belief!=null ? belief.neg() : null, m, minConf);
        }

        @NotNull @Override public final String toString() {
            return o + "PN";
        }

    }


    /** for when a conclusion's subterms have already been negated accordingly, so that conclusion confidence is positive and maximum
            //TASK      BELIEF      TRUTH
            //positive  positive    ___PP
            //positive  negative    ___PN
            //negative  positive    ___NP
            //negative  negative    ___NN
     */
    final class DepolarizedTruth extends ProxyTruthOperator {

        public DepolarizedTruth(@NotNull TruthOperator o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth T, @Nullable Truth B, NAR m, float minConf) {
            if ((B == null) || (T == null)) return null;
            else {
                boolean tn = T.isNegative();
                boolean bn = B.isNegative();
                Truth t = o.apply(T.negIf(tn), B.negIf(bn), m, minConf);
                if (t!=null && (o == BeliefFunction.Comparison /* || o == GoalFunction.Comparison */)) {
                    //special case(s): commutive xor
                    if (tn ^ bn)
                        t = t.neg();
                }
                return t;
            }
        }

        @NotNull @Override public final String toString() {
            return o + "Depolarized";
        }
    }
    final class DepolarizedTaskTruth extends ProxyTruthOperator {

        public DepolarizedTaskTruth(@NotNull TruthOperator o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth T, @Nullable Truth B, NAR m, float minConf) {
            if ((B == null) || (T == null)) return null;
            else {
                return o.apply(T.negIf(T.isNegative()), B, m, minConf);
            }
        }

        @NotNull @Override public final String toString() {
            return o + "DepolarizedTask";
        }
    }

    /** negates both task and belief frequency */
    final class NegatedTruths extends ProxyTruthOperator {

        public NegatedTruths(@NotNull TruthOperator o) {
            super(o);
        }

        @Override @Nullable public Truth apply(@Nullable Truth task, @Nullable Truth belief, NAR m, float minConf) {
            return task == null ? null : o.apply(task.neg(), belief!=null ? belief.neg() : null, m, minConf);
        }

        @NotNull @Override public final String toString() {
            return o + "NN";
        }

    }

    @Nullable
    static Truth identity(@Nullable Truth t, float minConf) {
        return (t == null || (t.conf() < minConf)) ? null : t;
    }
}
