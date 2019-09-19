package nars.derive.adjacent;

import nars.subterm.Subterms;
import nars.term.Term;

/** attaches to compounds with atoms in common */
public class CommonAtomIndexer extends AbstractAdjacentIndexer {

	@Override
	public boolean test(Term concept, Term target) {
		return Subterms.commonSubtermsRecursive(concept, target, true);
	}

}
