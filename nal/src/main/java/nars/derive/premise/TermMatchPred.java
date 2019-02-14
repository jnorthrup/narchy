package nars.derive.premise;

import jcog.WTF;
import jcog.data.list.FasterList;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.PREDICATE;
import nars.unify.constraint.TermMatch;

import java.util.function.Function;


/**
 * decodes a target from a provied context (X)
 * and matches it according to the matcher impl
 */
public final class TermMatchPred<X> extends AbstractTermMatchPred<X> {

    public final TermMatch match;
    private final boolean trueOrFalse;
    private final boolean exactOrSuper;

    @Deprecated public TermMatchPred(TermMatch match, Function<X, Term> resolve, int pathLen) {
        this(match, true, true, resolve, cost(pathLen));
    }

    public TermMatchPred(TermMatch match, boolean trueOrFalse, boolean exactOrSuper, boolean taskOrBelief, byte[] path) {
        this(match, trueOrFalse, exactOrSuper, TaskOrBelief(taskOrBelief).path(path), cost(path.length));
    }
    private TermMatchPred(TermMatch match, boolean trueOrFalse, boolean exactOrSuper, Function/*<X, Term>*/ resolve, float resolveCost) {
        super(name(match, resolve, exactOrSuper).negIf(!trueOrFalse), resolve, resolveCost);

        this.match = match;
        this.trueOrFalse = trueOrFalse;
        this.exactOrSuper = exactOrSuper;
        if (!trueOrFalse && !exactOrSuper)
            throw new WTF();
    }

    static Term name(TermMatch match, @Deprecated Function resolve, boolean exactOrSuper) {
        Atomic a = Atomic.the(match.getClass().getSimpleName());
        Term r = $.the(resolve.toString());
        r = exactOrSuper ? r : $.func("in", r);
        Term p = match.param();
        return p!=null ? $.func(a, r, p) : $.func(a, r);
    }

    protected static PremiseRuleSource.RootTermAccessor TaskOrBelief(boolean taskOrBelief) {
        return taskOrBelief ? PremiseRuleSource.TaskTerm : PremiseRuleSource.BeliefTerm;
    }

    @Override
    public boolean reduceIn(FasterList<PREDICATE<X>> p) {
        if (resolveCost == 0)
            return false; //dont bother grouping root accessors

        TermMatchPred other = null;
        for (int i = 0, pSize = p.size(); i < pSize; i++) {
            PREDICATE x = p.get(i);
            if (x != this && x instanceof TermMatchPred) {
                TermMatchPred t = ((TermMatchPred) x);
                if (resolve.equals(t.resolve)) {
                    other = t;
                    break;
                }
            }
        }
        if (other!=null) {

            int myIndex = p.indexOfInstance(this);
            TermMatchPred a = this; p.remove(this);
            TermMatchPred b = other; p.removeFirstInstance(other);
            if (a.match.cost() > b.match.cost()) {
                TermMatchPred x = a;
                a = b;
                b = x;
            }

            p.add(myIndex, merge(a, b));

            return true;
        }

        return false;
    }


    private AbstractTermMatchPred<X> merge(TermMatchPred a, TermMatchPred b) {
        return new AbstractTermMatchPred<>(Op.SETe.the(a, b), resolve, resolveCost) {
                    @Override
                    protected boolean match(Term y) {
                        return a.match(y) && b.match(y);
                    }

                    @Override
                    public float cost() {
                        return this.resolveCost + a.match.cost() + b.match.cost();
                    }
                };
    }

    @Override
    public float cost() {
        return resolveCost + match.cost();
    }

    @Override protected boolean match(Term y) {
        return (exactOrSuper ? match.test(y) : match.testSuper(y)) == trueOrFalse;
    }

//    public static class Subterm<X> extends AbstractPred<X> {
//
//        private final byte[] path;
//
//        private final boolean trueOrFalse;
//
//        private final TermMatch match;
//        private final Function<X, Term> resolve;
//        private final float cost;
//
//        /** whether to apply the matcher's super test as a prefilter before descending to match the subterm exactly */
//        private final boolean preTestSuper;
//
//        public Subterm(byte[] path, TermMatch m, boolean trueOrFalse, Function<X, Term> resolve) {
//            super($.func(Atomic.the(m.getClass().getSimpleName()),
//                    $.the(resolve.toString()), m.param(),
//                    $.p(path)
//            ).negIf(!trueOrFalse));
//
//            assert(path.length > 0): "use TermMatchPred for 0-length (root) paths";
//            this.resolve = resolve;
//            this.trueOrFalse = trueOrFalse;
//            this.match = m;
//            this.path = path;
//            this.cost = (float) (1 + Math.log(path.length)) * match.cost();
//            this.preTestSuper = trueOrFalse; //TODO param
//        }
//
//        @Override
//        public float cost() {
//            return cost;
//        }
//
//        @Override
//        public boolean test(X x) {
//            return test(resolve.apply(x), path);
//        }
//
//        private boolean test(Term superTerm, byte[] subPath) {
//
//            boolean superOK = !preTestSuper || (match.testSuperMust(superTerm)==trueOrFalse);
//            if (!superOK)
//                return false;
//
//            Term subTarget = superTerm.sub(subPath);
//            return subTarget!=null && (match.test(subTarget)) == trueOrFalse;
//
//        }
//
//    }
}
