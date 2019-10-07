package nars.derive.adjacent;

import nars.derive.Derivation;
import nars.link.AbstractTaskLink;
import nars.link.TaskLinks;
import nars.term.Term;

/**
 * samples the tasklink bag for a relevant reversal
 * memoryless, determined entirely by tasklink bag, O(n)
 */
public class DirectTangent implements AdjacentConcepts {

	public static final DirectTangent the = new DirectTangent();

	private DirectTangent() {

	}

	@Override
	public Term adjacent(Term from, Term to, byte punc, TaskLinks links, Derivation d) {

		int fromHash = from.hashCodeShort(), toHash = to.hashCodeShort();

		final Term[] T = {null};
		links.sampleUnique(d.random, y -> {
			Term z = ((AbstractTaskLink) y).matchReverse(from, fromHash, to, toHash);
			if (z == null) return true;

			T[0] = z;
			return false; //done
		});
		return T[0];
	}
}
