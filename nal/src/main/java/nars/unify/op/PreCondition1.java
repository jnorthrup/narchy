//package nars.nal.meta.pre;
//
//import nars.nal.meta.AtomicBooleanCondition;
//import nars.nal.meta.PremiseMatch;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///** tests the resolved terms specified by pattern variable terms */
//public abstract class PreCondition1 extends AtomicBooleanCondition<PremiseMatch> {
//    public final Term arg1;
//
//    protected PreCondition1(Term var1) {
//        arg1 = var1;
//    }
//
//    @Override
//    public boolean booleanValueOf(@NotNull PremiseMatch m) {
//        Term a = m.apply(arg1);
//        return a != null && test(m, a);
//    }
//
//    public abstract boolean test(PremiseMatch m, Term a);
//
//    @NotNull
//    @Override
//    public String toString() {
//        return getClass().getSimpleName() + ':' + arg1;
//    }
// }
