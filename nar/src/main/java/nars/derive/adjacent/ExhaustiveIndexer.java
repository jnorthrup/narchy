package nars.derive.adjacent;

import nars.term.Compound;
import nars.term.Term;

import java.util.function.Predicate;

import static nars.Op.ATOM;

public class ExhaustiveIndexer extends AbstractAdjacentIndexer {
	@Override
	public boolean test(Term t, Term target) {
		return t instanceof Compound &&
			((Compound) t).unifiesRecursively(target, new Predicate<Term>() {
                @Override
                public boolean test(Term z) {
                    return z.hasAny(ATOM);
                }
            });
	}
}
