package nars.concept.snapshot;

import jcog.decide.Roulette;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.link.TaskLink;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

/**
 * caches an array of tasklinks tangent to an atom
 */
public final class TaskLinkSnapshot {

	public final PriArrayBag<TaskLink> links = new PriArrayBag<>(PriMerge.replace, 0);


	public TaskLinkSnapshot() {
	}



	/**
	 * caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference
	 */

	protected int cap(int bagSize) {
		return Math.max(4, (int) Math.ceil(1f * Math.sqrt(bagSize)) /* estimate */);
		//return bagSize;
	}

	public void commit(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse) {

		links.capacity(cap(itemCount));

		links.commit();

		for (TaskLink t : items) {
			Term y = t.other(x, reverse);
			if (y != null)
				links.put(t.clone(1));
		}
	}


	@Nullable
	public Term sample(Predicate<TaskLink> filter, byte punc, Random rng) {
		@Nullable Object[] ll = links.items();
		int lls = Math.min(links.size(), ll.length);
		if (lls == 0)
			return null;
		else {
			TaskLink l;
			if (lls == 1) l = (TaskLink) ll[0];
			else {
				int li = Roulette.selectRouletteCached(lls, (int i) -> {

					TaskLink x = (TaskLink) ll[i];
					return x != null && filter.test(x) ?
						Math.max(ScalarValue.EPSILON, x.priPunc(punc))
						:
						Float.NaN;

				}, rng::nextFloat);
				l = li >= 0 ? (TaskLink)ll[li] : null;
			}
			return l != null ? l.from() : null;
		}

	}


}
