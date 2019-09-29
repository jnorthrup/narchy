package nars.unify;

import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.transform.TermTransform;

import java.util.function.Function;

public class LambdaUnifyTransform implements TermTransform {
	private final Function<Variable, Term> resolve;

	public LambdaUnifyTransform(Function<Variable, Term> resolve) {
		this.resolve = resolve;
	}

	@Override
	public final Term applyAtomic(Atomic a) {
		return a instanceof Variable ? resolve.apply((Variable) a) : a;
	}

}
