package nars.derive.hypothesis;

import nars.Task;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

/**
 * samples the tasklink bag for a relevant reversal
 * memoryless, determined entirely by tasklink bag, O(n)
 */
public class DirectTangent extends AbstractHypothesizer {

	public static final nars.derive.hypothesis.DirectTangent the = new nars.derive.hypothesis.DirectTangent();

	private DirectTangent() {

	}

	@Override
	protected Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d) {

		//< 1 .. 1.0 isnt good
		float probBase =
			0.5f;
		//0.33f;
		float probDirect =
			//(float) (probBase / Util.sqr(Math.pow(2, target.volume()-1)));
			(float) (probBase / Math.pow(target.volume(), 3));
		//(float) (probBase / Math.pow(2, target.volume()-1));
		//probBase * (target.volume() <= 2 ? 1 : 0);
		//probBase * 1f / Util.sqr(Util.sqr(target.volume()));
		//probBase * 1f / term.volume();
		//probBase * 1f / Util.sqr(term.volume());
		//probBase *  1f / (term.volume() * Math.max(1,(link.from().volume() - term.volume())));

		if (d.random.nextFloat() >= probDirect)
			return null; //term itself

		return sampleReverseMatch(target, link, links, d);

	}

	@Nullable
	public Term sampleReverseMatch(Term target, TaskLink link, TaskLinks links, Derivation d) {
		final Term[] T = {target};
		links.sampleUnique(d.random, (ll) -> {
			if (ll != link) {
				Term t = ll.reverseMatch(target);
				if (t != null) {
					T[0] = t;
					return false; //done
				}
			}
			return true;
		});
		return T[0];
	}
}
