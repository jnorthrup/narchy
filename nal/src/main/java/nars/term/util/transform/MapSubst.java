package nars.term.util.transform;

import jcog.WTF;
import jcog.data.list.FasterList;
import nars.NAL;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


abstract public class MapSubst implements Subst {


    public static Term replace(Term x, Map<? extends Term, Term> m) {

        int ms = m.size();
        switch (ms) {
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
                    return replace(src, target).apply(x);
            }
            case 2: {
                Iterator<? extends Map.Entry<? extends Term, Term>> ii = m.entrySet().iterator();
                Map.Entry<? extends Term, Term> e1 = ii.next(), e2 = ii.next();
                Term a = e1.getKey();
                if (x.equals(a))
                    return e1.getValue();

                Term b = e2.getKey();
                if (x.equals(b))
                    return e2.getValue();
                if (x.impossibleSubTerm(a)) {
                    return x.impossibleSubTerm(b) ?
                            x
                            :
                            replace(b, e2.getValue()).apply(x);
                } else {
                    if (x.impossibleSubTerm(b))
                        return replace(a, e1.getValue()).apply(x);
                    else
                        return new MapSubst2(e1, e2).apply(x);
                }
            }
            default: {
                List<Term> valid = new FasterList<>(ms);
                for (Map.Entry<? extends Term,? extends Term> e : m.entrySet()) {
                    Term k = e.getKey();
                    if (!x.impossibleSubTerm(k))
                        valid.add(k);
                }
                int validN = valid.size();
                switch (validN) {
                    case 0:
                        return x;
                    case 1: {
                        Term a = valid.get(0);
                        return replace(a, m.get(a)).apply(x);
                    } case 2: {
                        Term a = valid.get(0), b = valid.get(1);
                        return new MapSubst2(a, m.get(a), b, m.get(b)).apply(x);
                    }
                    default:
                        //TODO build key filter to sub-map only the applicable keys
                        return new MapSubstN(m).apply(x);
                }
            }
        }

    }


    private static final class MapSubst2 extends MapSubst {
        final Term ax, ay, bx, by;

        public MapSubst2(Map.Entry<? extends Term, Term> a, Map.Entry<? extends Term, Term> b){
            this(a.getKey(), a.getValue(), b.getKey(), b.getValue());
        }

        public MapSubst2(Term ax, Term ay, Term bx, Term by) {
            this.ax = ax;
            this.ay = ay;
            this.bx = bx;
            this.by = by;
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

    public static class MapSubstN extends MapSubst {
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

        if (NAL.DEBUG && from == to)
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

    final static class SubstCompound implements AbstractTermTransform {

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
        public @Nullable Term applyCompound(Compound c) {
            if (c.equals(from))
                return to;
            if (c.impossibleSubTerm(from))
                return c;
            return AbstractTermTransform.super.applyCompound(c);
        }

    }

    final static class SubstAtomic extends AbstractTermTransform.NegObliviousTermTransform {

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
        public @Nullable Term applyAtomic(Atomic x) {
            return x.equals(from) ? to : x;
        }

        @Override
        protected @Nullable Term applyPosCompound(Compound x) {
            return x.impossibleSubTerm(from) ? x : super.applyPosCompound(x);
        }

    }

}
