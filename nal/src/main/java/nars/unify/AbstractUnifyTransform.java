package nars.unify;

import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.transform.AbstractTermTransform;

import java.util.function.Function;

public abstract class AbstractUnifyTransform extends AbstractTermTransform.NegObliviousTermTransform {

	abstract protected Term resolveVar(Variable v);

	@Override
	public Term applyAtomic(Atomic x) {
		if (x instanceof Variable) {
			Term y = resolveVar((Variable) x);
//                if (y != null)
				return y;
		}
		return x;
	}

	public static class LambdaUnifyTransform extends AbstractUnifyTransform {
		private final Function<Variable, Term> resolve;

		public LambdaUnifyTransform(Function<Variable, Term> resolve) {
			this.resolve = resolve;
		}

		@Override protected Term resolveVar(Variable v) {
			return resolve.apply(v);
		}
	}
}
