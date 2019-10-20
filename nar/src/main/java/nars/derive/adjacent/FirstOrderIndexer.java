package nars.derive.adjacent;

import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.unify.UnifyAny;

import java.util.function.Predicate;

import static nars.Op.CONJ;

public class FirstOrderIndexer extends AbstractAdjacentIndexer {

	@Override
	public boolean test(Term concept, Term target) {
		if (concept instanceof Compound) {

			var u = new UnifyAny(); //TODO reuse from Derivation
			u.ttl = NAL.derive.TTL_UNISUBST;


			var targetOp = target.opID();
			if (concept.opID() == targetOp && u.unifies(concept, target))
				return true;

			Predicate<Term> subunification = x -> {
				var xx = x.unneg();
				return xx.opID()==targetOp && xx.hasAny(Op.AtomicConstant) && u.unifies(xx, target);
			};

			return subUnifies(subunification, concept);

//			if (op.statement) {
//				for (int i = 0; i < 2; i++) {
//					Term ss = concept.sub(i).unneg();
//					if (ss.op() == CONJ) {
//						if (subUnifies(subunification, ss))
//							return true;
//					}
//				}
//			}

		}
		return false;
	}

	protected static boolean subUnifies(Predicate<Term> u, Term concept) {

		return concept.opID() == CONJ.id ?
			concept.eventsOR((when, what) -> u.test(what), 0, true, true) :
			concept.subterms().OR(u);
	}
}
