//package nars.derive.meta.op;
//
//import nars.derive.meta.AtomicBoolCondition;
//import nars.premise.Derivation;
//import nars.term.Compound;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///**
// * compound 'container' contains (1st-level only) 'contained' but also not equal to it
// */
//public final class ComposedBy extends AtomicBoolCondition {
//
//    /** the values here refer to either: 0=task, 1=belief term */
//    private final int container, contained;
//    @NotNull
//    private final String id;
//
//    public ComposedBy(int container, int contained) {
//        this.id = getClass().getSimpleName() + '(' + container + "," + contained + ")";
//
//        this.container = container;
//        this.contained = contained;
//    }
//
//
//    @Override
//    public boolean run(@NotNull Derivation p) {
//        Term container = this.container==0 ? p.taskTerm : p.beliefTerm;
//        Term contained = this.contained==0 ? p.taskTerm : p.beliefTerm;
//        if (container instanceof Compound) {
//            return ((Compound)container).containsTermAtemporally(contained);
//        }
//        return false;
//    }
//
//    @Override
//    public @NotNull String toString() {
//        return id;
//    }
//}
