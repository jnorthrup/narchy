package nars.unify;

import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.transform.AbstractTermTransform;

import java.util.function.Function;

public abstract class AbstractUnifyTransform extends AbstractTermTransform.NegObliviousTermTransform {

	@Override
	public final Term applyAtomic(Atomic x) {
		return x instanceof Variable ? applyVariable((Variable) x) : applyAtomicConstant(x);
	}

	/** to be overridden */
	public Term applyAtomicConstant(Atomic x) {
		return x;
	}

	/** to be overridden */
	public Term applyVariable(Variable x) {
		return x;
	}

	public static class LambdaUnifyTransform extends AbstractUnifyTransform {
		private final Function<Variable, Term> resolve;

		public LambdaUnifyTransform(Function<Variable, Term> resolve) {
			this.resolve = resolve;
		}

		@Override public Term applyVariable(Variable v) {
			return resolve.apply(v);
		}
	}
}
