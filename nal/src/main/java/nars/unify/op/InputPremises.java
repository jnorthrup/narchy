//package nars.nal.meta.pre;
//
//import nars.nal.meta.PremiseMatch;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Created by me on 8/15/15.
// */
//public class InputPremises extends PreCondition2 {
//
//    public InputPremises(Term var1, Term var2) {
//        super(var1, var2);
//    }
//
//    @Override
//    public final boolean test(@NotNull PremiseMatch m, Term a, Term b) {
//        return m.premise.task().isInput() && m.premise.belief() != null && m.premise.belief().isInput();
//    }
//
// }
