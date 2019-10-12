package nars.derive;

import jcog.Util;
import jcog.data.set.PrioritySet;
import jcog.pri.HashedPLink;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.attention.What;
import nars.derive.premise.Premise;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Queue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

/**
 * instance of an execution model associating a Deriver with a Derivation
 */
public abstract class DeriverExecutor implements BooleanSupplier {

	public final Deriver deriver;
	final Derivation d;


	protected DeriverExecutor(Deriver deriver) {
		this.deriver = deriver;
		this.d = new Derivation(this);
	}

	static protected float pri(Premise p) {
		Task t = p.task(), b = p.belief();
		float tPri = t.priElseZero();
		if (b != null)
			return
				//Util.or(
					//Util.min(
					//Util.mean(
				Util.sum(
					tPri, b.priElseZero()
				);
		else
			return tPri;
	}

	/**
	 * gets next tasklink premise
	 */
	protected final Premise sample() {
		return d.what.sample(d.random);

//			//Pre-resolve
//			if (x!=null) {
//				Task y = TaskResolve.the.get(x, d);
//				if (y != null) // && !x.equals(y))
//					return new AbstractPremise(y, x.to());
//			}
//		return x;
	}

	/**
	 * run a premise
	 */
	protected final void run(Premise p, int ttl) {
		d.run(p, ttl).increment();
	}

	public abstract void next();

	public abstract void add(Premise p);

	public void nextSynch(What w) {

		next(w, () -> false);
	}



//	public long next(What w, long startNS, long useNS) {
////		if (w.tryCommit()) {
////			//update time
////			long actualStart = nanoTime();
////			long delayedStart = actualStart - startNS;
////			useNS = Math.max(0, useNS - delayedStart);
////			startNS = actualStart;
////		}
//
//		long now;
//		deadline = startNS + useNS;
//		try {
//			next(w, this);
//			//runs++;
//		} catch (Throwable t) {
//			Parts.logger.error("{} {}", t, this);
//			//t.printStackTrace();
//		}
//
//		now = nanoTime();
//		//h.use(useNS, now - startNS);
//		return now;
//	}

	public final void next(What w, final BooleanSupplier kontinue) {

		next(w);

//		if (((TaskLinkWhat) w).links.isEmpty())
//			return; //HACK

		next(kontinue);
	}

	public final void next(final BooleanSupplier kontinue) {

		do {

			next();

		} while (kontinue.getAsBoolean());

	}

	/** switches context */
	public final void next(What w) {
		d.next(w);
	}


	private transient long deadline = Long.MIN_VALUE;

	@Override
	public final boolean getAsBoolean() {
		return nanoTime() < deadline;
	}

	public final void nextCycle() {
		d.next();
	}


	public static class QueueDeriverExecutor extends DeriverExecutor {

		final Queue<Premise> queue =
			new PrioritySet<>(Deriver.sorter, new UnifiedSet<>()); //prevents duplicates (and caches pri calculation)
		//new CachedPriorityQueue<>(sorter); //caches pri calculation
		//new ArrayHashSet<>(capacity)


		int iterationTTL = 8;



		public QueueDeriverExecutor(Deriver deriver) {
			super(deriver);
		}

		@Override
		public void next() {

			queue.clear();

			int mainTTL = iterationTTL;
			int branchTTL = d.nar.deriveBranchTTL.intValue();

			Queue<Premise> q = this.queue;

			//TODO scale ttl by the priority normalized relative to the other items in the queue
			do {

				//if (queue.size() < mainTTL - 1)
				//if (q.isEmpty())
				if (q.size() < mainTTL/2) {
					Premise s = sample();
					if (s!=null)
						q.offer(s);
				}

				Premise r;
				if ((r = q.poll()) != null)
					run(r, branchTTL);

			} while (--mainTTL > 0);

		}


		@Override
		public void add(Premise p) {

			if (/*novel.putIfAbsent(p,p)==null && */ queue.offer(p)) {
//                int qs = queue.size();
//                if (qs >= capacity)
//                    d.unify.ttl = 0; //CUT the current premise by depleting its TTL, forcing it to return
			}

		}


	}

	private static class BagDeriverExecutor extends DeriverExecutor {
		/**
		 * queue of pending premises to fire
		 * TODO use a bag to deduplicate and rank
		 */
		final Bag<Premise, PLink<Premise>> bag =
			//new PLinkArrayBag<>(PriMerge.max, 0); //locking issues
			new PLinkHijackBag<>(PriMerge.max, 0, 3);
		int bufferCap = 4;
		int runBatch = bufferCap / 2;
		private Consumer<PLink<Premise>> each;

		private BagDeriverExecutor(Deriver d) {
			super(d);
		}


		@Deprecated protected void starting() {
			bag.clear();
			bag.capacity(bufferCap);

			each = p -> run(p.get(), d.nar.deriveBranchTTL.intValue());
		}

		@Override
		public void add(Premise p) {
			HashedPLink<Premise> x = new HashedPLink<>(p, pri(p));
			PriReference<Premise> y = bag.put(x);
			//return x == y && !x.isDeleted(); //non-duplicate and accepted
		}

		@Override
		public void next() {

			starting();

			int cap = bag.capacity();

			int tries = 1 + (cap - bag.size()); //TODO penalize duplicates more
			do {
				Premise x = sample();
				if (x != null) {
					add(x);
					if (bag.size() >= cap)
						break;
				}
			} while (--tries > 0);

			//System.out.println();
			//p.print();
			//System.out.println();

			bag.commit(null);
			bag.pop(d.random, runBatch, each); //HijackBag
			//p.pop(null, runBatch, each); //ArrayBag
//                p.commit();
//                p.sample(rng, runBatch, each);


		}
	}
}
