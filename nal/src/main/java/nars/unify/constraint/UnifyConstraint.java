package nars.unify.constraint;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import jcog.Util;
import nars.$;
import nars.NAL;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Stream;


/** must be stateless */
public abstract class UnifyConstraint<U extends Unify> extends AbstractPred<U> {

//    public static boolean valid(Term x, @Nullable Versioned<MatchConstraint> c, Unify u) {
//        return c == null || !c.anySatisfyWith((cc,X) -> cc.invalid(X, u), x);
//    }

    @Override
    public final boolean test(U p) {
        p.constrain(this);
        return true;
    }

    /**
     * cost of testing this, for sorting. higher value will be tested later than lower
     */
    @Override
    abstract public float cost();


    private final static Atomic UnifyIf = Atomic.the("unifyIf");
    public final Variable x;

    UnifyConstraint(Term id, Variable x) {
        this((Variable)x, (Term)id, null);
    }

    UnifyConstraint(Variable x, String func, @Nullable Term... args) {
        this(x, Atomic.atom(func), args);
    }

    UnifyConstraint(Variable x, Term func, @Nullable Term... args) {
        super($.funcFast(UnifyIf, x, args!=null ? $.inh($.p(args),func) : func));
        this.x = x;
    }



    /**
     * @param targetVariable current value of the target variable (null if none is setAt)
     * @param potentialValue potential value to assign to the target variable
     * @param f              match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    abstract public boolean invalid(Term y, U f);

    private static final class CompoundConstraint<U extends Unify> extends UnifyConstraint<U> {
        static final MultimapBuilder.ListMultimapBuilder matchConstraintMapBuilder = MultimapBuilder.hashKeys(4).arrayListValues(4);

        private final UnifyConstraint<U>[] cache;

        private CompoundConstraint(UnifyConstraint[] c) {
            super($.funcFast(UnifyIf, c[0].x, $.sFast(c)), c[0].x);
            this.cache = c;
        }

        /**
         * groups the constraints into their respective targets
         */
        private static Stream<UnifyConstraint> the(Stream<UnifyConstraint> c) {
            ListMultimap<Term, UnifyConstraint> m = matchConstraintMapBuilder.build();
            c.forEach(x -> m.put(x.x, x));
            return m.asMap().values().stream().map(cc -> {
                int ccn = cc.size();

                assert (ccn > 0);
                if (ccn == 1) {
                    return cc.iterator().next();
                } else {
                    UnifyConstraint<?>[] d = cc.toArray(new UnifyConstraint[ccn]);
                    Arrays.sort(d, PREDICATE.sortByCostIncreasing);

                    if (NAL.test.DEBUG_EXTRA) {
                        final Term target = d[0].x;
                        for (int i = 1; i < d.length; i++)
                            assert (d[i].x.equals(target));
                    }

                    return new CompoundConstraint(d);
                }
            });

        }

        @Override
        public float cost() {
            return Util.sum((FloatFunction<AbstractPred<U>>) PREDICATE::cost, cache);
        }

        @Override
        public boolean invalid(Term y, Unify f) {
            for (UnifyConstraint c : cache) {
                if (c.invalid(y, f))
                    return true;
            }
            return false;
        }


    }
}
//    public static MatchConstraint[] combineConstraints(MatchConstraint[] cc) {
//        RoaringBitmap constraints = new RoaringBitmap();
//        for (int i = 0, cl = cc.length; i < cl; i++) {
//            Term x = cc[i];
//                constraints.addAt(i);
//            }
//        }
//        if (constraints.getCardinality() < 2) {
//            return cc;
//        } else {
//
//            List<IntIntPair> ranges = new FasterList<>(1);
//            int start = -1, end = -1;
//            PeekableIntIterator ii = constraints.getIntIterator();
//            while (ii.hasNext()) {
//                int next = ii.next();
//                if (start == -1) {
//                    start = end = next;
//                } else {
//                    if (next == end + 1) {
//                        end++;
//                    } else {
//                        if (end - start >= 1) {
//
//                            ranges.addAt(pair(start, end));
//                        }
//                        start = -1;
//                    }
//                }
//            }
//            if (end - start >= 1)
//                ranges.addAt(pair(start, end));
//
//            if (ranges.size() > 1) throw new TODO();
//            IntIntPair rr = ranges.get(0);
//
//
//            List<PrediTerm<Derivation>> l = new FasterList();
//            int i;
//            for (i = 0; i < start; i++) {
//                l.addAt(a.cond[i]);
//            }
//
//            CompoundConstraint.the(
//                    Util.map(MatchConstraint.class::cast, MatchConstraint[]::new, ArrayUtils.subarray(a.cond, rr.getOne(), rr.getTwo() + 1))
//            ).forEach(l::addAt);
//
//            i = end + 1;
//            for (; i < a.cond.length; i++) {
//                l.addAt(a.cond[i]);
//            }
//            return AndCondition.the((List) l);
//        }
//    }