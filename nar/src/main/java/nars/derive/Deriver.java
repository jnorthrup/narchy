package nars.derive;

import jcog.Util;
import jcog.data.set.PrioritySet;
import jcog.pri.HashedPLink;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.Cause;
import nars.control.How;
import nars.derive.action.*;
import nars.derive.adjacent.AdjacentIndexer;
import nars.derive.premise.Premise;
import nars.derive.rule.DeriverProgram;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.derive.util.TimeFocus;
import nars.link.TaskLink;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Queue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver extends How {
    int innerLoops = 1;

	/**
	 * variable types unifiable in premise formation
	 */
	public static final int PremiseUnifyVars =
		//Op.VAR_QUERY.bit
		Op.VAR_QUERY.bit | Op.VAR_DEP.bit
		//Op.Variable //all
		;


	final Supplier<DeriverExecutor> _exe =
		() ->
			//new BagDeriverExecutor();
			new QueueDeriverExecutor();

	final ThreadLocal<DeriverExecutor> exe = ThreadLocal.withInitial(_exe);

	public final DeriverProgram program;


	/**
	 * determines the temporal focus of (TODO tasklink and ) belief resolution to be matched during premise formation
	 * input: premise Task, premise belief target
	 * output: long[2] time interval
	 **/
	public TimeFocus timing;
	//shift heuristic condition probabalities TODO refine
	float PREMISE_SHIFT_EQUALS_ROOT = 0.02f;
	float PREMISE_SHIFT_CONTAINS_RECURSIVELY = 0.05f;
	float PREMISE_SHIFT_OTHER = 0.9f;
	float PREMISE_SHIFT_RANDOM = 0.5f;

	public Deriver(PremiseRuleSet rules) {
		this(rules, new NonEternalTaskOccurenceOrPresentDeriverTiming());
	}

	public Deriver(PremiseRuleSet rules, TimeFocus timing) {
		this(rules
				//standard derivation behaviors
				.add(TaskResolve.the)
				.add(MatchBelief.the)
				.add(QuestionAnswering.the)
				.add(new CompoundDecompose(true))
				.add(new AdjacentLinks(new AdjacentIndexer()))
				.add(new ImageUnfold())
				//TODO functor evaluator
				.compile()
//			.print()
			,
			timing);
	}

	public Deriver(DeriverProgram program, TimeFocus timing) {
		super();
		NAR nar = program.nar;
		this.program = program;
		this.timing = timing;

		nar.add(this);
	}

	@Override
	public final void next(What w, final BooleanSupplier kontinue) {

		if (((TaskLinkWhat) w).links.isEmpty()) return;

		Deriver.DeriverExecutor e = exe.get();
		Derivation d = Derivation.derivation.get().start(this, w, e);

		do {

            for (int i = 0; i < innerLoops; i++)
                e.next();

		} while (kontinue.getAsBoolean());

	}

	public final TaskLink hypothesize(Derivation d) {
		return ((TaskLinkWhat) d.what).links.sample(d.random);  //HACK
	}

	@Override
	public float value() {
		//TODO cache this between cycles
		double v = Util.sumDouble(Cause::pri, program.cause);
		//System.out.println(this + " " + v);
		return (float) v;
	}

	public abstract static class DeriverExecutor {

		protected Derivation d;

		public final void start(Derivation d) {
			this.d = d;
			starting();
		}

		/**
		 * gets next premise
		 */
		protected final Premise premise() {
			TaskLink x = d.deriver.hypothesize(d);

//			//Pre-resolve
//			if (x!=null) {
//				Task y = TaskResolve.the.get(x, d);
//				if (y != null) // && !x.equals(y))
//					return new AbstractPremise(y, x.to());
//			}

			return x;
		}

		/**
		 * run a premise
		 */
		protected final void run(Premise p, int ttl) {
			d.run(p, ttl).increment();
		}

		public abstract void next();

		protected abstract void starting();

		public abstract void add(Premise p);

        static protected float pri(Premise p) {
            Task t = p.task(), b = p.belief();
			float tPri = t.priElseZero();
            if (b != null)
                return
					Util.or(
					//Util.min(
					//Util.mean(
					//Util.sum(
						tPri, b.priElseZero()
					);
            else
                return tPri;
        }

    }

	private static final FloatFunction<Premise> sorter = DeriverExecutor::pri;

	private static class QueueDeriverExecutor extends DeriverExecutor {

//		int hypotheses = 1;
//		int capacity = 3;
        int iterationTTL = 7;



		final Queue<Premise> queue =
			new PrioritySet<>(sorter, new UnifiedSet<>()); //prevents duplicates (and caches pri calculation)
			//new CachedPriorityQueue<>(sorter); //caches pri calculation
			//new ArrayHashSet<>(capacity)

		@Override
		protected void starting() {
			queue.clear();
		}

		@Override
		public void next() {

			//queue.clear();

			int mainTTL = iterationTTL;
			int branchTTL = d.nar.deriveBranchTTL.intValue();

			//TODO scale ttl by the priority normalized relative to the other items in the queue
			do {

				//if (queue.size() < mainTTL - 1)
				if (queue.isEmpty())
					queue.offer(premise());


				Premise r;
				if ((r = queue.poll()) != null)
					run(r, branchTTL);

				//if (queue.size() < capacity)


			} while (--mainTTL > 0);

		}

		void hypothesize(int branchTTL) {
			Premise p = premise();
			if (p!=null) {
				run(p, branchTTL);
				//queue.add(p);
			}
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


		@Override
		protected void starting() {
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
			int cap = bag.capacity();

			int tries = 1 + (cap - bag.size()); //TODO penalize duplicates more
			do {
				Premise x = premise();
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

















































































































