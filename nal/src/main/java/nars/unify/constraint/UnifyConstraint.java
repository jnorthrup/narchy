package nars.unify.constraint;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/** must be stateless */
public abstract class UnifyConstraint<U extends Unify> extends AbstractPred<U> {

    private static final Map<Term, UnifyConstraint> constra = new ConcurrentHashMap<>();

    public static final UnifyConstraint[] None = new UnifyConstraint[0];

    public static UnifyConstraint intern(UnifyConstraint x) {
        UnifyConstraint y = constra.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

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


    public final Variable x;


    UnifyConstraint(Variable x, String func, @Nullable Term... args) {
        this(x, Atomic.atom(func), args);
    }

    UnifyConstraint(Variable x, Term func, @Nullable Term... args) {
        super(args!=null && args.length > 0 ?
            $.impl( x, $.p(func, $.pOrOnly(args))) : $.impl( x, func));
        this.x = x;
    }



    /**
     * @param targetVariable current value of the target variable (null if none is setAt)
     * @param potentialValue potential value to assign to the target variable
     * @param f              match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    abstract public boolean invalid(Term x, U f);

    /**
     * returns a stream of constraints bundled by any multiple respective targets, and sorted by cost increasing
     */
    public static <U extends Unify> UnifyConstraint<U>[] the(Stream<UnifyConstraint<U>> c) {
        return c.collect(Collectors.groupingBy(x -> x.x, Collectors.toCollection(FasterList::new))).values().stream()
            .map(CompoundConstraint::the)
            .sorted(PREDICATE.sortByCostIncreasing)
            .map(UnifyConstraint::intern)
            .toArray(x -> x == 0 ? UnifyConstraint.None : new UnifyConstraint[x]);
    }

    /** TODO group multiple internal relationconstraints for the same target so only one xy(target) lookup invoked to use with all */
    public static final class CompoundConstraint<U extends Unify> extends UnifyConstraint<U> {

        private final UnifyConstraint<U>[] subConstraint;
        private final float cost;

        public static <U extends Unify> UnifyConstraint<U> the(List<UnifyConstraint<U>> cc) {

            int ccn = cc.size();

            if (ccn == 0)
                throw new UnsupportedOperationException();
            else if (ccn == 1)
                return cc.get(0);

            nextX: for (int i = 0, ccSize = cc.size(); i < ccSize; i++) {
                UnifyConstraint x = cc.get(i);
                for (UnifyConstraint y : cc) {
                    if (x != y) {
                        if (x instanceof RelationConstraint && y instanceof RelationConstraint) {
                            RelationConstraint X = (RelationConstraint) x;
                            RelationConstraint Y = (RelationConstraint) y;
                            if (X.x.equals(Y.x) && X.y.equals(Y.y)) {
                                if (!X.remainInAndWith(Y)) {
                                    cc.set(i, null);
                                    continue nextX;
                                }
                            }
                        }
                    }
                }
            }
            if (((FasterList)cc).removeNulls()) {
                ccn = cc.size();
                if (ccn == 0)
                    throw new UnsupportedOperationException();
                else if (ccn == 1)
                    return cc.get(0);
            }

            UnifyConstraint[] d = cc.toArray(new UnifyConstraint[ccn]);
            Arrays.sort(d, PREDICATE.sortByCostIncreasing);

            if (NAL.test.DEBUG_EXTRA) {
                final Term target = d[0].x;
                for (int i = 1; i < d.length; i++)
                    assert (d[i].x.equals(target));
            }

            return new CompoundConstraint<>(d);
        }

        static final Atom AND = Atomic.atom("&&");

        private CompoundConstraint(UnifyConstraint[] c) {
            super(c[0].x, AND, Op.SETe.the(Util.map(
                //extract the unique UnifyIf parameter
                cc -> $.pOrOnly(
                    cc.sub(1)
                    //cc.sub(0).subterms().subRangeArray(1, Integer.MAX_VALUE)
                    ), new Term[c.length], c)
            ));
            this.subConstraint = c;
            this.cost = Util.sum((FloatFunction<AbstractPred<U>>) PREDICATE::cost, subConstraint);
        }

        @Override
        public float cost() {
            return cost;
        }

        @Override
        public boolean invalid(Term x, U f) {
            for (UnifyConstraint<U> c : subConstraint)
                if (c.invalid(x, f))
                    return true;

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