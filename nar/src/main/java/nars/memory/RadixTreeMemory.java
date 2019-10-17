package nars.memory;

import jcog.data.byt.AbstractBytes;
import jcog.tree.radix.ConcurrentRadixTree;
import jcog.tree.radix.MyRadixTree;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.map.TermRadixTree;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static jcog.Util.PHI_min_1f;

/**
 * concurrent radix tree index
 * TODO restore byte[] sequence writing that doesnt prepend atom length making leaves unfoldable by natural ordering
 */
public class RadixTreeMemory extends Memory implements Consumer<NAR> {


    public final ConceptRadixTree concepts;


	private static AbstractBytes key(Term k) {
		return TermRadixTree.termByVolume(k.concept());
	}

	public RadixTreeMemory(int sizeLimit) {

		this.concepts = new ConceptRadixTree(sizeLimit);

	}

	@Override
	public Stream<Concept> stream() {
		return StreamSupport.stream(concepts.spliterator(), false);
	}

	@Override
	public void start(NAR nar) {
		super.start(nar);

		nar.onDur(this);
	}


	/**
	 * since the terms are sorted by a volume-byte prefix, we can scan for removals in the higher indices of this node
	 */
	private MyRadixTree.Node volumeWeightedRoot(Random rng) {

		List<MyRadixTree.Node> l = concepts.root.getOutgoingEdges();
		int levels = l.size();


		float r = rng.nextFloat();
		r = (r * r);


		return l.get(Math.round((levels - 1) * (1 - r)));
	}

	private int sizeEst() {
		return concepts.sizeEst();
	}


	private static boolean removeable(Concept c) {
		return !(c instanceof PermanentConcept);
	}


	@Override
	public Concept get(Term t, boolean createIfMissing) {
		AbstractBytes k = key(t);

		ConceptRadixTree c = this.concepts;

		return createIfMissing ?
			c.putIfAbsent(k, () -> nar.conceptBuilder.apply(t, null))
			:
			c.get(k);
	}

	@Override
	public void set(Term src, Concept target) {

		AbstractBytes k = key(src);

		concepts.acquireWriteLock();
		try {
			Termed existing = concepts.get(k);
			if (existing != target && !(existing instanceof PermanentConcept)) {
				concepts.put(k, target);
			}
		} finally {
			concepts.releaseWriteLock();
		}
	}

	@Override
	public void clear() {
		concepts.clear();
	}

	@Override
	public void forEach(Consumer<? super Concept> c) {
		concepts.forEach(c);
	}

	@Override
	public int size() {
		return concepts.size();
	}


	@Override
	public String summary() {

		return concepts.sizeEst() + " concepts";
	}


	@Override
	public @Nullable Concept remove(Term entry) {
		AbstractBytes k = key(entry);
		Concept result = concepts.get(k);
		if (result != null) {
			boolean removed = concepts.remove(k);
			if (removed)
				return result;
		}
		return null;
	}


	private void onRemoval(Concept value) {
		onRemove(value);
	}

	@Override
	public void accept(NAR eachFrame) {
		concepts.forgetNext();
	}


	public class ConceptRadixTree extends ConcurrentRadixTree<Concept> {

		private final int sizeLimit;

		ConceptRadixTree(int sizeLimit) {
			this.sizeLimit = sizeLimit;
		}

		@Override
		public final Concept put(Concept value) {
			return super.put(key(value.term()), value);
		}

		@Override
		public boolean onRemove(Concept r) {

			Concept c = r;
			if (removeable(c)) {
				onRemoval(r);
				return true;
			} else {
				return false;
			}

		}

		private void forgetNext() {

			int sizeBefore = sizeEst();

			int overflow = sizeBefore - sizeLimit;

			if (overflow < 0)
				return;

            float maxIterationRemovalPct = 0.05f;
            int maxConceptsThatCanBeRemovedAtATime = (int) Math.max(1, sizeBefore * maxIterationRemovalPct);
//        if (overflow < maxConceptsThatCanBeRemovedAtATime)
//            return;

            float overflowSafetyPct = 0.1f;
            if ((((float) overflow) / sizeLimit) > overflowSafetyPct) {
				//major collection, strong
				concepts.acquireWriteLock();
			} else {
				//minor collection, weak
				if (!concepts.tryAcquireWriteLock())
					return;
			}

			try {
				SearchResult s = null;

				while (/*(iterationLimit-- > 0) &&*/ ((sizeEst() - sizeLimit) > maxConceptsThatCanBeRemovedAtATime)) {

					Random rng = nar.random();

					Node subRoot = volumeWeightedRoot(rng);

                    if (s == null)
						s = concepts.random(subRoot, PHI_min_1f, rng);

					Node f = s.found;

					if (f != null && f != subRoot) {
						int subTreeSize = concepts.sizeIfLessThan(f, maxConceptsThatCanBeRemovedAtATime);

						if (subTreeSize > 0) {
							concepts.removeWithWriteLock(s, true);
						}

						s = null;
					}

				}
			} finally {
				concepts.releaseWriteLock();
			}


		}

	}
}
