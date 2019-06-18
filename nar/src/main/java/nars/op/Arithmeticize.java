package nars.op;

import jcog.Paper;
import jcog.Util;
import jcog.data.list.FasterIntArrayList;
import jcog.data.list.FasterList;
import jcog.decide.Roulette;
import jcog.memoize.Memoizers;
import nars.*;
import nars.eval.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.atom.Atom;
import nars.term.atom.Int;
import nars.term.util.transform.MapSubst;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static nars.Op.CONJ;
import static nars.Op.INT;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * introduces arithmetic relationships between differing numeric subterms
 * responsible for showing the reasoner mathematical relations between
 * numbers appearing in compound terms.
 * <p>
 * TODO
 * greater/less than comparisons
 */
@Paper
public class Arithmeticize {

//    private static final Logger logger = LoggerFactory.getLogger(Arithmeticize.class);

    private static final int minInts = 2;

    private final static Variable A = $.varDep("A_"), B = $.varDep("B_");

    private static final Function<Atom, Functor> ArithFunctors = Map.of(
            MathFunc.add.term, MathFunc.add,
            MathFunc.mul.term, MathFunc.mul,
            Equal.equal.term, Equal.equal,
            Cmp.cmp.term, Cmp.cmp
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
                            nar.termVolMax.intValue() >= x.volume() + 6 /* for && equals(x,y)) */;
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


            float intTerms = Util.unitize(numInts / (((float) tt.volume() - numInts)));
            return p * intTerms;
        }

        @Override
        @Nullable
        protected Term newTerm(Task xx) {
            return Arithmeticize.apply(xx.term(), null, nar.termVolMax.intValue(), nar.random());
        }
    }

    @Deprecated
    public static Term apply(Term x, Random random) {
        return apply(x, null, NAL.term.COMPOUND_VOLUME_MAX, random);
    }


    public static Term apply(Term x, @Nullable Anon anon, int volMax, Random random) {
        if (anon == null && !x.hasAny(INT))
            return null;

        int cdt = DTERNAL; //eternal ? DTERNAL : 0;

        //pre-evaluate using the arith operators; ignore other operators (unless they are already present, ex: member)
        //Term xx = Evaluation.solveFirst(x, ArithFunctors);
        Set<Term> xx = Evaluation.eval(x, true, false, ArithFunctors);
        int xxs = xx.size();

        if (xxs == 1) {
            Term xxx = xx.iterator().next();
            if (!xxx.hasAny(INT))
                return null;
            else
                x = xxx;
        } else if (xxs > 1) {
            Term xxx = CONJ.the(cdt, xx);
            if (!xxx.hasAny(INT))
                return null;
            else
                x = xxx;
        }

        IntHashSet ints = new IntHashSet(4);
        x.recurseTerms(t -> t.hasAny(Op.INT), t -> {
            if (anon != null && t instanceof Anom) {
                t = anon.get(t);
            }
            if (t instanceof Int) {
                ints.add(((Int) t).i);
            }
            return true;
        }, null);

        int ui = ints.size();
        if (ui < minInts)
            return null;

        Term y = mods(ints)[
                Roulette.selectRoulette(mods(ints).length, c -> mods(ints)[c].score, random)
                ].apply(x, cdt, anon);

        if (y == null || y.volume() > volMax) return null;

//        Term y = IMPL.the(equality, eternal ? DTERNAL : 0, yy);
//        if (y.op()!=IMPL) return null;

        if (x.isNormalized()) {
            y = y.normalize();
        }
        return y;
    }

    final static class IntArrayListCached extends FasterIntArrayList {
        private final int hash;

        IntArrayListCached(int[] ii) {
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
        public boolean equals(Object otherList) {
            return this==otherList || (hash==((IntArrayListCached)otherList).hash && super.equals(otherList));
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final Function<IntArrayListCached, ArithmeticOp[]> cached;

    static {
        cached = Memoizers.the.memoize(Arithmeticize.class.getSimpleName() + "_mods", 8 * 1092, Arithmeticize::_mods);
    }

    private static ArithmeticOp[] mods(IntHashSet ii) {
        return cached.apply(new IntArrayListCached(ii.toSortedArray()));
    }

    private static ArithmeticOp[] _mods(IntArrayListCached iii) {


        int[] ii = iii.toArray();

        FasterList<ArithmeticOp> ops = new FasterList();
        IntObjectHashMap<FasterList<Pair<Term, Function<Term, Term>>>> eqMods = new IntObjectHashMap<>(ii.length);

        for (int bIth = 0; bIth < ii.length; bIth++) {
            int b = ii[bIth];
            for (int aIth = bIth + 1; aIth < ii.length; aIth++) {
                int a = ii[aIth];

                //if (aIth == bIth) continue;
                assert (b < a);

                if (a == -b) {

                    maybe(eqMods, a).add(pair(
                            Int.the(b), v -> $.func(MathFunc.mul, v, Int.NEG_ONE)
                    ));


                } else{
                    if (a != 0 && Math.abs(a) != 1 && b != 0 && Math.abs(b) != 1 && Util.equals(b / a, (float) b / a)) {


                        maybe(eqMods, a).add(pair(
                                Int.the(b), v -> $.func(MathFunc.mul, v, $.the(b / a))
                        ));
                    }

//                    int BMinA = b - a;
//                    maybe(eqMods, a).add(pair(
//                            Int.the(b), v -> $.func(MathFunc.add, v, $.the(BMinA))
//                    ));
                    int AMinB = a - b;
                    maybe(eqMods, b).add(pair(
                            Int.the(a), v -> $.func(MathFunc.add, v, $.the(AMinB))
                    ));

                }


//                } else if (b < a) {
//
//                    maybe(eqMods, b).addAt(pair(
//                            Int.the(a), v-> $.func(MathFunc.addAt, v, $.the(a - b))
//                    ));

                ops.add(new CompareOp(b, a));

            }
        }

        eqMods.keyValuesView().forEach((kv) -> {
            assert (!kv.getTwo().isEmpty());
            ops.add(new BaseEqualExpressionArithmeticOp(kv.getOne(), kv.getTwo().toArrayRecycled(Pair[]::new)));
        });

        return ops.isEmpty() ? ArithmeticOp.EmptyArray : ops.toArray(ArithmeticOp.EmptyArray);
    }

    private static FasterList<Pair<Term, Function<Term, Term>>> maybe(IntObjectHashMap<FasterList<Pair<Term, Function<Term, Term>>>> mods, int ia) {
        return mods.getIfAbsentPut(ia, FasterList::new);
    }


    abstract static class ArithmeticOp {
        public static final ArithmeticOp[] EmptyArray = new ArithmeticOp[0];
        public final float score;

        ArithmeticOp(float score) {
            this.score = score;
        }

        abstract Term apply(Term x, int cdt, @Nullable Anon anon);
    }

    static class BaseEqualExpressionArithmeticOp extends ArithmeticOp {
        final int base;
        private final Pair<Term, Function<Term, Term>>[] mods;

        BaseEqualExpressionArithmeticOp(int base, Pair<Term, Function<Term, Term>>[] mods) {
            super(mods.length);
            this.base = base;
            this.mods = mods;
        }

        @Override
        public Term apply(Term x, int cdt, @Nullable Anon anon) {

            Term var = x.hasAny(A.op()) ? A : $.v(A.op(), (byte) 1); //safe to use normalized var?

            Term baseTerm = Int.the(base);
            if (anon != null)
                baseTerm = anon.put(baseTerm);

            Term yy = x.replace(baseTerm, var);

            for (Pair<Term, Function<Term, Term>> s : mods) {
                Term s0 = s.getOne();
                Term s1 = s.getTwo().apply(var);
                if (anon != null)
                    s0 = anon.put(s0);
                yy = yy.replace(s0, s1);
            }

            if (baseTerm.equals(var))
                return null;

            Term equality =
                    //SIM.the(baseTerm, V);
                    Equal.the(baseTerm, var);

            Term y = CONJ.the(equality, cdt, yy);

            return y.op() != CONJ ? null : y;
        }
    }

    private static class CompareOp extends ArithmeticOp {
        private final int a, b;

        CompareOp(int a, int b) {
            super(1);
            assert (a < b);
            this.a = a;
            this.b = b;
        }

        @Override
        Term apply(Term x, int cdt, @Nullable Anon anon) {
            //TODO anon
            Term cmp = Equal.cmp(A, B, -1);
            if (cmp == Null) return null;
            Int aaa = Int.the(a), bbb = Int.the(b);
            Term xx = new MapSubst.MapSubstN(Map.of(aaa, A, bbb, B), INT.bit).apply(x);
            if (xx == Null) return null; //HACK
            return CONJ.the(cdt, xx, cmp);
        }
    }

}
