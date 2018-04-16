//package nars.nal.meta.op;
//
//import nars.term.transform.subst.FindSubst;
//import org.jetbrains.annotations.NotNull;
//
///**
// * selects the ith sibling subterm of the current parent
// */
//public final class Subterm extends PatternOp {
//    public final int index;
//
//    public Subterm(int index) {
//        this.index = index;
//    }
//
//    @Override
//    public boolean run(@NotNull FindSubst f) {
//        f.goSubterm(index);
//        return true;
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return "t" + index; //s for subterm and sibling
//    }
// }
