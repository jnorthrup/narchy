//package nars.derive.op;
//
//import nars.control.Derivation;
//import nars.derive.AbstractPred;
//import nars.derive.Conclusion;
//import nars.derive.Fork;
//import nars.derive.PrediTerm;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.TreeSet;
//
///**
// * Establishes conditions for the Term unification
// * <p>
// * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
// */
//abstract public class UnificationPrototype extends AbstractPred<Derivation> {
//
//
//    @NotNull
//    protected final Term id;
//
//
//    public final Term pattern;
//
////    /**
////     * derivation handlers; use the array form for fast iteration
////     */
////    public final TreeSet<Conclusion> conclude = new TreeSet();
//
//    protected UnificationPrototype(@NotNull Term id, Term pattern) {
//        super(id);
//        this.id = id;
//
//        this.pattern = pattern;
//    }
//
//    //abstract protected PrediTerm<Derivation> build(@Nullable PrediTerm<Derivation> eachMatch);
//
////    public static final class MatchTaskBeliefPair extends MatchTerm {
////
////        public MatchTaskBeliefPair(@NotNull TaskBeliefPair x, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
////            super(id(x, constraints), x, constraints);
////        }
////
////        @Override
////        @Deprecated public final boolean booleanValueOf(@NotNull PremiseEval p) {
////            p.matchAll(x, p.term.get() /* current term */, this, constraints);
////            return true;
////        }
////    }
//
//
//
//
////    public final @Nullable PrediTerm<Derivation> buildEachMatch() {
////
//////        PrediTerm om;
//////
//////        int cs = conclude.size();
//////        switch (cs) {
//////            case 0:
//////                om = null;
//////                break;
//////            case 1:
//////                om = conclude.first();
//////                break;
//////            default:
//////                om = Fork.fork(
//////                    conclude.toArray(new Conclusion[cs])
//////                );
//////                break;
//////        }
////
////
////        return conc;
//////
//////                    $.func("unify", pid,
//////                            this.eachMatch = each.apply(om)  ) : //final part of match
//////
//////                    $.func( "unify", pid)
//////            ); //first part of match
////
////    }
//
//
//
//    @Override
//    public boolean test(Derivation derivation) {
//        throw new UnsupportedOperationException("Use the instance that this builds, not this. it should not result in the deriver");
//    }
//
//}
