//package nars.nal.meta.op;
//
//import nars.nal.meta.AtomicBoolCondition;
//import nars.nal.meta.PremiseEval;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///** tests the resolved terms specified by pattern variable terms */
//public abstract class PreCondition2 extends AtomicBoolCondition {
//    public final Term arg1, arg2;
//    @NotNull
//    private final String str;
//
//    protected PreCondition2(Term var1, Term var2) {
//        arg1 = var1;
//        arg2 = var2;
//        str = getClass().getSimpleName() + ":(" + arg1 + ',' + arg2 + ')';
//    }
//
//    @Override
//    public boolean booleanValueOf(@NotNull PremiseEval m) {
//        return test(m,
//                m.resolve(arg1),
//                m.resolve(arg2));
//    }
//
//    public abstract boolean test(PremiseEval m, Term a, Term b);
//
//    @NotNull
//    @Override
//    public final String toString() {
//        return str;
//    }
//}
