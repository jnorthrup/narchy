package nars.util.term.transform;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class MapSubst implements Subst {

    @Nullable public static Subst the(Map<Term, Term> m) {
        switch (m.size()) {
            case 0:
                return null;
            case 1:
                return new MapSubst1(m.entrySet().iterator().next());
            default:
                return new MapSubst(m);
        }
    }

    public final Map<Term, Term> xy;

    private MapSubst(Map<Term, Term> xy) {
        this.xy = xy;
    }

//    @Override
//    public void cache(@NotNull Term x, @NotNull Term y) {
//        //ignored
//    }

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

//    public void forEach(@NotNull BiConsumer<? super Term, ? super Term> each) {
//        if (xy.isEmpty()) return;
//        xy.forEach(each);
//    }
//
//    @Override
//    public boolean put(@NotNull Unify copied) {
//        throw new UnsupportedOperationException("TODO");
//    }

    @NotNull
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

        private MapSubst1(Map.Entry<Term,Term> e) {
            this(e.getKey(), e.getValue());
        }

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        public MapSubst1(/*@NotNull*/ Term from, /*@NotNull*/ Term to) {
            //assert(!from.equals(to)): "pointless substitution";
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

    //    @Override
    //    public boolean put(/*@NotNull*/ Unify copied) {
    //        throw new UnsupportedOperationException();
    //    }
    }
}
