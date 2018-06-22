package nars.unify.constraint;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import jcog.Util;
import nars.$;
import nars.Param;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import static nars.Op.SETe;


public abstract class MatchConstraint extends AbstractPred<Derivation> {

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

    final static Atomic UnifyIf = Atomic.the("unifyIf");
    public final Variable x;

    protected MatchConstraint(Term x, Term id) {
        super(id);
        this.x = (Variable) x;
    }

    protected MatchConstraint(Term x, String func, Term... args) {
        this(x, $.func(UnifyIf, x, $.func(func, args)));
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

        final static Term TASK_BELIEF = $.p(Derivation.Task, Derivation.Belief);
        final static Term BELIEF_TASK = $.p(Derivation.Belief, Derivation.Task);

        private final RelationConstraint constraint;
        private final boolean taskFirst;

        ConstraintAsPredicate(RelationConstraint m, boolean taskFirst) {
            super($.p(taskFirst ? TASK_BELIEF : BELIEF_TASK, m.term()));
            this.constraint = m;
            this.taskFirst = taskFirst;
        }

        @Override
        public boolean test(Derivation preDerivation) {
            Term x, y;
            if (taskFirst) {
                x = preDerivation.taskTerm;
                y = preDerivation.beliefTerm;
            } else {
                y = preDerivation.taskTerm;
                x = preDerivation.beliefTerm;
            }
            return !constraint.invalid(x, y);
        }

        @Override
        public float cost() {
            return constraint.cost();
        }
    }

    public static final MatchConstraint[] EmptyMatchConstraints = new MatchConstraint[0];


    public static MatchConstraint[] the(Set<MatchConstraint> c) {
        if (c.size() < 2)
            return c.toArray(EmptyMatchConstraints);
        else
            return CompoundConstraint.the(c.stream()).toArray(MatchConstraint[]::new);
    }

    static final class CompoundConstraint extends MatchConstraint {


        static final MultimapBuilder.ListMultimapBuilder matchConstraintMapBuilder = MultimapBuilder.hashKeys(4).arrayListValues(4);

        private final MatchConstraint[] cache;

        private CompoundConstraint(MatchConstraint[] c) {
            super(c[0].x, $.func(UnifyIf, c[0].x, SETe.the((Term[]) c)));
            this.cache = c;

            if (Param.DEBUG) {
                final Variable target = c[0].x;
                for (int i = 1; i < c.length; i++)
                    assert (c[i].x.equals(target));
            }
        }

        /**
         * groups the constraints into their respective targets
         */
        private static Stream<MatchConstraint> the(Stream<MatchConstraint> c) {
            ListMultimap<Term, MatchConstraint> m = matchConstraintMapBuilder.build();
            c.forEach(x -> m.put(x.x, x));
            return m.asMap().entrySet().stream().map(e -> {
                Collection<MatchConstraint> cc = e.getValue();
                int ccn = cc.size();

                assert (ccn > 0);
                if (ccn == 1) {
                    return (cc.iterator().next());
                } else {
                    MatchConstraint[] d = cc.toArray(new MatchConstraint[ccn]);
                    Arrays.sort(d, PREDICATE.sortByCostIncreasing);
                    return new CompoundConstraint(d);
                }
            });

        }

        @Override
        public float cost() {
            return Util.sum(MatchConstraint::cost, cache);
        }

        @Override
        public boolean invalid(Term y, Unify f) {
            for (MatchConstraint c : cache) {
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
