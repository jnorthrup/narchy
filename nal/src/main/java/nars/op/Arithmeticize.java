package nars.op;

import jcog.Paper;
import jcog.Util;
import jcog.data.list.FasterIntArrayList;
import jcog.data.list.FasterList;
import jcog.decide.Roulette;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoizers;
import nars.*;
import nars.eval.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.atom.Atom;
import nars.term.atom.Int;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.Set;
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

//    private static final Logger logger = LoggerFactory.getLogger(Arithmeticize.class);

    private static final int minInts = 2;

    final static Variable A = $.varDep("A_"), B = $.varDep("B_");

    private static final Function<Atom, Functor> ArithFunctors = Map.of(
        MathFunc.add.term, MathFunc.add,
        MathFunc.mul.term, MathFunc.mul,
        Equal.the.term, Equal.the,
        Equal.cmp.term, Equal.cmp
    )::get;

    public static class ArithmeticIntroduction extends Introduction {

        public ArithmeticIntroduction(NAR n, int capacity) {
            super(n, capacity);
        }

        @Override
        protected boolean filter(Term x) {
            return
                    x.hasAny(Op.INT) &&
                            x.complexity() >= 3 &&
                            nar.termVolumeMax.intValue() >= x.volume() + 6 /* for && equals(x,y)) */;
        }

        @Override
        protected float pri(Task t) {
//            if (t instanceof SignalTask)
//                return Float.NaN; //dont apply to signal names directly

            float p = super.pri(t);
            Term tt = t.term();
            int numInts = tt.intifyRecurse((n, sub) -> sub.op() == INT ? n + 1 : n, 0);

            assert (numInts > 0);
            if (numInts < 2)
                return Float.NaN;


            float intTerms = numInts / ((float) tt.volume());
            return p * intTerms;
        }

        @Override
        @Nullable
        protected Term newTerm(Task xx) {
            return Arithmeticize.apply(xx.term(), null, nar.termVolumeMax.intValue(), true, nar.random());
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

        int cdt = eternal ? DTERNAL : 0;

        //pre-evaluate using the arith operators; ignore other operators (unless they are already present, ex: member)
        //Term xx = Evaluation.solveFirst(x, ArithFunctors);
        Set<Term> xx = Evaluation.eval(x, true, false, ArithFunctors);
        int xxs = xx.size();

        if (xxs == 1) {
            Term xxx = xx.iterator().next();
            if (!xxx.hasAny(INT))
                return x;
            else
                x = xxx;
        } else if (xxs > 1) {
            Term xxx = CONJ.the(cdt, xx);
            if (!xxx.hasAny(INT))
                return x;
            else
                x = xxx;
        }

        IntHashSet ints = new IntHashSet(4);
        x.recurseTerms(t->t.hasAny(Op.INT), t -> {
            if (anon!=null && t instanceof Anom) {
                t = anon.get(t);
            }
            if (t instanceof Int) {
                ints.add(((Int) t).id);
            }
            return true;
        }, null);

        int ui = ints.size();
        if (ui < minInts)
            return x; 

        ArithmeticOp[] mmm = mods(ints);

        Term y = mmm[ Roulette.selectRoulette(mmm.length, c -> mmm[c].score, random) ]
                    .apply(x, cdt, anon);

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

    static final Function<IntArrayListCached,ArithmeticOp[]> cached;
    static {
        HijackMemoize<IntArrayListCached,ArithmeticOp[]>
                modsCache = new HijackMemoize<>(Arithmeticize::_mods, 512, 3);
        cached = Memoizers.the.add(Arithmeticize.class.getSimpleName() + "_mods", modsCache);
    }

    static ArithmeticOp[] mods(IntHashSet ii) {
        return cached.apply(new IntArrayListCached(ii.toSortedArray()));
    }

    static ArithmeticOp[] _mods(IntArrayListCached iii) {
        
        

        int[] ii = iii.toArray();

        FasterList<ArithmeticOp> ops = new FasterList();
        IntObjectHashMap<FasterList<Pair<Term, Function<Term,Term>>>> eqMods = new IntObjectHashMap<>(ii.length);

        for (int aIth = 0; aIth < ii.length; aIth++) {
            int a = ii[aIth];
            for (int bIth = 0; bIth < ii.length; bIth++) {
                if (aIth == bIth) continue;

                int b = ii[bIth];


                int BMinA = b - a;
                if (a == -b) {
                    
                    maybe(eqMods, a).add(pair(
                            Int.the(b), v-> $.func(MathFunc.mul, v,Int.NEG_ONE)
                    ));



                } else if (a!=0 && Math.abs(a)!=1 && b!=0 && Math.abs(b)!=1 && Util.equals(b/a, (float)b /a)) {

                    
                    maybe(eqMods, a).add(pair(
                            Int.the(b), v->$.func(MathFunc.mul, v, $.the(b/a))
                    ));
                } else if (a < b) {

                    maybe(eqMods, a).add(pair(
                            Int.the(b), v-> $.func(MathFunc.add, v, $.the(BMinA))
                    ));

                } else if (b < a) {

                    maybe(eqMods, b).add(pair(
                            Int.the(a), v-> $.func(MathFunc.add, v, $.the(a - b))
                    ));
                }

                if (a < b) {

                    ops.add(new CompareOp(a, b));
                }
            }
        }

        eqMods.keyValuesView().forEach((kv)->{
            assert(!kv.getTwo().isEmpty());
            ops.add(new BaseEqualExpressionArithmeticOp(kv.getOne(), kv.getTwo().toArrayRecycled(Pair[]::new)));
        });

        return ops.isEmpty() ? ArithmeticOp.EmptyArray : ops.toArray(ArithmeticOp.EmptyArray);
    }

    private static FasterList<Pair<Term, Function<Term, Term>>> maybe(IntObjectHashMap<FasterList<Pair<Term, Function<Term, Term>>>> mods, int ia) {
        return mods.getIfAbsentPut(ia, FasterList::new);
    }




    abstract static class ArithmeticOp  {
        public static final ArithmeticOp[] EmptyArray = new ArithmeticOp[0];
        public final float score;

        protected ArithmeticOp(float score) {
            this.score = score;
        }

        abstract Term apply(Term x, int cdt, @Nullable Anon anon);
    }

    static class BaseEqualExpressionArithmeticOp extends ArithmeticOp  {
        final int base;
        private final Pair<Term, Function<Term, Term>>[] mods;

        BaseEqualExpressionArithmeticOp(int base, Pair<Term, Function<Term, Term>>[] mods) {
            super(mods.length);
            this.base = base;
            this.mods = mods;
        }

        @Override
        public Term apply(Term x, int cdt, @Nullable Anon anon) {

            Term baseTerm = Int.the(base);
            if (anon!=null)
                baseTerm = anon.put(baseTerm);

            Term yy = x.replace(baseTerm, A);

            for (Pair<Term, Function<Term, Term>> s : mods) {
                Term s0 = s.getOne();
                Term s1 = s.getTwo().apply(A);
                if (anon!=null)
                    s0 = anon.put(s0);
                yy = yy.replace(s0, s1);
            }

            Term equality =
                    //SIM.the(baseTerm, V);
                    $.func(Equal.the, Terms.sorted(baseTerm, A));

            Term y = CONJ.the(equality, cdt, yy);

            if (y.op()!=CONJ) return null;
            return y;
        }
    }

    private static class CompareOp extends ArithmeticOp {
        private final int a, b;

        public CompareOp(int a, int b) {
            super(1);
            this.a = a;
            this.b = b;
        }

        @Override
        Term apply(Term x, int cdt, @Nullable Anon anon) {
            //TODO anon
            Int aaa = Int.the(a), bbb = Int.the(b);
            Term xx = x.replace(Map.of(aaa, A, bbb, B));
            return CONJ.the(cdt, xx, $.func(Equal.cmp, A, B, $.the(-1)));
        }
    }
}
