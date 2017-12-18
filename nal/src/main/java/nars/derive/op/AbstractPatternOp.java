package nars.derive.op;

import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * a condition on the op of a pattern term (task=0, or belief=1)
 */
public enum AbstractPatternOp  {
    ;

    static Term name(Class c, int subterm, String param) {
        return $.func(c.getSimpleName(), $.the(subterm) , $.quote(param));
    }

    public static final class PatternOp extends AbstractPred<Derivation> {

        public final int taskOrBelief;
        public final int opOrdinal;


        public PatternOp(int taskOrBelief, /*@NotNull*/ Op op) {
            super(name(PatternOp.class, taskOrBelief, op.str));
            this.taskOrBelief = taskOrBelief;
            this.opOrdinal = op.id;
        }


        @Override
        public boolean test(Derivation ff) {
            return (taskOrBelief == 0 ? ff.termSub0op : ff.termSub1op) == opOrdinal;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    }
   public static final class TaskBeliefOpEqual extends AbstractPred<Derivation> {

        public TaskBeliefOpEqual() {
            super($.the(TaskBeliefOpEqual.class.getSimpleName()));
        }

       @Override
       public boolean equals(Object o) {
           return super.equals(o);
       }

       @Override
        public boolean test(Derivation ff) {
            return ff.termSub0op == ff.termSub1op;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    }

//    /** tests op membership in a given vector
//      * the bit must not be set in the structure
//      * */
//    public static final class PatternOpNot extends BoolPredicate.DefaultBoolPredicate<Derivation> {
//
//        public PatternOpNot(int subterm, int structure) {
//            super(name(PatternOpNot.class, subterm, Integer.toString(structure,2)), (ff) -> {
//                return (structure & (subterm == 0 ? ff.termSub0opBit : ff.termSub1opBit)) == 0;
//            });
//        }
//
//    }

//    /** tests op membership wotjom in a given vector */
//    public static final class PatternOpNotContaining extends AtomicPredicate<Derivation> {
//
//        public final int subterm;
//        public final int opBits;
//
//        @NotNull private final transient String id;
//
//
//        public PatternOpNotContaining(int subterm, int structure) {
//            this.subterm = subterm;
//            this.opBits = structure;
//            this.id = name(getClass(), subterm, Integer.toString(structure,2)).toString();
//        }
//
//        @Override
//        public @NotNull String toString() {
//            return id;
//        }
//
//        @Override
//        public boolean test(@NotNull Derivation ff) {
//            return (opBits & (subterm == 0 ? ff.termSub0Struct : ff.termSub1Struct)) == 0;
//        }
//    }

}
