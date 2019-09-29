package nars.unify;

import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.transform.AbstractTermTransform;

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

}
