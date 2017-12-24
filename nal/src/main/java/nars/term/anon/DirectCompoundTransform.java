package nars.term.anon;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.CompoundTransform;

import static nars.Op.NEG;

/** involves some assumptions to reduce the work involved
 *  a) applies directly to the contents of negations, in which case a result is directly re-negated
 *  b) ...
 * */
public interface DirectCompoundTransform extends CompoundTransform {


    @Override
    default Term transform(Compound x) {
        Op o = x.op();
        if (o == NEG) {
            Term x0 = x.unneg();
            Term y = x0 instanceof Compound ?
                    CompoundTransform.super.transform((Compound) x0) :
                    applyTermOrNull(x0);
            if (y == null)
                return null;
            else if (y!=x0)
                return y.neg();
            else
                return x; //unmodified
        }
        return CompoundTransform.super.transform(x);
    }

//    @Override
//    default @Nullable Term transform(Compound x, Op op, int dt) {
//
//
////            } else if (dt == DTERNAL && !o.commute(DTERNAL, x.subs())) {
////
////                //replace in same positions, avoiding the more detailed term building processes
////
////                return transformDirect(o, x);
////
////            } else {
//            return CompoundTransform.super.transform(x, op, dt);
////            }
//    }

//        protected Term transformDirect(Op o, Compound x) {
//
//            Subterms xx = x.subterms();
//            Term[] yy = Util.map(this::applyTermOrNull, Term[]::new, xx.arrayShared());
//
//            if (Util.or((Term y) -> y instanceof Bool, yy))
//                return null;
//
//            Term z = transformDirect(o, yy);
//
//
//            Subterms zz = z.subterms();
//            if (!zz.isNormalized() && xx.isNormalized()) //propagate normalization, it should still hold
//                ((TermVector) zz).setNormalized();
//
//
//            return z;
//        }
//
//        @Nullable
//        protected Term transformDirect(Op o, Term[] yy) {
//            return The.compound(o, yy);
//        }

}
