//package nars.derive.op;
//
//import nars.$;
//import nars.Param;
//import nars.control.Derivation;
//import nars.derive.Conclusion;
//import nars.derive.PrediTerm;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Function;
//
///**
// * Created by me on 5/21/16.
// */
//public final class UnifyOneSubterm extends UnificationPrototype {
//
//    /**
//     * either 0 (task) or 1 (belief)
//     */
//    private final int subterm;
//
//    private final Conclusion conc;
//
//    public UnifyOneSubterm(@NotNull Term x, int subterm) {
//        this(x, subterm, null);
//    }
//
//    public UnifyOneSubterm(@NotNull Term x, int subterm, Conclusion conc) {
//        super( $.func("unifyTask", $.the(subterm==0 ? "task" : "belief"), x), x );
//        this.subterm = subterm;
//        this.conc = conc;
//    }
//
//    @Override
//    public final PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
//      PrediTerm<Derivation> eachMatch = buildEachMatch();
//      return build( eachMatch!=null ? eachMatch.transform(f) : null ).transform(f);
//    }
//
//
////    @Override @NotNull
////    protected PrediTerm build(@Nullable PrediTerm<Derivation> eachMatch) {
////        assert(finish ? eachMatch != null : eachMatch == null): "conclusion wrong";
////        if (!finish) {
////            return new UnifySubterm(subterm, pattern);
////        }else {
////            return new UnifySubtermThenConclude(subterm, pattern, eachMatch);
////        }
////    }
//
//
//}
