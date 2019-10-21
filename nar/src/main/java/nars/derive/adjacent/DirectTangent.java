package nars.derive.adjacent;

import nars.derive.Derivation;
import nars.link.AbstractTaskLink;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;

import java.util.function.Predicate;

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

		Term[] T = {null};
		links.sampleUnique(d.random, new Predicate<TaskLink>() {
            @Override
            public boolean test(TaskLink y) {
                Term z = ((AbstractTaskLink) y).matchReverse(from, fromHash, to, toHash);
                if (z == null) return true;

                T[0] = z;
                return false; //done
            }
        });
		return T[0];
	}
}
