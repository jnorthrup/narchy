package nars.derive.hypothesis;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.unify.UnifyAny;

import java.util.function.Predicate;

import static nars.Op.CONJ;

public class FirstOrderIndexer extends AbstractTangentIndexer {
	@Override
	public boolean test(Term concept, Term target) {
		if (concept instanceof Compound) {
			Op op = concept.op();

			UnifyAny u = new UnifyAny();

			Op targetOp = target.op();
			if (op == targetOp && u.unifies(concept, target))
				return true;

			Predicate<Term> subunification = x -> {
					Term xx = x.unneg();
					return xx.op()==targetOp && u.unifies(xx, target);
				};

			if (subUnifies(subunification, concept))
				return true;

			if (op.statement) {
				for (int i = 0; i < 2; i++) {
					Term ss = concept.sub(0).unneg();
					if (ss.op() == CONJ) {
						if (subUnifies(subunification, ss))
							return true;
					}
				}
			}

		}
		return false;
	}

	protected boolean subUnifies(Predicate<Term> u, Term concept) {

		if (concept.op() == CONJ) {
			if (concept.eventsOR((when,what)-> u.test(what), 0, true, true))
				return true;
		} else {
			if (concept.subterms().OR(u))
				return true;
		}
		return false;
	}
}
