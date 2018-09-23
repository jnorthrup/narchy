package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.TermBuilder;
import nars.term.util.builder.HeapTermBuilder;

/** bypasses interning and */
public abstract class DirectTermTransform extends TermTransform.NegObliviousTermTransform {

    final static TermBuilder localBuilder = HeapTermBuilder.the;

    @Override
    public Term the(Op op, int dt, Term[] t) {
        return localBuilder.compound(op, dt, t);
    }

    @Override
    public Term the(Op op, int dt, Subterms t) {
        return localBuilder.theCompound(op, dt, t);
    }

//    class CachedDirectTermTransform implements DirectTermTransform {
//        /** stores constructed Anon's locally, thread-local */
//        final QuickMemoize<HashCachedPair<Term,Subterms>,Term> localIntern;
//
//
//        public CachedDirectTermTransform(int capacity) {
//            this.localIntern = new QuickMemoize<>(capacity, this::term);
//        }
//
//        private Term term(HashCachedPair<Term,Subterms> xy) {
//            Term x = xy.getOne();
//            return localBuilder.theCompound(x.op(), x.dt(), xy.getTwo());
//        }
//
//
////        @Override
////        public Term the(Op op, int dt, TermList t) {
////            switch (dt) {
////                case 0:
////                case DTERNAL:
////                case XTERNAL:
////                    return localIntern.apply(pair(op, t));
////                default:
////                    return localBuilder.compound(op, dt, t.arrayShared());
////            }
////        }
//        @Nullable
//        public final Term transformedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
//            return localIntern.apply(new HashCachedPair<>(x,
//                    new TermList(yy.arrayShared())));
//        }
////        @Override
////        public Term the(Op op, int dt, Term[] t) {
////            if (dt == DTERNAL)
////                return localIntern.apply(pair(op, new TermList(t)));
////            else
////                return localBuilder.compound(op, dt, t);
////        }
//
//        public void resize(int s) {
//            localIntern.resize(s);
//        }
//    }

}
