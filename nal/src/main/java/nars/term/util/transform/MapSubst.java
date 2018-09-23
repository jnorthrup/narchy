package nars.term.util.transform;

import jcog.WTF;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public final class MapSubst implements Subst {



    private final Map<? extends Term, Term> xy;

    public MapSubst(Map<? extends Term, Term> xy) {
        this.xy = xy;
    }

    public static TermTransform the(Term from, Term to) {

        if (from.equals(to))
            throw new WTF("pointless substitution");

        if (from instanceof Atomic) {
            return new SubstAtomic((Atomic)from, to);
        } else {
            return new SubstCompound((Compound)from, to);
        }
    }

    public MapSubst clear() {
        xy.clear();
        return this;
    }

    @Override
    public boolean isEmpty() {
        return xy.isEmpty();
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












    @Override
    public String toString() {
        return "Substitution{" +
                "subs=" + xy +
                '}';
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
