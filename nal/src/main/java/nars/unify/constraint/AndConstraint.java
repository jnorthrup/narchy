//package nars.derive.meta.constraint;
//
//import com.google.common.base.Joiner;
//import nars.term.Term;
//import nars.term.subst.Unify;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Collection;
//
//public final class AndConstraint extends MatchConstraint {
//
//    @NotNull
//    final MatchConstraint[] subConst;
//
//    public AndConstraint(@NotNull Collection<MatchConstraint> m) {
//        if (m.size() < 2)
//            throw new RuntimeException("invalid size");
//
//        this.subConst = m.toArray(new MatchConstraint[m.size()]);
//    }
//
//    @Override
//    public boolean invalid(@NotNull Term assignee, @NotNull Term value, @NotNull Unify f) {
//        for (MatchConstraint m : subConst) {
//            if (m.invalid(assignee, value, f))
//                return true;
//        }
//        return false;
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return "(&&," + Joiner.on(",").join(subConst) + ')';
//    }
//}
