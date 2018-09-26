package nars.unify.constraint;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import jcog.Util;
import nars.$;
import nars.Param;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static nars.derive.premise.PremiseRuleSource.pp;


/** must be stateless */
public abstract class UnifyConstraint extends AbstractPred<Derivation> {

//    public static boolean valid(Term x, @Nullable Versioned<MatchConstraint> c, Unify u) {
//        return c == null || !c.anySatisfyWith((cc,X) -> cc.invalid(X, u), x);
//    }

    @Override
    public boolean test(Derivation p) {
        return p.constrain(this);
    }

    /**
     * cost of testing this, for sorting. higher value will be tested later than lower
     */
    @Override
    abstract public float cost();


    @Nullable
    public PREDICATE<Derivation> preFilter(Term taskPattern, Term beliefPattern) {
        return null;
    }

    private final static Atomic UnifyIf = Atomic.the("unifyIf");
    public final Term x;

    UnifyConstraint(Term x, Term id) {
        super(id);
        this.x = x;
    }

    protected UnifyConstraint(Term x, String func, @Nullable Term... args) {
        this(x, $.funcFast(UnifyIf, x, args!=null ? $.funcFast(func, args) : $.the(func)));
    }

//    public static MatchConstraint[] combineConstraints(MatchConstraint[] cc) {
//        RoaringBitmap constraints = new RoaringBitmap();
//        for (int i = 0, cl = cc.length; i < cl; i++) {
//            Term x = cc[i];
//                constraints.add(i);
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
//                            ranges.add(pair(start, end));
//                        }
//                        start = -1;
//                    }
//                }
//            }
//            if (end - start >= 1)
//                ranges.add(pair(start, end));
//
//            if (ranges.size() > 1) throw new TODO();
//            IntIntPair rr = ranges.get(0);
//
//
//            List<PrediTerm<Derivation>> l = new FasterList();
//            int i;
//            for (i = 0; i < start; i++) {
//                l.add(a.cond[i]);
//            }
//
//            CompoundConstraint.the(
//                    Util.map(MatchConstraint.class::cast, MatchConstraint[]::new, ArrayUtils.subarray(a.cond, rr.getOne(), rr.getTwo() + 1))
//            ).forEach(l::add);
//
//            i = end + 1;
//            for (; i < a.cond.length; i++) {
//                l.add(a.cond[i]);
//            }
//            return AndCondition.the((List) l);
//        }
//    }

    /**
     * @param targetVariable current value of the target variable (null if none is set)
     * @param potentialValue potential value to assign to the target variable
     * @param f              match context
     * @return true if match is INVALID, false if VALID (reversed)
     */
    abstract public boolean invalid(Term y, Unify f);

    final static class ConstraintAsPredicate extends AbstractPred<Derivation> {

        private final RelationConstraint constraint;

        /** taskterm, beliefterm -> extracted */
        final BiFunction<Term,Term,Term> extractX, extractY;

        ConstraintAsPredicate(RelationConstraint m, byte[] xInTask, byte[] xInBelief, byte[] yInTask, byte[] yInBelief) {
            super($.p(m.ref /*term()*/, $.p(pp(xInTask), pp(xInBelief), pp(yInTask), pp(yInBelief))));
            this.constraint = m;

            if (xInTask!=null && (xInBelief == null || xInTask.length < xInBelief.length))
                extractX = xInTask.length == 0 ? (t,b)->t : (t,b)->t.subPath(xInTask);
            else
                extractX = xInBelief.length == 0 ? (t,b)->b : (t,b)->b.subPath(xInBelief);

            if (yInTask!=null && (yInBelief == null || yInTask.length < yInBelief.length))
                extractY = yInTask.length == 0 ? (t,b)->t : (t,b)->t.subPath(yInTask);
            else
                extractY = yInBelief.length == 0 ? (t,b)->b : (t,b)->b.subPath(yInBelief);
        }

        @Override
        public boolean remainAndWith(PREDICATE[] p) {
            for (PREDICATE pp : p) {
                if (pp!=this && pp instanceof ConstraintAsPredicate)
                    if (!constraint.remainInAndWith(((ConstraintAsPredicate)pp).constraint))
                        return false;
            }
            return true;
        }

        @Override
        public boolean test(Derivation preDerivation) {
            Term t = preDerivation.taskTerm;
            Term b = preDerivation.beliefTerm;
            Term x = extractX.apply(t, b);
            if (x == null)
                return false;
            Term y = extractY.apply(t, b);
            if (y == null)
                return false;
            return !constraint.invalid(x, y);
        }

        @Override
        public float cost() {
            return constraint.cost();
        }
    }

    public static final UnifyConstraint[] EMPTY_UNIFY_CONSTRAINTS = new UnifyConstraint[0];


    public static UnifyConstraint[] the(Set<UnifyConstraint> c) {
        if (c.size() < 2)
            return c.toArray(EMPTY_UNIFY_CONSTRAINTS);
        else
            return CompoundConstraint.the(c.stream()).toArray(UnifyConstraint[]::new);
    }

    static final class CompoundConstraint extends UnifyConstraint {


        static final MultimapBuilder.ListMultimapBuilder matchConstraintMapBuilder = MultimapBuilder.hashKeys(4).arrayListValues(4);

        private final UnifyConstraint[] cache;

        private CompoundConstraint(UnifyConstraint[] c) {
            super(c[0].x, $.funcFast(UnifyIf, c[0].x, $.sFast(c)));
            this.cache = c;

            if (Param.DEBUG) {
                final Term target = c[0].x;
                for (int i = 1; i < c.length; i++)
                    assert (c[i].x.equals(target));
            }
        }

        /**
         * groups the constraints into their respective targets
         */
        private static Stream<UnifyConstraint> the(Stream<UnifyConstraint> c) {
            ListMultimap<Term, UnifyConstraint> m = matchConstraintMapBuilder.build();
            c.forEach(x -> m.put(x.x, x));
            return m.asMap().entrySet().stream().map(e -> {
                Collection<UnifyConstraint> cc = e.getValue();
                int ccn = cc.size();

                assert (ccn > 0);
                if (ccn == 1) {
                    return (cc.iterator().next());
                } else {
                    UnifyConstraint[] d = cc.toArray(new UnifyConstraint[ccn]);
                    Arrays.sort(d, PREDICATE.sortByCostIncreasing);
                    return new CompoundConstraint(d);
                }
            });

        }

        @Override
        public float cost() {
            return Util.sum(UnifyConstraint::cost, cache);
        }

        @Override
        public boolean invalid(Term y, Unify f) {
            for (UnifyConstraint c : cache) {
                if (c.invalid(y, f))
                    return true;
            }
            return false;
        }

        @Override
        public boolean test(Derivation derivation) {
            return derivation.constrain(this);
        }
    }
}
