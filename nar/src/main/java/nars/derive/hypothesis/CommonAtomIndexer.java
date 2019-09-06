package nars.derive.hypothesis;

import nars.subterm.Subterms;
import nars.term.Term;

/** attaches to compounds with atoms in common */
public class CommonAtomIndexer extends AbstractTangentIndexer  {

	@Override
	public boolean test(Term concept, Term target) {
		return Subterms.commonSubtermsRecursive(concept, target, true);
	}

}
