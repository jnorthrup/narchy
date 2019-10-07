package nars.term.util.transform;

import jcog.data.list.FasterList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


public enum MapSubst { ;

    public static Term replace(Term x, Map<? extends Term, Term> m) {

        Term y = m.get(x);
        if (y!=null)
            return y;
        else if (x instanceof Atomic)
            return x; //no subterms that could be changed


        int ms = m.size();
        switch (ms) {
            case 0:
                return x;
            case 1: {
                Map.Entry<? extends Term, Term> e = m.entrySet().iterator().next();
                Term src = e.getKey(), target = e.getValue();
                return x.replace(src, target);
            }
            case 2: {
                Iterator<? extends Map.Entry<? extends Term, Term>> ii = m.entrySet().iterator();
                Map.Entry<? extends Term, Term> A = ii.next(), B = ii.next();
                if(A == null || B == null)
                    throw new NullPointerException();
                Term a = A.getKey(), b = B.getKey();


                Term aa = A.getValue();
                Term bb = B.getValue();


//                //HACK detect cyclic interlock
//                if (aa.equals(b) && bb.equals(a)) {
//                    //TODO should this be attempted but in 2 steps?
//                    return x.replace(aa,);
//                }


                if (x.impossibleSubTerm(a)) {
                    return x.replace(b, bb);
                } if (x.impossibleSubTerm(b))
                    return x.replace(a, aa);
                else
                    return x.transform(new MapSubst2(A.getKey(), aa, B.getKey(), bb));
            }
            default: {
                List<Term> valid = null;
                int kStruct = 0;
                for (Map.Entry<? extends Term,? extends Term> e : m.entrySet()) {
                    Term k = e.getKey();

                    int ks = k.structure();
                    if (!x.impossibleSubStructure(ks) && !x.impossibleSubVolume(k.volume())) {
                        if (valid == null) valid = new FasterList<>(ms);
                        //TODO else if (valid.size() >=2) //.. the list wont be used for MapN
                        valid.add(k);
                        kStruct |= ks;
                    }
                    ms--;
                }
                if (valid==null)
                    return x;
                int validN = valid.size();
                switch (validN) {
                    case 1: {
                        Term a = valid.get(0);
                        return x.replace(a, m.get(a));
                    } case 2: {
                        Term a = valid.get(0), b = valid.get(1);
                        return x.transform(new MapSubst2(a, m.get(a), b, m.get(b), kStruct));
                    }
                    default:
                        //TODO build key filter to sub-map only the applicable keys
                        return x.transform(new MapSubstN(m, kStruct));
                }
            }
        }

    }


    private static final class MapSubst2 extends MapSubstWithStructFilter {
        final Term ax, ay, bx, by;

        MapSubst2(Term ax, Term ay, Term bx, Term by) {
            this(ax, ay, bx, by, ax.structure() | bx.structure());
        }

        MapSubst2(Term ax, Term ay, Term bx, Term by, int structure) {
            super(structure);
            this.ax = ax;
            this.ay = ay;
            this.bx = bx;
            this.by = by;
//            if ((ay.equals(bx)) || (by.equals(ax))) {
//                throw new TermException(MapSubst2.class.getSimpleName() + " interlock", $.p(ax, ay, bx, by));
//            }
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

    public static class MapSubstN extends MapSubstWithStructFilter {
        private final Map<? extends Term, Term> xy;

        public MapSubstN(Map < ? extends Term, Term > xy, int structure){
            super(structure);
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

//        if (from.equals(to))
//            throw new TermTransformException(from, to, "pointless substitution");

        return from instanceof Atomic ?
                new SubstAtomic((Atomic) from, to) :
                new SubstCompound((Compound) from, to);

    }

    public MapSubst clear() {
        throw new UnsupportedOperationException();
//        xy.clear();
//        return this;
    }

    final static class SubstCompound implements RecursiveTermTransform {

        private final Compound from;
        private final Term to;
        private final int fromStructure;
        private final int fromVolume;

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        SubstCompound(Compound from, Term to) {
            this.from = from;
            this.fromStructure = from.structure();
            this.fromVolume = from.volume();
            this.to = to;
        }


        @Override
        public @Nullable Term applyCompound(Compound c) {
            if (c.equals(from))
                return to;
            return c.impossibleSubTerm(fromStructure, fromVolume) ? c : RecursiveTermTransform.super.applyCompound(c);
        }

    }

    final static class SubstAtomic implements RecursiveTermTransform {

        private final Atomic from;
        private final Term to;
        private final int fromStructure;

        /**
         * creates a substitution of one variable; more efficient than supplying a Map
         */
        SubstAtomic(Atomic from, Term to) {
            this.from = from;
            this.fromStructure = from.structure();
            this.to = to;
        }

        @Override
        public @Nullable Term applyAtomic(Atomic x) {
            return x.equals(from) ? to : x;
        }

        @Override
        public @Nullable Term applyCompound(Compound x) {
            return
                x.impossibleSubStructure(fromStructure) ?
                //!x.containsRecursively(from) ?
                    x : x.transform(this);
        }

    }

}
