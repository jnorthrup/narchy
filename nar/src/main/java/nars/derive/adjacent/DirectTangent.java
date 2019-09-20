package nars.derive.adjacent;

import jcog.WTF;
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

		int fromHash = from.hashCodeShort();
		int toHash = to.hashCodeShort();

		final Term[] T = {null};
		links.sampleUnique(d.random, y -> {
			AbstractTaskLink Y = (AbstractTaskLink) y;
			Term z = Y.matchReverse(from, fromHash, to, toHash);
			if (z!=null) {
				//if (!term.equals(f)) //extra test to be sure
				T[0] = z;
				return false; //done
			}
			return true;
		});
		return T[0];
	}
}
