//package nars.nal.meta.op;
//
//import nars.Op;
//import nars.nal.meta.AtomicBooleanCondition;
//import nars.nal.meta.PremiseEval;
//import nars.term.Compound;
//import org.jetbrains.annotations.NotNull;
//
///**
// * assumes it will be matched against a 2-size compound (ex: (task,belief))
// */
//public final class SubTermOps2 extends AtomicBooleanCondition<PremiseEval> {
//
//
//    @NotNull
//    private final transient String id;
//    @NotNull private final Op left, right;
//
//
//    public SubTermOps2(/*@NotNull*/ Op left, /*@NotNull*/ Op right) {
//        this.left = left;
//        this.right = right;
//        this.id = "SubTermOps:(\"" + left + "\",\"" + right + '"' + "\")";
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return id;
//    }
//
//    @Override
//    public boolean booleanValueOf(@NotNull PremiseEval ff) {
//        Compound parent = ff.term;
//        return parent.isTerm(0, left) &&
//                parent.isTerm(1, right);
//    }
//}
