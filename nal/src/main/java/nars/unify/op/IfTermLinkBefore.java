//package nars.derive.meta.op;
//
//import nars.Task;
//import nars.derive.meta.BoolCondition;
//import nars.premise.Derivation;
//import nars.term.Compound;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import static nars.time.Tense.DTERNAL;
//import static nars.time.Tense.ETERNAL;
//
///**
// * Created by me on 7/11/16.
// */
//public class IfTermLinkBefore extends events {
//    public static final @Nullable BoolCondition ifTermLinkBefore = new IfTermLinkBefore();
//    public static final @Nullable BoolCondition ifBeliefBefore = new IfTermLinkBefore() {
//        @Override
//        public String toString() {
//            return "ifBeliefIsBefore";
//        }
//        @Override
//        public boolean requireBelief() {
//            return true;
//        }
//
//    };
//
//    @NotNull
//    @Override
//    public String toString() {
//        return "ifTermLinkBefore";
//    }
//
//    public boolean requireBelief() {
//        return false;
//    }
//
//    @Override
//    public boolean run(@NotNull Derivation m) {
//
//        Task belief = m.belief;
//        if (belief == null && requireBelief())
//            return false;
//
//        Task task = m.task;
//        Compound tt = task.term();
//        int ttdt = tt.dt();
//
//
//        if ((belief != null) && (belief.start() != ETERNAL) && (task.start() != ETERNAL)) {
//            //only allow a belief if it occurred before or during the task's specified occurrence
//            if (belief.start() > task.start())
//                return false;
//        }
//
//        if ((ttdt == DTERNAL) || (ttdt == 0)) {
//            return true;
//        } else {
//
//            final int targetMatch = ttdt < 0 ? 1 : 0;  //must match term
//            return tt.term(targetMatch).equals(m.beliefTerm);
//        }
//    }
//
//}
