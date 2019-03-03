package nars.unify;

import nars.term.Term;
import nars.term.util.transform.AbstractTermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * an individual solution
 */
abstract public class DeterministicUnification extends Unification {

    public DeterministicUnification() {
        super();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof nars.unify.DeterministicUnification)
            return equals((nars.unify.DeterministicUnification) obj);
        return false;
    }

    abstract protected boolean equals(nars.unify.DeterministicUnification obj);

    @Override
    public final int forkCount() {
        return 1;
    }

    @Override
    public final Iterable<Term> apply(Term x) {
        return List.of(transform(x));
    }

    public Term transform(Term x) {
        return AbstractTermTransform.transform(x, transform());
    }

    protected Unify.UnifyTransform.LambdaUnifyTransform transform() {
        return new Unify.UnifyTransform.LambdaUnifyTransform(this::xy);
    }

    @Nullable
    abstract public Term xy(Term t);

    /**
     * sets the mappings in a target unify
     */
    abstract void apply(Unify y);
}
