package nars.concept.snapshot;

import jcog.decide.Roulette;
import jcog.pri.HashedPLink;
import jcog.pri.PLink;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.PLinkArrayBag;
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

	public final PLinkArrayBag<Term> links = new PLinkArrayBag<>(PriMerge.replace, 0);

	public TaskLinkSnapshot() {
	}


	/**
	 * caches an AtomLinks instance in the Concept's meta table, attached by a SoftReference
	 */

	protected static int cap(int bagSize) {
		return Math.max(4, (int) Math.ceil(1f * Math.sqrt(bagSize)) /* estimate */);
		//return bagSize;
	}

	public void commit(Term x, Iterable<TaskLink> items, int itemCount, boolean reverse) {

		links.capacity(cap(itemCount));

		links.commit();

		int xh = x.hashCodeShort();
		for (TaskLink t : items) {
			Term y = t.other(x, xh, reverse);
			if (y != null)
				links.put(new HashedPLink<>(y, t.pri()));
		}
	}


	public @Nullable Term sample(Predicate<Term> filter, byte punc, Random rng) {
		@Nullable Object[] ll = links.items();
		int lls = Math.min(links.size(), ll.length);
		if (lls == 0)
			return null;
		else {

            int li = Roulette.selectRouletteCached(lls, (int i) -> {

				PLink<Term> x = (PLink) ll[i];
				return x != null && filter.test(x.id) ?
					Math.max(ScalarValue.EPSILON,
					//x.priPunc(punc)
					x.pri()
					)
					:
					Float.NaN;

			}, rng::nextFloat);

            PLink<Term> l = li >= 0 ? (PLink<Term>) ll[li] : null;

			return l != null ? l.id : null;
		}

	}


	public final boolean isEmpty() {
		return links.isEmpty();
	}
}
