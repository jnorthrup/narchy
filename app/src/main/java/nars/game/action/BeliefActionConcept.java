//package nars.concept.action;
//
//import jcog.TODO;
//import nars.NAR;
//import nars.target.Term;
//import nars.truth.Truth;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.function.Consumer;
//
///**
// * Action Concept which acts on belief state directly (no separate feedback involved)
// */
//public class BeliefActionConcept extends ActionConcept {
//
//    private final Consumer<Truth> action;
//
//
//    public BeliefActionConcept(@NotNull Term target, @NotNull NAR n, Consumer<Truth> action) {
//        super(target, n);
//        this.action = action;
//    }
//
//
//    @Override
//    public float dexterity(long start, long end, NAR n) {
//        throw new TODO();
//    }
//
//    @Override
//    public void update(long start, long end, long next, NAR nar) {
//
//        int dur = nar.dur();
//        long nowStart =
//
//                start - dur / 2;
//        long nowEnd =
//
//                start + dur / 2;
//
//        Truth belief = this.beliefs().truth(nowStart, nowEnd, nar);
//
//
//        action.accept(belief == null ? null : belief.truth());
//
//    }
//
//
//}
