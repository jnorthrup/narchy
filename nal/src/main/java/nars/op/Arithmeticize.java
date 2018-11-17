package nars.op;

import jcog.Paper;
import jcog.Util;
import jcog.data.list.FasterIntArrayList;
import jcog.data.list.FasterList;
import jcog.decide.Roulette;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoizers;
import nars.*;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.atom.Int;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static nars.Op.CONJ;
import static nars.Op.INT;
import static nars.time.Tense.DTERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * introduces arithmetic relationships between differing numeric subterms
 * responsible for showing the reasoner mathematical relations between
 * numbers appearing in compound terms.
 *
 * TODO
 *      greater/less than comparisons
 *
 *
 */
@Paper
public class Arithmeticize {

    private static int minInts = 2;

    final static Variable V = $.varDep("A_");

    public static class ArithmeticIntroduction extends Introduction {
        public ArithmeticIntroduction(int taskCapacity, NAR n) {
            super(taskCapacity, n);
        }

        @Override
        protected boolean filter(Term x) {
            return
                    x.hasAny(Op.INT) &&
                            x.complexity() >= 3 &&
                            nar.termVolumeMax.intValue() >= x.volume() + 5 /* for &&equals(x,y)) */;
        }

        @Override
        protected float pri(Task t) {
            float p = super.pri(t);
            Term tt = t.term();
            int numInts = tt.intifyRecurse((n, sub) -> sub.op() == INT ? n + 1 : n, 0);
//        if (numInts == 0) {
//            tt.intifyRecurse((n, sub) -> sub.op() == INT ? n + 1 : n, 0);
//            throw new WTF();
//        }
            assert (numInts > 0);
            if (numInts < 2)
                return Float.NaN;


            float intTerms = numInts / ((float) tt.volume());
            return p * intTerms;
        }

        @Override
        @Nullable
        protected Term newTerm(Task xx) {
            return Arithmeticize.apply(xx.term(), null, nar.termVolumeMax.intValue(), xx.isEternal(), nar.random());
        }
    }

    public static Term apply(Term x, Random random) {
        return apply(x, true, random);
    }

    @Deprecated public static Term apply(Term x, boolean eternal, Random random) {
        return apply(x, null, Param.COMPOUND_VOLUME_MAX, eternal, random);
    }


    public static Term apply(Term x, @Nullable Anon anon, int volMax, boolean eternal, Random random) {
        if (anon == null && !x.hasAny(INT))
            return x;


        IntHashSet ints = new IntHashSet(4);
        x.recurseTerms(t->t.hasAny(Op.INT), t -> {
            if (anon!=null && t instanceof Anom) {
                t = anon.get(t);
            }
            if (t instanceof Int) {
                ints.add(((Int) t).id);
            }
            return true;
        }, x);

        int ui = ints.size();
        if (ui < minInts)
            return x; 

        int[] ii = ints.toSortedArray();  

        List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>> mmm = mods(ii);

        int choice = Roulette.selectRoulette(mmm.size(), c -> mmm.get(c).getTwo().size(), random);

        IntObjectPair<List<Pair<Term, Function<Term, Term>>>> m = mmm.get(choice);

        Term baseTerm = Int.the(m.getOne());
        if (anon!=null)
            baseTerm = anon.put(baseTerm);



        Term yy = x.replace(baseTerm, V);

        for (Pair<Term, Function<Term, Term>> s : m.getTwo()) {
            Term s0 = s.getOne();
            Term s1 = s.getTwo().apply(V);
            if (anon!=null)
                s0 = anon.put(s0); 
            yy = yy.replace(s0, s1);
        }

        Term equality =
                //SIM.the(baseTerm, V);
                $.func(Equal.the, Terms.sorted(baseTerm, V));

        Term y = CONJ.the(equality, eternal ? DTERNAL : 0, yy);
        if (y.op()!=CONJ) return null;
        if (y.volume() > volMax) return null;

//        Term y = IMPL.the(equality, eternal ? DTERNAL : 0, yy);
//        if (y.op()!=IMPL) return null;

        if (x.isNormalized()) {
            y = y.normalize();
        }
        return y;
    }

    final static class IntArrayListCached extends FasterIntArrayList {
        private final int hash;

        public IntArrayListCached(int[] ii) {
            super(ii);
            int hash = ii[0];
            for (int i = 1; i < ii.length; i++)
                hash = Util.hashCombine(hash, ii[i]);
            this.hash = hash;
        }

        public int[] toArray() {
            return items;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    static final Function<IntArrayListCached,List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>>> cached;
    static {
        HijackMemoize<IntArrayListCached,List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>>>
                modsCache = new HijackMemoize<>(Arithmeticize::_mods, 512, 3);
        cached = Memoizers.the.add(Arithmeticize.class.getSimpleName() + "_mods", modsCache);
    }

    static List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>> mods(int[] ii) {
        return cached.apply(new IntArrayListCached(ii));
    }

    static List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>> _mods(IntArrayListCached iii) {
        
        

        int[] ii = iii.toArray();

        IntObjectHashMap<List<Pair<Term, Function<Term,Term>>>> mods = new IntObjectHashMap<>(ii.length);


        
        for (int a = 0; a < ii.length; a++) {
            int ia = ii[a];
            for (int b = 0; b < ii.length; b++) {
                if (a == b) continue;

                int ib = ii[b];


                int BMinA = ib - ia;
                if (ia == -ib) {
                    
                    maybe(mods, ia).add(pair(
                            Int.the(ib), v-> $.func(MathFunc.mul, v,Int.NEG_ONE)
                    ));



                } else if (ia!=0 && Math.abs(ia)!=1 && ib!=0 && Math.abs(ib)!=1 && Util.equals(ib/ia, (float)ib /ia, Float.MIN_NORMAL)) {

                    
                    maybe(mods, ia).add(pair(
                            Int.the(ib), v->$.func(MathFunc.mul, v, $.the(ib/ia))
                    ));
                } else if (ia < ib) { 

                    maybe(mods, ia).add(pair(
                            Int.the(ib), v-> $.func(MathFunc.add, v, $.the(BMinA))
                    ));

                } else if (ib < ia) {
                    maybe(mods, ib).add(pair(
                            Int.the(ia), v-> $.func(MathFunc.add, v, $.the(ia - ib))
                    ));

                }









            }
        }
        return !mods.isEmpty() ? mods.keyValuesView().toList() : List.of();
    }

    public static List<Pair<Term, Function<Term, Term>>> maybe(IntObjectHashMap<List<Pair<Term, Function<Term, Term>>>> mods, int ia) {
        return mods.getIfAbsentPut(ia, FasterList::new);
    }

    public static final Logger logger = LoggerFactory.getLogger(Arithmeticize.class);





}
