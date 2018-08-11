package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public final class MapSubst implements Subst {



    private final Map<? extends Term, Term> xy;

    public MapSubst(Map<? extends Term, Term> xy) {
        this.xy = xy;
    }






    @Override
    public void clear() {
        xy.clear();
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


    /**
     * 1-pair substitution
     */
    public static class MapSubst1 implements Subst {

        private final Term from;
        private final Term to;

        private MapSubst1(Map.Entry<? extends Term,Term> e) {
            this(e.getKey(), e.getValue());
        }

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        public MapSubst1(/*@NotNull*/ Term from, /*@NotNull*/ Term to) {
            
            if (from.equals(to))
                throw new RuntimeException("pointless substitution");

            this.from = from;
            this.to = to;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public @Nullable Term transformCompound(Compound x) {
            if (x.equals(from))
                return to;
            if (x.impossibleSubTerm(from))
                return x;
            return Subst.super.transformCompound(x);
        }

        @Override
        public @Nullable Term xy(Term t) {
            return t.equals(from) ? to : null;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

    
    
    
    
    }
}
