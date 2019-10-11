package nars.op;

import jcog.Paper;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.decide.Roulette;
import jcog.memoize.Memoizers;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.attention.What;
import nars.eval.Evaluation;
import nars.eval.Evaluator;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.anon.Anon;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.transform.MapSubst;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static nars.Op.CONJ;
import static nars.Op.INT;
import static nars.term.atom.Bool.Null;
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

    private static final Function<IntArrayListCached, ArithmeticOp[]> cached;

    static {
        cached = Memoizers.the.memoize(Arithmeticize.class.getSimpleName() + "_mods",
            16 * 1024, Arithmeticize::_mods);
    }

    private final static Variable A = $.varDep("A_"), B = $.varDep("B_");
    private static final Op Aop = A.op();

    private static final Function<Atom, Functor> ArithFunctors = Map.of(
            MathFunc.add, MathFunc.add,
            MathFunc.mul, MathFunc.mul,
            Equal.equal, Equal.equal,
            Cmp.cmp, Cmp.cmp
    )::get;

    public static class ArithmeticIntroduction extends EventIntroduction {

        /** rate at which input is pre-evaluated.  TODO make FloatRange etc */
        private float preEvalRate = 0.5f;

        static final int VOLUME_MARGIN = 6;

        public ArithmeticIntroduction() {
            super();
            //hasAny(TheTask, Op.INT);
            //TODO vol >= 3

        }


        @Override
        protected boolean filter(Term x) {
            return
                    x.hasAny(Op.INT) &&
                            x.complexity() >= 3 &&
                            volMax >= x.volume() + VOLUME_MARGIN /* for && equals(x,y)) */;
        }

//        @Override
//        protected float pri(Task t) {
////            if (t instanceof SignalTask)
////                return Float.NaN; //dont apply to signal names directly
//
//            float p = super.pri(t);
//            Term tt = t.term();
//            int numInts = tt.intifyRecurse((n, sub) -> sub.op() == INT ? n + 1 : n, 0);
//
//            assert (numInts > 0);
//            if (numInts < 2)
//                return Float.NaN;
//
//
//            float intTerms = Util.unitize(numInts / (((float) tt.volume() - numInts)));
//            return p * intTerms;
//        }

        @Override
        protected Term applyUnnormalized(Term x, int volMax, What w) {
            Random random = w.random();
            return Arithmeticize.apply(x, null, volMax, random.nextFloat() < (preEvalRate / x.volume()), random);
        }
    }

    @Deprecated
    public static Term apply(Term x, Random random) {
        return apply(x, null, NAL.term.COMPOUND_VOLUME_MAX, true, random);
    }

    private static final ThreadLocal<Evaluator> evaluator = ThreadLocal.withInitial(() ->
        new Evaluator(ArithFunctors)
    );

    public static Term apply(Term x, @Nullable Anon anon, int volMax, boolean preEval, Random random) {
        if (!x.hasAny(INT))
            return null;

        //pre-evaluate using the arith operators; ignore other operators (unless they are already present, ex: member)
        //Term xx = Evaluation.solveFirst(x, ArithFunctors);
        if (preEval) {
             Set<Term> xx = Evaluation.eval(x, true, false, evaluator.get());
            if (!xx.isEmpty()) {
                xx.removeIf(z -> !z.hasAny(INT));

                int xxs = xx.size();
                if (xxs == 1) {
                    Term xxx = xx.iterator().next();
                    if (xxx.hasAny(INT))
                        x = xxx;
                } else if (xxs > 1) {
                    if (Util.sum(Termlike::volume, xx) < volMax - 1) {
                        Term xxx = CONJ.the(xx);
                        if (xxx.hasAny(INT))
                            x = xxx;
                    }
                }
            }
        }

        IntHashSet ints = new IntHashSet(4);
        x.recurseTerms(t -> t.hasAny(Op.INT), t -> {
            if (t instanceof Int)
                ints.add(((Int) t).i);
            return true;
        }, null);

        int ui = ints.size();
        if (ui < minInts)
            return null;

        ArithmeticOp[] mm = mods(ints);
        Term y = mm[
                    Roulette.selectRoulette(mm.length, c -> mm[c].score, random)
                ].apply(x, anon);

        if (y == null || y instanceof Bool || y.volume() > volMax) return null;

//        Term y = IMPL.the(equality, eternal ? DTERNAL : 0, yy);
//        if (y.op()!=IMPL) return null;

        return x.isNormalized() ? y.normalize() : y;
    }

    final static class IntArrayListCached  {
        private final int hash;
        private final int[] val;

        IntArrayListCached(int[] ii) {
            int hash = ii[0];
            for (int i = 1; i < ii.length; i++)
                hash = Util.hashCombine(hash, ii[i]);
            this.hash = hash;
            this.val = ii;
        }

        @Override
        public boolean equals(Object otherList) {
            return this==otherList ||
                (hash==((IntArrayListCached)otherList).hash && Arrays.equals(val, ((IntArrayListCached)otherList).val));
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }


    private static ArithmeticOp[] mods(IntHashSet ii) {
        return cached.apply(new IntArrayListCached(ii.toSortedArray()));
    }


    private static ArithmeticOp[] _mods(IntArrayListCached iii) {


        int[] ii = iii.val;

        FasterList<ArithmeticOp> ops = new FasterList(2);
        IntObjectHashMap<FasterList<Pair<Term, Function<Term, Term>>>> eqMods = new IntObjectHashMap<>(ii.length);

        for (int bIth = 0; bIth < ii.length; bIth++) {
            int b = ii[bIth];
            for (int aIth = bIth + 1; aIth < ii.length; aIth++) {
                int a = ii[aIth];


                //assert (b < a);

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

        abstract Term apply(Term x, @Nullable Anon anon);
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
        public Term apply(Term x, @Nullable Anon anon) {


            Term baseTerm = Int.the(base);
            if (anon != null)
                baseTerm = anon.put(baseTerm);

            Term var = x.hasAny(Aop) ?
                A :
                $.v(Aop, (byte) 1); //optimistic prenormalization

            Term yy = x.replace(baseTerm, var);

            for (Pair<Term, Function<Term, Term>> s : mods) {
                Term s0 = s.getOne();
                if (anon != null)
                    s0 = anon.put(s0);
                Term s1 = s.getTwo().apply(var);
                yy = yy.replace(s0, s1);
                if (yy == Null)
                    return Null; //HACK
            }

            if (baseTerm.equals(var))
                return null;

            Term equality =
                    //SIM.the(baseTerm, V);
                    Equal.the(baseTerm, var);

            Term y = CONJ.the(equality, yy);

            return y.op() != CONJ ? null : y;
        }
    }

    private static class CompareOp extends ArithmeticOp {
        private final int a, b;

        CompareOp(int smaller, int bigger) {
            super(1);
            //assert (a < b);
            this.a = smaller;
            this.b = bigger;
        }

        @Override
        Term apply(Term x, @Nullable Anon anon) {
            //TODO anon
            Term cmp = !x.hasAny(Aop) ?
                Equal._cmp($.varDep(1), $.varDep(2), -1) : //optimistic prenormalization
                Equal._cmp(A, B, -1);
//            if (cmp == Null) return null;

            Term xx = x.transform(new MapSubst.MapSubstN(Map.of(Int.the(a), A, Int.the(b), B), INT.bit));

            return (xx instanceof Bool) ? null : CONJ.the(xx, cmp);
        }
    }

}
