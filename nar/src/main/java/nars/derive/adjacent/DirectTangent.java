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
			if (!y.isSelf()) {
				AbstractTaskLink Y = (AbstractTaskLink) y;
				if (Y.toHash() == toHash && (Y.fromHash() != fromHash)) {
					Term yt = y.to();
					if (to.equals(yt)) {
						Term yf = y.from();
						if (!from.equals(yf)) {
							//if (!term.equals(f)) //extra test to be sure
							T[0] = yf;
							return false; //done
						}
					}
				}
			}
			return true;
		});
		return T[0];
	}
}
