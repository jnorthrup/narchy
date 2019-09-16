package nars.term.control;

import jcog.WTF;
import jcog.data.list.FasterList;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.unify.Unify;
import nars.unify.constraint.TermMatcher;

import java.util.function.Function;


/**
 * decodes a target from a provied context (X)
 * and matches it according to the matcher impl
 */
public final class TermMatch<X extends Unify> extends AbstractTermMatchPred<X> {



    public final TermMatcher match;
    private final boolean trueOrFalse;


    @Deprecated public TermMatch(TermMatcher match, Function<X, Term> resolve, int pathLen) {
        this(match, true, resolve, cost(pathLen));
    }


    public TermMatch(TermMatcher match, boolean trueOrFalse, Function/*<X, Term>*/ resolve, float resolveCost) {
        super(name(match, resolve).negIf(!trueOrFalse), resolve, resolveCost);

        this.match = match;
        this.trueOrFalse = trueOrFalse;
    }

    private static Term name(TermMatcher match, @Deprecated Function resolve) {
        Class<? extends TermMatcher> mc = match.getClass();
        String cc = mc.isAnonymousClass() ? match.toString() : mc.getSimpleName();
        if (cc.isEmpty())
            throw new WTF();
        Atomic a = Atomic.the(cc);
        Term r = $.$$(resolve.toString());
        Term p = match.param();
        return p!=null ? $.func(a, r, p) : $.func(a, r);
    }


    @Override
    public boolean reduceIn(FasterList<PREDICATE<X>> p) {
        if (resolveCost == 0)
            return false; //dont bother grouping root accessors

        TermMatch other = null;
        for (PREDICATE x : p) {
            if (x != this && x instanceof TermMatch) {
                TermMatch t = ((TermMatch) x);
                if (resolve.equals(t.resolve)) {
                    other = t;
                    break;
                }
            }
        }
        if (other!=null) {

            int myIndex = p.indexOfInstance(this);
            TermMatch a = this; p.remove(this);
            TermMatch b = other; p.removeFirstInstance(other);
            if (a.match.cost() > b.match.cost()) {
                TermMatch x = a;
                a = b;
                b = x;
            }

            p.add(myIndex, merge(a, b));

            return true;
        }

        return false;
    }


    private AbstractTermMatchPred<X> merge(TermMatch a, TermMatch b) {
        return new Merge2TermMatch(a, b);
    }

    @Override
    public float cost() {
        return resolveCost + match.cost();
    }

    @Override protected final boolean match(Term y) {
        return match.test(y) == trueOrFalse;
    }

    private final class Merge2TermMatch extends AbstractTermMatchPred<X> {
        private final TermMatch<X> a, b;

        Merge2TermMatch(TermMatch<X> a, TermMatch<X> b) {
            super(Op.SETe.the(a, b), TermMatch.this.resolve, TermMatch.this.resolveCost);
            this.a = a;
            this.b = b;
        }

        @Override
        protected boolean match(Term y) {
            return a.match(y) && b.match(y);
        }

        @Override
        public float cost() {
            return this.resolveCost + a.match.cost() + b.match.cost();
        }
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
