package nars.derive;

import jcog.Util;
import jcog.data.set.PrioritySet;
import jcog.pri.HashedPLink;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.op.PriMerge;
import jcog.signal.meter.FastCounter;
import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.derive.premise.Premise;

import java.util.HashSet;
import java.util.Queue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * instance of an execution model associating a Deriver with a Derivation
 */
public abstract class DeriverExecutor extends Derivation {


	protected DeriverExecutor(Deriver deriver) {
		super(deriver);
	}

	protected static float pri(Premise p) {
		Task t = p.task(), b = p.belief();
		float tPri = t.priElseZero();
		return b != null ?
			//Math.max(
			Util.or(
			//Util.min(
			//Util.mean(
			//Util.sum(
			tPri, b.priElseZero()
		) : tPri;
	}

	/**
	 * gets next tasklink premise
	 */
	protected final Premise sample() {
		return this.x.sample(this.random);

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
	protected final FastCounter run(Premise p, int ttl) {
		FastCounter result = super.run(p, ttl);
		result.increment();
		return result;
	}

	public abstract void next(int loops);

	public void next(What w, int loops) {
		next(w);
		next(loops);
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

	public final void next(What w, BooleanSupplier kontinue) {

		next(w);

//		if (((TaskLinkWhat) w).links.isEmpty())
//			return; //HACK

		next(kontinue);
	}

	public final void next(BooleanSupplier kontinue) {

		do {

			next(1);

		} while (kontinue.getAsBoolean());

	}

//	/** simplest depth-first impl - wont work unless post[] is stack alloc */
//	public static class DepthFirstDeriverExecutor extends DeriverExecutor {
//
//		private int breadth;
//		int depth;
//		final int maxDepth = 3;
//
//		public DepthFirstDeriverExecutor(Deriver d) {
//			super(d);
//		}
//
//		@Override
//		public void next(int loops) {
//
//			breadth = nar.deriveBranchTTL.intValue();
//			for (int i = 0; i < loops; i++) {
//				Premise p = sample();
//				if (p==null)
//					break;
//				depth = 0;
//				run(p, breadth);
//			}
//		}
//
//		@Override
//		public void add(Premise p) {
//			if (depth >= maxDepth) {
//				unify.ttl = 0;
//				return;
//			}
//			depth++;
//			run(p, breadth);
//			depth--;
//		}
//	}

	public static class QueueDeriverExecutor extends DeriverExecutor {

		final Queue<Premise> queue =
			new PrioritySet<>(Deriver.sorter, new HashSet<>()); //prevents duplicates (and caches pri calculation)
		//new CachedPriorityQueue<>(sorter); //caches pri calculation
		//new ArrayHashSet<>(capacity)

		public QueueDeriverExecutor(Deriver deriver) {
			super(deriver);
		}

		@Override
		public void next(What w) {
			super.next(w);
			queue.clear();
		}

		@Override
		public void next(int mainTTL) {

			int branchTTL = nar.deriveBranchTTL.intValue();
//
			Queue<Premise> q = this.queue;
//			//q.clear();
//
//			//TODO scale ttl by the priority normalized relative to the other items in the queue
			do {

				if (q.size() < mainTTL - 1) {
				//if (q.isEmpty()) {
				//if (q.size() < mainTTL/2) {
					Premise s = sample();
					if (s!=null)
						q.offer(s);
					else
						return; // empty bag
				}

				Premise r;
				if ((r = q.poll()) != null)
					run(r, branchTTL);

			} while (--mainTTL > 0);

		}


		@Override
		public void add(Premise p) {

			if (NAL.TRACE)
				System.out.println("\t" + p);

			if (/*novel.putIfAbsent(p,p)==null && */ queue.offer(p)) {
				use(NAL.derive.TTL_COST_TASK_TASKIFY);
//                int qs = queue.size();
//                if (qs >= capacity)
//                    d.unify.ttl = 0; //CUT the current premise by depleting its TTL, forcing it to return
			} else
				use(NAL.derive.TTL_COST_DERIVE_TASK_SAME);

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

			each = p -> run(p.get(), nar.deriveBranchTTL.intValue());
		}

		@Override
		public void add(Premise p) {
			HashedPLink<Premise> x = new HashedPLink<>(p, pri(p));
			PriReference<Premise> y = bag.put(x);
			//return x == y && !x.isDeleted(); //non-duplicate and accepted
		}

		@Override
		public void next(int tries) {

			starting();

			int cap = bag.capacity();

			//int tries = 1 + (cap - bag.size()); //TODO penalize duplicates more
			do {
				Premise s = sample();
				if (s != null) {
					add(s);
					if (bag.size() >= cap)
						break;
				}
			} while (--tries > 0);

			//System.out.println();
			//p.print();
			//System.out.println();

			bag.commit(null);
			bag.pop(random, runBatch, each); //HijackBag
			//p.pop(null, runBatch, each); //ArrayBag
//                p.commit();
//                p.sample(rng, runBatch, each);


		}
	}
}
