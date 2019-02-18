package nars.term.util.transform;

import jcog.TODO;
import jcog.WTF;
import nars.Param;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.LazyCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;


abstract public class MapSubst implements Subst {

    @Override
    public @Nullable Term transformCompound(Compound x) {
        Term y = resolve(x);
        if (y==x) {
            return Subst.super.transformCompound(x);
        } else
            return y;
    }

    @Override
    public boolean transformCompound(Compound x, LazyCompound out) {
        throw new TODO();
    }

    public static Term replace(Term x, Map<? extends Term, Term> m) {

        switch (m.size()) {
            case 0:
                return x;
            case 1: {
                Map.Entry<? extends Term, Term> e = m.entrySet().iterator().next();
                Term src = e.getKey(), target = e.getValue();
                if (x.equals(src))
                    return target;
                else if (src.equals(target) || x.impossibleSubTerm(src))
                    return x; //no change
                else
                    return replace(src, target).transform(x);
            }
            case 2: {
                Iterator<? extends Map.Entry<? extends Term, Term>> ii = m.entrySet().iterator();
                Map.Entry<? extends Term, Term> e1 = ii.next(), e2 = ii.next();
                return new MapSubst2(e1, e2).transform(x);
            }
            default:
                return new MapSubstN(m).transform(x);
        }

    }
//    private static final class MapSubst1 extends MapSubst {
//        final Term x, y;
//
//        public MapSubst1(Term x, Term y) {
//            this.x = x; this.y = y;
//        }
//
//        @Override
//        public Term transform(Term t) {
//            if (t.impossibleSubTerm(x))
//                return t;
//            return super.transform(t);
//        }
//
//        /**
//         * gets the substitute
//         *
//         * @param t
//         */
//        @Nullable
//        @Override
//        public Term xy(Term t) {
//            if (t.equals(x))
//                return y;
//            return null;
//        }
//    }

    private static final class MapSubst2 extends MapSubst {
        final Term ax, ay, bx, by;

        public MapSubst2(Map.Entry<? extends Term, Term> a, Map.Entry<? extends Term, Term> b){
            this.ax = a.getKey();
            this.ay = a.getValue();
            this.bx = b.getKey();
            this.by = b.getValue();
        }

        /**
         * gets the substitute
         *
         * @param t
         */
        @Nullable
        @Override
        public Term xy(Term t) {
            if (t.equals(ax))
                return ay;
            if (t.equals(bx))
                return by;
            return null;
        }
    }

    private static final class MapSubstN extends MapSubst {
        private final Map<? extends Term, Term> xy;

        public MapSubstN(Map < ? extends Term, Term > xy){
            this.xy = xy;
        }

        /**
         * gets the substitute
         *
         * @param t
         */
        @Nullable
        @Override
        public Term xy(Term t) {
            return xy.get(t);
        }

    }

    public static TermTransform replace(Term from, Term to) {

        if (Param.DEBUG && from == to)
            throw new WTF("pointless substitution");

        if (from instanceof Atomic) {
            return new SubstAtomic((Atomic)from, to);
        } else {
            return new SubstCompound((Compound)from, to);
        }
    }

    public MapSubst clear() {
        throw new UnsupportedOperationException();
//        xy.clear();
//        return this;
    }

    @Override
    public boolean isEmpty() {
        return false;
//        return xy.isEmpty();
    }


    final static class SubstCompound implements TermTransform {

        private final Compound from;
        private final Term to;

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        SubstCompound(Compound from, Term to) {
            this.from = from;
            this.to = to;
        }


        @Override
        public @Nullable Term transformCompound(Compound x) {
            if (x.equals(from))
                return to;
            if (x.impossibleSubTerm(from))
                return x;
            return TermTransform.super.transformCompound(x);
        }

    }

    final static class SubstAtomic extends TermTransform.NegObliviousTermTransform {

        private final Atomic from;
        private final Term to;

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        SubstAtomic(Atomic from, Term to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public @Nullable Term transformAtomic(Atomic x) {
            return x.equals(from) ? to : x;
        }

        @Override
        protected @Nullable Term transformNonNegCompound(Compound x) {
            return x.impossibleSubTerm(from) ? x : super.transformNonNegCompound(x);
        }

    }

}
