//package nars.nal.meta.op;
//
//import nars.term.Compound;
//import nars.term.transform.subst.FindSubst;
//import org.jetbrains.annotations.NotNull;
//
///**
// * sets the term to its parent, and the parent to a hardcoded value (its parent)
// */
//public final class ParentTerm extends PatternOp {
//    public final Compound parent;
//
//    public ParentTerm(Compound parent) {
//        this.parent = parent;
//    }
//
//    @Override
//    public boolean run(@NotNull FindSubst f) {
//        f.term.set(f.parent.get());
//        f.parent.set(parent);
//
//        return true;
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return "parent(" + parent + ')'; //s for subterm and sibling
//    }
// }
