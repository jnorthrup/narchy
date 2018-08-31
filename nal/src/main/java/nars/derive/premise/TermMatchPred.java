package nars.derive.premise;

import nars.$;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.unify.op.TermMatch;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;


/**
 * decodes a term from a provied context (X)
 * and matches it according to the matcher impl
 */
public final class TermMatchPred<X> extends AbstractPred<X> {

    private final boolean trueOrFalse;
    private final boolean exactOrSuper;

    public final TermMatch match;
    public final Function<X, Term> resolve;

    public TermMatchPred(TermMatch match, boolean trueOrFalse, boolean exactOrSuper, Function<X, Term> resolve) {
        super($.func(Atomic.the(match.getClass().getSimpleName()), $.the(resolve.toString()), match.param())
                .negIf(!trueOrFalse));

        this.resolve = resolve;
        this.match = match;
        this.trueOrFalse = trueOrFalse;
        this.exactOrSuper = exactOrSuper;
    }


    @Override
    public float cost() {
        return match.cost();
    }

    @Override
    public boolean test(X x) {
        Term y = resolve.apply(x);
        return (exactOrSuper ? match.test(y) : match.testSuper(y)) == trueOrFalse;
        //return (!exactOrSuper || match.test(y) == trueOrFalse); //bypass testSuper, for testing testSuper
    }

    public static class Subterm<X> extends AbstractPred<X> {

        private final byte[] path;

        private final boolean trueOrFalse;

        private final TermMatch match;
        private final Function<X, Term> resolve;
        private final float cost;

        /** whether to apply the matcher's super test as a prefilter before descending to match the subterm exactly */
        private final boolean preTestSuper;

        public Subterm(byte[] path, TermMatch m, boolean trueOrFalse, Function<X, Term> resolve) {
            super($.func(Atomic.the(m.getClass().getSimpleName()),
                    $.the(resolve.toString()), m.param(),
                    $.p(path)
            ).negIf(!trueOrFalse));

            assert(path.length > 0): "use TermMatchPred for 0-length (root) paths";
            this.resolve = resolve;
            this.trueOrFalse = trueOrFalse;
            this.match = m;
            this.path = path;
            this.cost = (float) (1 + Math.log(path.length)) * match.cost();
            this.preTestSuper = true; //TODO param
        }

        @Override
        public float cost() {
            return cost;
        }

        @Override
        public boolean test(X x) {
            return test(resolve.apply(x), path);
        }

        private boolean test(Term superTerm, byte[] subPath) {

            boolean superOK = !preTestSuper || (match.testSuper(superTerm)==trueOrFalse);
            if (!superOK)
                return false;

            Term subTarget = superTerm.subPath(subPath);
            return subTarget!=null && (match.test(subTarget)) == trueOrFalse;

        }

    }
}
