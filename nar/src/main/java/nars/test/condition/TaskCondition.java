package nars.test.condition;


import jcog.Texts;
import jcog.sort.RankedN;
import jcog.sort.TopFilter;
import nars.Task;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * TODO evolve this into a generic tool for specifying constraints and conditions
 * on memory (beliefs, and other measurable quantities/qualities).
 * use these to form adaptive runtime hypervisors ensuring optimal and correct operation
 */
abstract public class TaskCondition implements NARCondition, Predicate<Task> {


	protected Task firstMatch = null;
	@Nullable
	protected TopFilter<Task> similar = null;
	/**
	 * whether to apply meta-feedback to drive the reasoner toward success conditions
	 */
	private boolean matched;

	public static String rangeStringN2(float min, float max) {
		return '(' + Texts.n2(min) + ',' + Texts.n2(max) + ')';
	}

	/**
	 * a heuristic for measuring the difference between terms
	 * in range of 0..100%, 0 meaning equal
	 */
	public static float termDistance(Term a, Term b, float ifLessThan) {
		if (a.equals(b)) return 0;

		float dist = 0;
		if (a.op() != b.op()) {

			dist += 0.4f;
			if (dist >= ifLessThan) return dist;
		}

		if (a.subs() != b.subs()) {
			dist += 0.3f;
			if (dist >= ifLessThan) return dist;
		}

		if (a.structure() != b.structure()) {
			dist += 0.2f;
			if (dist >= ifLessThan) return dist;
		}

		dist += Texts.levenshteinFraction(
			a.toString(),
			b.toString()) * 0.1f;

		if (a.dt() != b.dt()) {
			dist *= 2;
		}

		return dist;
	}

	abstract public boolean matches(@Nullable Task task);

	@Override
	public boolean test(Task t) {

		if (!matched && matches(t)) {
			matched = true;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public long getFinalCycle() {
		return -1;
	}

	@Override
	public final boolean isTrue() {
		return matched;
	}

	public void similars(int maxSimilars) {
		this.similar = new RankedN<>(new Task[maxSimilars], this::value);
	}

    abstract protected float value(Task task, float worstDiffNeg);

}



     
     
     

























