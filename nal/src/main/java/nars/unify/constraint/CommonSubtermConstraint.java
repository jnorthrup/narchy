//package nars.unify.constraint;
//
//import nars.subterm.Subterms;
//import nars.term.Term;
//
///**
// * invalid if any of the following:
// *      terms are equal
// *      a term contains the other
// *      the terms have no non-variable subterms in common
// */
//public final class CommonSubtermConstraint extends RelationConstraint {
//
//    public CommonSubtermConstraint(Term target, Term x) {
//        super(target, x, "common_subterms");
//    }
//
//    @Override
//    public float cost() {
//        return 2;
//    }
//
//    @Override
//    public boolean invalid(Term x, Term y) {
//
////        int vx = x.volume();
////        int vy = y.volume();
////        if (vx == vy || x.op().var || y.op().var /* variables excluded from recursive containment test */) {
////
////        } else {
////            if (vy > vx) {
////
////                Term c = x;
////                x = y;
////                y = c;
////            }
////            if (x.containsRecursively(y, true, (t)->true))
////                return false;
////        }
//
//        return !Subterms.hasCommonSubtermsRecursive(x, y, true);
//    }
//
//}
