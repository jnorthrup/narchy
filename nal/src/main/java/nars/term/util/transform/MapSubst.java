package nars.term.util.transform;

import jcog.WTF;
import nars.Param;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;


abstract public class MapSubst implements Subst {

    public static Term replace(Term x, Map<? extends Term, Term> m) {

        switch (m.size()) {
            case 0:
                return x;
            case 1: {
                Map.Entry<? extends Term, Term> e = m.entrySet().iterator().next();
                return x.replace(e.getKey(), e.getValue());
            }
            case 2: {
                Iterator<? extends Map.Entry<? extends Term, Term>> ii = m.entrySet().iterator();
                Map.Entry<? extends Term, Term> e1 = ii.next();
                Map.Entry<? extends Term, Term> e2 = ii.next();
                return new MapSubst2(e1, e2).transform(x);
            }
            default:
                return new MapSubstN(m).transform(x);
        }

    }
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
