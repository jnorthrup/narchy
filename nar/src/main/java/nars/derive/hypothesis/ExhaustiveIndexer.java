package nars.derive.hypothesis;

import nars.term.Compound;
import nars.term.Term;

import static nars.Op.ATOM;

public class ExhaustiveIndexer extends AbstractTangentIndexer {
	@Override
	public boolean test(Term t, Term target) {
		return t instanceof Compound &&
			((Compound) t).unifiesRecursively(target, z -> z.hasAny(ATOM));
	}
}
